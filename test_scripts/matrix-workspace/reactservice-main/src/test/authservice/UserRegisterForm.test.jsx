

import { render, screen, fireEvent, waitFor } from "@testing-library/react"
import "@testing-library/jest-dom"
import { vi } from "vitest"
import UserRegisterForm from "../../components/authservice/UserRegisterForm"
import * as EmailUtility from "../../utility/EmailUtility"

describe("UserRegisterForm component", () => {
  beforeEach(() => {
    vi.clearAllMocks()
    window.location.href = ""
    global.fetch = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ inviteOnly: false })
    })
    vi.spyOn(EmailUtility, "verifyEmailRegex").mockImplementation(() => true)
  })

  const renderRegisterForm = async () => {
    render(<UserRegisterForm />)
    await waitFor(() => {
      expect(screen.getByLabelText("Username:")).toBeInTheDocument()
    })
  }

  test("renders form fields and button", async () => {
    await renderRegisterForm()

    expect(screen.getByText("Create account")).toBeInTheDocument()
    expect(screen.getByLabelText("Username:")).toBeInTheDocument()
    expect(screen.getByLabelText("Email:")).toBeInTheDocument()
    expect(screen.getByLabelText("Password:")).toBeInTheDocument()
    expect(screen.getByLabelText("Confirm password:")).toBeInTheDocument()
    expect(screen.getByRole("button", { name: "SIGN UP" })).toBeInTheDocument()
  })

  test("shows validation errors when fields are empty", async () => {
    await renderRegisterForm()

    fireEvent.click(screen.getByRole("button", { name: "SIGN UP" }))

    await waitFor(() => {
      expect(screen.getByTitle("Username must not be blank")).toBeInTheDocument()
      expect(screen.getByTitle("Email must not be blank")).toBeInTheDocument()
      expect(screen.getByTitle("Password must not be blank")).toBeInTheDocument()
      expect(screen.getByTitle("Confirmation password must not be blank")).toBeInTheDocument()
    })
  })

  test("shows error when email is invalid", async () => {
    EmailUtility.verifyEmailRegex.mockReturnValueOnce(false)

    await renderRegisterForm()

    fireEvent.change(screen.getByLabelText("Username:"), { target: { value: "testuser" } })
    fireEvent.change(screen.getByLabelText("Email:"), { target: { value: "bademail" } })
    fireEvent.change(screen.getByLabelText("Password:"), { target: { value: "secret" } })
    fireEvent.change(screen.getByLabelText("Confirm password:"), { target: { value: "secret" } })

    fireEvent.click(screen.getByRole("button", { name: "SIGN UP" }))

    await waitFor(() => {
      expect(screen.getByTitle("Not a valid email")).toBeInTheDocument()
    })
  })

  test("shows error when passwords do not match", async () => {
    await renderRegisterForm()

    fireEvent.change(screen.getByLabelText("Username:"), { target: { value: "testuser" } })
    fireEvent.change(screen.getByLabelText("Email:"), { target: { value: "test@example.com" } })
    fireEvent.change(screen.getByLabelText("Password:"), { target: { value: "secret" } })
    fireEvent.change(screen.getByLabelText("Confirm password:"), { target: { value: "different" } })

    fireEvent.click(screen.getByRole("button", { name: "SIGN UP" }))

    await waitFor(() => {
      expect(screen.getByTitle("Passwords must match")).toBeInTheDocument()
    })
  })

  test("handles successful registration with email integration", async () => {
    global.fetch.mockResolvedValueOnce({
      ok: true,
      json: async () => ({ inviteOnly: false })
    }).mockResolvedValueOnce({
      ok: true,
      json: async () => ({ message: "" })
    })

    await renderRegisterForm()

    fireEvent.change(screen.getByLabelText("Username:"), { target: { value: "testuser" } })
    fireEvent.change(screen.getByLabelText("Email:"), { target: { value: "test@example.com" } })
    fireEvent.change(screen.getByLabelText("Password:"), { target: { value: "secret" } })
    fireEvent.change(screen.getByLabelText("Confirm password:"), { target: { value: "secret" } })

    fireEvent.click(screen.getByRole("button", { name: "SIGN UP" }))

    await waitFor(() => {
      expect(screen.getByText(/Thank you for signing up! An email has been sent to/i)).toBeInTheDocument()
    })
  })

  test("handles successful registration without email integration", async () => {
    global.fetch.mockResolvedValueOnce({
      ok: true,
      json: async () => ({ inviteOnly: false })
    }).mockResolvedValueOnce({
      ok: true,
      json: async () => ({ message: "Registered without email" })
    })

    await renderRegisterForm()

    fireEvent.change(screen.getByLabelText("Username:"), { target: { value: "testuser" } })
    fireEvent.change(screen.getByLabelText("Email:"), { target: { value: "test@example.com" } })
    fireEvent.change(screen.getByLabelText("Password:"), { target: { value: "secret" } })
    fireEvent.change(screen.getByLabelText("Confirm password:"), { target: { value: "secret" } })

    fireEvent.click(screen.getByRole("button", { name: "SIGN UP" }))

    await waitFor(() => {
      expect(screen.getByText(/Thank you for signing up!/i)).toBeInTheDocument()
      expect(screen.queryByText(/An email has been sent to/i)).not.toBeInTheDocument()
    })
  })

  test("handles 400 conflict: username already taken", async () => {
    global.fetch.mockResolvedValueOnce({
      ok: true,
      json: async () => ({ inviteOnly: false })
    }).mockResolvedValueOnce({
      ok: false,
      status: 400,
      json: async () => ({ errorMessage: "Username already registered" })
    })

    await renderRegisterForm()

    fireEvent.change(screen.getByLabelText("Username:"), { target: { value: "takenuser" } })
    fireEvent.change(screen.getByLabelText("Email:"), { target: { value: "test@example.com" } })
    fireEvent.change(screen.getByLabelText("Password:"), { target: { value: "secret" } })
    fireEvent.change(screen.getByLabelText("Confirm password:"), { target: { value: "secret" } })

    fireEvent.click(screen.getByRole("button", { name: "SIGN UP" }))

    await waitFor(() => {
      expect(screen.getByTitle("Username is already taken")).toBeInTheDocument()
    })
  })

  test("handles 400 conflict: email already taken", async () => {
    global.fetch.mockResolvedValueOnce({
      ok: true,
      json: async () => ({ inviteOnly: false })
    }).mockResolvedValueOnce({
      ok: false,
      status: 400,
      json: async () => ({ errorMessage: "Email already registered" })
    })

    await renderRegisterForm()

    fireEvent.change(screen.getByLabelText("Username:"), { target: { value: "testuser" } })
    fireEvent.change(screen.getByLabelText("Email:"), { target: { value: "used@example.com" } })
    fireEvent.change(screen.getByLabelText("Password:"), { target: { value: "secret" } })
    fireEvent.change(screen.getByLabelText("Confirm password:"), { target: { value: "secret" } })

    fireEvent.click(screen.getByRole("button", { name: "SIGN UP" }))

    await waitFor(() => {
      expect(screen.getByTitle("Email is already taken")).toBeInTheDocument()
    })
  })

  test("includes invitation token from URL fragment", async () => {
    window.location.href = "/register#inviteToken=invite-token"
    global.fetch.mockResolvedValueOnce({
      ok: true,
      json: async () => ({ inviteOnly: true })
    }).mockResolvedValueOnce({
      ok: true,
      json: async () => ({ message: "" })
    })

    render(<UserRegisterForm />)

    await waitFor(() => {
      expect(screen.getByLabelText("Invitation code:")).toHaveValue("invite-token")
    })

    fireEvent.change(screen.getByLabelText("Username:"), { target: { value: "testuser" } })
    fireEvent.change(screen.getByLabelText("Email:"), { target: { value: "test@example.com" } })
    fireEvent.change(screen.getByLabelText("Password:"), { target: { value: "secret" } })
    fireEvent.change(screen.getByLabelText("Confirm password:"), { target: { value: "secret" } })
    fireEvent.click(screen.getByRole("button", { name: "SIGN UP" }))

    await waitFor(() => {
      expect(global.fetch).toHaveBeenLastCalledWith(expect.any(String), expect.objectContaining({
        body: expect.stringContaining("invite-token")
      }))
    })
  })
})
