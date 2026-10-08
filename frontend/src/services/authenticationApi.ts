export interface AuthenticationResponse {
  status: string
  message?: string
  username?: string
}

async function readApiResponse(response: Response): Promise<AuthenticationResponse> {
  const result = await response.json() as AuthenticationResponse
  if (!response.ok) {
    throw new Error(result.message || 'サーバーエラーが発生しました')
  }
  return result
}

export async function submitAuthenticationForm(
  path: string,
  fields: Record<string, string> = {},
): Promise<AuthenticationResponse> {
  // Fetch a fresh token because login and logout rotate the session's CSRF token.
  const csrfResponse = await fetch('/api/csrf', { credentials: 'same-origin' })
  if (!csrfResponse.ok) throw new Error('接続を確認して、もう一度お試しください')
  const csrf = await csrfResponse.json() as { token: string; headerName: string }
  const response = await fetch(path, {
    method: 'POST',
    credentials: 'same-origin',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
      [csrf.headerName]: csrf.token,
    },
    body: new URLSearchParams(fields),
  })
  return readApiResponse(response)
}

export async function getAuthenticatedCustomer(): Promise<AuthenticationResponse | null> {
  const response = await fetch('/api/session', { credentials: 'same-origin' })
  if (response.status === 401) return null
  return readApiResponse(response)
}
