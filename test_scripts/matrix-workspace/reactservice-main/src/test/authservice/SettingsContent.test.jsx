

import { render, screen, fireEvent } from "@testing-library/react"
import "@testing-library/jest-dom"
import SettingsContent from "../../components/authservice/SettingsContent"

// Mock children components so we can detect them easily
vi.mock("../../components/authservice/AccountSettings", () => ({
  default: () => <div data-testid="account-settings">Mock AccountSettings</div>,
}))

vi.mock("../../components/authservice/SecuritySettings", () => ({
  default: () => <div data-testid="security-settings">Mock SecuritySettings</div>,
}))

// Mock navigation to call setSelectedSection directly
vi.mock("../../components/authservice/SettingsNavigation", () => ({
  default: ({ setSelectedSection }) => (
    <div>
      <button data-testid="nav-account" onClick={() => setSelectedSection("account")}>
        Account Nav
      </button>
      <button data-testid="nav-security" onClick={() => setSelectedSection("security")}>
        Security Nav
      </button>
    </div>
  ),
}))

describe("SettingsContent component", () => {
  test("renders AccountSettings by default", () => {
    render(<SettingsContent />)

    expect(screen.getByTestId("account-settings")).toBeInTheDocument()
    expect(screen.queryByTestId("security-settings")).not.toBeInTheDocument()
  })

  test("switches to SecuritySettings when navigation clicked", () => {
    render(<SettingsContent />)

    fireEvent.click(screen.getByTestId("nav-security"))

    expect(screen.getByTestId("security-settings")).toBeInTheDocument()
    expect(screen.queryByTestId("account-settings")).not.toBeInTheDocument()
  })

  test("switches back to AccountSettings when navigation clicked again", () => {
    render(<SettingsContent />)

    fireEvent.click(screen.getByTestId("nav-security"))
    expect(screen.getByTestId("security-settings")).toBeInTheDocument()

    fireEvent.click(screen.getByTestId("nav-account"))
    expect(screen.getByTestId("account-settings")).toBeInTheDocument()
  })
})
