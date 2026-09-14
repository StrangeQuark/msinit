import express from 'express'
import fetch from 'node-fetch'
import cors from 'cors'
import bodyParser from 'body-parser'
import JSZip from 'jszip'
import fs from 'fs/promises'
import { randomBytes, generateKeyPairSync } from 'crypto'
import { fileURLToPath } from 'url'

const app = express()

const integrationVariables = {
    authservice: ["AUTHSERVICE_INTEGRATION", "VITE_AUTHSERVICE_INTEGRATION"],
    emailservice: ["EMAILSERVICE_INTEGRATION", "VITE_EMAILSERVICE_INTEGRATION"],
    fileservice: ["FILESERVICE_INTEGRATION", "VITE_FILESERVICE_INTEGRATION"],
    gatewayservice: ["GATEWAYSERVICE_INTEGRATION", "VITE_GATEWAYSERVICE_INTEGRATION"],
    jenkinsservice: ["JENKINSSERVICE_INTEGRATION"],
    loggerservice: ["LOGGERSERVICE_INTEGRATION"],
    reactservice: ["REACTSERVICE_INTEGRATION", "VITE_REACTSERVICE_INTEGRATION"],
    telemetryservice: ["TELEMETRYSERVICE_INTEGRATION", "VITE_TELEMETRYSERVICE_INTEGRATION"],
    testservice: ["TESTSERVICE_INTEGRATION"],
    vaultservice: ["VAULTSERVICE_INTEGRATION", "VITE_VAULTSERVICE_INTEGRATION"]
}

const serviceAccountNames = {
    authservice: "auth",
    emailservice: "email",
    fileservice: "file",
    vaultservice: "vault",
    testservice: "test"
}

const vaultCicdTokens = {
    authservice: "AUTH_CICD_TOKEN",
    emailservice: "EMAIL_CICD_TOKEN",
    fileservice: "FILE_CICD_TOKEN",
    gatewayservice: "GATEWAY_CICD_TOKEN",
    loggerservice: "LOGGER_CICD_TOKEN",
    reactservice: "REACT_CICD_TOKEN",
    telemetryservice: "TELEMETRY_CICD_TOKEN"
}

// Middleware to parse JSON request bodies
app.use(bodyParser.json())
app.use(cors())

function randomToken() {
    return randomBytes(32).toString("base64url")
}

function randomEncryptionKey() {
    return randomBytes(16).toString("hex").toUpperCase()
}

function randomDatabaseUsername(serviceName) {
    return serviceName + "_" + randomBytes(4).toString("hex")
}

function randomOpenSearchPassword() {
    return "Aa1!" + randomBytes(24).toString("base64url")
}

function generateJwtKeys() {
    const keys = generateKeyPairSync("rsa", {
        modulusLength: 2048,
        publicKeyEncoding: { type: "spki", format: "der" },
        privateKeyEncoding: { type: "pkcs8", format: "der" }
    })

    return {
        privateKey: keys.privateKey.toString("base64"),
        publicKey: keys.publicKey.toString("base64")
    }
}

function addRepository(repositories, repo) {
    if(!repositories.some(repository => repository.repo === repo))
        repositories.push({ repo, branch: "main" })
}

export function createStackConfiguration(requestedRepositories, CICD) {
    const repositories = requestedRepositories.map(repository => ({ ...repository }))

    if(CICD === "jenkins")
        addRepository(repositories, "jenkinsservice")

    if(CICD === "githubactions")
        addRepository(repositories, "githubactions")

    const selectedServices = new Set(repositories.map(repository => repository.repo))
    const jwtKeys = generateJwtKeys()
    const serviceSecrets = {}
    const encryptionKeys = {}
    const databaseCredentials = {}
    const cicdTokens = {}

    for(const [serviceName, serviceAccountName] of Object.entries(serviceAccountNames)) {
        if(selectedServices.has(serviceName))
            serviceSecrets[serviceAccountName] = randomToken()
    }

    for(const serviceName of ["authservice", "emailservice", "fileservice", "telemetryservice", "vaultservice"]) {
        if(selectedServices.has(serviceName))
            encryptionKeys[serviceName] = randomEncryptionKey()
    }

    for(const serviceName of ["authservice", "emailservice", "fileservice", "vaultservice"]) {
        if(selectedServices.has(serviceName)) {
            databaseCredentials[serviceName] = {
                username: randomDatabaseUsername(serviceName),
                password: randomToken()
            }
        }
    }

    for(const [serviceName, tokenName] of Object.entries(vaultCicdTokens)) {
        if(selectedServices.has(serviceName))
            cicdTokens[tokenName] = randomToken()
    }

    return {
        repositories,
        selectedServices,
        jwtKeys,
        serviceSecrets,
        encryptionKeys,
        databaseCredentials,
        cicdTokens,
        bootstrapToken: randomToken(),
        authRedisPassword: randomToken(),
        gatewayRedisPassword: randomToken(),
        mongoRootUsername: randomDatabaseUsername("mongo_root"),
        mongoRootPassword: randomToken(),
        mongoAppUsername: randomDatabaseUsername("telemetry_app"),
        mongoAppPassword: randomToken(),
        mongoExpressUsername: randomDatabaseUsername("mongo_express"),
        mongoExpressPassword: randomToken(),
        openSearchPassword: randomOpenSearchPassword(),
        mailPassword: randomToken()
    }
}

function setEnvVariable(content, name, value) {
    const pattern = new RegExp("^" + name + "=.*$", "m")

    if(!pattern.test(content))
        return content

    return content.replace(pattern, () => name + "=" + value)
}

function addEnvVariable(content, name, value) {
    const pattern = new RegExp("^" + name + "=", "m")

    if(pattern.test(content))
        return setEnvVariable(content, name, value)

    return content.trimEnd() + "\n" + name + "=" + value + "\n"
}

function getServiceEnvValues(serviceName, stackConfiguration) {
    const envValues = {}

    for(const [integrationService, variableNames] of Object.entries(integrationVariables)) {
        for(const variableName of variableNames)
            envValues[variableName] = stackConfiguration.selectedServices.has(integrationService)
    }

    if(stackConfiguration.encryptionKeys[serviceName])
        envValues.ENCRYPTION_KEY = stackConfiguration.encryptionKeys[serviceName]

    if(serviceName === "authservice") {
        envValues.JWT_PRIVATE_KEY = stackConfiguration.jwtKeys.privateKey
        envValues.JWT_PUBLIC_KEY = stackConfiguration.jwtKeys.publicKey
        envValues.POSTGRES_USER = stackConfiguration.databaseCredentials.authservice.username
        envValues.POSTGRES_PASSWORD = stackConfiguration.databaseCredentials.authservice.password
        envValues.AUTH_RATE_LIMIT_REDIS_PASSWORD = stackConfiguration.authRedisPassword
        envValues.SERVICE_ACCOUNTS = Object.entries(serviceAccountNames)
            .filter(([integrationService]) => stackConfiguration.selectedServices.has(integrationService))
            .map(([, serviceAccountName]) => serviceAccountName)
            .join(",")

        for(const [serviceAccountName, secret] of Object.entries(stackConfiguration.serviceSecrets))
            envValues["SERVICE_SECRET_" + serviceAccountName.toUpperCase()] = secret
    }

    if(serviceName === "emailservice") {
        envValues.JWT_PUBLIC_KEY = stackConfiguration.jwtKeys.publicKey
        envValues.POSTGRES_USER = stackConfiguration.databaseCredentials.emailservice.username
        envValues.POSTGRES_PASSWORD = stackConfiguration.databaseCredentials.emailservice.password
        envValues.SPRING_MAIL_PASSWORD = stackConfiguration.mailPassword
        envValues.SERVICE_SECRET_EMAIL = stackConfiguration.serviceSecrets.email
    }

    if(serviceName === "fileservice") {
        envValues.JWT_PUBLIC_KEY = stackConfiguration.jwtKeys.publicKey
        envValues.POSTGRES_USER = stackConfiguration.databaseCredentials.fileservice.username
        envValues.POSTGRES_PASSWORD = stackConfiguration.databaseCredentials.fileservice.password
        envValues.SERVICE_SECRET_FILE = stackConfiguration.serviceSecrets.file
    }

    if(serviceName === "gatewayservice")
        envValues.REDIS_PASSWORD = stackConfiguration.gatewayRedisPassword

    if(serviceName === "loggerservice")
        envValues.OPENSEARCH_PASSWORD = stackConfiguration.openSearchPassword

    if(serviceName === "telemetryservice") {
        envValues.JWT_PUBLIC_KEY = stackConfiguration.jwtKeys.publicKey
        envValues.MONGO_ROOT_USERNAME = stackConfiguration.mongoRootUsername
        envValues.MONGO_ROOT_PASSWORD = stackConfiguration.mongoRootPassword
        envValues.MONGO_APP_USERNAME = stackConfiguration.mongoAppUsername
        envValues.MONGO_APP_PASSWORD = stackConfiguration.mongoAppPassword
        envValues.MONGO_EXPRESS_USERNAME = stackConfiguration.mongoExpressUsername
        envValues.MONGO_EXPRESS_PASSWORD = stackConfiguration.mongoExpressPassword
    }

    if(serviceName === "vaultservice") {
        envValues.JWT_PUBLIC_KEY = stackConfiguration.jwtKeys.publicKey
        envValues.POSTGRES_USER = stackConfiguration.databaseCredentials.vaultservice.username
        envValues.POSTGRES_PASSWORD = stackConfiguration.databaseCredentials.vaultservice.password
        envValues.BOOTSTRAP_TOKEN = stackConfiguration.bootstrapToken
        envValues.SERVICE_SECRET_VAULT = stackConfiguration.serviceSecrets.vault

        for(const [tokenName, token] of Object.entries(stackConfiguration.cicdTokens))
            envValues[tokenName] = token
    }

    if(serviceName === "testservice") {
        envValues.BOOTSTRAP_TOKEN = stackConfiguration.bootstrapToken
        envValues.SERVICE_SECRET_TEST = stackConfiguration.serviceSecrets.test
        envValues.SERVICE_SECRET_AUTH = stackConfiguration.serviceSecrets.auth
        envValues.SERVICE_SECRET_EMAIL = stackConfiguration.serviceSecrets.email

        const authserviceRepository = stackConfiguration.repositories.find(repository => repository.repo === "authservice")

        if(authserviceRepository) {
            const credentialsDirectory = "../" + authserviceRepository.repo + "-" + authserviceRepository.branch + "/bootstrap-credentials"
            envValues.INITIAL_SUPER_CREDENTIALS_DIRECTORY = credentialsDirectory
            envValues.INITIAL_SUPER_CREDENTIALS_FILE = credentialsDirectory + "/initial-super-user.txt"
        } else {
            envValues.INITIAL_SUPER_CREDENTIALS_DIRECTORY = "."
            envValues.INITIAL_SUPER_CREDENTIALS_FILE = "initial-super-user.txt"
        }
    }

    return envValues
}

export function createEnvFile(template, serviceName, stackConfiguration) {
    let envContent = "# Generated by MSINIT. Do not commit this file.\n" + template
    const envValues = getServiceEnvValues(serviceName, stackConfiguration)

    for(const [name, value] of Object.entries(envValues)) {
        if(value !== undefined)
            envContent = setEnvVariable(envContent, name, value)
    }

    if(serviceName === "testservice" && stackConfiguration.selectedServices.has("emailservice"))
        envContent = addEnvVariable(envContent, "SERVICE_SECRET_EMAIL", stackConfiguration.serviceSecrets.email)

    return envContent
}

export function createTestEnvFile(template, serviceName, stackConfiguration) {
    let envContent = createEnvFile(template, serviceName, stackConfiguration)

    if(serviceName === "authservice") {
        envContent = setEnvVariable(envContent, "INVITE_ONLY", true)
        envContent = setEnvVariable(envContent, "COOKIE_SECURE", false)
        envContent = setEnvVariable(envContent, "LOGIN_RATE_LIMIT_MAX_REQUESTS", 1000)
        envContent = setEnvVariable(envContent, "REGISTER_RATE_LIMIT_MAX_REQUESTS", 1000)
        envContent = setEnvVariable(envContent, "PASSWORD_RESET_RATE_LIMIT_MAX_REQUESTS", 1000)
        envContent = setEnvVariable(envContent, "SERVICE_ACCOUNT_RATE_LIMIT_MAX_REQUESTS", 1000)
    }

    if(serviceName === "gatewayservice") {
        envContent = setEnvVariable(envContent, "AUTH_GATEWAY_RATE_LIMIT_REPLENISH_RATE", 1000)
        envContent = setEnvVariable(envContent, "AUTH_GATEWAY_RATE_LIMIT_BURST_CAPACITY", 1000)
    }

    return envContent
}

function isEnvExample(fileName) {
    const pathParts = fileName.split("/")

    return pathParts.length === 2 && pathParts[1] === ".env.example"
}

// Fetch the selected repositories and build a configured archive
app.post('/batch-download', async (req, res) => {
    try {
        const requestedRepositories = req.body.repositories
        const projectGroup = req.body.projectGroup
        const javaVersion = req.body.javaVersion
        const OS = req.body.OS
        const CICD = req.body.CICD

        if (typeof projectGroup !== 'string' || !projectGroup.includes('.')) {
            return res.status(400).send('Invalid projectGroup format. Expected format like "com.example".')
        }

        const projectDomains = projectGroup.split(".")
        if (projectDomains.length < 2) {
            return res.status(400).send('projectGroup must have at least two segments separated by a dot.')
        }

        if (!Array.isArray(requestedRepositories) || requestedRepositories.length === 0) {
            return res.status(400).send('Invalid or empty repository list')
        }

        const stackConfiguration = createStackConfiguration(requestedRepositories, CICD)
        const zip = new JSZip()
        const envTemplates = []

        if (OS === "windows")
            zip.file("launch_script.bat", await fs.readFile("launch_scripts/launch_script.bat", "utf8"))
        else
            zip.file("launch_script.sh", await fs.readFile("launch_scripts/launch_script.sh", "utf8"))

        for (const { repo, branch } of stackConfiguration.repositories) {
            const url = `https://github.com/StrangeQuark/${repo}/archive/refs/heads/${branch}.zip`

            try {
                const response = await fetch(url)
                if (!response.ok) {
                    console.error(`Failed to fetch ${repo}: ${response.statusText}`)
                    continue
                }

                const blob = await response.arrayBuffer()
                const repoZip = await JSZip.loadAsync(blob)

                for (let fileName in repoZip.files) {
                    const file = repoZip.files[fileName]

                    if (/\.jar$/.test(fileName) || /\.png$/.test(fileName)) {
                        zip.file(fileName, await file.async('nodebuffer'))
                        continue
                    }

                    let pathParts = fileName.split("/")

                    const javaSourceRootIndex = pathParts.findIndex((segment, i) =>
                        segment === "src" && (pathParts[i + 1] === "main" || pathParts[i + 1] === "test") && pathParts[i + 2] === "java"
                    )

                    if (javaSourceRootIndex !== -1) {
                        const packageRootIndex = javaSourceRootIndex + 3

                        if (
                            pathParts[packageRootIndex] === "com" &&
                            pathParts[packageRootIndex + 1] === "strangequark"
                        ) {
                            pathParts[packageRootIndex] = projectDomains[0]
                            pathParts[packageRootIndex + 1] = projectDomains[1]
                        }
                    }

                    fileName = pathParts.join("/")

                    if (!file.dir) {
                        try {
                            let fileContent = await file.async("text")

                            fileContent = fileContent.replaceAll("com.strangequark", projectGroup)
                            fileContent = fileContent.replaceAll("21-alpine", javaVersion + "-alpine")
                            fileContent = fileContent.replaceAll("<java.version>21", "<java.version>" + javaVersion)

                            if (OS === "windows") {
                                fileContent = fileContent.replaceAll("\"start\": \"PORT=", "\"start\": \"set PORT=")
                                fileContent = fileContent.replaceAll("react-scripts start", "&& react-scripts start")
                            }

                            zip.file(fileName, fileContent)

                            if(isEnvExample(fileName))
                                envTemplates.push({ repo, fileName, fileContent })
                        } catch (error) {
                            console.error(`Error processing file ${fileName}: ${error.message}`)
                        }
                    }
                }
            } catch (error) {
                console.error(`Error fetching ${repo}: ${error.message}`)
            }
        }

        for(const envTemplate of envTemplates) {
            const envFileName = envTemplate.fileName.replace(".env.example", ".env")
            zip.file(envFileName, createEnvFile(envTemplate.fileContent, envTemplate.repo, stackConfiguration))

            if(stackConfiguration.selectedServices.has("testservice") && (envTemplate.repo === "authservice" || envTemplate.repo === "gatewayservice"))
                zip.file(envTemplate.fileName.replace(".env.example", ".env.test"), createTestEnvFile(envTemplate.fileContent, envTemplate.repo, stackConfiguration))
        }

        zip.generateAsync({ type: "nodebuffer" }).then((content) => {
            res.set({
                "Content-Type": "application/zip",
                "Content-Disposition": 'attachment filename="repositories_with_batch.zip"',
                "Cache-Control": "no-store"
            })
            res.send(content)
        }).catch((error) => {
            console.error('Error generating ZIP:', error)
            res.status(500).send('Failed to generate ZIP file.')
        })
    } catch (error) {
        console.error('Batch download error:', error)
        res.status(500).send("Internal Server Error")
    }
})

app.get('/health', (req, res) => {
    res.status(200).json({ status: 'UP' })
})

if(process.argv[1] === fileURLToPath(import.meta.url))
    app.listen(3000, () => console.log('Server running on http://localhost:3000'))

export default app
