

import { useEffect, useState } from "react"
import { FaRegCircleXmark } from "react-icons/fa6"
import { FaCheckCircle } from "react-icons/fa"
import { verifyEmailRegex } from "../../utility/EmailUtility"
import { AUTH_ENDPOINTS } from "../../config";
import "./css/UserRegisterForm.css"

const UserRegisterForm = () => {
  const[username, setUsername] = useState('')
  const[isUsernameValid, setIsUsernameValid] = useState(true)

  const[email, setEmail] = useState('')
  const[isEmailValid, setIsEmailValid] = useState(true)

  const[password, setPassword] = useState('')
  const[isPasswordValid, setIsPasswordValid] = useState(true)

  const[confirmPassword, setConfirmPassword] = useState('')
  const[isConfirmPasswordValid, setIsConfirmPasswordValid] = useState(true)

  const[isSubmitted, setIsSubmitted] = useState(false)
  const[isSuccess, setIsSuccess] = useState(false)
  const[registrationMessage, setRegistrationMessage] = useState("")
  const[isInviteOnly, setIsInviteOnly] = useState(null)
  const[inviteToken, setInviteToken] = useState("")
  const[inviteErrorMessage, setInviteErrorMessage] = useState("")

  const[usernameErrorMessage, setUsernameErrorMessage] = useState("")
  const[emailErrorMessage, setEmailErrorMessage] = useState("")
  const[passwordErrorMessage, setPasswordErrorMessage] = useState("")
  const[confirmPasswordErrorMessage, setConfirmPasswordErrorMessage] = useState("")

  useEffect(() => {
    const hash = window.location.hash || window.location.href.substring(window.location.href.indexOf("#"))
    const inviteToken = new URLSearchParams(hash.substring(1)).get("inviteToken")
    if(inviteToken) {
      setInviteToken(inviteToken)
      window.history.replaceState({}, "", "/register")
    }

    fetch(AUTH_ENDPOINTS.INVITE_ONLY)
      .then(async (response) => {
        if(!response.ok)
          throw new Error("Unable to load invitation settings")

        const data = await response.json()
        setIsInviteOnly(data.inviteOnly)
      })
      .catch(() => setInviteErrorMessage("Unable to load registration settings"))
  }, [])

  const handleKeyDown = (event) => {
      if (event.key === "Enter") {
          requestHandler()
      }
  }

  const requestHandler = () => {
    setIsSubmitted(true)

    setIsUsernameValid(username !== '')
    setUsernameErrorMessage(username !== '' ? "" : "Username must not be blank")

    if (email === '') {
      setIsEmailValid(false)
      setEmailErrorMessage("Email must not be blank")
    } else if (!verifyEmailRegex(email)) {
      setIsEmailValid(false)
      setEmailErrorMessage("Not a valid email")
    } else {
      setIsEmailValid(true)
      setEmailErrorMessage("")
    }

    setIsPasswordValid(password !== '')
    setPasswordErrorMessage(password !== '' ? "" : "Password must not be blank")

    if(isInviteOnly && inviteToken === '')
      setInviteErrorMessage("Invitation code must not be blank")
    else
      setInviteErrorMessage("")

    if (confirmPassword === '') {
      setIsConfirmPasswordValid(false)
      setConfirmPasswordErrorMessage("Confirmation password must not be blank")
    } else if (confirmPassword !== password) {
      setIsConfirmPasswordValid(false)
      setConfirmPasswordErrorMessage("Passwords must match")
    } else {
      setIsConfirmPasswordValid(true)
      setConfirmPasswordErrorMessage("")
    }

    const isFormValid =
      username !== '' &&
      email !== '' &&
      password !== '' &&
      confirmPassword !== '' &&
      confirmPassword === password &&
      verifyEmailRegex(email) &&
      (!isInviteOnly || inviteToken !== '')

    if (!isFormValid)
      return

    var registerJSON = {"username": username, "password": password, "email": email}
    if(inviteToken !== '')
      registerJSON.inviteToken = inviteToken

    fetch(AUTH_ENDPOINTS.REGISTER, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(registerJSON),
    })
      .then(async (response) => {
        if (!response.ok) {
          const data = await response.json().catch(() => ({}))
          if (data.errorMessage === "Username already registered") {
            setIsUsernameValid(false)
            setUsernameErrorMessage("Username is already taken")
          } else if (data.errorMessage === "Email already registered") {
            setIsEmailValid(false)
            setEmailErrorMessage("Email is already taken")
          } else if (data.errorMessage === "Invitation is invalid or expired") {
            setInviteErrorMessage(data.errorMessage)
          }
        } else {
          const data = await response.json()

          if(data.message === "Registered without email")
            setRegistrationMessage("Thank you for signing up!")
          else
            setRegistrationMessage(`Thank you for signing up! An email has been sent to ${email} with a confirmation link to activate your account.`)

          setIsSuccess(true)
        }
      })
      .catch((error) => {
        console.error('Network error or unexpected issue:', error)
      })
  }

  return(
    <>
      {!isSuccess && (<div id="register-div" className="auth-div">
        {isInviteOnly === null && inviteErrorMessage === "" && <p>Loading registration settings</p>}
        {isInviteOnly === null && inviteErrorMessage !== "" && <p>{inviteErrorMessage}</p>}
        {isInviteOnly !== null && <>
        <h1>{isInviteOnly ? "Create your invited account" : "Create account"}</h1>
        {isInviteOnly && <p>An invitation is required to create an account.</p>}

        <form id="register-form" className="register-form" onKeyDown={handleKeyDown} onSubmit={(e) => e.preventDefault()}>
          <label htmlFor="username">Username:</label>
          <div>
            {isSubmitted && (isUsernameValid ? <FaCheckCircle className="check-circle" /> : <FaRegCircleXmark title={usernameErrorMessage} className="circle-x-mark" />)}
            <input type="text" id="username" name="username" placeholder="Type your username" spellCheck="false" value={username} onChange={(e) => setUsername(e.target.value)} />
          </div>
          <hr />

          <label htmlFor="email">Email:</label>
          <div>
            {isSubmitted && (isEmailValid ? <FaCheckCircle className="check-circle" /> : <FaRegCircleXmark title={emailErrorMessage} className="circle-x-mark" />)}
            <input type="text" id="email" name="email" placeholder="Type your email" spellCheck="false" value={email} onChange={(e) => setEmail(e.target.value)} />
          </div>
          <hr />

          <label htmlFor="password">Password:</label>
          <div>
            {isSubmitted && (isPasswordValid ? <FaCheckCircle className="check-circle" /> : <FaRegCircleXmark title={passwordErrorMessage} className="circle-x-mark" />)}
            <input type="password" id="password" name="password" placeholder="Type your password" spellCheck="false" value={password} onChange={(e) => setPassword(e.target.value)} />
          </div>
          <hr />

          <label htmlFor="confirm-password">Confirm password:</label>
          <div>
            {isSubmitted && (isConfirmPasswordValid ? <FaCheckCircle className="check-circle" /> : <FaRegCircleXmark title={confirmPasswordErrorMessage} className="circle-x-mark" />)}
            <input type="password" id="confirm-password" name="confirm-password" placeholder="Confirm your password" spellCheck="false" value={confirmPassword} onChange={(e) => setConfirmPassword(e.target.value)} />
          </div>
          <hr />

          {isInviteOnly && <>
          <label htmlFor="invite-token">Invitation code:</label>
          <div>
            {isSubmitted && (inviteErrorMessage === "" ? <FaCheckCircle className="check-circle" /> : <FaRegCircleXmark title={inviteErrorMessage} className="circle-x-mark" />)}
            <input type="text" id="invite-token" name="invite-token" placeholder="Type your invitation code" spellCheck="false" value={inviteToken} onChange={(e) => setInviteToken(e.target.value)} />
          </div>
          <hr />
          </>}
        </form>

        <button id='submit-button' onClick={() => requestHandler()}>SIGN UP</button>
        </>}
      </div>)}

      {isSuccess && (<div id="request-success-div" className="auth-div">
        <p id="request-success-text-field">
          {registrationMessage}
        </p>
        <p>
          <a className="request-success-anchor" href="/login">Click here</a> to return to the login page.
        </p>
      </div>)}
    </>
  )
}

export default UserRegisterForm
