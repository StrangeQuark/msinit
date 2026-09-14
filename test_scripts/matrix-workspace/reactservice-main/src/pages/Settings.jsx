

import Toolbar from './../components/Toolbar'
import SettingsContent from '../components/authservice/SettingsContent'
import { RequireAuth } from "../context/AuthContext"

const Settings = () => {
    return(
        <RequireAuth>
            <Toolbar />

            <SettingsContent />
        </RequireAuth>
    )
}

export default Settings