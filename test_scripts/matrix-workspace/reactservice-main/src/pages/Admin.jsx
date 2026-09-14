

import Toolbar from "../components/Toolbar"
import AdminContent from "../components/authservice/AdminContent"
import { RequireAuth } from "../context/AuthContext"

const Admin = () => {
    return(
        <RequireAuth>
            <Toolbar />

            <AdminContent />
        </RequireAuth>
    )
}

export default Admin
