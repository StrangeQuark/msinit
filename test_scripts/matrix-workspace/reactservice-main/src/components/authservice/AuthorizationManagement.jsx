

import { useEffect, useState } from "react"
import { AUTH_ENDPOINTS } from "../../config"
import { useAuth } from "../../context/AuthContext"

const AuthorizationManagement = () => {
    const { getAccessToken } = useAuth()
    const [authorizations, setAuthorizations] = useState([])
    const [roles, setRoles] = useState([])
    const [role, setRole] = useState("")
    const [roleAuthorizations, setRoleAuthorizations] = useState([])

    useEffect(() => {
        getAuthorizations()
        getRoles()
    }, [])

    useEffect(() => {
        if(role)
            getRoleAuthorizations()
    }, [role])

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
            if(Array.isArray(values)) {
                setRoles(values)
                setRole(values[0])
            }
        }
    }

    const getRoleAuthorizations = async () => {
        const response = await request(AUTH_ENDPOINTS.GET_ROLE_AUTHORIZATIONS, "POST", { role })
        if(response.ok)
            setRoleAuthorizations((await response.json()).map(value => value.authorization.name))
    }

    const updateRoleAuthorization = async (authorization, checked) => {
        const response = await request(checked ? AUTH_ENDPOINTS.ADD_ROLE_AUTHORIZATION : AUTH_ENDPOINTS.REMOVE_ROLE_AUTHORIZATION,
            checked ? "POST" : "DELETE", { role, authorization })

        if(response.ok)
            getRoleAuthorizations()
    }

    return (
        <div className="admin-management">
            <div className="admin-section-header">
                <div>
                    <h2>Authorizations</h2>
                    <p>Define the default authorizations for each role.</p>
                </div>
            </div>
            <label className="admin-select-label">Role
                <select value={role} onChange={event => setRole(event.target.value)}>
                    {roles.map(value => <option key={value}>{value}</option>)}
                </select>
            </label>
            <div className="admin-checkboxes">
                {authorizations.map(authorization => <label key={authorization}>
                    <input type="checkbox" checked={roleAuthorizations.includes(authorization)}
                           onChange={event => updateRoleAuthorization(authorization, event.target.checked)} />
                    {authorization}
                </label>)}
            </div>
        </div>
    )
}

export default AuthorizationManagement
