

import { render, screen, fireEvent, waitFor, cleanup } from "@testing-library/react"
import "@testing-library/jest-dom"
import { vi } from "vitest"
import ImageViewer from "../../components/fileservice/ImageViewer"

// Mock useAuth
vi.mock("../../context/AuthContext", () => ({
  useAuth: () => ({
    getAccessToken: vi.fn(() => "mock-token"),
  }),
}))

describe("ImageViewer", () => {
  let mockBlobUrl
  
  beforeEach(() => {
    vi.clearAllMocks()

    mockBlobUrl = "blob:http://localhost/mock"
    global.URL.createObjectURL = vi.fn(() => mockBlobUrl)
    global.URL.revokeObjectURL = vi.fn()

    global.fetch = vi.fn(() =>
      Promise.resolve({
        blob: () => Promise.resolve(new Blob(["test"], { type: "image/png" })),
      })
    )
  })

  afterEach(() => {
    cleanup()
  })
  
  test("renders the image with given URL", async () => {
    render(<ImageViewer imageUrl="http://example.com/test.png" onClose={() => {}} />)

    const img = await screen.findByTestId("image")
    expect(img).toHaveAttribute("src", mockBlobUrl)
  })

  test("calls onClose when close button is clicked", () => {
    const onCloseMock = vi.fn()
    render(<ImageViewer imageUrl="http://example.com/test.png" onClose={onCloseMock} />)

    const button = screen.getByRole("button")
    fireEvent.click(button)

    expect(onCloseMock).toHaveBeenCalledTimes(1)
  })

  test("applies correct CSS classes", () => {
    render(<ImageViewer imageUrl="test.png" onClose={() => {}} />)

    expect(screen.getByRole("button").className).toContain("close-btn")
    expect(document.querySelector(".popup-overlay")).toBeInTheDocument()
    expect(document.querySelector(".popup-content")).toBeInTheDocument()
  })
})
