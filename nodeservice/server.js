import express from 'express';
import fetch from 'node-fetch';
import cors from 'cors';
import bodyParser from 'body-parser';
import JSZip from 'jszip';
import fs from 'fs/promises';

const app = express();

// Middleware to parse JSON request bodies
app.use(bodyParser.json());
app.use(cors());

app.post('/batch-download', async (req, res) => {
    const repositories = req.body.repositories;

    if (!Array.isArray(repositories) || repositories.length === 0) {
        return res.status(400).send('Invalid or empty repository list');
    }

    const zip = new JSZip();

    // Define services with the same structure as the Python script
    const services = {
        "emailservice-main": {
            "file": "Integration file: Email",
            "line": "Integration line: Email",
            "function_start": "Integration function start: Email",
            "function_end": "Integration function end: Email"
        },
        "authservice-main": {
            "file": "Integration file: Auth",
            "line": "Integration line: Auth",
            "function_start": "Integration function start: Auth",
            "function_end": "Integration function end: Auth"
        },
        "reactservice-main": {
            "file": "Integration file: React",
            "line": "Integration line: React",
            "function_start": "Integration function start: React",
            "function_end": "Integration function end: React"
        },
        "testservice-main": {
            "file": "Integration file: Test",
            "line": "Integration line: Test",
            "function_start": "Integration function start: Test",
            "function_end": "Integration function end: Test"
        },
        "gatewayservice-main": {
            "file": "Integration file: Gateway",
            "line": "Integration line: Gateway",
            "function_start": "Integration function start: Gateway",
            "function_end": "Integration function end: Gateway"
        },
        "vaultservice-main": {
            "file": "Integration file: Vault",
            "line": "Integration line: Vault",
            "function_start": "Integration function start: Vault",
            "function_end": "Integration function end: Vault"
        }
    };

    for (const { repo, branch = 'main' } of repositories) {
        const url = `https://github.com/StrangeQuark/${repo}/archive/refs/heads/${branch}.zip`;

        try {
            // Fetch the ZIP file
            const response = await fetch(url);
            if (!response.ok) {
                console.error(`Failed to fetch ${repo}: ${response.statusText}`);
                continue;
            }

            const blob = await response.arrayBuffer();
            // Load the ZIP file
            const repoZip = await JSZip.loadAsync(blob);

            // Iterate through the files in the ZIP and process them
            for (const fileName in repoZip.files) {
            
                const file = repoZip.files[fileName];

                if(/\.jar$/.test(fileName) || /\.png$/.test(fileName)) {
                    zip.file(fileName, file.async('nodebuffer'));
                    continue;
                }

                if (!file.dir) { // Skip directories
                    try {
                        const fileContent = await file.async("text");

                        // Apply modifications based on the service configuration
                        for (const service in services) {
                            const { file: targetFile, line: targetLine, function_start, function_end } = services[service];

                            // Check if the file contains the target file string
                            if (fileContent.includes(targetFile)) {
                                console.log(`Deleting file: ${fileName}`);
                                // Simulate file deletion by skipping the addition of this file to the ZIP
                                continue;
                            }

                            // Remove lines containing the target line string and function blocks
                            let modifiedContent = fileContent.split('\n').filter(line => {
                                let insideFunctionBlock = false;

                                // Remove lines within the function block
                                if (line.includes(function_start)) {
                                    insideFunctionBlock = true;
                                    console.log(`Removing block starting with '${function_start}' in file: ${fileName}`);
                                    return false; // Skip the start of the block
                                }

                                if (line.includes(function_end)) {
                                    insideFunctionBlock = false;
                                    console.log(`Removing block ending with '${function_end}' in file: ${fileName}`);
                                    return false; // Skip the end of the block
                                }

                                // Skip lines containing the target line string or inside function blocks
                                if (insideFunctionBlock || line.includes(targetLine)) {
                                    if (line.includes(targetLine)) {
                                        console.log(`Removing line containing '${targetLine}' in file: ${fileName}`);
                                    }
                                    return false; // Skip this line
                                }

                                // Keep other lines
                                return true;
                            }).join('\n');

                            // If content was modified, update the file content in the ZIP
                            if (fileContent !== modifiedContent) {
                                zip.file(fileName, modifiedContent);
                            }
                            else 
                                zip.file(fileName, fileContent)
                        }

                    } catch (error) {
                        console.error(`Error processing file ${fileName}: ${error.message}`);
                    }
                }
            }
        } catch (error) {
            console.error(`Error fetching ${repo}: ${error.message}`);
        }
    }

    // Generate the combined ZIP and send it to the client
    zip.generateAsync({ type: "nodebuffer" }).then((content) => {
        res.set({
            "Content-Type": "application/zip",
            "Content-Disposition": 'attachment; filename="repositories_with_batch.zip"'
        });
        res.send(content);
    });
});

app.listen(3000, () => console.log('Server running on http://localhost:3000'));
