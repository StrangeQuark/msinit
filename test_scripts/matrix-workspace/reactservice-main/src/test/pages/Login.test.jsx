

import { render, screen } from "@testing-library/react"
import Login from "../../pages/Login"

vi.mock("../../components/Toolbar", () => ({
  default: () => <div data-testid="toolbar" />,
}))

vi.mock("../../components/authservice/UserLoginForm", () => ({
  default: () => <div data-testid="login-form" />,
}))

test("renders Login page with Toolbar and UserLoginForm", () => {
  render(<Login />)

  expect(screen.getByTestId("toolbar")).toBeInTheDocument()
  expect(screen.getByTestId("login-form")).toBeInTheDocument()
})
