import assert from 'node:assert/strict'
import { afterEach, mock, test } from 'node:test'
import { createPurchaseOrder, getPurchaseOrders, receivePurchaseOrder } from '../src/services/procurementApi.ts'

afterEach(() => mock.restoreAll())

const jsonResponse = (body: object): Response => new Response(JSON.stringify(body), {
  status: 200, headers: { 'Content-Type': 'application/json' },
})

test('発注履歴を管理者APIから取得する', async () => {
  mock.method(globalThis, 'fetch', async () => jsonResponse([{ id: 1, productName: 'テスト商品' }]))
  const orders = await getPurchaseOrders()
  assert.equal(orders[0]?.productName, 'テスト商品')
})

test('CSRFトークンを付けて手動発注を登録する', async () => {
  const requests: { path: string; options?: RequestInit }[] = []
  mock.method(globalThis, 'fetch', async (input: string | URL | Request, options?: RequestInit) => {
    const path = String(input)
    requests.push({ path, options })
    if (path === '/api/csrf') return jsonResponse({ token: 'procurement-token', headerName: 'X-CSRF-TOKEN' })
    return jsonResponse({ id: 2, productName: 'テスト商品', quantity: 10 })
  })
  const input = { productId: 1, supplierName: 'テスト商事', expectedDeliveryDate: '2026-12-01', unitCost: 500, quantity: 10 }
  const order = await createPurchaseOrder(input)
  assert.equal(order.quantity, 10)
  assert.equal(requests[1]?.options?.method, 'POST')
  assert.equal((requests[1]?.options?.headers as Record<string, string>)['X-CSRF-TOKEN'], 'procurement-token')
  assert.deepEqual(JSON.parse(String(requests[1]?.options?.body)), input)
})

test('入荷登録APIを呼び出す', async () => {
  const paths: string[] = []
  mock.method(globalThis, 'fetch', async (input: string | URL | Request) => {
    const path = String(input)
    paths.push(path)
    if (path === '/api/csrf') return jsonResponse({ token: 'procurement-token', headerName: 'X-CSRF-TOKEN' })
    return jsonResponse({ id: 3, status: 'RECEIVED' })
  })
  const order = await receivePurchaseOrder(3)
  assert.equal(order.status, 'RECEIVED')
  assert.deepEqual(paths, ['/api/csrf', '/api/admin/procurements/3/receive'])
})
