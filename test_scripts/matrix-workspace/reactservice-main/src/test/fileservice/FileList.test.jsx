

import { render, screen, fireEvent, waitFor } from "@testing-library/react"
import "@testing-library/jest-dom"
import { vi } from "vitest"
import FilesList from "../../components/fileservice/FilesList"

// Mock AuthContext
vi.mock("../../context/AuthContext", () => ({
  useAuth: () => ({
    getAccessToken: () => "mock-token"
  })
}))

describe("FilesList", () => {
  beforeEach(() => {
    vi.clearAllMocks()
    global.fetch = vi.fn()
    window.alert = vi.fn()
  })

  test("fetches and displays collections", async () => {
    global.fetch.mockResolvedValueOnce({
      json: async () => [{ name: "Collection A" }, { name: "Collection B" }],
    })

    render(<FilesList />)

    await waitFor(() => {
      expect(screen.getByText("Collection A")).toBeInTheDocument()
      expect(screen.getByText("Collection B")).toBeInTheDocument()
    })
  })

  test("filters collections by search term", async () => {
    global.fetch.mockResolvedValueOnce({
      json: async () => [{ name: "Music" }, { name: "Photos" }],
    })

    render(<FilesList />)

    await waitFor(() => {
      expect(screen.getByText("Music")).toBeInTheDocument()
      expect(screen.getByText("Photos")).toBeInTheDocument()
    })

    fireEvent.change(screen.getByPlaceholderText(/search collections/i), {
      target: { value: "music" },
    })

    await waitFor(() => {
      expect(screen.getByText("Music")).toBeInTheDocument()
      expect(screen.queryByText("Photos")).not.toBeInTheDocument()
    })
  })

  test("selecting a collection fetches files and role", async () => {
    // collections fetch
    global.fetch
      .mockResolvedValueOnce({
        json: async () => [{ name: "MyCollection" }],
      })
      // files fetch
      .mockResolvedValueOnce({
        json: async () => ["song.mp3", "movie.mp4"],
      })
      // role fetch
      .mockResolvedValueOnce({
        json: async () => "OWNER",
      })

    render(<FilesList />)

    const collection = await screen.findByText("MyCollection")
    fireEvent.click(collection)

    await waitFor(() => {
      expect(screen.getByText("song.mp3")).toBeInTheDocument()
      expect(screen.getByText("movie.mp4")).toBeInTheDocument()
    })
  })

  test("opens create collection popup", async () => {
    global.fetch.mockResolvedValueOnce({ json: async () => [] })

    render(<FilesList />)

    const button = screen.getByRole("button", { name: /create collection/i })
    fireEvent.click(button)

    expect(await screen.findByText(/create a new collection/i)).toBeInTheDocument()
  })

  test("shows upload button when user role is not READ", async () => {
    global.fetch
      .mockResolvedValueOnce({
        json: async () => [{ name: "UploadTest" }],
      })
      .mockResolvedValueOnce({
        json: async () => ["test.txt"],
      })
      .mockResolvedValueOnce({
        json: async () => "MANAGER",
      })

    render(<FilesList />)

    const collection = await screen.findByText("UploadTest")
    fireEvent.click(collection)

    await waitFor(() => {
      expect(screen.getByRole("button", { name: /upload/i })).toBeInTheDocument()
    })
  })

  test("does not remove file when deletion fails", async () => {
    global.fetch
      .mockResolvedValueOnce({ json: async () => [{ name: "MyCollection" }] })
      .mockResolvedValueOnce({ json: async () => ["test.txt"] })
      .mockResolvedValueOnce({ json: async () => "OWNER" })
      .mockResolvedValueOnce({ ok: false, status: 500, json: async () => ({ errorMessage: "Unable to delete file" }) })

    render(<FilesList />)

    fireEvent.click(await screen.findByText("MyCollection"))
    await screen.findByText("test.txt")
    fireEvent.click(screen.getByRole("button", { name: "Delete" }))

    await waitFor(() => expect(window.alert).toHaveBeenCalledWith("Unable to delete file"))
    expect(screen.getByText("test.txt")).toBeInTheDocument()
    expect(global.fetch).toHaveBeenCalledTimes(4)
  })
})
