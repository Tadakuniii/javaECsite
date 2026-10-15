export type ProductCategory = 'サプリメント' | '食品・飲料' | '健康グッズ'
export type ProductIllustrationKind = 'bottle' | 'pouch' | 'tea' | 'water-bottle' | 'mat'

export interface HealthProduct {
  id: number
  productCode: string
  name: string
  category: ProductCategory
  description: string
  size: string
  price: number
  stockQuantity: number
  imageUrl?: string
  label: string
  accentColor: string
  illustration: ProductIllustrationKind
}

/** 認証済みユーザー向けの商品一覧をDBから取得する。 */
export async function getAvailableProducts(): Promise<HealthProduct[]> {
  const response = await fetch('/api/products', { credentials: 'same-origin' })
  if (!response.ok) {
    throw new Error(response.status === 401
      ? 'ログイン状態を確認できませんでした'
      : '商品情報を取得できませんでした')
  }
  try {
    const body = await response.json() as { status?: string; products?: HealthProduct[] }
    if (body.status !== 'SUCCESS' || !Array.isArray(body.products)) {
      throw new Error('商品情報の形式を確認できませんでした')
    }
    return body.products
  } catch {
    throw new Error('商品情報の形式を確認できませんでした')
  }
}
