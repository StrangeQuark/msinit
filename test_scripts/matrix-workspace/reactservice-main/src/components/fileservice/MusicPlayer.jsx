

import "./css/MusicPlayer.css"
import { useEffect, useState } from "react"
import { getAuthHeaders } from "../../config"
import { useAuth } from "../../context/AuthContext"

const MusicPlayer = ({ audioUrl, onClose, token }) => {
    const [blobUrl, setBlobUrl] = useState(null)
    const { getAccessToken } = useAuth()

    useEffect(() => {
        const loadAudio = async () => {
            const response = await fetch(audioUrl, {
                headers: getAuthHeaders(getAccessToken())
            })
            const blob = await response.blob()
            setBlobUrl(URL.createObjectURL(blob))
        }

        loadAudio()

        return () => {
            if (blobUrl) 
                URL.revokeObjectURL(blobUrl)
        }
    }, [audioUrl, token])

    const getMimeType = (url) => {
        const extension = url.split('.').pop().toLowerCase();
        const mimeTypes = {
            mp3: "audio/mpeg",
            wav: "audio/wav",
            flac: "audio/flac",
            aac: "audio/aac",
            m4a: "audio/mp4",
            ogg: "audio/ogg",
        };
        return mimeTypes[extension] || "audio/mpeg"
    };    

    return (
        <div className="popup-overlay">
            <div className="popup-content">
                <button className="close-btn" onClick={onClose}>×</button>
                <div>
                    <audio key={blobUrl} data-testid="audio-player" controls>
                        <source src={blobUrl} data-testid="audio-source" type={getMimeType(audioUrl)} />
                    </audio>
                </div>
            </div>
        </div>
    )
}

export default MusicPlayer
