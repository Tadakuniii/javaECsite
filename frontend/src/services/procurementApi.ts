export interface PurchaseOrder {
  id: number
  productId: number
  productName: string
  supplierName: string
  expectedDeliveryDate: string
  unitCost: number
  quantity: number
  totalCost: number
  automatic: boolean
  status: 'ORDERED' | 'RECEIVED'
  statusLabel: string
  orderedAt: string
  receivedAt?: string
}

export interface PurchaseOrderInput {
  productId: number
  supplierName: string
  expectedDeliveryDate: string
  unitCost: number
  quantity: number
}

async function csrfHeaders(): Promise<Record<string, string>> {
  const response = await fetch('/api/csrf', { credentials: 'same-origin' })
  if (!response.ok) throw new Error('接続を確認してください')
  const csrf = await response.json() as { token: string; headerName: string }
  return { [csrf.headerName]: csrf.token }
}

async function readResponse<T>(response: Response, fallback: string): Promise<T> {
  if (!response.ok) {
    const body = await response.json().catch(() => null) as { detail?: string; message?: string } | null
    throw new Error(body?.detail || body?.message || fallback)
  }
  return await response.json() as T
}

export async function getPurchaseOrders(): Promise<PurchaseOrder[]> {
  return readResponse<PurchaseOrder[]>(
    await fetch('/api/admin/procurements', { credentials: 'same-origin' }), '発注履歴を取得できませんでした',
  )
}

export async function createPurchaseOrder(input: PurchaseOrderInput): Promise<PurchaseOrder> {
  return readResponse<PurchaseOrder>(await fetch('/api/admin/procurements', {
    method: 'POST', credentials: 'same-origin',
    headers: { 'Content-Type': 'application/json', ...await csrfHeaders() },
    body: JSON.stringify(input),
  }), '発注を登録できませんでした')
}

export async function receivePurchaseOrder(id: number): Promise<PurchaseOrder> {
  return readResponse<PurchaseOrder>(await fetch(`/api/admin/procurements/${id}/receive`, {
    method: 'POST', credentials: 'same-origin', headers: await csrfHeaders(),
  }), '入荷を登録できませんでした')
}
