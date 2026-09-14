


import { useState } from "react"
import { AUTH_ENDPOINTS } from "../../config"
import { verifyEmailRegex } from "../../utility/EmailUtility"

const ResetPasswordSearchForm = () => {
    const[credentials, setCredentials] = useState("")
    const[isError, setIsError] = useState(false)
    const[isSuccess, setIsSuccess] = useState(false)

    const handleKeyDown = (event) => {
        if (event.key === "Enter") {
            requestHandler()
        }
    }

    const requestHandler = async () => {
        //If the user enters nothing, do nothing
        if(credentials === "") {
            return
        }

        var credentialsJson = {"username": credentials}
        //If we're sending an email, then change the credentialsJson to send an email instead of a username
        if(verifyEmailRegex(credentials))
            credentialsJson = {"email": credentials}

        fetch(AUTH_ENDPOINTS.PASSWORD_RESET, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(credentialsJson)
        }).then(response => {
                if(!response.ok) {
                    setIsError(true)
                    return
                }

                setIsSuccess(true)
            }
        )
    }

    return(
        <>
        {!isSuccess && (<div id="request-div" className="auth-div">
            <h1>Reset password</h1>
            <div className="error-div" >
                {isError && (<b id="no-credentials-message">Sorry, we could not find your account</b>)}
            </div>
            <form id="request-form" onKeyDown={handleKeyDown} onSubmit={(e) => e.preventDefault()}>
                <label htmlFor="credentials">Username or email:</label><br />
                <input type="text" id="credentials" name="credentials" placeholder="Type your username or email" value={credentials} onChange={(e) => setCredentials(e.target.value)}/>
                <hr />
            </form>
            <button id='submit-button' onClick={() => requestHandler()}>SUBMIT</button>
        </div>)}
        {isSuccess && (<div id="request-success-div" className="auth-div">
            <p id="request-success-text-field">Please check your email for a password reset link</p>
        </div>)}
        </>
    )
}

export default ResetPasswordSearchForm
