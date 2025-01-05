import React from "react";
import "./css/Main.css";
import ServiceContainer from "./ServiceContainer";
import ConfigurationContainer from "./ConfigurationContainer";

const Main = ({items}) => {
    return(
        <div id="main" className="main">
            <div id="leftDiv" className="left">
                <ConfigurationContainer />
            </div>
            <div id="rightDiv" className="right">
                {
                    items.map((item, index) => (
                        <ServiceContainer key={index} item={item} />
                    ))
                }
            </div>
        </div>
    )
}

export default Main;