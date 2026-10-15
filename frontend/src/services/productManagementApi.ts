import type { ProductCategory } from './productApi'

export interface ManagedProduct {
  id: number
  productCode: string
  name: string
  category: ProductCategory
  description: string
  price: number
  stockQuantity: number
  imageUrl?: string
  active: boolean
}

export type ProductInput = Pick<ManagedProduct,
  'name' | 'category' | 'description' | 'price' | 'stockQuantity' | 'imageUrl' | 'active'>

async function getCsrfHeader(): Promise<Record<string, string>> {
  const response = await fetch('/api/csrf', { credentials: 'same-origin' })
  if (!response.ok) throw new Error('接続を確認してください')
  const csrf = await response.json() as { token: string; headerName: string }
  return { [csrf.headerName]: csrf.token }
}

export async function getManagedProducts(): Promise<ManagedProduct[]> {
  const response = await fetch('/api/admin/products', { credentials: 'same-origin' })
  if (!response.ok) throw new Error(response.status === 403 ? '管理者権限が必要です' : '商品情報を取得できませんでした')
  return await response.json() as ManagedProduct[]
}

export async function saveProduct(product: ProductInput, productId?: number): Promise<ManagedProduct> {
  const response = await fetch(productId ? `/api/admin/products/${productId}` : '/api/admin/products', {
    method: productId ? 'PUT' : 'POST',
    credentials: 'same-origin',
    headers: { 'Content-Type': 'application/json', ...await getCsrfHeader() },
    body: JSON.stringify(product),
  })
  if (!response.ok) {
    const body = await response.json().catch(() => null) as { message?: string } | null
    throw new Error(body?.message || '商品情報を保存できませんでした')
  }
  return await response.json() as ManagedProduct
}
