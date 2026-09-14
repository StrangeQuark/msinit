


import { render, screen, waitFor } from "@testing-library/react"
import "@testing-library/jest-dom"
import { vi } from "vitest"
import ConfirmEmailMessage from "../../components/authservice/ConfirmEmailMessage"

describe("ConfirmEmailMessage", () => {
  beforeEach(() => {
    vi.clearAllMocks()
    global.fetch = vi.fn()
  })

  test("fetches message and displays it", async () => {
    const mockMessage = "Email confirmed successfully"

    global.fetch.mockResolvedValueOnce({
      json: async () => ({ message: mockMessage }),
    })

    render(<ConfirmEmailMessage />)

    await waitFor(() => {
      expect(screen.getByText(mockMessage)).toBeInTheDocument()
    })
  })
})
