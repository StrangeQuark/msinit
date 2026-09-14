

import { render, screen, fireEvent, waitFor } from "@testing-library/react"
import "@testing-library/jest-dom"
import { vi } from "vitest"
import AccountSettings from "../../components/authservice/AccountSettings"
import { useAuth } from "../../context/AuthContext"
import { AUTH_ENDPOINTS } from "../../config"

vi.mock("../../context/AuthContext")

describe("AccountSettings component", () => {
  const mockLogout = vi.fn()
  const mockGetAccessToken = vi.fn(() => "mock-token")
  const mockRefreshAccessToken = vi.fn(async () => true)

  beforeEach(() => {
    vi.clearAllMocks()
    window.alert = vi.fn()
    useAuth.mockReturnValue({
      username: "testuser",
      getAccessToken: mockGetAccessToken,
      refreshAccessToken: mockRefreshAccessToken,
      logout: mockLogout,
    })
    global.fetch = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ([{ email: "test@example.com" }])
    })
  })

  test("renders username and fetches email", async () => {
    fetch
      .mockResolvedValueOnce({ json: async () => ({ id: 1 }) }) // GET_USER_ID
      .mockResolvedValueOnce({ json: async () => [{ email: "test@example.com" }] }) // GET_USER_DETAILS_BY_IDS

    render(<AccountSettings />)

    expect(screen.getByTestId("username")).toHaveTextContent("testuser")
    
    await waitFor(() => {
      expect(screen.getByTestId("email")).toHaveTextContent("test@example.com")
    })
  })

  test("opens username popup when pencil clicked", async () => {
    fetch.mockResolvedValueOnce({ json: async () => ({ id: 1 }) })
    fetch.mockResolvedValueOnce({ json: async () => [{ email: "test@example.com" }] })

    render(<AccountSettings />)

    fireEvent.click(screen.getByTestId("update-username"))

    expect(await screen.findByText("Edit username")).toBeInTheDocument()
  })

  test("opens email popup when pencil clicked", async () => {
    fetch.mockResolvedValueOnce({ json: async () => ({ id: 1 }) })
    fetch.mockResolvedValueOnce({ json: async () => [{ email: "test@example.com" }] })

    render(<AccountSettings />)

    fireEvent.click(screen.getByTestId("update-email"))

    expect(await screen.findByText("Edit email")).toBeInTheDocument()
  })

  test("opens password popup when button clicked", async () => {
    fetch.mockResolvedValueOnce({ json: async () => ({ id: 1 }) })
    fetch.mockResolvedValueOnce({ json: async () => [{ email: "test@example.com" }] })

    render(<AccountSettings />)

    fireEvent.click(screen.getByTestId("update-password"))

    expect(await screen.findByText("Edit password")).toBeInTheDocument()
  })

  test("opens delete popup when button clicked", async () => {
    fetch.mockResolvedValueOnce({ json: async () => ({ id: 1 }) })
    fetch.mockResolvedValueOnce({ json: async () => [{ email: "test@example.com" }] })

    render(<AccountSettings />)

    fireEvent.click(screen.getByTestId("delete-account-button"))

    expect(await screen.findByText("Delete account")).toBeInTheDocument()
  })

  test("updateUsername calls API and refreshes access on success via popup", async () => {
    // Mock fetch for initial user data
    fetch
      .mockResolvedValueOnce({ json: async () => ({ id: 1 }) }) // GET_USER_ID
      .mockResolvedValueOnce({ json: async () => [{ email: "test@example.com" }] }) // GET_USER_DETAILS_BY_IDS
      .mockResolvedValueOnce({ ok: true, json: async () => ([{ email: "test@example.com" }]) })

    render(<AccountSettings />)

    await waitFor(() => {
      expect(screen.getByTestId("email")).toHaveTextContent("test@example.com")
    })

    // Open username popup
    fireEvent.click(await screen.getByTestId("update-username"))

    // Fill in new username and password
    fireEvent.change(screen.getByLabelText("New username"), { target: { value: "newuser" } })
    fireEvent.change(screen.getByLabelText("Password"), { target: { value: "password123" } })

    // Click Save
    fireEvent.click(screen.getByText("Save"))

    await waitFor(() => {
      expect(fetch).toHaveBeenCalledWith(
        AUTH_ENDPOINTS.UPDATE_USERNAME,
        expect.objectContaining({
          method: "POST",
          body: JSON.stringify({ newUsername: "newuser", password: "password123" }),
        })
      )
      expect(mockRefreshAccessToken).toHaveBeenCalled()
      expect(mockLogout).not.toHaveBeenCalled()
    })
  })


  test("updateEmail sets email on success", async () => {
    fetch
      .mockResolvedValueOnce({ json: async () => ({ id: 1 }) }) // GET_USER_ID
      .mockResolvedValueOnce({ json: async () => [{ email: "test@example.com" }] }) // GET_USER_DETAILS_BY_IDS
      .mockResolvedValueOnce({ ok: true, json: async () => ([{ email: "test123@example.com" }]) })

    render(<AccountSettings />)

    // Open email popup
    fireEvent.click(await screen.getByTestId("update-email"))

    // Fill in new email and password
    fireEvent.change(screen.getByLabelText("New email"), { target: { value: "test123@example.com" } })
    fireEvent.change(screen.getByLabelText("Password"), { target: { value: "password123" } })

    // Click Save
    fireEvent.click(screen.getByText("Save"))

    await waitFor(() => {
            expect(fetch).toHaveBeenCalledWith(
                AUTH_ENDPOINTS.UPDATE_EMAIL,
                expect.objectContaining({
                    method: "POST",
                    body: JSON.stringify({ newEmail: "test123@example.com", password: "password123" }),
                })
            )
        })

    expect(screen.getByTestId("username")).toHaveTextContent("testuser")
    
    await waitFor(() => {
      expect(screen.getByTestId("email")).toHaveTextContent("test123@example.com")
    })
  })

  test("updatePassword calls API and refreshes access on success", async () => {
    fetch
      .mockResolvedValueOnce({ json: async () => ({ id: 1 }) }) // GET_USER_ID
      .mockResolvedValueOnce({ json: async () => [{ email: "test@example.com" }] }) // GET_USER_DETAILS_BY_IDS
      .mockResolvedValueOnce({ ok: true, json: async () => ([{ email: "test@example.com" }]) })

    render(<AccountSettings />)

    await waitFor(() => {
      expect(screen.getByTestId("email")).toHaveTextContent("test@example.com")
    })

    // Open password popup
    fireEvent.click(await screen.getByTestId("update-password"))

    // Fill in new password and existing password
    fireEvent.change(screen.getByLabelText("Current password"), { target: { value: "password123" } })
    fireEvent.change(screen.getByLabelText("New password"), { target: { value: "password1234" } })

    // Click Save
    fireEvent.click(screen.getByText("Save"))

    await waitFor(() => {
      expect(fetch).toHaveBeenCalledWith(
        AUTH_ENDPOINTS.UPDATE_PASSWORD,
        expect.objectContaining({
          method: "POST",
          body: JSON.stringify({ password: "password123", newPassword: "password1234" }),
        })
      )
      expect(mockRefreshAccessToken).toHaveBeenCalled()
      expect(mockLogout).not.toHaveBeenCalled()
    })
  })

  test("keeps username popup open when update fails", async () => {
    fetch
      .mockResolvedValueOnce({ json: async () => ({ id: 1 }) })
      .mockResolvedValueOnce({ json: async () => [{ email: "test@example.com" }] })
      .mockResolvedValueOnce({ ok: false, status: 400, json: async () => ({ errorMessage: "Invalid password" }) })

    render(<AccountSettings />)

    await waitFor(() => expect(screen.getByTestId("email")).toHaveTextContent("test@example.com"))
    fireEvent.click(await screen.getByTestId("update-username"))
    fireEvent.change(screen.getByLabelText("New username"), { target: { value: "newuser" } })
    fireEvent.change(screen.getByLabelText("Password"), { target: { value: "password123" } })
    fireEvent.click(screen.getByText("Save"))

    await waitFor(() => expect(window.alert).toHaveBeenCalledWith("Invalid password"))
    expect(screen.getByText("Edit username")).toBeInTheDocument()
    expect(mockRefreshAccessToken).not.toHaveBeenCalled()
  })

  test("deleteProfile calls API and logs out on success", async () => {
    fetch
      .mockResolvedValueOnce({ json: async () => ({ id: 1 }) }) // GET_USER_ID
      .mockResolvedValueOnce({ json: async () => [{ email: "test@example.com" }] }) // GET_USER_DETAILS_BY_IDS
      .mockResolvedValueOnce({ json: async () => ([{ email: "test@example.com" }]) })

    render(<AccountSettings />)

    // Open password popup
    fireEvent.click(await screen.getByTestId("delete-account-button"))

    // Fill in new password and existing password
    fireEvent.change(screen.getByLabelText("Username"), { target: { value: "testuser" } })
    fireEvent.change(screen.getByLabelText("Password"), { target: { value: "password123" } })

    // Click Save
    fireEvent.click(screen.getByText("Save"))

    await waitFor(() => {
      expect(fetch).toHaveBeenCalledWith(
        AUTH_ENDPOINTS.DELETE_USER,
        expect.objectContaining({
          method: "POST",
          body: JSON.stringify({ username: "testuser", password: "password123" }),
        })
      )
    })
  })
})
