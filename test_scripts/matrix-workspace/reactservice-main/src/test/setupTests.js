import "@testing-library/jest-dom"

// Silence React act() warnings (optional)
const originalError = console.error
beforeAll(() => {
  console.error = (...args) => {
    if (/Warning.*not wrapped in act/.test(args[0])) {
      return
    }
    originalError.call(console, ...args)
  }
})

afterAll(() => {
  console.error = originalError
})

// Mock window.location if needed
Object.defineProperty(window, "location", {
  value: { href: "" },
  writable: true,
})
