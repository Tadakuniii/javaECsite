import assert from 'node:assert/strict'
import { test } from 'node:test'
import { addCartProduct, cartQuantity, cartSubtotal, setCartQuantity } from '../src/services/cart.ts'
import type { HealthProduct } from '../src/services/productApi.ts'

const vitamin: HealthProduct = {
  id: 1, productCode: 'vitamin-c', name: 'ビタミンC', category: 'サプリメント',
  description: '', size: '120粒', price: 1480, stockQuantity: 2,
  label: 'VITAMIN C', accentColor: '#d28d38', illustration: 'bottle',
}

test('同じ商品をまとめて追加し、在庫を超える追加を防ぐ', () => {
  let lines = addCartProduct([], vitamin)
  for (let i = 0; i < 3; i++) lines = addCartProduct(lines, vitamin)
  assert.equal(lines.length, 1)
  assert.equal(lines[0]?.quantity, 2)
  assert.equal(cartQuantity(lines), 2)
  assert.equal(cartSubtotal(lines), 2960)
})

test('在庫なしの商品はカートに入れない', () => {
  assert.deepEqual(addCartProduct([], { ...vitamin, stockQuantity: 0 }), [])
})

test('数量は正の整数に制限し、指定商品のみ変更する', () => {
  const lines = addCartProduct(addCartProduct([], vitamin), { ...vitamin, id: 2, price: 980 })
  for (const quantity of [0, -1, 1.5, NaN, Infinity]) {
    assert.deepEqual(setCartQuantity(lines, 1, quantity), lines)
  }
  const updated = setCartQuantity(lines, 1, 999)
  assert.equal(updated[0]?.quantity, 2)
  assert.equal(updated[1]?.quantity, 1)
  assert.equal(cartSubtotal(updated), 3940)
  assert.equal(cartQuantity(updated), 3)
  assert.equal(cartSubtotal(setCartQuantity(updated, 1, 1)), 2460)
  assert.equal(lines[0]?.quantity, 1)
})

test('空のカートは数量・合計ともゼロになる', () => {
  assert.equal(cartQuantity([]), 0)
  assert.equal(cartSubtotal([]), 0)
})
