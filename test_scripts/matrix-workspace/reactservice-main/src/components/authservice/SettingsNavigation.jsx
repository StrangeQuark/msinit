

import { useState } from 'react'
import "./css/SettingsNavigation.css"

const SettingsNavigation = ({ setSelectedSection }) => {
    const [selected, setSelected] = useState('account')

    const handleSelection = (section) => {
        setSelected(section)
        setSelectedSection(section)
    }

    return (
        <div className="settings-navigation">
            <button className={selected === 'account' ? 'selected' : ''} onClick={() => handleSelection('account')}>Account Settings</button>
            {/* <button className={selected === 'security' ? 'selected' : ''} onClick={() => handleSelection('security')}>Security Settings</button> */}
        </div>
    )
}

export default SettingsNavigation
