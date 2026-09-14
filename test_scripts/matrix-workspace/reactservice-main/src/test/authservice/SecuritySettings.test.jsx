

import { render, screen } from "@testing-library/react"
import "@testing-library/jest-dom"
import SecuritySettings from "../../components/authservice/SecuritySettings"

describe("SecuritySettings component", () => {
  test("renders static content", () => {
    render(<SecuritySettings />)

    expect(screen.getByText("Security Settings Content")).toBeInTheDocument()
  })
})
