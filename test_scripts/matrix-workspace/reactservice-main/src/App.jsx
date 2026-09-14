import Home from './pages/Home'
import Login from './pages/Login'
import Register from './pages/Register'
import UserProfile from './pages/UserProfile'
import ResetPassword from './pages/ResetPassword'
import ResetPasswordSearch from './pages/ResetPasswordSearch'
import ConfirmEmail from './pages/ConfirmEmail'
import Settings from './pages/Settings'
import Admin from './pages/Admin'
import Files from './pages/Files'
import Vault from './pages/Vault'
import { createBrowserRouter, RouterProvider } from 'react-router-dom'
import { AuthProvider } from './context/AuthContext'
import { AUTHSERVICE_INTEGRATION, EMAILSERVICE_INTEGRATION, FILESERVICE_INTEGRATION, VAULTSERVICE_INTEGRATION } from './config'

const router = createBrowserRouter(
  [
    { path: "/", element: <Home /> },
    ...(AUTHSERVICE_INTEGRATION ? [
      { path: "/login", element: <Login /> },
      { path: "/register", element: <Register /> },
      { path: "/user/:id", element: <UserProfile /> },
      { path: "/settings", element: <Settings /> },
      { path: "/admin", element: <Admin /> }
    ] : []),
    ...(AUTHSERVICE_INTEGRATION && EMAILSERVICE_INTEGRATION ? [
      { path: "/password-reset", element: <ResetPasswordSearch /> },
      { path: "/new-password", element: <ResetPassword /> },
      { path: "/confirm-email", element: <ConfirmEmail /> }
    ] : []),
    ...(FILESERVICE_INTEGRATION ? [{ path: "/files", element: <Files /> }] : []),
    ...(VAULTSERVICE_INTEGRATION ? [{ path: "/vault", element: <Vault /> }] : [])
  ],
  {
    future: {
      v7_startTransition: true
    },
  }
)

const App = () => {
  return (
    <AuthProvider>
      <RouterProvider router={router} />
    </AuthProvider>
  )
}

export default App
