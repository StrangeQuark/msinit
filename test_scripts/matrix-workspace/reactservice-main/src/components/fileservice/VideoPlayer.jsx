

import "./css/VideoPlayer.css"

const VideoPlayer = ({ videoUrl, onClose }) => {
    return (
        <div className="popup-overlay">
            <div className="popup-content">
                <button className="video-close-btn" onClick={onClose}>X</button>
                <div>
                    <video data-testid="video-player" className="video-player" controls crossOrigin="use-credentials" src={videoUrl}>
                        Your browser does not support the video tag
                    </video>
                </div>
            </div>
        </div>
    )
}


export default VideoPlayer
