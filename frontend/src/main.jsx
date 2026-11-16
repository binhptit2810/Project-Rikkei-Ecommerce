import React from 'react'
import ReactDOM from 'react-dom/client'
import { Auth0Provider } from '@auth0/auth0-react'
import App from './App.jsx'
import './index.css'

const domain = import.meta.env.VITE_AUTH0_DOMAIN || 'dev-rikkei-ecommerce.auth0.com'
const clientId = import.meta.env.VITE_AUTH0_CLIENT_ID || 'placeholder-client-id'
const redirectUri = typeof window !== 'undefined' ? window.location.origin : 'http://localhost:3000'

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <Auth0Provider
      domain={domain}
      clientId={clientId}
      authorizationParams={{
        redirect_uri: redirectUri,
        scope: 'openid profile email',
      }}
    >
      <App />
    </Auth0Provider>
  </React.StrictMode>,
)
