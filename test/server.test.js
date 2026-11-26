import test from 'node:test'
import assert from 'node:assert/strict'
import { createPrivateKey, createPublicKey } from 'crypto'
import JSZip from 'jszip'
import { createEnvFile, createStackConfiguration, createTestEnvFile, readRepositoryFile, shouldRemoveCicdFile } from '../server.js'

function getEnvValue(content, name) {
    return content.match(new RegExp("^" + name + "=(.*)$", "m"))[1]
}

test("generates Jenkins integration settings and the matching Kubernetes token", () => {
    for(const vaultEnabled of [true, false]) {
        const repositories = [
            { repo: "vpnservice", branch: "main" },
            { repo: "kubernetesservice", branch: "main" }
        ]

        if(vaultEnabled)
            repositories.push({ repo: "vaultservice", branch: "main" })

        const stackConfiguration = createStackConfiguration(repositories, "jenkins")
        const jenkinsEnv = createEnvFile(
            "VPNSERVICE_INTEGRATION=false\nVAULTSERVICE_ENABLED=true\nKUBERNETES_CICD_TOKEN=change-me",
            "jenkinsservice",
            stackConfiguration
        )
        const kubernetesEnv = createEnvFile(
            "KUBERNETES_CICD_TOKEN=change-me",
            "kubernetesservice",
            stackConfiguration
        )

        assert.equal(getEnvValue(jenkinsEnv, "VPNSERVICE_INTEGRATION"), "true")
        assert.equal(getEnvValue(jenkinsEnv, "VAULTSERVICE_ENABLED"), String(vaultEnabled))
        assert.equal(
            getEnvValue(jenkinsEnv, "KUBERNETES_CICD_TOKEN"),
            getEnvValue(kubernetesEnv, "KUBERNETES_CICD_TOKEN")
        )
        assert.notEqual(getEnvValue(jenkinsEnv, "KUBERNETES_CICD_TOKEN"), "change-me")
    }
})

test("disables the Jenkins VPN job when VPN is not selected", () => {
    const stackConfiguration = createStackConfiguration([
        { repo: "authservice", branch: "main" }
    ], "jenkins")
    const jenkinsEnv = createEnvFile(
        "VPNSERVICE_INTEGRATION=true",
        "jenkinsservice",
        stackConfiguration
    )

    assert.equal(getEnvValue(jenkinsEnv, "VPNSERVICE_INTEGRATION"), "false")
})

test("preserves binary bytes when copying repository archives", async () => {
    const sourceZip = new JSZip()
    const outputZip = new JSZip()
    const bytes = Buffer.concat([
        Buffer.from([0x00, 0xff, 0xfe, 0x80]),
        Buffer.from("com.strangequark 21-alpine")
    ])
    const extensions = ["mp3", "webm", "png", "ico", "jar", "pyc", "bin"]

    for(const extension of extensions)
        sourceZip.file("testservice-main/sample." + extension, bytes)

    const sourceArchive = await JSZip.loadAsync(
        await sourceZip.generateAsync({ type: "nodebuffer" })
    )

    for(const extension of extensions) {
        const fileName = "testservice-main/sample." + extension
        const content = await readRepositoryFile(fileName, sourceArchive.file(fileName))

        assert.ok(Buffer.isBuffer(content))
        outputZip.file(fileName, content)
    }

    const outputArchive = await JSZip.loadAsync(
        await outputZip.generateAsync({ type: "nodebuffer" })
    )

    for(const extension of extensions) {
        const fileName = "testservice-main/sample." + extension

        assert.deepEqual(await outputArchive.file(fileName).async("nodebuffer"), bytes)
    }
})

test("reads source and configuration files as text", async () => {
    const zip = new JSZip()
    const text = "com.strangequark\nUnicode: café\n"
    const fileNames = [
        "service-main/src/main/java/com/strangequark/Example.java",
        "service-main/pom.xml",
        "service-main/.env.example",
        "service-main/.env.test",
        "service-main/Dockerfile",
        "service-main/Jenkinsfile",
        "service-main/mvnw",
        "service-main/.gitignore",
        "service-main/README.md",
        "service-main/.github/workflows/build.yml",
        "service-main/src/App.jsx",
        "service-main/public/logo.svg"
    ]

    for(const fileName of fileNames) {
        zip.file(fileName, text)

        assert.equal(await readRepositoryFile(fileName, zip.file(fileName)), text)
    }
})

test("generates matching values for selected services", () => {
    const stackConfiguration = createStackConfiguration([
        { repo: "authservice", branch: "main" },
        { repo: "emailservice", branch: "main" },
        { repo: "fileservice", branch: "main" },
        { repo: "vaultservice", branch: "main" },
        { repo: "testservice", branch: "main" }
    ])
    const authEnv = createEnvFile("AUTHSERVICE_INTEGRATION=true\nFILESERVICE_INTEGRATION=true\nEMAILSERVICE_INTEGRATION=true\nVAULTSERVICE_INTEGRATION=true\nSERVICE_SECRET_FILE=change-me\nJWT_PRIVATE_KEY=change-me\nJWT_PUBLIC_KEY=change-me\nPOSTGRES_USER=change-me\nPOSTGRES_PASSWORD=change-me\nENCRYPTION_KEY=change-me", "authservice", stackConfiguration)
    const fileEnv = createEnvFile("AUTHSERVICE_INTEGRATION=true\nTELEMETRYSERVICE_INTEGRATION=true\nSERVICE_SECRET_FILE=change-me\nJWT_PUBLIC_KEY=change-me\nPOSTGRES_USER=change-me\nPOSTGRES_PASSWORD=change-me\nFILE_STORAGE_ACCESS_KEY=change-me\nFILE_STORAGE_SECRET_KEY=change-me\nENCRYPTION_KEY=change-me", "fileservice", stackConfiguration)
    const testEnv = createEnvFile("EMAILSERVICE_INTEGRATION=true\nSERVICE_SECRET_TEST=change-me\nINITIAL_SUPER_USERNAME=change-me\nINITIAL_SUPER_PASSWORD=change-me", "testservice", stackConfiguration)
    const privateKey = createPrivateKey({
        key: Buffer.from(getEnvValue(authEnv, "JWT_PRIVATE_KEY"), "base64"),
        format: "der",
        type: "pkcs8"
    })
    const publicKey = createPublicKey(privateKey).export({ type: "spki", format: "der" }).toString("base64")

    assert.match(authEnv, /^# Generated by MSINIT\. Do not commit this file\.$/m)
    assert.equal(getEnvValue(authEnv, "FILESERVICE_INTEGRATION"), "true")
    assert.equal(getEnvValue(fileEnv, "AUTHSERVICE_INTEGRATION"), "true")
    assert.equal(getEnvValue(fileEnv, "TELEMETRYSERVICE_INTEGRATION"), "false")
    assert.equal(getEnvValue(authEnv, "SERVICE_SECRET_FILE"), getEnvValue(fileEnv, "SERVICE_SECRET_FILE"))
    assert.equal(publicKey, getEnvValue(fileEnv, "JWT_PUBLIC_KEY"))
    assert.notEqual(getEnvValue(authEnv, "POSTGRES_PASSWORD"), getEnvValue(fileEnv, "POSTGRES_PASSWORD"))
    assert.notEqual(getEnvValue(fileEnv, "FILE_STORAGE_ACCESS_KEY"), "change-me")
    assert.notEqual(getEnvValue(fileEnv, "FILE_STORAGE_SECRET_KEY"), "change-me")
    assert.match(getEnvValue(authEnv, "ENCRYPTION_KEY"), /^[A-F0-9]{32}$/)
    assert.equal(getEnvValue(testEnv, "SERVICE_SECRET_EMAIL"), stackConfiguration.serviceSecrets.email)
    assert.equal(getEnvValue(testEnv, "INITIAL_SUPER_USERNAME"), "test-super")
    assert.equal(getEnvValue(testEnv, "INITIAL_SUPER_PASSWORD"), "test-password")
})

test("disables absent integrations", () => {
    const stackConfiguration = createStackConfiguration([
        { repo: "fileservice", branch: "main" }
    ])
    const fileEnv = createEnvFile("AUTHSERVICE_INTEGRATION=true\nTELEMETRYSERVICE_INTEGRATION=true\nJWT_PUBLIC_KEY=change-me\nSERVICE_SECRET_FILE=change-me\nFILE_STORAGE_ACCESS_KEY=change-me\nFILE_STORAGE_SECRET_KEY=change-me", "fileservice", stackConfiguration)

    assert.equal(getEnvValue(fileEnv, "AUTHSERVICE_INTEGRATION"), "false")
    assert.equal(getEnvValue(fileEnv, "TELEMETRYSERVICE_INTEGRATION"), "false")
    assert.notEqual(getEnvValue(fileEnv, "JWT_PUBLIC_KEY"), "change-me")
    assert.notEqual(getEnvValue(fileEnv, "SERVICE_SECRET_FILE"), "change-me")
})

test("generates VPN and Kubernetes credentials", () => {
    const stackConfiguration = createStackConfiguration([
        { repo: "authservice", branch: "main" },
        { repo: "reactservice", branch: "main" },
        { repo: "vpnservice", branch: "main" },
        { repo: "kubernetesservice", branch: "main" }
    ])
    const vpnEnv = createEnvFile(
        "AUTHSERVICE_INTEGRATION=false\nREACTSERVICE_INTEGRATION=false\nJWT_PUBLIC_KEY=change-me\nPOSTGRES_USER=change-me\nPOSTGRES_PASSWORD=change-me\nENCRYPTION_KEY=change-me\nWIREGUARD_CONTROL_TOKEN=change-me",
        "vpnservice",
        stackConfiguration
    )
    const reactEnv = createEnvFile(
        "VITE_VPNSERVICE_INTEGRATION=false\nVITE_VPNROUTER_INTEGRATION=false",
        "reactservice",
        stackConfiguration
    )
    const kubernetesEnv = createEnvFile(
        "KUBERNETES_CICD_TOKEN=change-me",
        "kubernetesservice",
        stackConfiguration
    )

    assert.equal(getEnvValue(vpnEnv, "AUTHSERVICE_INTEGRATION"), "true")
    assert.equal(getEnvValue(vpnEnv, "REACTSERVICE_INTEGRATION"), "true")
    assert.equal(getEnvValue(vpnEnv, "JWT_PUBLIC_KEY"), stackConfiguration.jwtKeys.publicKey)
    assert.notEqual(getEnvValue(vpnEnv, "POSTGRES_USER"), "change-me")
    assert.notEqual(getEnvValue(vpnEnv, "POSTGRES_PASSWORD"), "change-me")
    assert.match(getEnvValue(vpnEnv, "ENCRYPTION_KEY"), /^[A-F0-9]{32}$/)
    assert.notEqual(getEnvValue(vpnEnv, "WIREGUARD_CONTROL_TOKEN"), "change-me")
    assert.equal(getEnvValue(reactEnv, "VITE_VPNSERVICE_INTEGRATION"), "true")
    assert.equal(getEnvValue(reactEnv, "VITE_VPNROUTER_INTEGRATION"), "true")
    assert.notEqual(getEnvValue(kubernetesEnv, "KUBERNETES_CICD_TOKEN"), "change-me")
})

test("generates matching scoped CI/CD tokens for Vault and Jenkins", () => {
    const repositories = [
        "authservice", "emailservice", "fileservice", "gatewayservice", "loggerservice",
        "reactservice", "telemetryservice", "testservice", "vaultservice", "vpnservice"
    ].map(repo => ({ repo, branch: "main" }))
    const tokenNames = [
        "AUTH_CICD_TOKEN", "EMAIL_CICD_TOKEN", "FILE_CICD_TOKEN", "GATEWAY_CICD_TOKEN",
        "LOGGER_CICD_TOKEN", "REACT_CICD_TOKEN", "TELEMETRY_CICD_TOKEN", "TEST_CICD_TOKEN", "VPN_CICD_TOKEN"
    ]
    const template = tokenNames.map(name => name + "=change-me").join("\n")
    const stackConfiguration = createStackConfiguration(repositories, "jenkins")
    const vaultEnv = createEnvFile(template, "vaultservice", stackConfiguration)
    const jenkinsEnv = createEnvFile(template, "jenkinsservice", stackConfiguration)
    const nextStackConfiguration = createStackConfiguration(repositories, "jenkins")
    const tokens = []

    for(const tokenName of tokenNames) {
        const token = getEnvValue(vaultEnv, tokenName)

        assert.match(token, /^[A-Za-z0-9_-]{43}$/)
        assert.equal(getEnvValue(jenkinsEnv, tokenName), token)
        assert.equal(stackConfiguration.cicdTokens[tokenName], token)
        assert.notEqual(nextStackConfiguration.cicdTokens[tokenName], token)
        tokens.push(token)
    }

    assert.equal(new Set(tokens).size, tokenNames.length)
})

test("generates CI/CD tokens only for selected services", () => {
    const stackConfiguration = createStackConfiguration([
        { repo: "vaultservice", branch: "main" },
        { repo: "testservice", branch: "main" }
    ], "jenkins")
    const vaultEnv = createEnvFile("TEST_CICD_TOKEN=change-me", "vaultservice", stackConfiguration)
    const jenkinsEnv = createEnvFile("TEST_CICD_TOKEN=change-me", "jenkinsservice", stackConfiguration)

    assert.deepEqual(Object.keys(stackConfiguration.cicdTokens), ["TEST_CICD_TOKEN"])
    assert.notEqual(getEnvValue(vaultEnv, "TEST_CICD_TOKEN"), "change-me")
    assert.equal(getEnvValue(jenkinsEnv, "TEST_CICD_TOKEN"), getEnvValue(vaultEnv, "TEST_CICD_TOKEN"))
})

test("adds the selected CICD repository once", () => {
    const stackConfiguration = createStackConfiguration([
        { repo: "authservice", branch: "develop" },
        { repo: "jenkinsservice", branch: "develop" }
    ], "jenkins")

    assert.equal(stackConfiguration.repositories.filter(repository => repository.repo === "jenkinsservice").length, 1)
})

test("does not add a GitHub Actions repository", () => {
    const stackConfiguration = createStackConfiguration([
        { repo: "authservice", branch: "develop" }
    ], "githubactions")

    assert.equal(stackConfiguration.repositories.some(repository => repository.repo === "githubactions"), false)
})

test("removes unselected CICD files", () => {
    const jenkinsFile = "authservice-main/Jenkinsfile"
    const githubActionsFile = "authservice-main/.github/workflows/build.yml"

    assert.equal(shouldRemoveCicdFile(jenkinsFile, "jenkins"), false)
    assert.equal(shouldRemoveCicdFile(githubActionsFile, "jenkins"), true)

    assert.equal(shouldRemoveCicdFile(jenkinsFile, "githubactions"), true)
    assert.equal(shouldRemoveCicdFile(githubActionsFile, "githubactions"), false)

    assert.equal(shouldRemoveCicdFile(jenkinsFile, "none"), true)
    assert.equal(shouldRemoveCicdFile(githubActionsFile, "none"), true)
})

test("generates test rate limits", () => {
    const stackConfiguration = createStackConfiguration([
        { repo: "authservice", branch: "main" },
        { repo: "gatewayservice", branch: "main" },
        { repo: "testservice", branch: "main" }
    ])
    const authEnv = createTestEnvFile("INVITE_ONLY=false\nCOOKIE_SECURE=true\nLOGIN_RATE_LIMIT_MAX_REQUESTS=10\nREGISTER_RATE_LIMIT_MAX_REQUESTS=2\nPASSWORD_RESET_RATE_LIMIT_MAX_REQUESTS=3\nSERVICE_ACCOUNT_RATE_LIMIT_MAX_REQUESTS=30\nINITIAL_SUPER_USERNAME=\nINITIAL_SUPER_PASSWORD=", "authservice", stackConfiguration)
    const gatewayEnv = createTestEnvFile("AUTH_GATEWAY_RATE_LIMIT_REPLENISH_RATE=5\nAUTH_GATEWAY_RATE_LIMIT_BURST_CAPACITY=60", "gatewayservice", stackConfiguration)

    assert.equal(getEnvValue(authEnv, "INVITE_ONLY"), "true")
    assert.equal(getEnvValue(authEnv, "COOKIE_SECURE"), "false")
    assert.equal(getEnvValue(authEnv, "REGISTER_RATE_LIMIT_MAX_REQUESTS"), "1000")
    assert.equal(getEnvValue(authEnv, "INITIAL_SUPER_USERNAME"), "test-super")
    assert.equal(getEnvValue(authEnv, "INITIAL_SUPER_PASSWORD"), "test-password")
    assert.equal(getEnvValue(gatewayEnv, "AUTH_GATEWAY_RATE_LIMIT_REPLENISH_RATE"), "1000")
    assert.equal(getEnvValue(gatewayEnv, "AUTH_GATEWAY_RATE_LIMIT_BURST_CAPACITY"), "1000")
})
