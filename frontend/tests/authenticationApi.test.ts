import assert from 'node:assert/strict'
import { afterEach, mock, test } from 'node:test'
import { getAuthenticatedCustomer, loginCustomer, submitAuthenticationForm } from '../src/services/authenticationApi.ts'

/** API応答をJSON形式で用意する。 */
function jsonResponse(body: object, status = 200): Response {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })
}

/** 順番どおりにAPI応答または通信エラーを返す。 */
function mockApiResponses(...responses: (Response | Error)[]) {
  return mock.method(globalThis, 'fetch', async () => {
    const response = responses.shift()
    if (!response) throw new Error('Unexpected API request')
    if (response instanceof Error) throw response
    return response
  })
}

/** ログインまで成功した状態からセッション確認の応答を差し替える。 */
function mockLoginAndSession(sessionResponse: Response | Error, loginUsername: string | undefined = 'ログイン応答の名前') {
  return mockApiResponses(
    jsonResponse({ token: 'csrf-token', headerName: 'X-CSRF-TOKEN' }),
    jsonResponse({ status: 'SUCCESS', username: loginUsername }),
    sessionResponse,
  )
}

afterEach(() => mock.restoreAll())

test('HTMLの502エラーを日本語の案内に置き換える', async () => {
  mockApiResponses(new Response('<html>Bad Gateway</html>', { status: 502 }))
  await assert.rejects(getAuthenticatedCustomer(), { message: 'サーバーエラーが発生しました' })
})

test('空の504エラーを日本語の案内に置き換える', async () => {
  mockApiResponses(new Response(null, { status: 504 }))
  await assert.rejects(getAuthenticatedCustomer(), { message: 'サーバーエラーが発生しました' })
})

test('JSONエラーに含まれる案内を保持する', async () => {
  mockApiResponses(jsonResponse({ message: '入力内容を確認してください' }, 400))
  await assert.rejects(getAuthenticatedCustomer(), { message: '入力内容を確認してください' })
})

test('成功応答でも壊れたJSONの解析エラーを画面へ出さない', async () => {
  mockApiResponses(new Response('invalid json'))
  await assert.rejects(getAuthenticatedCustomer(), { message: 'サーバーからの応答を確認できませんでした' })
})

test('セッション確認に成功した場合はその表示名を使う', async () => {
  mockLoginAndSession(jsonResponse({ status: 'SUCCESS', username: 'セッションの名前' }))
  assert.equal(await loginCustomer('test-user', 'password123'), 'セッションの名前')
})

test('セッション確認の通信失敗時だけログイン応答を使う', async () => {
  mockLoginAndSession(new TypeError('Network unavailable'))
  assert.equal(await loginCustomer('test-user', 'password123'), 'ログイン応答の名前')
})

test('セッション確認が502の場合もログイン応答を利用できる', async () => {
  mockLoginAndSession(new Response('<html>Bad Gateway</html>', { status: 502 }))
  assert.equal(await loginCustomer('test-user', 'password123'), 'ログイン応答の名前')
})

test('セッション確認が401ならログイン応答へフォールバックしない', async () => {
  mockLoginAndSession(new Response(null, { status: 401 }))
  await assert.rejects(loginCustomer('test-user', 'password123'), { message: 'ログイン状態を確認できませんでした' })
})

test('通信失敗時にログイン応答の表示名もなければ成功にしない', async () => {
  mockLoginAndSession(new TypeError('Network unavailable'), '')
  await assert.rejects(loginCustomer('test-user', 'password123'), { message: 'ログイン状態を確認できませんでした' })
})

test('ログイン自体が401の場合はセッション確認へ進まない', async () => {
  const fetchMock = mockApiResponses(
    jsonResponse({ token: 'csrf-token', headerName: 'X-CSRF-TOKEN' }),
    jsonResponse({ message: 'ユーザIDまたはパスワードが間違っています' }, 401),
  )
  await assert.rejects(loginCustomer('test-user', 'wrong'), { message: 'ユーザIDまたはパスワードが間違っています' })
  assert.equal(fetchMock.mock.callCount(), 2)
})

test('フォームはセッションCookieと取得したCSRFトークンを送信する', async () => {
  const fetchMock = mockApiResponses(
    jsonResponse({ token: 'csrf-token', headerName: 'X-CSRF-TOKEN' }),
    jsonResponse({ status: 'SUCCESS' }),
  )
  await submitAuthenticationForm('/api/register', { userid: 'test-user' })
  const [path, options] = fetchMock.mock.calls[1]!.arguments as [string, RequestInit]
  assert.equal(path, '/api/register')
  assert.equal(options.credentials, 'same-origin')
  assert.equal((options.headers as Record<string, string>)['X-CSRF-TOKEN'], 'csrf-token')
  assert.equal(String(options.body), 'userid=test-user')
})
