

import { useState } from "react"
import { useAuth } from "../../context/AuthContext"
import InvitationManagement from "./InvitationManagement"
import UserAdministration from "./UserAdministration"
import AuthorizationManagement from "./AuthorizationManagement"
import EmailTemplateManagement from "./EmailTemplateManagement"
import "./css/AdminContent.css"
import { EMAILSERVICE_INTEGRATION } from "../../config"

const AdminContent = () => {
    const { hasAuthorization } = useAuth()
    const [activeSection, setActiveSection] = useState(null)

    const sections = [
        { name: "Invitations", authorization: "INVITATION_MANAGEMENT", component: <InvitationManagement /> },
        { name: "Users", authorization: "USER_MANAGEMENT", component: <UserAdministration /> },
        { name: "Authorizations", authorization: "AUTHORIZATION_MANAGEMENT", component: <AuthorizationManagement /> },
        ...(EMAILSERVICE_INTEGRATION ? [{ name: "Email Templates", authorization: "EMAIL_TEMPLATE_MANAGEMENT", component: <EmailTemplateManagement /> }] : [])
    ].filter(section => hasAuthorization(section.authorization))

    if(sections.length === 0)
        return <div className="admin-content"><h2>Admin access is required</h2></div>

    const section = sections.find(section => section.name === activeSection) || sections[0]

    return (
        <div className="admin-content">
            <div className="admin-header">
                <h1>Administration</h1>
            </div>
            <div className="admin-navigation">
                {sections.map(section =>
                    <button key={section.name} className={section.name === activeSection || (!activeSection && section === sections[0]) ? "active" : ""}
                            onClick={() => setActiveSection(section.name)}>{section.name}</button>
                )}
            </div>
            <div className="admin-section" key={section.name}>
                {section.component}
            </div>
        </div>
    )
}

export default AdminContent
