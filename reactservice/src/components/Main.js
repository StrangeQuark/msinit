import React from "react";
import "./css/Main.css";
import ServiceContainer from "./ServiceContainer";

const Main = ({items}) => {
    return(
        <div id="main" className="main">
            {
                items.map((item, index) => (
                    <ServiceContainer key={index} item={item} />
                ))
            }
        </div>
    )
}

export default Main;