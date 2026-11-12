import assert from 'node:assert/strict'
import { afterEach, mock, test } from 'node:test'
import { checkoutShoppingCart, getOrderHistory } from '../src/services/orderApi.ts'

afterEach(() => mock.restoreAll())

const jsonResponse = (body: object, status = 200): Response => new Response(JSON.stringify(body), {
  status,
  headers: { 'Content-Type': 'application/json' },
})

test('購入処理にCSRFトークンを付けて注文APIを呼び出す', async () => {
  const requests: { path: string; options?: RequestInit }[] = []
  mock.method(globalThis, 'fetch', async (input: string | URL | Request, options?: RequestInit) => {
    const path = String(input)
    requests.push({ path, options })
    if (path === '/api/csrf') return jsonResponse({ token: 'order-token', headerName: 'X-CSRF-TOKEN' })
    return jsonResponse({ id: 1, status: 'PAID', items: [] })
  })

  const order = await checkoutShoppingCart()
  assert.equal(order.id, 1)
  assert.equal(requests[1]?.path, '/api/orders')
  assert.equal(requests[1]?.options?.method, 'POST')
  assert.deepEqual(requests[1]?.options?.headers, { 'X-CSRF-TOKEN': 'order-token' })
})

test('ログイン中の利用者の注文履歴を取得する', async () => {
  mock.method(globalThis, 'fetch', async () => jsonResponse([{ id: 2, status: 'PAID', items: [] }]))
  const orders = await getOrderHistory()
  assert.equal(orders[0]?.id, 2)
})
