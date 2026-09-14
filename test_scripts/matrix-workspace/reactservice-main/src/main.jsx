import ReactDOM from 'react-dom/client'
import './index.css'
import App from './App'

if (window.localStorage.getItem('reactservice-theme') === 'dark') {
  document.documentElement.setAttribute('data-theme', 'dark')
}

const root = ReactDOM.createRoot(document.getElementById('root'))
root.render(
  <App />
)
