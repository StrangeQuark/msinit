import React from "react";
import "./css/ConfigurationContainer.css";

const ConfigurationContainer = ({projectUtil}) => {
    return(
        <div id="configurationContainer" className="configurationContainer">
            <div></div>
            <div></div>
            <div id="bottom-config-div">
                <h2>Metadata</h2>

                <label>Project name:</label>
                <input value={projectUtil.projectName} onChange={(e) => projectUtil.setProjectName(e.target.value)} />

                <label>Project group:</label>
                <input value={projectUtil.projectGroup} onChange={(e) => projectUtil.setProjectGroup(e.target.value)}/>
            </div>
        </div>
    )
}

export default ConfigurationContainer;