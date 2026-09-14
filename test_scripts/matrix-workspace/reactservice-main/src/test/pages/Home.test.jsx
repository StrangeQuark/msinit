import { render, screen } from "@testing-library/react"
import Home from "../../pages/Home"

vi.mock("../../components/Toolbar", () => ({
  default: () => <div data-testid="toolbar" />,
}))

test("renders Home page with Toolbar", () => {
  render(<Home />)

  expect(screen.getByTestId("toolbar")).toBeInTheDocument()
})
