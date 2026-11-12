import assert from 'node:assert/strict'
import { afterEach, mock, test } from 'node:test'
import { addProductToCart, getShoppingCart } from '../src/services/shoppingCartApi.ts'

afterEach(() => mock.restoreAll())

const jsonResponse = (body: object): Response => new Response(JSON.stringify(body), {
  status: 200,
  headers: { 'Content-Type': 'application/json' },
})

test('初期取得が完了してからカゴの追加処理を開始する', async () => {
  const requestedPaths: string[] = []
  let finishInitialRequest: ((response: Response) => void) | undefined
  mock.method(globalThis, 'fetch', async (input: string | URL | Request) => {
    const path = String(input)
    requestedPaths.push(path)
    if (path === '/api/cart') {
      return await new Promise<Response>(resolve => { finishInitialRequest = resolve })
    }
    if (path === '/api/csrf') return jsonResponse({ token: 'csrf-token', headerName: 'X-CSRF-TOKEN' })
    return jsonResponse({ items: [{ id: 1 }], totalQuantity: 1, totalPrice: 100 })
  })

  const initialCart = getShoppingCart()
  const updatedCart = addProductToCart(1)
  await new Promise(resolve => setImmediate(resolve))
  assert.deepEqual(requestedPaths, ['/api/cart'])

  finishInitialRequest?.(jsonResponse({ items: [], totalQuantity: 0, totalPrice: 0 }))
  await initialCart
  assert.equal((await updatedCart).totalQuantity, 1)
  assert.deepEqual(requestedPaths, ['/api/cart', '/api/csrf', '/api/cart/items'])
})
