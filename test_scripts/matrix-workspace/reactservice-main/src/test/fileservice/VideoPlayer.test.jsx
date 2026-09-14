

import { render, screen, fireEvent, cleanup } from "@testing-library/react"
import "@testing-library/jest-dom"
import { vi } from "vitest"
import VideoPlayer from "../../components/fileservice/VideoPlayer"

describe("VideoPlayer", () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  afterEach(() => {
    cleanup()
  })

  test("passes the stream url directly to the video tag", async () => {
    render(<VideoPlayer videoUrl="/video/test" onClose={() => {}} />)

    const video = await screen.findByTestId("video-player")
    expect(video).toHaveAttribute("src", "/video/test")
    expect(video).toHaveAttribute("crossorigin", "use-credentials")
  })

  test("calls onClose when close button clicked", async () => {
    const onCloseMock = vi.fn()
    render(<VideoPlayer videoUrl="/video/test" onClose={onCloseMock} />)

    const button = screen.getByRole("button", { name: /x/i })
    fireEvent.click(button)

    expect(onCloseMock).toHaveBeenCalledTimes(1)
  })

  test("applies correct CSS classes", async () => {
    render(<VideoPlayer videoUrl="/video/test" onClose={() => {}} />)

    expect(document.querySelector(".popup-overlay")).toBeInTheDocument()
    expect(document.querySelector(".popup-content")).toBeInTheDocument()
    expect(await screen.findByTestId("video-player")).toHaveClass("video-player")
  })
})
