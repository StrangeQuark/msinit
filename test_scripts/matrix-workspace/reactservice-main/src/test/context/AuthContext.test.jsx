

import "@testing-library/jest-dom"
import { renderHook, act, waitFor } from "@testing-library/react"
import { vi } from "vitest"
import { AuthProvider, decodeJWT, useAuth } from "../../context/AuthContext"

describe("AuthContext context", () => {
  beforeEach(() => {
    document.cookie = "access_token=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/"
    document.cookie = "refresh_token=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/"
    global.fetch = vi.fn()
  })

  test("decodes Base64URL JWT payloads", () => {
    const token = "header.eyJzdWIiOiJ0ZXN0IiwiZXhwIjoxMjMsInZhbHVlIjoi4K-_In0.signature"

    const payload = decodeJWT(token)

    expect(payload.sub).toBe("test")
    expect(payload.exp).toBe(123)
  })

  test("initial state is correct", () => {
    const wrapper = ({ children }) => <AuthProvider>{children}</AuthProvider>
    const { result } = renderHook(() => useAuth(), { wrapper })

    expect(result.current.isLoggedIn).toBe(false)
    expect(result.current.username).toBe(null)
  })

  test("getAccessToken returns access endpoint value", async () => {
    const payload = btoa(JSON.stringify({ exp: Date.now() / 1000 + 100, sub: "john_doe" }))
    global.fetch.mockResolvedValue({
      ok: true,
      json: vi.fn().mockResolvedValue({ jwtToken: `header.${payload}.sig` }),
    })

    const wrapper = ({ children }) => <AuthProvider>{children}</AuthProvider>
    const { result } = renderHook(() => useAuth(), { wrapper })

    await waitFor(() => {
      expect(result.current.getAccessToken()).toBe(`header.${payload}.sig`)
    })
  })

  test("logout clears cookies and state", () => {
    document.cookie = "access_token=abc; refresh_token=xyz"
    const wrapper = ({ children }) => <AuthProvider>{children}</AuthProvider>
    const { result } = renderHook(() => useAuth(), { wrapper })

    act(() => result.current.logout())

    expect(global.fetch).toHaveBeenCalledWith(expect.any(String), expect.objectContaining({
      method: "POST",
      credentials: "include"
    }))
    expect(result.current.getAccessToken()).toBe(null)
    expect(result.current.isLoggedIn).toBe(false)
    expect(result.current.username).toBe(null)
  })

  test("initializes from refresh cookie", async () => {
    const payload = btoa(JSON.stringify({ exp: Date.now() / 1000 + 100, sub: "john_doe", authorizations: ["USER_MANAGEMENT"] }))
    global.fetch.mockResolvedValue({
      ok: true,
      json: vi.fn().mockResolvedValue({ jwtToken: `header.${payload}.sig` }),
    })

    const wrapper = ({ children }) => <AuthProvider>{children}</AuthProvider>
    const { result } = renderHook(() => useAuth(), { wrapper })

    await waitFor(() => {
      expect(result.current.isLoggedIn).toBe(true)
      expect(result.current.username).toBe("john_doe")
      expect(result.current.hasAuthorization("USER_MANAGEMENT")).toBe(true)
    })
  })

  test("initializes from refresh token when access token is missing", async () => {
    const payload = btoa(JSON.stringify({ exp: Date.now() / 1000 + 100, sub: "jane_doe" }))
    global.fetch.mockImplementation(async () => {
      return {
        ok: true,
        json: vi.fn().mockResolvedValue({ jwtToken: `header.${payload}.sig` }),
      }
    })

    const wrapper = ({ children }) => <AuthProvider>{children}</AuthProvider>
    const { result } = renderHook(() => useAuth(), { wrapper })

    await waitFor(() => {
      expect(result.current.isLoggedIn).toBe(true)
      expect(result.current.username).toBe("jane_doe")
    })
  })
})
