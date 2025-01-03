import React from "react";
import "./css/Toolbar.css";

const Toolbar = () => {
    return (
        <div id="toolbar" className="toolbar">
            {/* Left Section: Image and Website Name */}
            <div className="toolbar-left">
                <h1 className="toolbar-logo">Genesis</h1>
            </div>

            {/* Right Section: OS Selection */}
            <div className="toolbar-right">
                <label>Operating System:</label>
                <div id='spacer' className="spacer"></div>
                <select className="os-selector">
                    <option value="windows">Windows</option>
                    <option value="linux">Linux</option>
                    <option value="macos">MacOS</option>
                </select>
            </div>
        </div>
    );
};

export default Toolbar;
