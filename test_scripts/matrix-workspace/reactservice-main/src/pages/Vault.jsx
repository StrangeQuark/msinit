

import Toolbar from '../components/Toolbar'
import VaultList from '../components/vaultservice/VaultList'
import { RequireAuth } from '../context/AuthContext'

const Vault = () => {
    return(
        <>
        <RequireAuth> {}
            <Toolbar />

            <VaultList />
        </RequireAuth> {}
        </>
    )
}

export default Vault