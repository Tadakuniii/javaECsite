import type { HealthProduct } from './productApi.ts'

export interface CartLine {
  product: HealthProduct
  quantity: number
}

/** 在庫数を上限にし、同じ商品をひとつの行にまとめる。 */
export function addCartProduct(lines: CartLine[], product: HealthProduct): CartLine[] {
  if (product.stockQuantity < 1) return lines
  const current = lines.find(line => line.product.id === product.id)
  if (current) return setCartQuantity(lines, product.id, current.quantity + 1)
  return [...lines, { product, quantity: 1 }]
}

export function setCartQuantity(lines: CartLine[], productId: number, quantity: number): CartLine[] {
  if (!Number.isInteger(quantity) || quantity < 1) return lines
  return lines.map(line => line.product.id === productId
    ? { ...line, quantity: Math.min(quantity, line.product.stockQuantity) }
    : line)
}

export function cartQuantity(lines: CartLine[]): number {
  return lines.reduce((sum, line) => sum + line.quantity, 0)
}

export function cartSubtotal(lines: CartLine[]): number {
  return lines.reduce((sum, line) => sum + line.product.price * line.quantity, 0)
}
