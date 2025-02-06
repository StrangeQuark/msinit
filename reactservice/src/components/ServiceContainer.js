import React, { useState } from "react";
import "./css/ServiceContainer.css";

const ServiceContainer = ({ item, toggleItem }) => {
  return (
    <div
      id="service-container"
      className={`service-container ${item.isSelected ? "isSelected" : ""}`}
      onClick={toggleItem}
    >
      <h1 className={`container-text ${item.isSelected ? "container-text-selected" : ""}`}>{item.name}</h1>
    </div>
  );
};

export default ServiceContainer;
