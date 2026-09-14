

import { useEffect, useState, useRef } from "react"
import "./css/VaultList.css"
import { AUTHSERVICE_INTEGRATION, VAULT_ENDPOINTS, getAuthHeaders } from "../../config"
import { useAuth } from "../../context/AuthContext"
import InputPopup from "../InputPopup"
import { FaEye, FaTrash, FaRegClipboard, FaFileUpload, FaFileDownload, FaCog } from "react-icons/fa";
import UserManagementPopup from "../authservice/UserManagementPopup"

const VaultList = () => {
    const { getAccessToken } = useAuth()

    const [services, setServices] = useState([])
    const [environments, setEnvironments] = useState([])
    const [variables, setVariables] = useState([]) // all variables from vaultservice
    const [filteredVariables, setFilteredVariables] = useState([]) // filtered via search
    const [selectedService, setSelectedService] = useState("")
    const [selectedEnvironment, setSelectedEnvironment] = useState("")
    const [searchTerm, setSearchTerm] = useState("")
    const [maskAll, setMaskAll] = useState(true)
    const [changesMade, setChangesMade] = useState(false)
    const [popupType, setPopupType] = useState(null)
    const [displayPopout, setDisplayPopout] = useState(false)
    const [currentUserRole, setCurrentUserRole] = useState(AUTHSERVICE_INTEGRATION ? null : "OWNER")
    const fileInputRef = useRef(null)
    const selectedVault = useRef({ service: "", environment: "" })

    // Pagination
    const [currentPage, setCurrentPage] = useState(1)
    const varsPerPage = 10

    useEffect(() => {
        fetchServices()
    }, [])

    useEffect(() => {
        if (searchTerm.trim() === "") {
            setFilteredVariables(variables)
        } else {
            setFilteredVariables(variables.filter(v =>
                v.key.toLowerCase().includes(searchTerm.toLowerCase())
            ))
        }
    }, [searchTerm, variables])

    useEffect(() => {
        setCurrentPage(1)
    }, [searchTerm])

    const isSelectedVault = (service, environment) => {
        return selectedVault.current.service === service &&
            selectedVault.current.environment === environment
    }

    const fetchServices = async () => {
        const response = await fetch(`${VAULT_ENDPOINTS.GET_ALL_SERVICES}`, {
            headers: { ...getAuthHeaders(getAccessToken()) }
        })
        const data = await response.json()
        if(response.ok)
            setServices(data)
    }

    const fetchEnvironments = async (service) => {
        const response = await fetch(VAULT_ENDPOINTS.GET_ALL_ENVS_BY_SERVICE, {
            method: "POST",
            headers: { ...getAuthHeaders(getAccessToken()), "Content-Type": "application/json" },
            body: JSON.stringify({ serviceName: service })
        })
        const data = await response.json()
        if(response.ok)
            setEnvironments(data)
    }

    const fetchVariables = async (service, environment) => {
        try {
            const response = await fetch(VAULT_ENDPOINTS.GET_VARS_BY_ENV, {
                method: "POST",
                headers: { ...getAuthHeaders(getAccessToken()), "Content-Type": "application/json" },
                body: JSON.stringify({ serviceName: service, environmentName: environment })
            })

            if (!response.ok) {
                const message = await response.json()
                throw new Error(message.errorMessage || "Failed to fetch variables")
            }

            const data = await response.json()

            if(!isSelectedVault(service, environment))
                return

            // mask all initially
            const masked = data.map(v => ({ ...v, masked: true }))

            setVariables(masked)

            // preserve searchTerm when refetching
            setFilteredVariables(prev =>
                searchTerm.trim() === ""
                    ? masked
                    : masked.filter(v => v.key.toLowerCase().includes(searchTerm.toLowerCase()))
            )

            // reset maskAll if you want
            setMaskAll(true)

            // adjust pagination if currentPage is now out of range
            const newTotalPages = Math.ceil(masked.length / varsPerPage)
            if (currentPage > newTotalPages) {
                setCurrentPage(newTotalPages === 0 ? 1 : newTotalPages)
            }
        } catch (err) {
            console.error("fetchVariables error:", err)
            if(isSelectedVault(service, environment))
                alert(err.message)
        }
    }

    const loadUsers = async () => {
        const response = await fetch(VAULT_ENDPOINTS.GET_USERS_BY_SERVICE, {
            method: "POST",
            headers: { ...getAuthHeaders(getAccessToken()), "Content-Type": "application/json" },
            body: JSON.stringify({ serviceName: selectedService })
        })

        const data = await response.json()

        return data
    }

    const getCurrentUserRole = async (service) => {
        if(!AUTHSERVICE_INTEGRATION)
            return

        const response = await fetch(VAULT_ENDPOINTS.GET_CURRENT_USER_ROLE, {
            method: "POST",
            headers: { ...getAuthHeaders(getAccessToken()), "Content-Type": "application/json" },
            body: JSON.stringify({ serviceName: service })
        })

        const data = await response.json()

        setCurrentUserRole(data)
    }

    const getAllRoles = async () => {
        const response = await fetch(`${VAULT_ENDPOINTS.GET_ALL_ROLES}`, {
            headers: { ...getAuthHeaders(getAccessToken()) }
        })

        const data = await response.json()

        return data
    }

    const updateUserRole = async (username, newRole) => {
        const request = {
            serviceName: selectedService,
            username: username,
            role: newRole
        }

        const response = await fetch(`${VAULT_ENDPOINTS.UPDATE_USER_ROLE}`, {
            method: "POST",
            headers: { 
                ...getAuthHeaders(getAccessToken()),
                "Content-Type": "application/json",
             },
            body: JSON.stringify(request)
        })

        if(!response.ok) {
            const data = await response.json()
            alert(data.errorMessage)
            return false
        }

        return true
    }

    const addUser = async (user) => {
        const request = {
            serviceName: selectedService,
            username: user.username,
            role: "MAINTAINER"
        }

        const response = await fetch(`${VAULT_ENDPOINTS.ADD_USER_TO_SERVICE}`, {
            method: "POST",
            headers: { 
                ...getAuthHeaders(getAccessToken()),
                "Content-Type": "application/json",
             },
            body: JSON.stringify(request)
        })

        if(!response.ok) {
            const data = await response.json()
            alert(data.errorMessage)
            return false
        }

        loadUsers()
        return true
    }

    const deleteUser = async (user) => {
        const request = {
            serviceName: selectedService,
            username: user.username
        }

        const response = await fetch(`${VAULT_ENDPOINTS.DELETE_USER_FROM_SERVICE}`, {
            method: "POST",
            headers: { 
                ...getAuthHeaders(getAccessToken()),
                "Content-Type": "application/json",
             },
            body: JSON.stringify(request)
        })

        if(!response.ok) {
            const data = await response.json()
            alert(data.errorMessage)
            return false
        }

        loadUsers()
        return true
    }

    const createService = async (serviceName) => {
        const response = await fetch(VAULT_ENDPOINTS.CREATE_SERVICE, {
            method: "POST",
            headers: { ...getAuthHeaders(getAccessToken()), "Content-Type": "application/json" },
            body: JSON.stringify({ serviceName })
        })

        if(!response.ok) {
            const message = await response.json()
            alert(message.errorMessage)
            return false
        }

        fetchServices()
        return true
    }

    const createEnvironment = async (environmentName) => {
        const response = await fetch(VAULT_ENDPOINTS.CREATE_ENVIRONMENT, {
            method: "POST",
            headers: { ...getAuthHeaders(getAccessToken()), "Content-Type": "application/json" },
            body: JSON.stringify({ serviceName: selectedService, environmentName })
        })

        if(!response.ok) {
            const message = await response.json()
            alert(message.errorMessage)
            return false
        }

        fetchEnvironments(selectedService)
        return true
    }

     const addVariable = async (key, value) => {
        const service = selectedService
        const environment = selectedEnvironment
        let v = {key, value}

        const response = await fetch(VAULT_ENDPOINTS.ADD_VAR, {
            method: "POST",
            headers: { 
                "Content-Type": "application/json",
                ...getAuthHeaders(getAccessToken())
            },
            body: JSON.stringify({ serviceName: service, environmentName: environment, variable: v })
        })

        if(!response.ok) {
            const message = await response.json()
            if(isSelectedVault(service, environment))
                alert(message.errorMessage)
            return false
        }

        if(!isSelectedVault(service, environment))
            return true

        setVariables(prevVars => {
            const updated = [...prevVars, { ...v, masked: true }]
            const newTotalPages = Math.ceil(updated.length / varsPerPage)

            // If adding created a new last page, jump there
            if (newTotalPages > Math.ceil(prevVars.length / varsPerPage)) {
                setCurrentPage(newTotalPages)
            }

            return updated
        })

        return true
    }

    const deleteVariable = async (variable) => {
        if(!confirm("Are you sure you want to delete variable: " + variable))
            return

        const service = selectedService
        const environment = selectedEnvironment

        const response = await fetch(VAULT_ENDPOINTS.DELETE_VAR, {
            method: "DELETE",
            headers: { ...getAuthHeaders(getAccessToken()), "Content-Type": "application/json" },
            body: JSON.stringify({ serviceName: service, environmentName: environment, variableName: variable })
        })

        if(!response.ok) {
            const message = await response.json()
            if(isSelectedVault(service, environment))
                alert(message.errorMessage)
            return
        }
        
        if(!isSelectedVault(service, environment))
            return

        setVariables(prevVars => {
            const updated = prevVars.filter(v => v.key !== variable)
            const newTotalPages = Math.ceil(updated.length / varsPerPage)

            // If current page is now invalid, move back
            if (currentPage > newTotalPages) {
                setCurrentPage(newTotalPages === 0 ? 1 : newTotalPages)
            }

            return updated
        })
    }

    const deleteEnvironment = async () => {
        if(!confirm("Are you sure you want to delete environment: " + selectedEnvironment))
            return

        const response = await fetch(VAULT_ENDPOINTS.DELETE_ENVIRONMENT, {
            method: "DELETE",
            headers: { ...getAuthHeaders(getAccessToken()), "Content-Type": "application/json" },
            body: JSON.stringify({ serviceName: selectedService, environmentName: selectedEnvironment })
        })

        if(!response.ok) {
            const message = await response.json()
            alert(message.errorMessage)
            return
        }

        fetchEnvironments(selectedService)
        setSelectedEnvironment("")
    }

    const deleteService = async () => {
        if(!confirm("Are you sure you want to delete service: " + selectedService))
            return

        const response = await fetch(VAULT_ENDPOINTS.DELETE_SERVICE, {
            method: "DELETE",
            headers: { ...getAuthHeaders(getAccessToken()), "Content-Type": "application/json" },
            body: JSON.stringify({ serviceName: selectedService })
        })

        if(!response.ok) {
            const message = await response.json()
            alert(message.errorMessage)
            return
        }

        fetchServices()
        setEnvironments([])
        setSelectedService("")
        setSelectedEnvironment("")
    }

    const handleSave = async () => {
        const service = selectedService
        const environment = selectedEnvironment

        const response = await fetch(VAULT_ENDPOINTS.UPDATE_VARS, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                ...getAuthHeaders(getAccessToken())
            },
            body: JSON.stringify({ serviceName: service, environmentName: environment, variables })
        })

        if(!response.ok) {
            const message = await response.json()
            if(isSelectedVault(service, environment))
                alert(message.errorMessage)
            return
        }

        const data = await response.text()
        if(isSelectedVault(service, environment) &&
            data !== "All variables updated successfully") {
            alert(data)
        }

        if(isSelectedVault(service, environment))
            setChangesMade(false)
    }

    const uploadEnvFile = async (event) => {
        const file = event.target.files[0]
        if (!file) 
            return

        const service = selectedService
        const environment = selectedEnvironment

        const formData = new FormData()
        formData.append("file", file)
        formData.append("serviceName", service)
        formData.append("environmentName", environment)

        try {
            const response = await fetch(VAULT_ENDPOINTS.ADD_ENV_FILE, {
                method: "POST",
                body: formData,
                headers: { ...getAuthHeaders(getAccessToken()) }
            })

            if(!response.ok) {
                const message = await response.json()
                if(isSelectedVault(service, environment))
                    alert(message.errorMessage)
                return
            }
            
            if(isSelectedVault(service, environment))
                fetchVariables(service, environment)
        } catch (error) {
            console.error("Upload failed", error)
        }
    }

    const openFilePicker = () => {
        fileInputRef.current.click()
    }

    const downloadEnvFile = async () => {
        try {
            const res = await fetch(VAULT_ENDPOINTS.DOWNLOAD_ENV_FILE, {
                    method: "POST",
                    headers: { ...getAuthHeaders(getAccessToken()), "Content-Type": "application/json" },
                    body: JSON.stringify({ serviceName: selectedService, environmentName: selectedEnvironment })
                })

            if (!res.ok) 
                throw new Error("Failed to download file")

            const blob = await res.blob()
            const url = window.URL.createObjectURL(blob)
            const a = document.createElement("a")
            a.href = url
            a.download = `${selectedService}.${selectedEnvironment}.env`
            document.body.appendChild(a)
            a.click()
            a.remove()
            window.URL.revokeObjectURL(url)
        } catch (error) {
            console.error(error)
        }
    }

    const toggleMaskAll = () => {
        const newMaskState = !maskAll
        setVariables(vars => vars.map(v => ({ ...v, masked: newMaskState })))
        setMaskAll(newMaskState)
    }

    const toggleMaskOne = (index) => {
        setVariables(vars => vars.map((v, i) => i === index ? { ...v, masked: !v.masked } : v))
    }

    const copyValue = (value) => {
        navigator.clipboard.writeText(value)
    }

    const handleChangeVar = (index, field, value) => {
        setVariables(vars => vars.map((v, i) => i === index ? { ...v, [field]: value } : v))
        setChangesMade(true)
    }

    // Pagination logic
    const indexOfLastVar = currentPage * varsPerPage
    const indexOfFirstVar = indexOfLastVar - varsPerPage
    const currentVars = filteredVariables.slice(indexOfFirstVar, indexOfLastVar)
    const totalPages = Math.ceil(filteredVariables.length / varsPerPage)

    return (
        <div className="vault-page">
            <div className="vault-header">
                <button className="add-btn" onClick={() => setPopupType("create-service")}>Create service</button>
                <select
                    id="service-select"
                    value={selectedService}
                    onChange={(e) => {
                        if(e.target.value === "")
                            return

                        selectedVault.current = { service: e.target.value, environment: "" }
                        setSelectedService(e.target.value)
                        getCurrentUserRole(e.target.value)
                        setSelectedEnvironment("")
                        setEnvironments([])
                        setVariables([])
                        setChangesMade(false)
                        fetchEnvironments(e.target.value)
                    }}
                >
                    <option value="">Select Service</option>
                    {services.map((svc, idx) => (
                        <option key={idx} value={svc}>{svc}</option>
                    ))}
                </select>

                <select
                    id="environment-select"
                    value={selectedEnvironment}
                    onChange={(e) => {
                        if(e.target.value === "")
                            return

                        selectedVault.current = { service: selectedService, environment: e.target.value }
                        setSelectedEnvironment(e.target.value)
                        setVariables([])
                        setChangesMade(false)
                        fetchVariables(selectedService, e.target.value)
                    }}
                >
                    <option value="">Select Environment</option>
                    {environments.map((env, idx) => (
                        <option key={idx} value={env}>{env}</option>
                    ))}
                </select>

                {(currentUserRole === "OWNER" || currentUserRole === "MANAGER") && (
                    <button className="add-btn" onClick={() => setPopupType("create-environment")}>Create environment</button>
                )}

                {selectedService && selectedEnvironment && (
                    <>
                        <button className="add-btn" onClick={() => setPopupType("add-variable")}>Add Var</button>
                        <FaFileUpload id="env-file-upload" onClick={() => openFilePicker()}/>
                        <FaFileDownload id="env-file-download" onClick={() => downloadEnvFile()}/>
                        <input type="file" ref={fileInputRef} onChange={uploadEnvFile} className="hidden-input" />
                    </>
                )}

                {changesMade && (
                    <button className="save-btn" onClick={handleSave}>Save</button>
                )}

                {(currentUserRole === "OWNER" || currentUserRole === "MANAGER") && (
                    <div className="cog-wrapper">
                        {selectedService && (
                            <FaCog data-testid="cog-icon" onClick={() => setDisplayPopout(!displayPopout)}/>
                        )}

                        {displayPopout && (
                            <div id="vault-popout-container" className="vault-popout-container">
                            {AUTHSERVICE_INTEGRATION && <button onClick={() => {
                                    setPopupType("user-management") 
                                    setDisplayPopout(false) 
                                }}>
                                Manage Users
                            </button>
                            }
                            {currentUserRole === "OWNER" && (
                                <button onClick={() => {
                                    deleteService()
                                    setDisplayPopout(false)
                                }}>
                                Delete Service
                                </button>
                            )}
                            {selectedEnvironment && (
                                <button onClick={() => {
                                    deleteEnvironment()
                                    setDisplayPopout(false)
                                }}>
                                Delete Environment
                                </button>
                            )}
                            </div>
                        )}
                    </div>
                )}
            </div>

            {selectedService && selectedEnvironment && (
                <>
                    <div className="vault-controls">
                        <button onClick={toggleMaskAll}>{maskAll ? "Unmask All" : "Mask All"}</button>
                        <input
                            type="text"
                            placeholder="Search variables..."
                            value={searchTerm}
                            onChange={(e) => setSearchTerm(e.target.value)}
                            autoComplete="off"
                            name="search-input"
                        />
                    </div>

                    <div className="vault-table">
                        {currentVars.map((v, index) => (
                            <div key={index} className="vault-row">
                                <input
                                    id={`key-${v.key}`}
                                    type="text"
                                    value={v.key}
                                    disabled
                                />
                                <input
                                    id={`value-${v.key}`}
                                    type="text"
                                    value={v.value}
                                    onChange={(e) =>
                                        handleChangeVar(indexOfFirstVar + index, "value", e.target.value)
                                    }
                                    className={v.masked ? "masked-input" : ""}
                                    placeholder="value"
                                    autoComplete="off"
                                    name={`vault-var-${indexOfFirstVar + index}`}
                                />
                                <FaEye id={`unmask-${v.key}`} className="row-icon" onClick={() => toggleMaskOne(indexOfFirstVar + index)} />
                                <FaRegClipboard id={`copy-${v.key}`} data-testid="clipboard-icon" className="row-icon" onClick={() => copyValue(v.value)} />
                                <FaTrash id={`delete-${v.key}`} data-testid="trash-icon" className="row-icon" onClick={() => deleteVariable(v.key)} />
                            </div>
                        ))}
                    </div>

                    <div className="pagination">
                        <button
                            disabled={currentPage === 1}
                            onClick={() => setCurrentPage(prev => prev - 1)}
                        >←</button>
                        {[...Array(totalPages)].map((_, idx) => (
                            <button
                                key={idx}
                                className={currentPage === idx + 1 ? "active" : ""}
                                onClick={() => setCurrentPage(idx + 1)}
                            >
                                {idx + 1}
                            </button>
                        ))}
                        <button
                            disabled={currentPage === totalPages}
                            onClick={() => setCurrentPage(prev => prev + 1)}
                        >→</button>
                    </div>
                </>
            )}

            {/* Create service popup */}
            {popupType === "create-service" && (
                <InputPopup
                    label="Create new service"
                    inputs={[
                        { name: "serviceName", labelValue: "Service name" }
                    ]}
                    onSubmit={(values) => createService(values.serviceName)}
                    onClose={() => setPopupType(null)}
                />
            )}

            {/* Create environment popup */}
            {popupType === "create-environment" && (
                <InputPopup
                    label={`Create new environment for service: ${selectedService}`}
                    inputs={[
                        { name: "environmentName", labelValue: "Environment name" }
                    ]}
                    onSubmit={(values) => createEnvironment(values.environmentName)}
                    onClose={() => setPopupType(null)}
                />
            )}

            {/* Add variable popup */}
            {popupType === "add-variable" && (
                <InputPopup
                    label={`Add a new variable for service/environment: ${selectedService}/${selectedEnvironment}`}
                    inputs={[
                        { name: "key", labelValue: "Key" },
                        { name: "value", labelValue: "Value"}
                    ]}
                    onSubmit={(values) => addVariable(values.key, values.value)}
                    onClose={() => setPopupType(null)}
                />
            )}

            {AUTHSERVICE_INTEGRATION && popupType === "user-management" && (
                <UserManagementPopup
                    onClose={() => setPopupType(null)}
                    loadUsers={() => loadUsers()}
                    addUser={(user) => addUser(user)}
                    deleteUser={(user) => deleteUser(user)}
                    getAllRoles={() => getAllRoles()}
                    updateUserRole={(username, newRole) => updateUserRole(username, newRole)}
                />
            )}
        </div>
    )
}

export default VaultList
