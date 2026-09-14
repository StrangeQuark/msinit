

import { render, screen } from "@testing-library/react"
import Register from "../../pages/Register"

vi.mock("../../components/Toolbar", () => ({
  default: () => <div data-testid="toolbar" />,
}))

vi.mock("../../components/authservice/UserRegisterForm", () => ({
  default: () => <div data-testid="register-form" />,
}))

test("renders Register page with Toolbar and UserRegisterForm", () => {
  render(<Register />)

  expect(screen.getByTestId("toolbar")).toBeInTheDocument()
  expect(screen.getByTestId("register-form")).toBeInTheDocument()
})
