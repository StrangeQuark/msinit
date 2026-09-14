

import { useState, useEffect } from "react"
import { SlPencil } from "react-icons/sl"
import { AUTH_ENDPOINTS } from "../../config"
import "./css/AccountSettings.css"
import InputPopup from "../InputPopup"
import { useAuth } from "../../context/AuthContext"

const AccountSettings = () => {
    const { username, getAccessToken, logout, refreshAccessToken } = useAuth()
    const [popupType, setPopupType] = useState(null)
    const [email, setEmail] = useState(null)

    useEffect(() => {
            fetchAccountDetails()
        }, [])

    const fetchAccountDetails = async () => {
        const data = await fetch(AUTH_ENDPOINTS.GET_USER_ID, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                Authorization: "Bearer " + getAccessToken() 
            },
            body: JSON.stringify({ username })
        }).then(res => res.json())

        const request = [data]

        const response = await fetch(`${AUTH_ENDPOINTS.GET_USER_DETAILS_BY_IDS}`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                Authorization: "Bearer " + getAccessToken() 
            },
            body: JSON.stringify(request)
        })

        const userData = await response.json()

        setEmail(userData[0].email)
    }

    const updateUsername = async (newUsername, password) => {
        const response = await fetch(AUTH_ENDPOINTS.UPDATE_USERNAME, {
            method: "POST",
            credentials: "include",
            headers: {
                Authorization: "Bearer " + getAccessToken(),
                "Content-Type": "application/json",
            },
            body: JSON.stringify({ newUsername, password }),
        })

        if(!response.ok) {
            const data = await response.json()
            alert(data.errorMessage)
            return false
        }

        if(!await refreshAccessToken()) {
            logout()
            return false
        }

        return true
    }

    const updateEmail = async (newEmail, password) => {
        const response = await fetch(AUTH_ENDPOINTS.UPDATE_EMAIL, {
            method: "POST",
            credentials: "include",
            headers: {
                Authorization: "Bearer " + getAccessToken(),
                "Content-Type": "application/json",
            },
            body: JSON.stringify({ newEmail, password }),
        })

        if(!response.ok) {
            const data = await response.json()
            alert(data.errorMessage)
            return false
        }

        if(!await refreshAccessToken()) {
            logout()
            return false
        }

        setEmail(newEmail)
        return true
    }

    const updatePassword = async (password, newPassword) => {
        const response = await fetch(AUTH_ENDPOINTS.UPDATE_PASSWORD, {
            method: "POST",
            credentials: "include",
            headers: {
                Authorization: "Bearer " + getAccessToken(),
                "Content-Type": "application/json",
            },
            body: JSON.stringify({ password, newPassword })
        })

        if(!response.ok) {
            const data = await response.json()
            alert(data.errorMessage)
            return false
        }

        if(!await refreshAccessToken()) {
            logout()
            return false
        }

        return true
    }

    const deleteProfile = async (username, password) => {
        const response = await fetch(AUTH_ENDPOINTS.DELETE_USER, {
            method: "POST",
            headers: {
                Authorization: "Bearer " + getAccessToken(),
                "Content-Type": "application/json",
            },
            body: JSON.stringify({ username, password }),
        })

        if(!response.ok) {
            const data = await response.json()
            alert(data.errorMessage)
            return false
        }

        logout()
        return true
    }

    return (
        <div className="account-settings">
            <div className="account-section">
                <h2>Account Information</h2>
                <div className="account-info">
                    <p data-testid="username">
                        <strong>Username:</strong> {username}
                    </p>
                    <SlPencil data-testid="update-username" onClick={() => setPopupType("username")} />
                </div>
                <div className="account-info">
                    <p data-testid="email">
                        <strong>Email:</strong> {email}
                    </p>
                    <SlPencil data-testid="update-email" onClick={() => setPopupType("email")} />
                </div>
                <button data-testid="update-password" onClick={() => setPopupType("password")}>Change password</button>
            </div>

            <div className="delete-section">
                <h3>Delete Account</h3>
                <button data-testid="delete-account-button" onClick={() => setPopupType("delete")}>Delete Account</button>
            </div>

            {popupType === "username" && (
                <InputPopup
                    label="Edit username"
                    inputs={[
                        { name: "newUsername", labelValue: "New username", defaultValue: username },
                        { name: "password", labelValue: "Password", className: "masked-input" }
                    ]}
                    onSubmit={(values) => updateUsername(values.newUsername, values.password)}
                    onClose={() => setPopupType(null)}
                />
            )}

            {popupType === "email" && (
                <InputPopup
                    label="Edit email"
                    inputs={[
                        { name: "newEmail", labelValue: "New email", defaultValue: email },
                        { name: "password", labelValue: "Password", className: "masked-input" }
                    ]}
                    onSubmit={(values) => updateEmail(values.newEmail, values.password)}
                    onClose={() => setPopupType(null)}
                />
            )}

            {popupType === "password" && (
                <InputPopup
                    label="Edit password"
                    inputs={[
                        { name: "password", labelValue: "Current password", className: "masked-input" },
                        { name: "newPassword", labelValue: "New password", className: "masked-input" }
                    ]}
                    onSubmit={(values) => updatePassword(values.password, values.newPassword)}
                    onClose={() => setPopupType(null)}
                />
            )}

            {popupType === "delete" && (
                <InputPopup
                    label="Delete account"
                    inputs={[
                        { name: "username", labelValue: "Username"},
                        { name: "password", labelValue: "Password", className: "masked-input" }
                    ]}
                    onSubmit={(values) => deleteProfile(values.username, values.password)}
                    onClose={() => setPopupType(null)}
                />
            )}
        </div>
    )
}

export default AccountSettings
