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

app.post('/batch-download', async (req, res) => {
    const repositories = req.body.repositories
    const projectGroup = req.body.projectGroup
    const projectDomains = projectGroup.split(".")
    const javaVersion = req.body.javaVersion

    if (!Array.isArray(repositories) || repositories.length === 0) {
        return res.status(400).send('Invalid or empty repository list')
    }

    const zip = new JSZip()

    //Add the launch script
    const launchScript = await fs.readFile("launch_script.py", "utf8")
    zip.file("launch_script.py", launchScript)


    // Define services with the same structure as the Python script
    const services = {
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
        }
    }

    for (const { repo, branch } of repositories) {
        delete services[repo]
    }

    for (const { repo, branch } of repositories) {
        const url = `https://github.com/StrangeQuark/${repo}/archive/refs/heads/${branch}.zip`

        try {
            // Fetch the ZIP file
            const response = await fetch(url)
            if (!response.ok) {
                console.error(`Failed to fetch ${repo}: ${response.statusText}`)
                continue
            }

            const blob = await response.arrayBuffer()
            // Load the ZIP file
            const repoZip = await JSZip.loadAsync(blob)

            // Iterate through the files in the ZIP and process them
            for (let fileName in repoZip.files) {
                const file = repoZip.files[fileName]
            
                if (/\.jar$/.test(fileName) || /\.png$/.test(fileName)) {
                    zip.file(fileName, file.async('nodebuffer'))
                    continue
                }

                let pathParts = fileName.split("/");
            
                for (let i = 0; i < pathParts.length; i++) {
                    if (pathParts[i] === "com") {
                        pathParts[i] = projectDomains[0];
                    } else if (pathParts[i] === "strangequark") {
                        pathParts[i] = projectDomains[1];
                    }
                }
            
                fileName = pathParts.join("/"); // Reconstruct the path                
            
                if (!file.dir) { // Skip directories
                    try {
                        const fileContent = await file.async("text")
            
                        let shouldDeleteFile = false
                        let modifiedContent = fileContent.replace("com.strangequark", projectGroup)
                        modifiedContent = modifiedContent.replace("21-alpine", javaVersion + "-alpine")
                        modifiedContent = modifiedContent.replace("<java.version>21", "<java.version>" + javaVersion)
            
                        for (const service in services) {
                            const { file: targetFile, line: targetLine, function_start, function_end } = services[service]
            
                            if (modifiedContent.includes(targetFile)) {
                                shouldDeleteFile = true
                                break // No need to process further if we're deleting it
                            }
            
                            let insideFunctionBlock = false // Reset this for each service check
                            let newContent = []
            
                            for (let line of modifiedContent.split('\n')) {
                                if (line.includes(function_start)) {
                                    insideFunctionBlock = true
                                    continue // Skip this line
                                }
            
                                if (insideFunctionBlock) {
                                    if (line.includes(function_end)) {
                                        insideFunctionBlock = false
                                    }
                                    continue // Skip lines inside the block
                                }
            
                                if (line.includes(targetLine)) {
                                    continue // Skip this line
                                }
            
                                newContent.push(line)
                            }
            
                            modifiedContent = newContent.join('\n')
                        }
            
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

    // Generate the combined ZIP and send it to the client
    zip.generateAsync({ type: "nodebuffer" }).then((content) => {
        res.set({
            "Content-Type": "application/zip",
            "Content-Disposition": 'attachment filename="repositories_with_batch.zip"'
        })
        res.send(content)
    })
})

app.listen(3000, () => console.log('Server running on http://localhost:3000'))
