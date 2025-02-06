import React from "react";
import "./css/ConfigurationContainer.css";

const ConfigurationContainer = ({projectUtil}) => {
    const handleGroupInputChange = (e) => {
        const newValue = e.target.value
        const isValid = /^[a-zA-Z]+(\.[a-zA-Z]+)*$/.test(newValue)
      
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
            <div id="top-config-div" className="topConfigDiv">
              <h2>Java version</h2>
              
              <label>
                  21<input type="radio" name="option" value="21" onChange={(e) => projectUtil.setJavaVersion(e.target.value)} checked={projectUtil.javaVersion === '21'} />
              </label>
              <label>
                  17<input type="radio" name="option" value="17" onChange={(e) => projectUtil.setJavaVersion(e.target.value)} checked={projectUtil.javaVersion === '17'}/>
              </label>
              <label>
                  11<input type="radio" name="option" value="11" onChange={(e) => projectUtil.setJavaVersion(e.target.value)} checked={projectUtil.javaVersion === '11'}/>
              </label>
            </div>

            <div>

            </div>

            <div id="bottom-config-div" className="bottomConfigDiv">
              <h2>Metadata</h2>

              <label>Project name:</label>
              <input value={projectUtil.projectName} spellCheck="false" onInput={handleNameInputChange} />

              <label>Project group:</label>
              <input value={projectUtil.projectGroup} spellCheck="false" onInput={handleGroupInputChange}/>
            </div>
        </div>
    )
}

export default ConfigurationContainer;