

import { useEffect, useState } from "react"
import { AUTH_ENDPOINTS } from "../../config"
import { useAuth } from "../../context/AuthContext"

const InvitationManagement = () => {
    const { getAccessToken } = useAuth()
    const [email, setEmail] = useState("")
    const [invitations, setInvitations] = useState([])
    const [message, setMessage] = useState("")
    const [createdToken, setCreatedToken] = useState("")

    useEffect(() => {
        getInvitations()
    }, [])

    const getInvitations = async () => {
        const response = await fetch(AUTH_ENDPOINTS.GET_ALL_INVITATIONS, {
            headers: { Authorization: "Bearer " + getAccessToken() }
        })

        if(response.ok)
            setInvitations(await response.json())
    }

    const createInvitation = async (event) => {
        event.preventDefault()
        const response = await fetch(AUTH_ENDPOINTS.CREATE_INVITATION, {
            method: "POST",
            headers: { "Content-Type": "application/json", Authorization: "Bearer " + getAccessToken() },
            body: JSON.stringify({ email })
        })
        const data = await response.json()

        if(!response.ok) {
            setMessage(data.errorMessage)
            return
        }

        setCreatedToken(data.token)
        setEmail("")
        getInvitations()
    }

    const deleteInvitation = async (id) => {
        const response = await fetch(AUTH_ENDPOINTS.DELETE_INVITATION, {
            method: "DELETE",
            headers: { "Content-Type": "application/json", Authorization: "Bearer " + getAccessToken() },
            body: JSON.stringify({ id })
        })

        if(response.ok)
            getInvitations()
    }

    const deleteAllInvitations = async () => {
        const response = await fetch(AUTH_ENDPOINTS.DELETE_ALL_INVITATIONS, {
            method: "DELETE",
            headers: { Authorization: "Bearer " + getAccessToken() }
        })

        if(response.ok) {
            setMessage("All invitations deleted")
            getInvitations()
        }
    }

    const copyToken = async () => {
        await navigator.clipboard.writeText(createdToken)
        setMessage("Invitation token copied")
    }

    return (
        <div className="admin-management">
            <div className="admin-section-header">
                <div>
                    <h2>Invitations</h2>
                    <p>Create and manage access invitations.</p>
                </div>
                {invitations.length > 0 && <button className="admin-danger-button" onClick={deleteAllInvitations}>Delete all</button>}
            </div>
            <form className="admin-form" onSubmit={createInvitation}>
                <input aria-label="Invitation email" value={email} onChange={event => setEmail(event.target.value)} placeholder="Email address" required />
                <button type="submit">Create invitation</button>
            </form>
            {message && <p className="admin-message">{message}</p>}
            <div className="admin-list">
                {invitations.map(invitation =>
                    <div className="admin-list-item" key={invitation.id}>
                        <div>
                            <strong>{invitation.email}</strong>
                            <span>{invitation.used ? "Used" : "Available"}</span>
                        </div>
                        <button className="admin-danger-button" onClick={() => deleteInvitation(invitation.id)}>Delete</button>
                    </div>
                )}
                {invitations.length === 0 && <p className="admin-empty-state">No invitations have been created.</p>}
            </div>
            {createdToken && <div className="admin-modal-overlay">
                <div className="admin-modal admin-token-modal">
                    <div className="admin-modal-header">
                        <h2>Invitation created</h2>
                        <button className="admin-close-button" onClick={() => setCreatedToken("")}>×</button>
                    </div>
                    <p>Invitation created. Token:</p>
                    <code>{createdToken}</code>
                    <div className="admin-modal-actions">
                        <button className="admin-secondary-button" onClick={() => setCreatedToken("")}>Close</button>
                        <button onClick={copyToken}>Copy token</button>
                    </div>
                </div>
            </div>}
        </div>
    )
}

export default InvitationManagement
