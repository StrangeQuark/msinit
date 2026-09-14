


import { useEffect, useState } from "react"
import { EMAIL_ENDPOINTS } from "../../config"

const ConfirmEmailMessage = () => {
    const [message, setMessage] = useState()
    const [expiredTokenMessage, setExpiredTokenMessage] = useState(false)

    useEffect(() => {
        const urlParams = new URLSearchParams(window.location.search)
        const token = urlParams.get("token")
        fetch(EMAIL_ENDPOINTS.ENABLE_USER + token, {
            method: 'GET',
            }).then(response => response.json().then(
                (data) => {
                    if(data.message === "The token has expired - A new confirmation email has been sent") {
                        setExpiredTokenMessage(true)
                    }
                    else
                        setMessage(data.message)
                }
            ))
    }, [])

    return(
        <>
            {message && (
                <div id="message-div" className="auth-div">
                    <p id="message-text-field">{message}</p>
                </div>
            )}

            {expiredTokenMessage && (
                <div id="message-div" className="auth-div">
                    <p id="message-text-field">
                        This token has expired - A new confirmation email has been sent to the attached email address
                    </p>
                </div>
            )}
        </>
    )
}

export default ConfirmEmailMessage
