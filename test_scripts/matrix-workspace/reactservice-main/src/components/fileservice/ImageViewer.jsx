

import "./css/ImageViewer.css"
import { useEffect, useState } from "react"
import { getAuthHeaders } from "../../config"
import { useAuth } from "../../context/AuthContext"

const ImageViewer = ({ imageUrl, onClose, token }) => {
    const [blobUrl, setBlobUrl] = useState(null)
    const { getAccessToken } = useAuth()

    useEffect(() => {
        const loadImage = async () => {
            const response = await fetch(imageUrl, {
                headers: getAuthHeaders(getAccessToken())
            })
            const blob = await response.blob()
            setBlobUrl(URL.createObjectURL(blob))
        }

        loadImage()

        return () => {
            if (blobUrl) 
                URL.revokeObjectURL(blobUrl)
        }
    }, [imageUrl, token])

    return (
        <div className="popup-overlay">
            <div className="popup-content">
                <button className="close-btn" onClick={onClose}>×</button>
                <div>
                    <img data-testid="image" src={blobUrl} alt=""/>
                </div>
            </div>
        </div>
    )
}

export default ImageViewer
