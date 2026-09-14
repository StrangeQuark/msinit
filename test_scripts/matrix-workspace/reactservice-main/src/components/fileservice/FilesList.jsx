

import { useEffect, useRef, useState } from "react"
import VideoPlayer from "./VideoPlayer"
import ImageViewer from "./ImageViewer"
import MusicPlayer from "./MusicPlayer"
import "./css/FilesList.css"
import { AUTHSERVICE_INTEGRATION, FILE_ENDPOINTS, getAuthHeaders, setAuthHeader } from "../../config"
import { useAuth } from "../../context/AuthContext"
import InputPopup from "../InputPopup"
import { FaCog } from "react-icons/fa"
import UserManagementPopup from "../authservice/UserManagementPopup"

const FilesList = () => {
    const [transferProgress, setTransferProgress] = useState(0)
    const [isTransferring, setIsTransferring] = useState(false)
    const [collections, setCollections] = useState([])
    const [filteredCollections, setFilteredCollections] = useState([])
    const [files, setFiles] = useState([])
    const [selectedCollection, setSelectedCollection] = useState(null)
    const [viewMode, setViewMode] = useState("grid") // "grid" or "list"
    const [searchTerm, setSearchTerm] = useState("")
    const [selectedVideoFile, setSelectedVideoFile] = useState(null)
    const [selectedImageFile, setSelectedImageFile] = useState(null)
    const [selectedAudioFile, setSelectedAudioFile] = useState(null)
    const [popupType, setPopupType] = useState("")
    const [displayPopout, setDisplayPopout] = useState(false)
    const [currentUserRole, setCurrentUserRole] = useState(AUTHSERVICE_INTEGRATION ? null : "OWNER")
    const fileInputRef = useRef(null)
    const { getAccessToken } = useAuth()
    

    const videoExtensions = ["mp4", "webm", "ogg"]
    const audioExtensions = ["mp3", "wav", "flac", "aac", "m4a", "ogg"]
    const imageExtensions = ["jpg", "jpeg", "png", "gif", "bmp", "webp", "svg", "ico", "avif"]

    useEffect(() => {
        fetchCollections()
    }, [])

    useEffect(() => {
        if (searchTerm.trim() === "") {
            setFilteredCollections(collections)
        } else {
            setFilteredCollections(
                collections.filter(c =>
                    c.name.toLowerCase().includes(searchTerm.toLowerCase())
                )
            )
        }
    }, [searchTerm, collections])

    const fetchCollections = async () => {
        const response = await fetch(FILE_ENDPOINTS.GET_ALL_COLLECTIONS, {
            method: "GET",
            headers: {
                ...getAuthHeaders(getAccessToken())
            }
        })
        const data = await response.json()
        setCollections(data)
        setFilteredCollections(data)
    }

    const createCollection = async (collectionName) => {
        const response = await fetch(FILE_ENDPOINTS.NEW_COLLECTION, {
            method: "POST",
            headers: {
                ...getAuthHeaders(getAccessToken()),
                "Content-Type": "application/json"
            },
            body: JSON.stringify({ collectionName })
        })

        if(!response.ok) {
            const message = await response.json()
            alert(message.errorMessage)
            return false
        }

        const data = await response.text()
        if(data !== "New collection successfully created") {
            alert(data)
            return false
        }

        fetchCollections()
        return true
    }
    const getCurrentUserRole = async (collectionName) => {
        if(!AUTHSERVICE_INTEGRATION)
            return

        const response = await fetch(FILE_ENDPOINTS.GET_CURRENT_USER_ROLE, {
            method: "POST",
            headers: { ...getAuthHeaders(getAccessToken()), "Content-Type": "application/json" },
            body: JSON.stringify({ collectionName })
        })

        const data = await response.json()

        setCurrentUserRole(data)
    }

    const loadUsers = async () => {
        const response = await fetch(FILE_ENDPOINTS.GET_USERS_BY_COLLECTION,{
            method: "POST",
            headers: { 
                ...getAuthHeaders(getAccessToken()),
                "Content-Type": "application/json",
            },
            body: JSON.stringify({ collectionName: selectedCollection.name })
        })

        const data = await response.json()

        return data
    }

    const getAllRoles = async () => {
        const response = await fetch(`${FILE_ENDPOINTS.GET_ALL_ROLES}`, {
            headers: { ...getAuthHeaders(getAccessToken()) }
        })

        const data = await response.json()

        return data
    }

    const updateUserRole = async (username, newRole) => {
        const request = {
            collectionName: selectedCollection.name,
            username: username,
            role: newRole
        }

        const response = await fetch(`${FILE_ENDPOINTS.UPDATE_USER_ROLE}`, {
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
            collectionName: selectedCollection.name,
            username: user.username,
            role: "READ"
        }

        const response = await fetch(`${FILE_ENDPOINTS.ADD_USER_TO_COLLECTION}`, {
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
            collectionName: selectedCollection.name,
            username: user.username
        }

        const response = await fetch(`${FILE_ENDPOINTS.DELETE_USER_FROM_COLLECTION}`, {
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

    const fetchFiles = async (collectionName) => {
        try {
            const response = await fetch(FILE_ENDPOINTS.GET_ALL, {
                method: "POST",
                headers: { ...getAuthHeaders(getAccessToken()), "Content-Type": "application/json" },
                body: JSON.stringify({ collectionName })
            })
            const data = await response.json()
            setFiles(data)
        } catch (error) {
            console.error("Error fetching files", error)
        }
    }

    const handleCollectionSelect = (collection) => {
        setSelectedCollection(collection)
        fetchFiles(collection.name)
        getCurrentUserRole(collection.name)
    }

    const handleDownload = async (fileName) => {
        const xhr = new XMLHttpRequest()
        xhr.open(
            "POST",
            FILE_ENDPOINTS.DOWNLOAD
        )
        xhr.responseType = "blob"

        setAuthHeader(xhr, getAccessToken())
        xhr.setRequestHeader("Content-Type", "application/json")

        setIsTransferring(true)
        setTransferProgress(0)

        xhr.onprogress = (event) => {
            if (event.lengthComputable) {
                const percent = Math.round((event.loaded / event.total) * 100)
                setTransferProgress(percent)
            }
        }

        xhr.onload = () => {
            setIsTransferring(false)
            setTransferProgress(0)

            if (xhr.status >= 200 && xhr.status < 300) {
                const url = window.URL.createObjectURL(xhr.response)
                const a = document.createElement("a")
                a.href = url
                a.download = fileName
                document.body.appendChild(a)
                a.click()
                a.remove()
                window.URL.revokeObjectURL(url)
            } else {
                console.error("Download failed", xhr.responseText)
                alert("File download failed")
            }
        }

        xhr.onerror = () => {
            setIsTransferring(false)
            setTransferProgress(0)
            console.error("Download failed")
            alert("File download failed")
        }

        xhr.send(JSON.stringify({ collectionName: selectedCollection.name, fileName }))
    }

    const handleDownloadAll = async () => {
        const xhr = new XMLHttpRequest()
        xhr.open(
            "POST",
            FILE_ENDPOINTS.DOWNLOAD_ALL
        )
        xhr.responseType = "blob"

        setAuthHeader(xhr, getAccessToken())
        xhr.setRequestHeader("Content-Type", "application/json")

        setIsTransferring(true)
        setTransferProgress(0)

        xhr.onprogress = (event) => {
            if (event.lengthComputable) {
                const percent = Math.round((event.loaded / event.total) * 100)
                setTransferProgress(percent)
            }
        }

        xhr.onload = () => {
            setIsTransferring(false)
            setTransferProgress(0)

            if (xhr.status >= 200 && xhr.status < 300) {
                const url = window.URL.createObjectURL(xhr.response)
                const a = document.createElement("a")
                a.href = url
                a.download = `${selectedCollection.name}.zip`
                document.body.appendChild(a)
                a.click()
                a.remove()
                window.URL.revokeObjectURL(url)
            } else {
                console.error("Download failed", xhr.responseText)
                alert("File download failed")
            }
        }

        xhr.onerror = () => {
            setIsTransferring(false)
            setTransferProgress(0)
            console.error("Download failed")
            alert("File download failed")
        }

        xhr.send(JSON.stringify({ collectionName: selectedCollection.name }))
    }

    const handleDelete = async (fileName) => {
        try {
            const response = await fetch(FILE_ENDPOINTS.DELETE, {
                method: "DELETE",
                headers: { ...getAuthHeaders(getAccessToken()), "Content-Type": "application/json" },
                body: JSON.stringify({ collectionName: selectedCollection.name, fileName })
            })

            if(!response.ok) {
                const message = await response.json()
                alert(message.errorMessage)
                return
            }

            fetchFiles(selectedCollection.name)
        } catch (error) {
            console.error("Delete failed", error)
        }
    }

    const handleFileUpload = async (event) => {
        const file = event.target.files[0]
        if (!file) 
            return

        if(files.includes(file.name)) {
            alert("File with this name already exists in this collection")
            return
        }

        const formData = new FormData()
        formData.append("file", file)
        formData.append("collectionName", selectedCollection.name)

        setIsTransferring(true)
        setTransferProgress(0)

        const xhr = new XMLHttpRequest()
        xhr.open("POST", FILE_ENDPOINTS.UPLOAD)

        setAuthHeader(xhr, getAccessToken())

        xhr.upload.onprogress = (event) => {
            if (event.lengthComputable) {
                const percent = Math.round((event.loaded / event.total) * 100)
                setTransferProgress(percent)
            }
        }

        xhr.onload = () => {
            setIsTransferring(false)
            setTransferProgress(0)

            if (xhr.status >= 200 && xhr.status < 300) {
                fetchFiles(selectedCollection.name)
            } else {
                console.error("Upload failed", xhr.responseText)
                alert("File upload failed")
            }
        }

        xhr.onerror = () => {
            setIsTransferring(false)
            setTransferProgress(0)
            console.error("Upload failed")
            alert("File upload failed")
        }

        xhr.send(formData)

        // Reset the target value to always trigger onChange
        event.target.value = ""
    }

    const deleteCollection = async () => {
        if(!confirm("Are you sure you want to delete collection: " + selectedCollection.name))
            return

        const response = await fetch(FILE_ENDPOINTS.DELETE_COLLECTION, {
            method: "DELETE",
            headers: { ...getAuthHeaders(getAccessToken()), "Content-Type": "application/json" },
            body: JSON.stringify({ collectionName: selectedCollection.name })
        })

        if(!response.ok) {
            const message = await response.json()
            alert(message.errorMessage)
            return
        }

        fetchCollections()
        setSelectedCollection("")
    }

    const openFilePicker = () => {
        fileInputRef.current.click()
    }

    const getStreamUrl = (fileName) => `${FILE_ENDPOINTS.STREAM}?collectionName=${encodeURIComponent(selectedCollection.name)}&fileName=${encodeURIComponent(fileName)}`
    const handleVideo = (fileName) => setSelectedVideoFile(getStreamUrl(fileName))
    const handleImage = (fileName) => setSelectedImageFile(getStreamUrl(fileName))
    const handleAudio = (fileName) => setSelectedAudioFile(getStreamUrl(fileName))
    const closeVideo = () => setSelectedVideoFile(null)
    const closeImage = () => setSelectedImageFile(null)
    const closeAudio = () => setSelectedAudioFile(null)

    return (
        <div className="files-page">
            <div className={isTransferring ? "disabled-during-transfer" : ""}>
                {!selectedCollection && (
                    <>
                        <div className="collections-header">
                            <h2>Select a Collection</h2>
                            <input
                                type="text"
                                placeholder="Search collections..."
                                value={searchTerm}
                                onChange={(e) => setSearchTerm(e.target.value)}
                                className="collection-search"
                            />
                            <div className="view-toggle">
                                <button onClick={() => setViewMode("grid")} className={viewMode === "grid" ? "active" : ""}>Grid</button>
                                <button onClick={() => setViewMode("list")} className={viewMode === "list" ? "active" : ""}>List</button>
                            </div>
                            <button className="collection-add-btn" onClick={() => setPopupType("create-collection")}>Create collection</button>
                        </div>

                        {viewMode === "grid" ? (
                            <div className="collection-grid">
                                {filteredCollections.map((collection, index) => (
                                    <div
                                        key={index}
                                        className="collection-card"
                                        onClick={() => handleCollectionSelect(collection)}
                                    >
                                        <div className="collection-icon">📁</div>
                                        <div className="collection-name">{collection.name}</div>
                                    </div>
                                ))}
                            </div>
                        ) : (
                            <ul className="collection-list">
                                {filteredCollections.map((collection, index) => (
                                    <li key={index} onClick={() => handleCollectionSelect(collection)}>
                                        📁 {collection.name}
                                    </li>
                                ))}
                            </ul>
                        )}
                    </>
                )}

                {selectedCollection && (
                    <div className="files-list">
                        <div className="files-list-header">
                            <button onClick={() => setSelectedCollection(null)} className="back-button">← Back</button>
                            <h2>{selectedCollection.name}</h2>
                            <input type="file" ref={fileInputRef} onChange={handleFileUpload} className="hidden-input" />
                            <div className="files-list-right-div">
                                {(currentUserRole !== "READ") && (
                                    <>
                                        <button onClick={() => handleDownloadAll()} className="file-button">Download all</button>
                                        <button onClick={openFilePicker} className="file-button">Upload</button>
                                    </>
                                )}
                                {(currentUserRole === "OWNER" || currentUserRole === "MANAGER") && (
                                    <div className="cog-wrapper">
                                        {selectedCollection && (
                                            <FaCog data-testid="cog-icon" onClick={() => setDisplayPopout(!displayPopout)}/>
                                        )}

                                        {displayPopout && (
                                            <div id="file-popout-container" className="file-popout-container">
                                                {AUTHSERVICE_INTEGRATION && <button onClick={() => {
                                                        setPopupType("user-management") 
                                                        setDisplayPopout(false) 
                                                    }}>
                                                    Manage Users
                                                </button>
                                                }
                                                {currentUserRole === "OWNER" && (
                                                    <button onClick={() => {
                                                        deleteCollection()
                                                        setDisplayPopout(false)
                                                    }}>
                                                    Delete Collection
                                                    </button>
                                                )}
                                            </div>
                                        )}
                                    </div>
                                )}
                            </div>
                        </div>

                        {isTransferring && (
                            <div className="transfer-progress">
                                <progress
                                    className="progress-bar"
                                    value={transferProgress}
                                    max="100"
                                ></progress>
                                <span>{transferProgress}%</span>
                            </div>
                        )}

                        <ul>
                            {files.map((file, index) => {
                                const fileExtension = file.split('.').pop().toLowerCase()
                                const isVideo = videoExtensions.includes(fileExtension)
                                const isImage = imageExtensions.includes(fileExtension)
                                const isAudio = audioExtensions.includes(fileExtension)

                                return(
                                    <li key={index} className="file-item">
                                        <span>{file}</span>
                                        <div className="file-item-button-div">
                                            {isVideo && <button onClick={() => handleVideo(file)} className="stream-btn">Stream</button>}
                                            {isImage && <button onClick={() => handleImage(file)} className="view-btn">View</button>}
                                            {isAudio && <button onClick={() => handleAudio(file)} className="listen-btn">Listen</button>}
                                            <button onClick={() => handleDownload(file)} className="download-btn">Download</button>
                                            {(currentUserRole !== "READ") && <button onClick={() => handleDelete(file)} className="file-delete-btn">Delete</button>}
                                        </div>
                                    </li>
                                )
                            })}
                        </ul>
                        
                        {selectedVideoFile && <VideoPlayer videoUrl={selectedVideoFile} onClose={closeVideo} />}
                        {selectedImageFile && <ImageViewer imageUrl={selectedImageFile} onClose={closeImage} />}
                        {selectedAudioFile && <MusicPlayer audioUrl={selectedAudioFile} onClose={closeAudio} />}
                    </div>
                )}

                {/* Create collection popup */}
                {popupType === "create-collection" && (
                    <InputPopup
                        label={"Create a new collection"}
                        inputs={[
                            { name: "collectionName", labelValue: "Collection name" }
                        ]}
                        onSubmit={(values) => createCollection(values.collectionName)}
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
        </div>
    )
}

export default FilesList
