import React from "react";
import "./css/ConfigurationContainer.css";

const ConfigurationContainer = () => {
    return(
        <div id="configurationContainer" className="configurationContainer">
            <div></div>
            <div></div>
            <div id="bottom-config-div">
                <h2>Metadata</h2>

                <label>Project name:</label>
                <input placeholder="genesis"></input>

                <label>Service group:</label>
                <input placeholder="com.example"/>
            </div>
        </div>
    )
}

export default ConfigurationContainer;