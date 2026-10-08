export interface AuthenticationResponse {
  status: string
  message?: string
  username?: string
}

/** APIのJSON応答を読み取り、HTMLや空のエラー応答には日本語の案内を返す。 */
async function readApiResponse(response: Response): Promise<AuthenticationResponse> {
  if (!response.ok) {
    const body = await response.json().catch(() => null) as AuthenticationResponse | null
    throw new Error(body?.message || 'サーバーエラーが発生しました')
  }
  try {
    return await response.json() as AuthenticationResponse
  } catch {
    throw new Error('サーバーからの応答を確認できませんでした')
  }
}

/** 最新のCSRFトークンとセッションCookieを使ってフォームを送信する。 */
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

/** ログイン中の表示名を取得する。未認証（401）はnull、通信失敗は例外を返す。 */
export async function getAuthenticatedCustomer(): Promise<AuthenticationResponse | null> {
  const response = await fetch('/api/session', { credentials: 'same-origin' })
  if (response.status === 401) return null
  return readApiResponse(response)
}

/**
 * ログインして表示名を返す。セッション確認の通信失敗時だけログイン応答を利用する。
 * セッション確認が401を返した場合は、ログイン成功として扱わない。
 */
export async function loginCustomer(userId: string, password: string): Promise<string> {
  const loginResponse = await submitAuthenticationForm('/api/login', { userid: userId, password })
  const customer = await getAuthenticatedCustomer().catch(() => undefined)
  if (customer === null) throw new Error('ログイン状態を確認できませんでした')
  const displayName = customer === undefined ? loginResponse.username : customer.username
  if (!displayName) throw new Error('ログイン状態を確認できませんでした')
  return displayName
}
