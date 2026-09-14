


import { render, screen } from "@testing-library/react"
import ResetPasswordSearch from "../../pages/ResetPasswordSearch"

vi.mock("../../components/Toolbar", () => ({
  default: () => <div data-testid="toolbar" />,
}))

vi.mock("../../components/authservice/ResetPasswordSearchForm", () => ({
  default: () => <div data-testid="reset-password-search-form" />,
}))

test("renders ResetPasswordSearch page with Toolbar and ResetPasswordSearchForm", () => {
  render(<ResetPasswordSearch />)

  expect(screen.getByTestId("toolbar")).toBeInTheDocument()
  expect(screen.getByTestId("reset-password-search-form")).toBeInTheDocument()
})
