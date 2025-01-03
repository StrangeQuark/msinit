import React from "react";
import "./css/Footer.css";

const Footer = ({generateClick}) => {
    return(
        <div id="footer" className="footer">
            <div className="center">
                <button id="generateButton" className="generateButton" onClick={generateClick}>Generate</button>
            </div>
        </div>
    )
}

export default Footer;