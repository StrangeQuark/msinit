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
            // Add the ZIP file to the archive
            zip.file(`${repo}-${branch}.zip`, blob);
        } catch (error) {
            console.error(`Error fetching ${repo}: ${error.message}`);
        }
    }

    try {
        // Read the batch files and add them to the ZIP
        const pythonScript = await fs.readFile('./scripts/common/script.py', 'utf-8');
        const dockerLaunch = await fs.readFile('./scripts/windows/docker_launch.bat', 'utf-8');
        const extractSetupCleanup = await fs.readFile('./scripts/windows/extract_setup_cleanup.bat', 'utf-8');
        
        zip.file('script.py', pythonScript);
        zip.file('docker_launch.bat', dockerLaunch);
        zip.file('extract_setup_cleanup.bat', extractSetupCleanup);
    } catch (error) {
        console.error(`Error reading batch files: ${error.message}`);
        return res.status(500).send('Failed to read batch files');
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
