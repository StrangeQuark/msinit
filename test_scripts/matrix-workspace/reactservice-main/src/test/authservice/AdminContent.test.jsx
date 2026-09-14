

import { render, screen, fireEvent } from "@testing-library/react"
import "@testing-library/jest-dom"
import { vi } from "vitest"
import AdminContent from "../../components/authservice/AdminContent"

vi.mock("../../context/AuthContext", () => ({ useAuth: vi.fn() }))
vi.mock("../../components/authservice/InvitationManagement", () => ({ default: () => <div>Invitations content</div> }))
vi.mock("../../components/authservice/UserAdministration", () => ({ default: () => <div>Users content</div> }))
vi.mock("../../components/authservice/AuthorizationManagement", () => ({ default: () => <div>Authorizations content</div> }))
vi.mock("../../components/authservice/EmailTemplateManagement", () => ({ default: () => <div>Email templates content</div> }))

import { useAuth } from "../../context/AuthContext"

describe("AdminContent component", () => {
  test("shows only sections granted by authorization", () => {
    useAuth.mockReturnValue({ hasAuthorization: authorization => authorization === "INVITATION_MANAGEMENT" })

    render(<AdminContent />)

    expect(screen.getByText("Invitations")).toBeInTheDocument()
    expect(screen.queryByText("Users")).not.toBeInTheDocument()
    expect(screen.getByText("Invitations content")).toBeInTheDocument()
  })

  test("switches administration sections", () => {
    useAuth.mockReturnValue({ hasAuthorization: authorization => ["INVITATION_MANAGEMENT", "USER_MANAGEMENT"].includes(authorization) })

    render(<AdminContent />)
    fireEvent.click(screen.getByText("Users"))

    expect(screen.getByText("Users content")).toBeInTheDocument()
  })

  test("shows an access message without management authorization", () => {
    useAuth.mockReturnValue({ hasAuthorization: () => false })

    render(<AdminContent />)

    expect(screen.getByText("Admin access is required")).toBeInTheDocument()
  })
})
