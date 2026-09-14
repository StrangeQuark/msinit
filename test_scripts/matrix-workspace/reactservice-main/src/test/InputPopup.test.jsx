import { render, screen, fireEvent, waitFor } from "@testing-library/react"
import "@testing-library/jest-dom"
import { vi } from "vitest"
import InputPopup from "../components/InputPopup"

describe("InputPopup component", () => {
  const mockOnSubmit = vi.fn()
  const mockOnClose = vi.fn()

  const defaultInputs = [
    { name: "username", labelValue: "Username", placeholder: "Enter username" },
    { name: "password", type: "password", labelValue: "Password", placeholder: "Enter password" },
  ]

  beforeEach(() => {
    vi.clearAllMocks()
  })

  test("renders label and inputs", () => {
    render(
      <InputPopup
        label="Test Form"
        inputs={defaultInputs}
        onSubmit={mockOnSubmit}
        onClose={mockOnClose}
      />
    )

    expect(screen.getByText("Test Form")).toBeInTheDocument()
    expect(screen.getByPlaceholderText("Enter username")).toBeInTheDocument()
    expect(screen.getByPlaceholderText("Enter password")).toBeInTheDocument()
  })

  test("initializes with default values", () => {
    const inputsWithDefaults = [
      { name: "email", labelValue: "Email", defaultValue: "test@example.com", placeholder: "Enter email" },
    ]

    render(
      <InputPopup
        label="Defaults Test"
        inputs={inputsWithDefaults}
        onSubmit={mockOnSubmit}
        onClose={mockOnClose}
      />
    )

    const emailInput = screen.getByPlaceholderText("Enter email")
    expect(emailInput.value).toBe("test@example.com")
  })

  test("replaces default values on change", () => {
    const inputsWithDefaults = [
      { name: "email", labelValue: "Email", defaultValue: "test@example.com", placeholder: "Enter email" },
    ]

    render(
      <InputPopup
        label="Change Default Test"
        inputs={inputsWithDefaults}
        onSubmit={mockOnSubmit}
        onClose={mockOnClose}
      />
    )

    const emailInput = screen.getByPlaceholderText("Enter email")
    fireEvent.change(emailInput, { target: { value: "new@example.com" } })

    expect(emailInput.value).toBe("new@example.com")
  })

  test("updates form values on change", () => {
    render(
      <InputPopup
        label="Change Test"
        inputs={defaultInputs}
        onSubmit={mockOnSubmit}
        onClose={mockOnClose}
      />
    )

    const usernameInput = screen.getByPlaceholderText("Enter username")
    fireEvent.change(usernameInput, { target: { value: "newuser" } })
    expect(usernameInput.value).toBe("newuser")
  })

  test("calls onSubmit with form values and closes popup", async () => {
    render(
      <InputPopup
        label="Submit Test"
        inputs={defaultInputs}
        onSubmit={mockOnSubmit}
        onClose={mockOnClose}
      />
    )

    // Fill username
    const usernameInput = screen.getByPlaceholderText("Enter username")
    fireEvent.change(usernameInput, { target: { value: "submittedUser" } })

    // Click save
    fireEvent.click(screen.getByText("Save"))

    expect(mockOnSubmit).toHaveBeenCalledWith({
      username: "submittedUser",
      password: "",
    })
    await waitFor(() => expect(mockOnClose).toHaveBeenCalled())
  })

  test("does not close popup when submit fails", async () => {
    const failedSubmit = vi.fn().mockResolvedValue(false)

    render(
      <InputPopup
        label="Submit Test"
        inputs={defaultInputs}
        onSubmit={failedSubmit}
        onClose={mockOnClose}
      />
    )

    fireEvent.click(screen.getByText("Save"))

    await waitFor(() => expect(failedSubmit).toHaveBeenCalled())
    expect(mockOnClose).not.toHaveBeenCalled()
  })

  test("closes popup when Cancel is clicked", () => {
    render(
      <InputPopup
        label="Cancel Test"
        inputs={defaultInputs}
        onSubmit={mockOnSubmit}
        onClose={mockOnClose}
      />
    )

    fireEvent.click(screen.getByText("Cancel"))
    expect(mockOnClose).toHaveBeenCalled()
  })
})
