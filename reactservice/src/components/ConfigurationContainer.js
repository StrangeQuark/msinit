import React from "react";
import "./css/ConfigurationContainer.css";

const ConfigurationContainer = ({projectUtil}) => {
    const handleGroupInputChange = (e) => {
        const newValue = e.target.value
        const isValid = /^[a-zA-Z0-9]+(\.[a-zA-Z0-9]+)*$/.test(newValue)
      
        if (isValid || newValue === '') {
          projectUtil.setProjectGroup(newValue)
        }
      }

      const handleNameInputChange = (e) => {
        const newValue = e.target.value
        const isValidFilename = /^[^<>:"/\\|?*]+$/.test(newValue); // Valid filename characters check
      
        if (isValidFilename || newValue === '') {
          projectUtil.setProjectName(newValue)
        }
      }
    
    return(
        <div id="configurationContainer" className="configurationContainer">
            <div></div>
            <div></div>
            <div id="bottom-config-div">
                <h2>Metadata</h2>

                <label>Project name:</label>
                <input value={projectUtil.projectName} onInput={handleNameInputChange} />

                <label>Project group:</label>
                <input value={projectUtil.projectGroup} onInput={handleGroupInputChange}/>
            </div>
        </div>
    )
}

export default ConfigurationContainer;