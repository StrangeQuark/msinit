import { createContext, useContext, useEffect, useState } from "react"
import { AUTH_ENDPOINTS, AUTHSERVICE_INTEGRATION } from "../config"
import { Navigate } from "react-router-dom"

const AuthContext = createContext(null)

export const decodeJWT = (token) => {
    const payloadBase64 = token.split(".")[1]
        .replace(/-/g, "+")
        .replace(/_/g, "/")

    const payloadJson = atob(payloadBase64.padEnd(Math.ceil(payloadBase64.length / 4) * 4, "="))
    return JSON.parse(payloadJson)
}

export const AuthProvider = ({ children }) => {
    const [isLoggedIn, setIsLoggedIn] = useState(false)
    const [username, setUsername] = useState(null)
    const [accessToken, setAccessToken] = useState(null)
    const [authorizations, setAuthorizations] = useState([])
    const [loading, setLoading] = useState(true)
    const [refreshTimer, setRefreshTimer] = useState(null)

    useEffect(() => {
        if(!AUTHSERVICE_INTEGRATION) {
            setLoading(false)
            return
        }

        const initAuth = async () => {
            await refreshAccessToken()
            setLoading(false)
        }
        initAuth()
    }, [])

    const refreshAccessToken = async () => {
        if(!AUTHSERVICE_INTEGRATION)
            return false

        try {
            const response = await fetch(AUTH_ENDPOINTS.ACCESS, {
                method: "POST",
                credentials: "include"
            })
            if (!response.ok) 
                return false

            const data = await response.json()

            setAccessToken(data.jwtToken)
            setIsLoggedIn(true)
            setUsername(getUsernameFromJWT(data.jwtToken))
            setAuthorizations(getAuthorizationsFromJWT(data.jwtToken))
            scheduleTokenRefresh(data.jwtToken)
            return true
        } catch {
            return false
        }
    }

    const getUsernameFromJWT = (token) => {
        try {
            return decodeJWT(token).sub
        } catch {
            return null
        }
    }

    const getAuthorizationsFromJWT = (token) => {
        try {
            return decodeJWT(token).authorizations || []
        } catch {
            return []
        }
    }

    const logout = () => {
        if (refreshTimer) 
            clearTimeout(refreshTimer)

        if(AUTHSERVICE_INTEGRATION) {
            fetch(AUTH_ENDPOINTS.LOGOUT, {
                method: "POST",
                credentials: "include"
            })
        }
        setAccessToken(null)
        setIsLoggedIn(false)
        setUsername(null)
        setAuthorizations([])
    }

    const getAccessToken = () => accessToken

    const hasAuthorization = (authorization) => authorizations.includes(authorization)

    const scheduleTokenRefresh = (token) => {
        try {
            const { exp } = decodeJWT(token)

            const expiresAt = exp * 1000
            const timeout = expiresAt - Date.now() - 60000  

            if (timeout > 0) {
                if (refreshTimer) 
                    clearTimeout(refreshTimer)

                const timerId = setTimeout(async () => {
                    const valid = await refreshAccessToken()
                    
                    if (!valid) {
                        logout()
                    }
                }, timeout)

                setRefreshTimer(timerId)
            } else {
                refreshAccessToken().then((valid) => {
                    if (!valid) 
                        logout()
                })
            }
        } catch {
            logout()
        }
    }

    return (
        <AuthContext.Provider value={{ isLoggedIn, username, loading, authorizations, setUsername, logout, getAccessToken, hasAuthorization, refreshAccessToken }}>
            {children}
        </AuthContext.Provider>
    )
}

export const useAuth = () => useContext(AuthContext)

export const RequireAuth = ({ children }) => {
    if(!AUTHSERVICE_INTEGRATION)
        return children

    const { loading, isLoggedIn } = useAuth()

    if (loading) return null // or <Spinner /> for nicer UX
    if (!isLoggedIn) return <Navigate to="/login" replace />

    return children
}
