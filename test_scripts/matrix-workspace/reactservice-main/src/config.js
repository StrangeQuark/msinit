const {
  VITE_AUTH_API_BASE_URL,
  VITE_EMAIL_API_BASE_URL,
  VITE_FILE_API_BASE_URL,
  VITE_VAULT_API_BASE_URL,
  VITE_GATEWAY_BASE_URL,
  VITE_AUTHSERVICE_INTEGRATION,
  VITE_EMAILSERVICE_INTEGRATION,
  VITE_FILESERVICE_INTEGRATION,
  VITE_VAULTSERVICE_INTEGRATION,
  VITE_GATEWAYSERVICE_INTEGRATION,
} = import.meta.env;

export const AUTHSERVICE_INTEGRATION = VITE_AUTHSERVICE_INTEGRATION === "true"
export const EMAILSERVICE_INTEGRATION = VITE_EMAILSERVICE_INTEGRATION === "true"
export const FILESERVICE_INTEGRATION = VITE_FILESERVICE_INTEGRATION === "true"
export const VAULTSERVICE_INTEGRATION = VITE_VAULTSERVICE_INTEGRATION === "true"
export const GATEWAYSERVICE_INTEGRATION = VITE_GATEWAYSERVICE_INTEGRATION === "true"

const isLocalBrowser = window.location.hostname === "localhost" || window.location.hostname === "127.0.0.1";

function adaptBaseUrl(url) {
  if (!url || !isLocalBrowser)
    return url

  return url
            .replace(/auth-service(:\d+)?/, "localhost$1")
            .replace(/email-service(:\d+)?/, "localhost$1")
            .replace(/file-service(:\d+)?/, "localhost$1")
            .replace(/vault-service(:\d+)?/, "localhost$1")
            .replace(/gateway-service(:\d+)?/, "localhost$1")
}

export const AUTH_API_BASE_URL = adaptBaseUrl(VITE_AUTH_API_BASE_URL)
export const EMAIL_API_BASE_URL = adaptBaseUrl(VITE_EMAIL_API_BASE_URL)
export const FILE_API_BASE_URL = adaptBaseUrl(VITE_FILE_API_BASE_URL)
export const VAULT_API_BASE_URL = adaptBaseUrl(VITE_VAULT_API_BASE_URL)
export const GATEWAY_BASE_URL = adaptBaseUrl(VITE_GATEWAY_BASE_URL)

let AUTH_ENDPOINTS = {
  REGISTER: `${AUTH_API_BASE_URL}/api/auth/register`,
  INVITE_ONLY: `${AUTH_API_BASE_URL}/api/auth/invitation/invite-only`,
  AUTHENTICATE: `${AUTH_API_BASE_URL}/api/auth/authenticate`,
  ACCESS: `${AUTH_API_BASE_URL}/api/auth/access`,
  LOGOUT: `${AUTH_API_BASE_URL}/api/auth/access/logout`,
  UPDATE_EMAIL: `${AUTH_API_BASE_URL}/api/auth/user/update-email`,
  DELETE_USER: `${AUTH_API_BASE_URL}/api/auth/user/delete-user`,
  PASSWORD_RESET: `${AUTH_API_BASE_URL}/api/auth/user/send-password-reset-email`,
  UPDATE_USERNAME: `${AUTH_API_BASE_URL}/api/auth/user/update-username`,
  UPDATE_PASSWORD: `${AUTH_API_BASE_URL}/api/auth/user/update-password`,
  SEARCH_USERS: `${AUTH_API_BASE_URL}/api/auth/user/search-users`,
  GET_USER_ID: `${AUTH_API_BASE_URL}/api/auth/user/get-user-id`,
  GET_USER_DETAILS_BY_IDS: `${AUTH_API_BASE_URL}/api/auth/user/get-user-details-by-ids`,
  GET_ADMIN_USER: `${AUTH_API_BASE_URL}/api/auth/user/get-admin-user`,
  UPDATE_ROLE: `${AUTH_API_BASE_URL}/api/auth/user/update-role`,
  ADD_AUTHORIZATIONS_TO_USER: `${AUTH_API_BASE_URL}/api/auth/user/add-authorizations-to-user`,
  REMOVE_AUTHORIZATIONS: `${AUTH_API_BASE_URL}/api/auth/user/remove-authorizations`,
  CREATE_INVITATION: `${AUTH_API_BASE_URL}/api/auth/invitation/create`,
  DELETE_INVITATION: `${AUTH_API_BASE_URL}/api/auth/invitation/delete`,
  DELETE_ALL_INVITATIONS: `${AUTH_API_BASE_URL}/api/auth/invitation/delete-all`,
  GET_ALL_INVITATIONS: `${AUTH_API_BASE_URL}/api/auth/invitation/get-all`,
  GET_ALL_AUTHORIZATIONS: `${AUTH_API_BASE_URL}/api/auth/authorization/get-all`,
  ADD_ROLE_AUTHORIZATION: `${AUTH_API_BASE_URL}/api/auth/role-authorization/add`,
  GET_ROLE_AUTHORIZATIONS: `${AUTH_API_BASE_URL}/api/auth/role-authorization/get`,
  REMOVE_ROLE_AUTHORIZATION: `${AUTH_API_BASE_URL}/api/auth/role-authorization/remove`,
  GET_ALL_ROLES: `${AUTH_API_BASE_URL}/api/auth/role-authorization/get-all-roles`
}

let EMAIL_ENDPOINTS = {
  CONFIRM_TOKEN: `${EMAIL_API_BASE_URL}/api/email/confirm-token?token=`,
  ENABLE_USER: `${EMAIL_API_BASE_URL}/api/email/enable-user?token=`,
  RESET_USER_PASSWORD: `${EMAIL_API_BASE_URL}/api/email/reset-user-password`,
  GET_TEMPLATE_EMAIL: `${EMAIL_API_BASE_URL}/api/email/get-template-email`,
  GET_ALL_TEMPLATE_EMAILS: `${EMAIL_API_BASE_URL}/api/email/get-all-template-emails`,
  CREATE_TEMPLATE_EMAIL: `${EMAIL_API_BASE_URL}/api/email/create-template-email`,
  UPDATE_TEMPLATE_EMAIL: `${EMAIL_API_BASE_URL}/api/email/update-template-email`,
  DELETE_TEMPLATE_EMAIL: `${EMAIL_API_BASE_URL}/api/email/delete-template-email`
}

let FILE_ENDPOINTS = {
  GET_ALL: `${FILE_API_BASE_URL}/api/file/get-all`,
  GET_ALL_COLLECTIONS: `${FILE_API_BASE_URL}/api/file/get-all-collections`,
  NEW_COLLECTION: `${FILE_API_BASE_URL}/api/file/new-collection`,
  DOWNLOAD: `${FILE_API_BASE_URL}/api/file/download`,
  DOWNLOAD_ALL: `${FILE_API_BASE_URL}/api/file/downloadAll`,
  UPLOAD: `${FILE_API_BASE_URL}/api/file/upload`,
  STREAM: `${FILE_API_BASE_URL}/api/file/stream`,
  DELETE: `${FILE_API_BASE_URL}/api/file/delete`,
  DELETE_COLLECTION: `${FILE_API_BASE_URL}/api/file/delete-collection`,
  GET_CURRENT_USER_ROLE: `${FILE_API_BASE_URL}/api/file/get-current-user-role`,
  GET_USERS_BY_COLLECTION: `${FILE_API_BASE_URL}/api/file/get-users-by-collection`,
  GET_ALL_ROLES: `${FILE_API_BASE_URL}/api/file/get-all-roles`,
  UPDATE_USER_ROLE: `${FILE_API_BASE_URL}/api/file/update-user-role`,
  ADD_USER_TO_COLLECTION: `${FILE_API_BASE_URL}/api/file/add-user-to-collection`,
  DELETE_USER_FROM_COLLECTION: `${FILE_API_BASE_URL}/api/file/delete-user-from-collection`
}

let VAULT_ENDPOINTS = {
  GET_ALL_SERVICES: `${VAULT_API_BASE_URL}/api/vault/get-all-services`,
  GET_SERVICE: `${VAULT_API_BASE_URL}/api/vault/get-service`,
  GET_ALL_ENVS_BY_SERVICE: `${VAULT_API_BASE_URL}/api/vault/get-environments-by-service`,
  GET_ENV: `${VAULT_API_BASE_URL}/api/vault/get-environment`,
  GET_VARS_BY_ENV: `${VAULT_API_BASE_URL}/api/vault/get-variables-by-environment`,
  CREATE_SERVICE: `${VAULT_API_BASE_URL}/api/vault/create-service`,
  CREATE_ENVIRONMENT: `${VAULT_API_BASE_URL}/api/vault/create-environment`,
  ADD_VAR: `${VAULT_API_BASE_URL}/api/vault/add-variable`,
  UPDATE_VARS: `${VAULT_API_BASE_URL}/api/vault/update-variables`,
  DELETE_VAR: `${VAULT_API_BASE_URL}/api/vault/delete-variable`,
  DELETE_ENVIRONMENT: `${VAULT_API_BASE_URL}/api/vault/delete-environment`,
  DELETE_SERVICE: `${VAULT_API_BASE_URL}/api/vault/delete-service`,
  ADD_ENV_FILE: `${VAULT_API_BASE_URL}/api/vault/add-env-file`,
  DOWNLOAD_ENV_FILE: `${VAULT_API_BASE_URL}/api/vault/download-env-file`,
  GET_USERS_BY_SERVICE: `${VAULT_API_BASE_URL}/api/vault/get-users-by-service`,
  ADD_USER_TO_SERVICE: `${VAULT_API_BASE_URL}/api/vault/add-user-to-service`,
  DELETE_USER_FROM_SERVICE: `${VAULT_API_BASE_URL}/api/vault/delete-user-from-service`,
  GET_CURRENT_USER_ROLE: `${VAULT_API_BASE_URL}/api/vault/get-current-user-role`,
  GET_ALL_ROLES: `${VAULT_API_BASE_URL}/api/vault/get-all-roles`,
  UPDATE_USER_ROLE: `${VAULT_API_BASE_URL}/api/vault/update-user-role`
}

const replaceBaseUrl = (endpoints, newBase) => {
  return Object.fromEntries(
    Object.entries(endpoints).map(([key, url]) => {
      const path = url.replace(/https?:\/\/[^/]+/, "")
      return [key, `${newBase}${path}`]
    })
  )
}
if(GATEWAYSERVICE_INTEGRATION && GATEWAY_BASE_URL) {
  AUTH_ENDPOINTS = replaceBaseUrl(AUTH_ENDPOINTS, GATEWAY_BASE_URL)
  EMAIL_ENDPOINTS = replaceBaseUrl(EMAIL_ENDPOINTS, GATEWAY_BASE_URL)
  FILE_ENDPOINTS = replaceBaseUrl(FILE_ENDPOINTS, GATEWAY_BASE_URL)
  VAULT_ENDPOINTS = replaceBaseUrl(VAULT_ENDPOINTS, GATEWAY_BASE_URL)
}

export const getAuthHeaders = (token) => {
  if(!AUTHSERVICE_INTEGRATION)
    return {}

  return { Authorization: "Bearer " + token }
}

export const setAuthHeader = (request, token) => {
  if(AUTHSERVICE_INTEGRATION)
    request.setRequestHeader("Authorization", "Bearer " + token)
}

export {
  AUTH_ENDPOINTS,
  EMAIL_ENDPOINTS,
  FILE_ENDPOINTS,
  VAULT_ENDPOINTS,
}
