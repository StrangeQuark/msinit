

import { render, screen } from "@testing-library/react"
import Settings from "../../pages/Settings"

vi.mock("../../components/Toolbar", () => ({
  default: () => <div data-testid="toolbar" />,
}))

vi.mock("../../components/authservice/SettingsContent", () => ({
  default: () => <div data-testid="settings-content" />,
}))

vi.mock("../../context/AuthContext", () => ({
  RequireAuth: ({ children }) => <div data-testid="require-auth">{children}</div>,
}))

test("renders Settings page with RequireAuth, Toolbar, and SettingsContent", () => {
  render(<Settings />)

  expect(screen.getByTestId("require-auth")).toBeInTheDocument()
  expect(screen.getByTestId("toolbar")).toBeInTheDocument()
  expect(screen.getByTestId("settings-content")).toBeInTheDocument()
})
