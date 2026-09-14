


import { render, screen } from "@testing-library/react"
import ConfirmEmail from "../../pages/ConfirmEmail"

vi.mock("../../components/Toolbar", () => ({
  default: () => <div data-testid="toolbar" />,
}))

vi.mock("../../components/authservice/ConfirmEmailMessage", () => ({
  default: () => <div data-testid="confirm-email-message" />,
}))

test("renders ConfirmEmail page with Toolbar and ConfirmEmailMessage", () => {
  render(<ConfirmEmail />)

  expect(screen.getByTestId("toolbar")).toBeInTheDocument()
  expect(screen.getByTestId("confirm-email-message")).toBeInTheDocument()
})
