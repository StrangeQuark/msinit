import { useEffect, useState } from "react"
import "./css/Toolbar.css"
import logo from "../res/logo.png"
import darkModeLogo from "../res/logo_dark_mode.png"
import { RiLoginCircleLine } from "react-icons/ri"
import { MdDarkMode, MdLightMode } from "react-icons/md"
import { useAuth } from "../context/AuthContext"
import { AUTHSERVICE_INTEGRATION, FILESERVICE_INTEGRATION, VAULTSERVICE_INTEGRATION } from "../config"

const THEME_STORAGE_KEY = "reactservice-theme"

const getStoredTheme = () => {
    if (typeof window === "undefined") {
        return "light"
    }

    return window.localStorage.getItem(THEME_STORAGE_KEY) === "dark" ? "dark" : "light"
}

const Toolbar = () => {
    const [displayPopout, setDisplayPopout] = useState(false)
    const { isLoggedIn, username, logout, hasAuthorization = () => false } = useAuth()
    const [centerDropdownOpen, setCenterDropdownOpen] = useState(false);
    const [theme, setTheme] = useState(getStoredTheme)

    const navigateTo = (path) => {
        window.location.href = path
        setCenterDropdownOpen(false)
    }

    useEffect(() => {
        document.documentElement.setAttribute("data-theme", theme)
        window.localStorage.setItem(THEME_STORAGE_KEY, theme)
    }, [theme])

    const toggleTheme = () => {
        setTheme((currentTheme) => currentTheme === "light" ? "dark" : "light")
    }

    return (
        <div className="toolbar">
            <div className="left-div">
                <img alt="Logo" src={ theme === "dark" ? darkModeLogo : logo } />
            </div>
            <div className="center-div">
                <a href="/" data-testid="home-nav-link">Home</a>
                {FILESERVICE_INTEGRATION && <a href="/files" data-testid="files-nav-link">Files</a>}
                {VAULTSERVICE_INTEGRATION && <a href="/vault" data-testid="vault-nav-link">Vault</a>}

                {/* Mobile dropdown start*/}
                <button className="center-dropdown-button" onClick={() => setCenterDropdownOpen(!centerDropdownOpen)}>Menu</button>

                <div className={`center-dropdown-container ${centerDropdownOpen ? "show" : ""}`}>
                    <button onClick={() => { navigateTo("/") }}>Home</button>
                    {FILESERVICE_INTEGRATION && <button onClick={() => { navigateTo("/files") }}>Files</button>}
                    {VAULTSERVICE_INTEGRATION && <button onClick={() => { navigateTo("/vault") }}>Vault</button>}
                </div>
                {/* Mobile dropdown end*/}
            </div>
            <div className="right-div">
                <button
                    type="button"
                    className={`theme-toggle ${theme === "dark" ? "dark" : ""}`}
                    data-testid="theme-toggle"
                    aria-label={`Switch to ${theme === "dark" ? "light" : "dark"} mode`}
                    aria-pressed={theme === "dark"}
                    onClick={toggleTheme}
                >
                    <span className="theme-toggle-track">
                        <MdLightMode className="theme-toggle-icon theme-toggle-sun" />
                        <MdDarkMode className="theme-toggle-icon theme-toggle-moon" />
                        <span className="theme-toggle-thumb" />
                    </span>
                </button>
                {AUTHSERVICE_INTEGRATION && (!isLoggedIn ? <RiLoginCircleLine id="loginButton" data-testid="loginButton" size={"2em"} onClick={() => navigateTo("/login")}/> : <button id="userButton" className="user-button" onClick={() => setDisplayPopout(!displayPopout)}>{username}</button>)}
                {
                    AUTHSERVICE_INTEGRATION && displayPopout && (<div id='center-popout-container' className="center-popout-container">
                        <button onClick={() => {
                            navigateTo(`/user/${username}`)
                            setDisplayPopout(false)
                        }}>
                            Profile
                        </button>

                        <button onClick={() => {
                            navigateTo(`/settings`)
                            setDisplayPopout(false)
                        }}>
                            Settings
                        </button>

                        {(hasAuthorization("INVITATION_MANAGEMENT") || hasAuthorization("USER_MANAGEMENT") || hasAuthorization("AUTHORIZATION_MANAGEMENT") || hasAuthorization("EMAIL_TEMPLATE_MANAGEMENT")) &&
                        <button onClick={() => {
                            navigateTo(`/admin`)
                            setDisplayPopout(false)
                        }}>
                            Admin
                        </button>}

                        <button onClick={() => {
                            logout()
                            setDisplayPopout(false)
                        }}>
                            Logout
                        </button>
                    </div>)}
            </div>
        </div>
    )
}

export default Toolbar
