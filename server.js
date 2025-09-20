import express from 'express'
import fetch from 'node-fetch'
import cors from 'cors'
import bodyParser from 'body-parser'
import JSZip from 'jszip'
import fs from 'fs/promises'

const app = express()

// Middleware to parse JSON request bodies
app.use(bodyParser.json())
app.use(cors())

// --- Helper: process integration markers in a file ---
function processIntegrationMarkers(content, servicesToPrune) {
    let modifiedContent = content
    let shouldDeleteFile = false

    for (const service in servicesToPrune) {
        const { file: targetFile, line: targetLine, function_start, function_end } = servicesToPrune[service]

        if (modifiedContent.includes(targetFile)) {
            shouldDeleteFile = true
            break
        }

        let insideFunctionBlock = false
        let newContent = []

        for (let line of modifiedContent.split('\n')) {
            if (line.includes(function_start)) {
                insideFunctionBlock = true
                continue
            }

            if (insideFunctionBlock) {
                if (line.includes(function_end)) {
                    insideFunctionBlock = false
                }
                continue
            }

            if (line.includes(targetLine)) {
                continue
            }

            newContent.push(line)
        }

        modifiedContent = newContent.join('\n')
    }

    return { modifiedContent, shouldDeleteFile }
}

app.post('/batch-download', async (req, res) => {
    const repositories = req.body.repositories
    const projectGroup = req.body.projectGroup
    const javaVersion = req.body.javaVersion
    const OS = req.body.OS

    if (typeof projectGroup !== 'string' || !projectGroup.includes('.')) {
        return res.status(400).send('Invalid projectGroup format. Expected format like "com.example".')
    }

    const projectDomains = projectGroup.split(".")
    if (projectDomains.length < 2) {
        return res.status(400).send('projectGroup must have at least two segments separated by a dot.')
    }

    if (!Array.isArray(repositories) || repositories.length === 0) {
        return res.status(400).send('Invalid or empty repository list')
    }

    const zip = new JSZip()

    // Define services that are to be pruned from the repos
    const servicesToPrune = {
        "emailservice": {
            "file": "Integration file: Email",
            "line": "Integration line: Email",
            "function_start": "Integration function start: Email",
            "function_end": "Integration function end: Email"
        },
        "authservice": {
            "file": "Integration file: Auth",
            "line": "Integration line: Auth",
            "function_start": "Integration function start: Auth",
            "function_end": "Integration function end: Auth"
        },
        "reactservice": {
            "file": "Integration file: React",
            "line": "Integration line: React",
            "function_start": "Integration function start: React",
            "function_end": "Integration function end: React"
        },
        "testservice": {
            "file": "Integration file: Test",
            "line": "Integration line: Test",
            "function_start": "Integration function start: Test",
            "function_end": "Integration function end: Test"
        },
        "gatewayservice": {
            "file": "Integration file: Gateway",
            "line": "Integration line: Gateway",
            "function_start": "Integration function start: Gateway",
            "function_end": "Integration function end: Gateway"
        },
        "vaultservice": {
            "file": "Integration file: Vault",
            "line": "Integration line: Vault",
            "function_start": "Integration function start: Vault",
            "function_end": "Integration function end: Vault"
        },
        "fileservice": {
            "file": "Integration file: File",
            "line": "Integration line: File",
            "function_start": "Integration function start: File",
            "function_end": "Integration function end: File"
        }
    }

    // Prune out repos that were requested
    for (const { repo } of repositories) {
        delete servicesToPrune[repo]
    }

    // Add the launch script (bat/sh) with integration pruning
    if (OS === "windows") {
        let launchScript = await fs.readFile("launch_script.bat", "utf8")
        const { modifiedContent } = processIntegrationMarkers(launchScript, servicesToPrune)
        zip.file("launch_script.bat", modifiedContent)
    } else {
        let launchScript = await fs.readFile("launch_script.sh", "utf8")
        const { modifiedContent } = processIntegrationMarkers(launchScript, servicesToPrune)
        zip.file("launch_script.sh", modifiedContent)
    }

    for (const { repo, branch } of repositories) {
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

                        const { modifiedContent, shouldDeleteFile } = processIntegrationMarkers(fileContent, servicesToPrune)

                        if (!shouldDeleteFile) {
                            zip.file(fileName, modifiedContent)
                        }
                    } catch (error) {
                        console.error(`Error processing file ${fileName}: ${error.message}`)
                    }
                }
            }
        } catch (error) {
            console.error(`Error fetching ${repo}: ${error.message}`)
        }
    }

    zip.generateAsync({ type: "nodebuffer" }).then((content) => {
        res.set({
            "Content-Type": "application/zip",
            "Content-Disposition": 'attachment filename="repositories_with_batch.zip"'
        })
        res.send(content)
    }).catch((error) => {
        console.error('Error generating ZIP:', error)
        res.status(500).send('Failed to generate ZIP file.')
    })
})

app.listen(3000, () => console.log('Server running on http://localhost:3000'))
