


import { render, screen, fireEvent, waitFor } from "@testing-library/react"
import "@testing-library/jest-dom"
import { vi } from "vitest"
import ResetPasswordForm from "../../components/authservice/ResetPasswordForm"
import { EMAIL_ENDPOINTS } from "../../config"

describe("ResetPasswordForm", () => {
  beforeEach(() => {
    vi.clearAllMocks()
    global.fetch = vi.fn()

    window.location = { search: "?token=abc123" }
  })

  test("shows error if passwords do not match", () => {
    render(<ResetPasswordForm />)

    fireEvent.change(screen.getByLabelText("New password:"), {
      target: { value: "password1" },
    })
    fireEvent.change(screen.getByLabelText("Confirm password:"), {
      target: { value: "password2" },
    })

    fireEvent.click(screen.getByText("SUBMIT"))

    expect(
      screen.getByText("Passwords do not match")
    ).toBeInTheDocument()
  })

  test("submits and shows success on valid response", async () => {
    global.fetch.mockResolvedValueOnce({
      ok: true,
      json: async () => ({ message: "ok" }),
    })

    render(<ResetPasswordForm />)

    fireEvent.change(screen.getByLabelText("New password:"), {
      target: { value: "password123" },
    })
    fireEvent.change(screen.getByLabelText("Confirm password:"), {
      target: { value: "password123" },
    })

    fireEvent.click(screen.getByText("SUBMIT"))

    await waitFor(() => {
      expect(
        screen.getByText("Your password has been successfully reset")
      ).toBeInTheDocument()
    })

    expect(fetch).toHaveBeenCalledWith(
      EMAIL_ENDPOINTS.RESET_USER_PASSWORD,
      expect.objectContaining({
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ token: "abc123", newPassword: "password123" })
      })
    )
  })

  test("shows server error message on failed response", async () => {
    global.fetch.mockResolvedValueOnce({
      ok: false,
      json: async () => ({ message: "Invalid token" }),
    })

    render(<ResetPasswordForm />)

    fireEvent.change(screen.getByLabelText("New password:"), {
      target: { value: "password123" },
    })
    fireEvent.change(screen.getByLabelText("Confirm password:"), {
      target: { value: "password123" },
    })

    fireEvent.click(screen.getByText("SUBMIT"))

    await waitFor(() => {
      expect(screen.getByText("Invalid token")).toBeInTheDocument()
    })
  })

  test("does nothing if password field is empty", () => {
    render(<ResetPasswordForm />)

    fireEvent.click(screen.getByText("SUBMIT"))

    expect(fetch).not.toHaveBeenCalled()
  })
})
