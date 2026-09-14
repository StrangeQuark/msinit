


import { render, screen } from "@testing-library/react"
import ResetPassword from "../../pages/ResetPassword"

vi.mock("../../components/Toolbar", () => ({
  default: () => <div data-testid="toolbar" />,
}))

vi.mock("../../components/authservice/ResetPasswordForm", () => ({
  default: () => <div data-testid="reset-password-form" />,
}))

test("renders ResetPassword page with Toolbar and ResetPasswordForm", () => {
  render(<ResetPassword />)

  expect(screen.getByTestId("toolbar")).toBeInTheDocument()
  expect(screen.getByTestId("reset-password-form")).toBeInTheDocument()
})
