

import { render, screen } from "@testing-library/react"
import UserProfile from "../../pages/UserProfile"

vi.mock("../../components/Toolbar", () => ({
  default: () => <div data-testid="toolbar" />,
}))

test("renders UserProfile page with Toolbar", () => {
  render(<UserProfile />)

  expect(screen.getByTestId("toolbar")).toBeInTheDocument()
})
