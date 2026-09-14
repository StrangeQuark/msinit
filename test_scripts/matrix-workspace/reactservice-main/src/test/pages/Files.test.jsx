

import { render, screen } from "@testing-library/react"
import Files from "../../pages/Files"

vi.mock("../../components/Toolbar", () => ({
  default: () => <div data-testid="toolbar" />,
}))

vi.mock("../../components/fileservice/FilesList", () => ({
  default: () => <div data-testid="files-list" />,
}))


vi.mock("../../context/AuthContext", () => ({
  RequireAuth: ({ children }) => <div data-testid="require-auth">{children}</div>,
}))

test("renders Files page with RequireAuth (If Auth integrated), Toolbar, and FilesList", () => {
  render(<Files />)

  expect(screen.getByTestId("require-auth")).toBeInTheDocument()
  expect(screen.getByTestId("toolbar")).toBeInTheDocument()
  expect(screen.getByTestId("files-list")).toBeInTheDocument()
})
