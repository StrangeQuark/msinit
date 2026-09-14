

import { render, screen, fireEvent, waitFor } from "@testing-library/react"
import "@testing-library/jest-dom"
import { vi } from "vitest"
import UserManagementPopup from "../../components/authservice/UserManagementPopup"
import { useAuth } from "../../context/AuthContext"

vi.mock("../../context/AuthContext")

describe("UserManagementPopup", () => {
  const mockOnClose = vi.fn()
  const mockLoadUsers = vi.fn()
  const mockAddUser = vi.fn()
  const mockDeleteUser = vi.fn()
  const mockGetAllRoles = vi.fn()
  const mockUpdateUserRole = vi.fn()
  const mockGetAccessToken = vi.fn(() => "mock-token")

  beforeEach(() => {
    vi.clearAllMocks()
    useAuth.mockReturnValue({ getAccessToken: mockGetAccessToken })

    global.fetch = vi.fn()
    mockLoadUsers.mockResolvedValue([
      { userId: "1", username: "alice", role: "USER" },
      { userId: "2", username: "bob", role: "ADMIN" },
    ])
    mockGetAllRoles.mockResolvedValue(["USER", "ADMIN"])
  })

  const setup = async () => {
    render(
      <UserManagementPopup
        onClose={mockOnClose}
        loadUsers={mockLoadUsers}
        addUser={mockAddUser}
        deleteUser={mockDeleteUser}
        getAllRoles={mockGetAllRoles}
        updateUserRole={mockUpdateUserRole}
      />
    )

    // Wait for useEffect to load users
    await waitFor(() => expect(mockLoadUsers).toHaveBeenCalled())
  }

  test("renders popup with header and close button", async () => {
    global.fetch.mockResolvedValueOnce({
      ok: true,
      json: async () => [{ userId: "1", username: "alice", email: "alice@test.com" }],
    })

    await setup()
    expect(screen.getByText("Manage Users")).toBeInTheDocument()

    fireEvent.click(screen.getByTestId("close-button"))
    expect(mockOnClose).toHaveBeenCalled()
  })

  test("fetches and displays users in table", async () => {
    global.fetch.mockResolvedValueOnce({
      ok: true,
      json: async () => [
        { userId: "1", username: "alice", email: "alice@test.com", role: "ADMIN" },
        { userId: "2", username: "bob", email: "bob@test.com" },
      ],
    })

    await setup()

    expect(await screen.findByText("alice")).toBeInTheDocument()
    expect(await screen.findByText("bob")).toBeInTheDocument()
    expect(await screen.findByText("USER")).toBeInTheDocument()
  })

  test("search users and add user from results", async () => {
    global.fetch.mockResolvedValueOnce({ ok: true, json: async () => [] }) // for fetchUsers
    global.fetch.mockResolvedValueOnce({
      ok: true,
      json: async () => ({ userId: "1", username: "charlie", email: "c@test.com" }),
    })
    global.fetch.mockResolvedValueOnce({
      ok: true,
      json: async () => [{ userId: "1", username: "charlie", email: "c@test.com" }],
    })

    await setup()

    fireEvent.change(screen.getByPlaceholderText("Search users..."), {
      target: { value: "charlie" },
    })
    fireEvent.click(screen.getByText("Search"))

    expect(await screen.findByText("charlie (c@test.com)")).toBeInTheDocument()

    fireEvent.click(screen.getByText("charlie (c@test.com)"))
    expect(mockAddUser).toHaveBeenCalled()
  })

  test("start editing user role and save", async () => {
    global.fetch.mockResolvedValueOnce({
      ok: true,
      json: async () => [
        { userId: "1", username: "alice", email: "alice@test.com", role: "USER" },
        { userId: "2", username: "bob", email: "bob@test.com", role: "ADMIN" },
      ],
    })
    global.fetch.mockResolvedValueOnce({
      ok: true,
      json: async () => [
        { userId: "1", username: "alice", email: "alice@test.com", role: "USER" },
        { userId: "2", username: "bob", email: "bob@test.com", role: "ADMIN" },
      ],
    })

    await setup()

    fireEvent.click(screen.getAllByTestId("edit-user-role-button")[0])

    const select = screen.getByDisplayValue("USER")
    fireEvent.change(select, { target: { value: "ADMIN" } })

    fireEvent.click(screen.getByText("Save"))
    expect(mockUpdateUserRole).toHaveBeenCalledWith("alice", "ADMIN")
  })

  test("delete user confirmation cancel", async () => {
    global.fetch.mockResolvedValueOnce({
      ok: true,
      json: async () => [{ userId: "1", username: "alice", email: "alice@test.com" }],
    })

    window.confirm = vi.fn(() => false) // cancel deletion
    await setup()

    fireEvent.click(screen.getAllByTestId("delete-user-button")[0])
    expect(mockDeleteUser).not.toHaveBeenCalled()
  })

  test("delete user confirmation accept", async () => {
    global.fetch.mockResolvedValueOnce({
      ok: true,
      json: async () => [{ userId: "1", username: "alice", email: "alice@test.com" }],
    })
    global.fetch.mockResolvedValueOnce({
      ok: true,
      json: async () => [],
    })

    window.confirm = vi.fn(() => true) // accept deletion
    await setup()

    fireEvent.click(screen.getAllByTestId("delete-user-button")[0])
    expect(mockDeleteUser).toHaveBeenCalled()
  })

  test("paginates through users", async () => {
    // Generate 15 users
    mockLoadUsers.mockResolvedValue(
      Array.from({ length: 15 }, (_, i) => ({ userId: String(i), username: `user${i}`, role: "USER" }))
    )

    global.fetch.mockResolvedValueOnce({
      ok: true,
      json: async () =>
        Array.from({ length: 15 }, (_, i) => ({
          userId: String(i),
          username: `user${i}`,
          email: `u${i}@t.com`,
        })),
    })

    await setup()

    expect(screen.getByText("user0")).toBeInTheDocument()
    fireEvent.click(screen.getByText("2")) // go to page 2
    expect(await screen.findByText("user10")).toBeInTheDocument()
  })
})
