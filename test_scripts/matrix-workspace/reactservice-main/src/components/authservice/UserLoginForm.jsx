

import { useState } from "react"
import { AUTH_ENDPOINTS, EMAILSERVICE_INTEGRATION } from "../../config"
import "./css/UserLoginForm.css"

const UserLoginForm = () => {
    const[username, setUsername] = useState("")
    const[password, setPassword] = useState("")
    const[errorMessage, setErrorMessage] = useState("")

    const handleKeyDown = (event) => {
        if (event.key === "Enter") {
            requestHandler()
        }
    }

    const requestHandler = () => {
        //JWT 
        var loginJSON = {"username": username, "password": password}

        //Check if the username and password are empty. If they are, display error message
        if(username === "") {
            setErrorMessage('Username is empty')
            return
        }
        if(password === "") {
            setErrorMessage('Password is empty')
            return
        }

        fetch(AUTH_ENDPOINTS.AUTHENTICATE, {
            method: 'POST',
            credentials: "include",
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(loginJSON)
            }).then(response => response.json().then(
                (data) => {
                    if(!response.ok) {
                        if(data.errorMessage === "User is disabled")
                            setErrorMessage("User is disabled - Check your email inbox for a confirmation link")
                        else
                            setErrorMessage(data.errorMessage)
                        return
                    }

                    // Fetch access token
                    fetch(AUTH_ENDPOINTS.ACCESS, {
                        method: "POST",
                        credentials: "include",
                    }).then(res => res.json().then(
                        (d) => {
                            if(!res.ok) {
                                setErrorMessage(d.errorMessage)
                                return
                            }

                            // //Navigate back, or go to homepage if coming from the registration page
                            // if(!document.referrer.endsWith('/register'))
                            //     window.history.back()
                            // else
                            window.location.href="/"
                        }
                    ))
                }
            ))
    }

    return(
        <div id="login-div" className="auth-div">
            <h1>Login</h1>
            <div className="error-div" >
                {errorMessage && (<b id="error-message">{errorMessage}</b>)}
            </div>
            <form id="login-form" onKeyDown={handleKeyDown} onSubmit={(e) => e.preventDefault()}>
                <label htmlFor="username">Username:</label>
                <br />
                <input type="text" id="username" name="username" placeholder="Type your username" spellCheck="false" value={username} onChange={(e) => setUsername(e.target.value)}/>
                <hr />
                <label htmlFor="password">Password:</label>
                <br />
                <input type="password" id="password" name="password" placeholder="Type your password" spellCheck="false" value={password} onChange={(e) => setPassword(e.target.value)}/>
                <hr />
            </form>
            <button id='submit-button' onClick={() => requestHandler()}>LOGIN</button>
            <div className="auth-help-div">
                {EMAILSERVICE_INTEGRATION && <a id="forgot-password-link" href="/password-reset">Forgot password?</a>}
                <a id="sign-up-link" href="/register">Sign up</a>
            </div>
        </div>
    )
}

export default UserLoginForm
