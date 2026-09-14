

import { useState } from 'react'
import SettingsNavigation from './SettingsNavigation'
import AccountSettings from './AccountSettings'
import SecuritySettings from './SecuritySettings'
import "./css/SettingsContent.css"

const SettingsContent = () => {
    const [selectedSection, setSelectedSection] = useState('account')

    return (
        <div className="main-settings-container">
            <SettingsNavigation setSelectedSection={setSelectedSection} />
            <div className="settings-content">
                {selectedSection === 'account' && <AccountSettings />}
                {selectedSection === 'security' && <SecuritySettings />}
            </div>
        </div>
    )
}

export default SettingsContent
