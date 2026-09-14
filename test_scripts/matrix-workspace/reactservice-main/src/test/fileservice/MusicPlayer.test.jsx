

import { render, screen, fireEvent, waitFor, cleanup } from "@testing-library/react"
import "@testing-library/jest-dom"
import { vi } from "vitest"
import MusicPlayer from "../../components/fileservice/MusicPlayer"

// Mock useAuth
vi.mock("../../context/AuthContext", () => ({
  useAuth: () => ({
    getAccessToken: vi.fn(() => "mock-token"),
  }),
}))

describe("MusicPlayer", () => {
  let mockBlobUrl
  
  beforeEach(() => {
    vi.clearAllMocks()

    mockBlobUrl = "blob:http://localhost/mock"
    global.URL.createObjectURL = vi.fn(() => mockBlobUrl)
    global.URL.revokeObjectURL = vi.fn()

    global.fetch = vi.fn(() =>
      Promise.resolve({
        blob: () => Promise.resolve(new Blob(["test"], { type: "audio/mpeg" })),
      })
    )
  })

  afterEach(() => {
    cleanup()
  })
  
  test("renders audio element with given URL", async () => {
    render(<MusicPlayer audioUrl="http://example.com/test.mp3" onClose={() => {}} />)

    await waitFor(() =>
      expect(screen.getByTestId("audio-source")).toHaveAttribute("src", mockBlobUrl)
    )
  })

  test("sets correct MIME type for mp3", () => {
    render(<MusicPlayer audioUrl="http://example.com/test.mp3" onClose={() => {}} />)

    const source = screen.getByTestId("audio-player").querySelector("source")
    expect(source).toHaveAttribute("type", "audio/mpeg")
  })

  test("sets correct MIME type for wav", () => {
    render(<MusicPlayer audioUrl="http://example.com/test.wav" onClose={() => {}} />)

    const source = screen.getByTestId("audio-player").querySelector("source")
    expect(source).toHaveAttribute("type", "audio/wav")
  })

  test("calls onClose when close button is clicked", () => {
    const onCloseMock = vi.fn()
    render(<MusicPlayer audioUrl="http://example.com/test.mp3" onClose={onCloseMock} />)

    const button = screen.getByRole("button")
    fireEvent.click(button)

    expect(onCloseMock).toHaveBeenCalledTimes(1)
  })

  test("applies correct CSS classes", () => {
    render(<MusicPlayer audioUrl="http://example.com/test.mp3" onClose={() => {}} />)

    expect(screen.getByRole("button").className).toContain("close-btn")
    expect(document.querySelector(".popup-overlay")).toBeInTheDocument()
    expect(document.querySelector(".popup-content")).toBeInTheDocument()
  })
})
