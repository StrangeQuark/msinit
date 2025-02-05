import React, { useState } from 'react';
import Main from '../components/Main';
import "./css/Home.css";
import Toolbar from '../components/Toolbar';
import Footer from '../components/Footer';

function Home() {
    const[projectName, setProjectName] = useState('genesis')
    const[projectGroup, setProjectGroup] = useState("com.strangequark")

    const projectUtil = {
        projectName,
        setProjectName,
        projectGroup,
        setProjectGroup
    }

    const authService = {
        "name": "Auth Service",
        "isSelected": false,
        "repo": "authservice",
        "branch": "main"
    }

    const emailService = {
        "name": "Email Service",
        "isSelected": false,
        "repo": "emailservice",
        "branch": "main"
    }

    const reactService = {
        "name": "React Service",
        "isSelected": false,
        "repo": "reactservice",
        "branch": "main"
    }

    const gatewayService = {
        "name": "Gateway Service",
        "isSelected": false,
        "repo": "gatewayservice",
        "branch": "main"
    }

    const testService = {
        "name": "Test Service",
        "isSelected": false,
        "repo": "testservice",
        "branch": "main"
    }

    const vaultService = {
        "name": "Vault Service",
        "isSelected": false,
        "repo": "vaultservice",
        "branch": "main"
    }

    const items = [authService, emailService, reactService]

    async function generate() {
        const repositories = []

        for(const item of items)
            if(item.isSelected)
                repositories.push({repo: item.repo, branch: item.branch})

        if(repositories.length === 0)
            return;
    
        const url = 'http://localhost:3000/batch-download'
    
        try {
            const response = await fetch(url, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ repositories, projectGroup })
            });
    
            if (!response.ok) {
                throw new Error(`Failed to download ZIP: ${response.statusText}`);
            }
    
            const blob = await response.blob()
            const anchor = document.createElement('a')
            anchor.href = URL.createObjectURL(blob)
            anchor.download = projectName + '.zip'
            anchor.click()
            URL.revokeObjectURL(anchor.href)
        } catch (error) {
            console.error('Error downloading ZIP:', error.message)
        }
    }    

    return(
        <div id='app' className='app'>
            <Toolbar />
            <Main items={items} projectUtil={projectUtil}/>
            <Footer generateClick={generate}/>
        </div>
    )
}

export default Home;