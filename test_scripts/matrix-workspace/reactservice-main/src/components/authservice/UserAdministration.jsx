

import { useEffect, useState } from "react"
import { AUTH_ENDPOINTS } from "../../config"
import { useAuth } from "../../context/AuthContext"

const UserAdministration = () => {
    const { getAccessToken } = useAuth()
    const [query, setQuery] = useState("")
    const [user, setUser] = useState(null)
    const [authorizations, setAuthorizations] = useState([])
    const [roles, setRoles] = useState([])
    const [message, setMessage] = useState("")

    useEffect(() => {
        getAuthorizations()
        getRoles()
    }, [])

    const request = (url, method, body) => fetch(url, {
        method,
        headers: { "Content-Type": "application/json", Authorization: "Bearer " + getAccessToken() },
        body: body ? JSON.stringify(body) : undefined
    })

    const getAuthorizations = async () => {
        const response = await request(AUTH_ENDPOINTS.GET_ALL_AUTHORIZATIONS, "GET")
        if(response.ok)
            setAuthorizations((await response.json()).map(authorization => authorization.name))
    }

    const getRoles = async () => {
        const response = await request(AUTH_ENDPOINTS.GET_ALL_ROLES, "GET")
        if(response.ok) {
            const values = await response.json()
            if(Array.isArray(values))
                setRoles(values)
        }
    }

    const findUser = async () => {
        const response = await request(AUTH_ENDPOINTS.GET_ADMIN_USER, "POST", { query })
        const data = await response.json()

        if(!response.ok) {
            setMessage(data.errorMessage)
            return
        }

        setUser(data)
        setMessage("")
    }

    const getUser = async (event) => {
        event.preventDefault()
        findUser()
    }

    const updateRole = async (event) => {
        const newRole = event.target.value
        const response = await request(AUTH_ENDPOINTS.UPDATE_ROLE, "POST", { username: user.username, newRole })

        if(response.ok) {
            setUser({ ...user, role: newRole })
            findUser()
        }
    }

    const updateAuthorization = async (authorization, checked) => {
        const response = await request(checked ? AUTH_ENDPOINTS.ADD_AUTHORIZATIONS_TO_USER : AUTH_ENDPOINTS.REMOVE_AUTHORIZATIONS,
            "POST", { username: user.username, authorizations: [authorization] })

        if(response.ok) {
            const directAuthorizations = user.directAuthorizations || []
            const roleAuthorizations = user.roleAuthorizations || []
            const updatedDirectAuthorizations = checked ? [...directAuthorizations, authorization] :
                directAuthorizations.filter(value => value !== authorization)
            const updatedAuthorizations = [...new Set([...updatedDirectAuthorizations, ...roleAuthorizations])]
            setUser({ ...user, authorizations: updatedAuthorizations, directAuthorizations: updatedDirectAuthorizations })
        }
    }

    return (
        <div className="admin-management">
            <div className="admin-section-header">
                <div>
                    <h2>User management</h2>
                    <p>Find a user to manage their role and direct authorizations.</p>
                </div>
            </div>
            <form className="admin-form" onSubmit={getUser}>
                <input aria-label="User search" value={query} onChange={event => setQuery(event.target.value)} placeholder="Username or email" required />
                <button type="submit">Find user</button>
            </form>
            {message && <p className="admin-message">{message}</p>}
            {user && <div className="admin-user-details">
                <div className="admin-user-summary">
                    <div><span>Username</span><strong>{user.username}</strong></div>
                    <div><span>Email</span><strong>{user.email}</strong></div>
                    <div><span>Status</span><strong>{user.enabled ? "Enabled" : "Disabled"}</strong></div>
                </div>
                <label className="admin-select-label">Role
                    <select value={user.role} onChange={updateRole}>
                        {roles.map(role => <option key={role}>{role}</option>)}
                    </select>
                </label>
                <div className="admin-checkboxes">
                    <div className="admin-checkboxes-header">
                        <strong>Authorizations</strong>
                        <span>Role authorizations are selected and managed from the Authorizations page.</span>
                    </div>
                    {authorizations.map(authorization => <label key={authorization} className={(user.roleAuthorizations || []).includes(authorization) ? "admin-role-authorization" : ""}>
                        <input type="checkbox" checked={user.authorizations.includes(authorization)}
                               disabled={(user.roleAuthorizations || []).includes(authorization)}
                               onChange={event => updateAuthorization(authorization, event.target.checked)} />
                        {authorization}
                        {(user.roleAuthorizations || []).includes(authorization) && <span>Role</span>}
                    </label>)}
                </div>
            </div>}
        </div>
    )
}

export default UserAdministration
