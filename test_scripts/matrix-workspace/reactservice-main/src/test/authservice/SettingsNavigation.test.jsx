

import { render, screen, fireEvent } from "@testing-library/react"
import "@testing-library/jest-dom"
import { vi } from "vitest"
import SettingsNavigation from "../../components/authservice/SettingsNavigation"

describe("SettingsNavigation component", () => {
  test("renders Account Settings button", () => {
    const mockSetSelectedSection = vi.fn()
    render(<SettingsNavigation setSelectedSection={mockSetSelectedSection} />)

    expect(screen.getByText("Account Settings")).toBeInTheDocument()
  })

  test("selects Account Settings when clicked and calls setSelectedSection", () => {
    const mockSetSelectedSection = vi.fn()
    render(<SettingsNavigation setSelectedSection={mockSetSelectedSection} />)

    const accountButton = screen.getByText("Account Settings")

    // Initially, should have "selected" class
    expect(accountButton).toHaveClass("selected")

    // Click again (re-select account)
    fireEvent.click(accountButton)

    // Parent callback should be called
    expect(mockSetSelectedSection).toHaveBeenCalledWith("account")
    expect(accountButton).toHaveClass("selected")
  })

  // Future-proof: Uncomment if Security Settings is enabled
//   test("selects Security Settings when clicked and calls setSelectedSection", () => {
//     const mockSetSelectedSection = vi.fn()
//     render(<SettingsNavigation setSelectedSection={mockSetSelectedSection} />)

//     const securityButton = screen.getByText("Security Settings")

//     fireEvent.click(securityButton)

//     expect(mockSetSelectedSection).toHaveBeenCalledWith("security")
//     expect(securityButton).toHaveClass("selected")
//   })
})
