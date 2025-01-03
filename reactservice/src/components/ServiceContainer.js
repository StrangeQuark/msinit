import React, { useState } from "react";
import "./css/ServiceContainer.css";

const ServiceContainer = ({ item }) => {
  const [isSelected, setIsSelected] = useState(false);

  function toggleStyle() {
    setIsSelected(!isSelected);
    item.isSelected = !isSelected; // Assuming `item` is a prop to store this state.
  }

  return (
    <div
      id="service-container"
      className={`service-container ${isSelected ? "isSelected" : ""}`}
      onClick={toggleStyle}
    >
      <h1 className={`container-text ${isSelected ? "container-text-selected" : ""}`}>{item.name}</h1>
      {/* <h3 className={`container-text h3 ${isSelected ? "container-text-selected" : ""}`}>{item.description}</h3> */}
    </div>
  );
};

export default ServiceContainer;
