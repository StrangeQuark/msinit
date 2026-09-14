

import { useEffect, useState } from "react"
import { EMAIL_ENDPOINTS } from "../../config"
import { useAuth } from "../../context/AuthContext"

const EmailTemplateManagement = () => {
    const { getAccessToken } = useAuth()
    const [templates, setTemplates] = useState([])
    const [templateName, setTemplateName] = useState("")
    const [subject, setSubject] = useState("")
    const [body, setBody] = useState("")
    const [tokenPurpose, setTokenPurpose] = useState("")
    const [message, setMessage] = useState("")
    const [editingTemplate, setEditingTemplate] = useState(null)

    useEffect(() => {
        getTemplates()
    }, [])

    const request = (url, method, body) => fetch(url, {
        method,
        headers: { "Content-Type": "application/json", Authorization: "Bearer " + getAccessToken() },
        body: body ? JSON.stringify(body) : undefined
    })

    const getTemplates = async () => {
        const response = await request(EMAIL_ENDPOINTS.GET_ALL_TEMPLATE_EMAILS, "GET")
        if(response.ok)
            setTemplates(await response.json())
    }

    const openTemplate = (template) => {
        setTemplateName(template.name)
        setSubject(template.subject)
        setBody(template.body)
        setTokenPurpose(template.tokenPurpose || "")
        setEditingTemplate(template)
    }

    const createTemplate = () => {
        setTemplateName("")
        setSubject("")
        setBody("")
        setTokenPurpose("")
        setEditingTemplate({})
    }

    const closeTemplate = () => setEditingTemplate(null)

    const saveTemplate = async (event) => {
        event.preventDefault()
        const response = await request(editingTemplate.id ? EMAIL_ENDPOINTS.UPDATE_TEMPLATE_EMAIL : EMAIL_ENDPOINTS.CREATE_TEMPLATE_EMAIL,
            editingTemplate.id ? "PUT" : "POST", { templateName, subject, body, tokenPurpose: tokenPurpose || null })
        const data = await response.json()

        setMessage(data.message || data.errorMessage)
        if(response.ok) {
            getTemplates()
            closeTemplate()
        }
    }

    const deleteTemplate = async (template) => {
        const response = await request(EMAIL_ENDPOINTS.DELETE_TEMPLATE_EMAIL, "DELETE", { templateName: template.name })
        const data = await response.json()
        setMessage(data.message || data.errorMessage)

        if(response.ok) {
            closeTemplate()
            getTemplates()
        }
    }

    return (
        <div className="admin-management">
            <div className="admin-section-header">
                <div>
                    <h2>Email templates</h2>
                    <p>Create and update reusable email templates.</p>
                </div>
                <button onClick={createTemplate}>Create template</button>
            </div>
            {message && <p className="admin-message">{message}</p>}
            <div className="admin-list">
                {templates.map(template => <div className="admin-list-item" key={template.id}>
                    <div>
                        <strong>{template.name}</strong>
                        <span>{template.subject}</span>
                    </div>
                    <div className="admin-list-actions">
                        <button onClick={() => openTemplate(template)}>Edit</button>
                        <button className="admin-danger-button" onClick={() => deleteTemplate(template)}>Delete</button>
                    </div>
                </div>)}
                {templates.length === 0 && <p className="admin-empty-state">No email templates have been created.</p>}
            </div>
            {editingTemplate && <div className="admin-modal-overlay">
            <form className="admin-modal admin-form admin-template-form" onSubmit={saveTemplate}>
                <div className="admin-modal-header">
                    <h2>{editingTemplate.id ? "Edit template" : "Create template"}</h2>
                    <button type="button" className="admin-close-button" onClick={closeTemplate}>×</button>
                </div>
                <label className="admin-field">
                    Template name
                    <input aria-label="Template name" value={templateName} onChange={event => setTemplateName(event.target.value)} required />
                </label>
                <label className="admin-field">
                    Subject
                    <input aria-label="Template subject" value={subject} onChange={event => setSubject(event.target.value)} required />
                </label>
                <label className="admin-field">
                    Token purpose (optional)
                    <input aria-label="Template token purpose" value={tokenPurpose} onChange={event => setTokenPurpose(event.target.value)} />
                </label>
                <label className="admin-field">
                    Template body
                    <textarea aria-label="Template body" value={body} onChange={event => setBody(event.target.value)} required />
                </label>
                <div className="admin-modal-actions">
                    <button type="button" className="admin-secondary-button" onClick={closeTemplate}>Cancel</button>
                    <button type="submit">{editingTemplate.id ? "Save changes" : "Create template"}</button>
                </div>
            </form>
            </div>}
        </div>
    )
}

export default EmailTemplateManagement
