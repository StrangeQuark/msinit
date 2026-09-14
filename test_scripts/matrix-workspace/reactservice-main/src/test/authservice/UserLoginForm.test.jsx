

import { render, screen, fireEvent, waitFor } from "@testing-library/react"
import "@testing-library/jest-dom"
import { vi } from "vitest"
import UserLoginForm from "../../components/authservice/UserLoginForm"
import { AUTH_ENDPOINTS } from "../../config"

describe("UserLoginForm component", () => {
  beforeEach(() => {
    vi.clearAllMocks()
    global.fetch = vi.fn()
    delete window.location
    window.location = { href: "" }
    document.cookie = "access_token=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/"
    document.cookie = "refresh_token=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/"
  })

  test("renders form elements correctly", () => {
    render(<UserLoginForm />)

    expect(screen.getByText("Login")).toBeInTheDocument()
    expect(screen.getByLabelText("Username:")).toBeInTheDocument()
    expect(screen.getByLabelText("Password:")).toBeInTheDocument()
    expect(screen.getByRole("button", { name: "LOGIN" })).toBeInTheDocument()
    expect(screen.getByText("Forgot password?")).toHaveAttribute("href", "/password-reset")
    expect(screen.getByText("Sign up")).toHaveAttribute("href", "/register")
  })

  test("shows error if username is empty", () => {
    render(<UserLoginForm />)

    fireEvent.click(screen.getByRole("button", { name: "LOGIN" }))
    expect(screen.getByText("Username is empty")).toBeInTheDocument()
  })

  test("shows error if password is empty", () => {
    render(<UserLoginForm />)

    fireEvent.change(screen.getByLabelText("Username:"), { target: { value: "testuser" } })
    fireEvent.click(screen.getByRole("button", { name: "LOGIN" }))

    expect(screen.getByText("Password is empty")).toBeInTheDocument()
  })

  test("handles 401 unauthorized response", async () => {
    const mockResponse = {
      ok: false,
      status: 401,
      json: vi.fn().mockResolvedValue({ errorMessage: "Invalid credentials" })
    }
    global.fetch.mockResolvedValueOnce(mockResponse)

    render(<UserLoginForm />)

    fireEvent.change(screen.getByLabelText("Username:"), { target: { value: "testuser" } })
    fireEvent.change(screen.getByLabelText("Password:"), { target: { value: "wrongpass" } })
    fireEvent.click(screen.getByRole("button", { name: "LOGIN" }))

    await waitFor(() => {
      expect(screen.getByText("Invalid credentials")).toBeInTheDocument()
    })
  })

  test("successful login refreshes access and redirects", async () => {
    // Mock authenticate response
    const mockAuthResponse = {
      ok: true,
      status: 200,
      json: vi.fn().mockResolvedValue({})
    }

    // Mock access token response
    const mockAccessResponse = {
      ok: true,
      json: vi.fn().mockResolvedValue({ jwtToken: "mock-access-token" })
    }

    global.fetch
      .mockResolvedValueOnce(mockAuthResponse)
      .mockResolvedValueOnce(mockAccessResponse)

    render(<UserLoginForm />)

    fireEvent.change(screen.getByLabelText("Username:"), { target: { value: "testuser" } })
    fireEvent.change(screen.getByLabelText("Password:"), { target: { value: "password123" } })
    fireEvent.click(screen.getByRole("button", { name: "LOGIN" }))

    await waitFor(() => {
      expect(window.location.href).toBe("/")
    })

    expect(global.fetch).toHaveBeenCalledWith(
      AUTH_ENDPOINTS.AUTHENTICATE,
      expect.objectContaining({
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ username: "testuser", password: "password123" })
      })
    )

    expect(global.fetch).toHaveBeenCalledWith(
      AUTH_ENDPOINTS.ACCESS,
      expect.objectContaining({
        method: "POST",
        credentials: "include",
      })
    )
  })

  test("handles 500 response without requesting an access token", async () => {
    global.fetch.mockResolvedValueOnce({
      ok: false,
      status: 500,
      json: vi.fn().mockResolvedValue({ errorMessage: "Unable to authenticate" })
    })

    render(<UserLoginForm />)

    fireEvent.change(screen.getByLabelText("Username:"), { target: { value: "testuser" } })
    fireEvent.change(screen.getByLabelText("Password:"), { target: { value: "password123" } })
    fireEvent.click(screen.getByRole("button", { name: "LOGIN" }))

    await waitFor(() => expect(screen.getByText("Unable to authenticate")).toBeInTheDocument())
    expect(global.fetch).toHaveBeenCalledTimes(1)
  })
})
