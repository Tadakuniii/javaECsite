import assert from 'node:assert/strict'
import { afterEach, mock, test } from 'node:test'
import { getAvailableProducts } from '../src/services/productApi.ts'

afterEach(() => mock.restoreAll())

/** 商品APIの応答をJSON形式で用意する。 */
function jsonResponse(body: object, status = 200): Response {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })
}

test('成功応答のproductsから商品一覧を取り出す', async () => {
  mock.method(globalThis, 'fetch', async () => jsonResponse({
    status: 'SUCCESS',
    products: [{ id: 1, productCode: 'vitamin-c', name: 'ビタミンC' }],
  }))

  const products = await getAvailableProducts()
  assert.equal(products.length, 1)
  assert.equal(products[0]?.productCode, 'vitamin-c')
})

test('productsのない成功応答を不正な形式として扱う', async () => {
  mock.method(globalThis, 'fetch', async () => jsonResponse({ status: 'SUCCESS' }))
  await assert.rejects(getAvailableProducts(), { message: '商品情報の形式を確認できませんでした' })
})

test('未認証時はログイン状態のエラーを返す', async () => {
  mock.method(globalThis, 'fetch', async () => jsonResponse({}, 401))
  await assert.rejects(getAvailableProducts(), { message: 'ログイン状態を確認できませんでした' })
})
