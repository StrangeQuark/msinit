import React from "react";
import "./css/Main.css";
import ServiceContainer from "./ServiceContainer";
import ConfigurationContainer from "./ConfigurationContainer";

const Main = ({items, toggleItem, projectUtil}) => {
    return(
        <div id="main" className="main">
            <div id="leftDiv" className="left">
                <ConfigurationContainer projectUtil={projectUtil}/>
            </div>
            <div id="rightDiv" className="right">
                {
                    items.map((item, index) => (
                        <ServiceContainer key={index} item={item} toggleItem={() => toggleItem(index)} />
                    ))
                }
            </div>
        </div>
    )
}

export default Main;