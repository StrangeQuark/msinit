import { afterEach, describe, expect, test, vi } from "vitest"

const loadConfig = async (gatewayBaseUrl, gatewayIntegration = "true", authIntegration = "true") => {
  vi.resetModules()
  vi.stubEnv("VITE_AUTH_API_BASE_URL", "http://auth-service:6001")
  vi.stubEnv("VITE_EMAIL_API_BASE_URL", "http://email-service:6005")
  vi.stubEnv("VITE_FILE_API_BASE_URL", "http://file-service:6010")
  vi.stubEnv("VITE_VAULT_API_BASE_URL", "http://vault-service:6020")
  vi.stubEnv("VITE_GATEWAY_BASE_URL", gatewayBaseUrl)
  vi.stubEnv("VITE_AUTHSERVICE_INTEGRATION", authIntegration)
  vi.stubEnv("VITE_EMAILSERVICE_INTEGRATION", "true")
  vi.stubEnv("VITE_FILESERVICE_INTEGRATION", "true")
  vi.stubEnv("VITE_VAULTSERVICE_INTEGRATION", "true")
  vi.stubEnv("VITE_GATEWAYSERVICE_INTEGRATION", gatewayIntegration)

  return await import("../config")
}

afterEach(() => {
  vi.unstubAllEnvs()
})

describe("config", () => {
  test("uses service URLs when gateway URL is missing", async () => {
    const { AUTH_ENDPOINTS, FILE_ENDPOINTS } = await loadConfig("", "false")

    expect(AUTH_ENDPOINTS.AUTHENTICATE).toBe("http://auth-service:6001/api/auth/authenticate")
    expect(FILE_ENDPOINTS.GET_ALL).toBe("http://file-service:6010/api/file/get-all")
  })

  test("uses gateway URL when gateway URL is present", async () => {
    const { AUTH_ENDPOINTS, FILE_ENDPOINTS } = await loadConfig("http://gateway-service:8080")

    expect(AUTH_ENDPOINTS.AUTHENTICATE).toBe("http://gateway-service:8080/api/auth/authenticate")
    expect(FILE_ENDPOINTS.GET_ALL).toBe("http://gateway-service:8080/api/file/get-all")
  })

  test("does not add an authorization header when Authservice is disabled", async () => {
    const { getAuthHeaders } = await loadConfig("", "false", "false")

    expect(getAuthHeaders("token")).toEqual({})
  })
})
