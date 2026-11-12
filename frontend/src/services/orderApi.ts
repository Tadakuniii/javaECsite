import type { ProductIllustrationKind } from './productApi.js'

export interface OrderHistoryItem {
  productId: number
  productName: string
  size: string
  unitPrice: number
  quantity: number
  subtotal: number
  imageUrl?: string
  label: string
  accentColor: string
  illustration: ProductIllustrationKind
}

export interface CustomerOrder {
  id: number
  status: string
  statusLabel: string
  totalAmount: number
  orderedAt: string
  items: OrderHistoryItem[]
}

async function csrfHeaders(): Promise<Record<string, string>> {
  const response = await fetch('/api/csrf', { credentials: 'same-origin' })
  if (!response.ok) throw new Error('接続を確認してください')
  const csrf = await response.json() as { token: string; headerName: string }
  return { [csrf.headerName]: csrf.token }
}

async function readResponse<T>(response: Response, fallbackMessage: string): Promise<T> {
  if (!response.ok) {
    const body = await response.json().catch(() => null) as { detail?: string; message?: string } | null
    throw new Error(body?.detail || body?.message || fallbackMessage)
  }
  return await response.json() as T
}

export async function getOrderHistory(): Promise<CustomerOrder[]> {
  return readResponse<CustomerOrder[]>(
    await fetch('/api/orders', { credentials: 'same-origin' }),
    '注文履歴を取得できませんでした',
  )
}

export async function checkoutShoppingCart(): Promise<CustomerOrder> {
  return readResponse<CustomerOrder>(await fetch('/api/orders', {
    method: 'POST',
    credentials: 'same-origin',
    headers: await csrfHeaders(),
  }), '購入手続きを完了できませんでした')
}
