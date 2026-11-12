import type { ProductIllustrationKind } from './productApi'

export interface ShoppingCartItem {
  id: number
  productId: number
  name: string
  size: string
  unitPrice: number
  quantity: number
  stockQuantity: number
  imageUrl?: string
  label: string
  accentColor: string
  illustration: ProductIllustrationKind
}

export interface ShoppingCart {
  items: ShoppingCartItem[]
  totalQuantity: number
  totalPrice: number
}

export const emptyShoppingCart = (): ShoppingCart => ({ items: [], totalQuantity: 0, totalPrice: 0 })

let cartOperationQueue: Promise<void> = Promise.resolve()

/** カゴの取得と更新を順番に実行し、古い応答による表示の巻き戻りを防ぐ。 */
function enqueueCartOperation(operation: () => Promise<ShoppingCart>): Promise<ShoppingCart> {
  const result = cartOperationQueue.then(operation, operation)
  cartOperationQueue = result.then(() => undefined, () => undefined)
  return result
}

async function csrfHeaders(): Promise<Record<string, string>> {
  const response = await fetch('/api/csrf', { credentials: 'same-origin' })
  if (!response.ok) throw new Error('接続を確認してください')
  const csrf = await response.json() as { token: string; headerName: string }
  return { [csrf.headerName]: csrf.token }
}

async function readCartResponse(response: Response): Promise<ShoppingCart> {
  if (!response.ok) {
    const body = await response.json().catch(() => null) as { detail?: string; message?: string } | null
    throw new Error(body?.detail || body?.message || '買い物カゴを更新できませんでした')
  }
  return await response.json() as ShoppingCart
}

export async function getShoppingCart(): Promise<ShoppingCart> {
  return enqueueCartOperation(async () => readCartResponse(
    await fetch('/api/cart', { credentials: 'same-origin' }),
  ))
}

export async function addProductToCart(productId: number): Promise<ShoppingCart> {
  return enqueueCartOperation(async () => readCartResponse(await fetch('/api/cart/items', {
    method: 'POST',
    credentials: 'same-origin',
    headers: { 'Content-Type': 'application/json', ...await csrfHeaders() },
    body: JSON.stringify({ productId, quantity: 1 }),
  })))
}

export async function changeCartItemQuantity(itemId: number, quantity: number): Promise<ShoppingCart> {
  return enqueueCartOperation(async () => readCartResponse(await fetch(`/api/cart/items/${itemId}`, {
    method: 'PUT',
    credentials: 'same-origin',
    headers: { 'Content-Type': 'application/json', ...await csrfHeaders() },
    body: JSON.stringify({ quantity }),
  })))
}

export async function removeCartItem(itemId: number): Promise<ShoppingCart> {
  return enqueueCartOperation(async () => readCartResponse(await fetch(`/api/cart/items/${itemId}`, {
    method: 'DELETE',
    credentials: 'same-origin',
    headers: await csrfHeaders(),
  })))
}
