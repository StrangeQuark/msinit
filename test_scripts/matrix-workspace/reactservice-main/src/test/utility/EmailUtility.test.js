import { describe, it, expect } from "vitest"
import { verifyEmailRegex } from "../../utility/EmailUtility"

describe("verifyEmailRegex", () => {
  it("returns true for valid emails", () => {
    expect(verifyEmailRegex("test@example.com")).toBe(true)
    expect(verifyEmailRegex("user.name+tag+sorting@example.co.uk")).toBe(true)
    expect(verifyEmailRegex('"quoted"@example.com')).toBe(true)
  })

  it("returns false for invalid emails", () => {
    expect(verifyEmailRegex("plainaddress")).toBe(false)
    expect(verifyEmailRegex("@missingusername.com")).toBe(false)
    expect(verifyEmailRegex("username@.com")).toBe(false)
    expect(verifyEmailRegex("username@com")).toBe(false)
    expect(verifyEmailRegex("username@com.")).toBe(false)
  })

  it("returns false for non-string or empty input", () => {
    expect(verifyEmailRegex(null)).toBe(false)
    expect(verifyEmailRegex(undefined)).toBe(false)
    expect(verifyEmailRegex(123)).toBe(false)
    expect(verifyEmailRegex("")).toBe(false)
  })
})
