<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue'
import type { HealthProduct } from '../services/productApi'
import { changeCartItemQuantity, removeCartItem, type ShoppingCart } from '../services/shoppingCartApi'
import ProductIllustration from './ProductIllustration.vue'

const props = defineProps<{ cart: ShoppingCart; products: HealthProduct[] }>()
const emit = defineEmits<{
  (e: 'close'): void
  (e: 'updated', cart: ShoppingCart): void
  (e: 'add', product: HealthProduct): void
}>()
const updatingItemId = ref<number>()
const errorMessage = ref('')
const failedImageIds = ref<Set<number>>(new Set())
const drawer = ref<HTMLElement>()
const closeButton = ref<HTMLButtonElement>()
const yen = new Intl.NumberFormat('ja-JP', { style: 'currency', currency: 'JPY' })
const recommendedProducts = computed(() => props.products
  .filter(product => product.stockQuantity > 0 && !props.cart.items.some(item => item.productId === product.id))
  .slice(0, 3))

onMounted(async () => {
  await nextTick()
  closeButton.value?.focus()
})

/** Tabキーの移動をカゴ内に制限し、Escapeキーで閉じる。 */
const handleKeydown = (event: KeyboardEvent) => {
  if (event.key === 'Escape') {
    event.preventDefault()
    emit('close')
    return
  }
  if (event.key !== 'Tab' || !drawer.value) return
  const focusableElements = [...drawer.value.querySelectorAll<HTMLElement>(
    'button:not([disabled]), select:not([disabled]), [href], [tabindex]:not([tabindex="-1"])',
  )].filter(element => !element.hasAttribute('hidden'))
  if (!focusableElements.length) return
  const first = focusableElements[0]
  const last = focusableElements[focusableElements.length - 1]
  if (event.shiftKey && document.activeElement === first) {
    event.preventDefault()
    last?.focus()
  } else if (!event.shiftKey && document.activeElement === last) {
    event.preventDefault()
    first?.focus()
  }
}

const updateQuantity = async (itemId: number, quantity: number) => {
  updatingItemId.value = itemId
  errorMessage.value = ''
  try {
    emit('updated', await changeCartItemQuantity(itemId, quantity))
  } catch (error: unknown) {
    errorMessage.value = error instanceof Error ? error.message : '数量を変更できませんでした'
  } finally {
    updatingItemId.value = undefined
  }
}

const removeItem = async (itemId: number) => {
  updatingItemId.value = itemId
  errorMessage.value = ''
  try {
    emit('updated', await removeCartItem(itemId))
  } catch (error: unknown) {
    errorMessage.value = error instanceof Error ? error.message : '商品を削除できませんでした'
  } finally {
    updatingItemId.value = undefined
  }
}
</script>

<template>
  <div class="cart-backdrop" @click.self="emit('close')">
    <aside ref="drawer" class="cart-drawer" role="dialog" aria-modal="true" aria-labelledby="cart-title" @keydown="handleKeydown">
      <header class="cart-header">
        <div><span class="cart-caption">SHOPPING CART</span><h2 id="cart-title">買い物カゴ</h2></div>
        <button ref="closeButton" type="button" class="cart-close" aria-label="買い物カゴを閉じる" @click="emit('close')">×</button>
      </header>
      <div v-if="errorMessage" class="alert alert-danger" role="alert">{{ errorMessage }}</div>

      <div class="cart-content">
      <div v-if="cart.items.length" class="cart-items">
        <article v-for="item in cart.items" :key="item.id" class="cart-item">
          <div class="cart-item-image" :style="{ '--product-accent': item.accentColor }">
            <img v-if="item.imageUrl && !failedImageIds.has(item.id)" :src="item.imageUrl" :alt="item.name"
              @error="failedImageIds = new Set([...failedImageIds, item.id])" />
            <ProductIllustration v-else :kind="item.illustration" :label="item.label" :accent-color="item.accentColor" />
          </div>
          <div class="cart-item-content">
            <h3>{{ item.name }}</h3><p>{{ item.size }}</p><strong>{{ yen.format(item.unitPrice) }}</strong>
            <div class="item-actions">
              <span class="quantity-label">数量</span>
              <div class="quantity-control" role="group" :aria-label="`${item.name}の数量`">
                <button type="button" :disabled="updatingItemId === item.id || item.quantity <= 1"
                  :aria-label="`${item.name}を1点減らす`" @click="updateQuantity(item.id, item.quantity - 1)">−</button>
                <span aria-live="polite">{{ item.quantity }}</span>
                <button type="button" :disabled="updatingItemId === item.id || item.quantity >= item.stockQuantity"
                  :aria-label="`${item.name}を1点増やす`" @click="updateQuantity(item.id, item.quantity + 1)">＋</button>
              </div>
              <button type="button" class="remove-item" :disabled="updatingItemId === item.id" @click="removeItem(item.id)">削除</button>
            </div>
            <p v-if="item.quantity >= item.stockQuantity" class="stock-limit">在庫数の上限です</p>
          </div>
        </article>
      </div>
      <div v-else class="empty-cart">
        <svg viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M3 4h2l2.1 10.2a2 2 0 0 0 2 1.6h7.8a2 2 0 0 0 2-1.6L20 7H6"/><circle cx="10" cy="20" r="1"/><circle cx="17" cy="20" r="1"/></svg>
        <h3>買い物カゴは空です</h3><p>商品一覧から商品を追加してください。</p>
      </div>
      <section v-if="recommendedProducts.length" class="recommendations" aria-labelledby="recommendations-title">
        <div><span class="cart-caption">RECOMMENDED</span><h3 id="recommendations-title">こちらもおすすめ</h3></div>
        <article v-for="product in recommendedProducts" :key="product.id" class="recommendation-item">
          <div><strong>{{ product.name }}</strong><span>{{ yen.format(product.price) }}</span></div>
          <button type="button" :aria-label="`${product.name}を買い物カゴに追加`" @click="emit('add', product)">＋ 追加</button>
        </article>
      </section>
      </div>
      <footer class="cart-footer">
        <div><span>合計（{{ cart.totalQuantity }}点）</span><strong>{{ yen.format(cart.totalPrice) }}</strong></div>
        <button type="button" class="btn" :disabled="!cart.items.length">購入手続きへ</button>
        <button type="button" class="continue-button" @click="emit('close')">買い物を続ける</button>
      </footer>
    </aside>
  </div>
</template>

<style scoped>
.cart-backdrop { position: fixed; inset: 0; z-index: 100; display: flex; justify-content: flex-end; background: rgba(15, 23, 42, .45); backdrop-filter: blur(3px); }
.cart-drawer { box-sizing: border-box; width: min(430px, 100%); height: 100dvh; display: flex; flex-direction: column; padding: 26px; background: var(--card-bg); box-shadow: -12px 0 36px rgba(15, 23, 42, .18); overflow: hidden; }
.cart-header { flex: 0 0 auto; display: flex; align-items: center; justify-content: space-between; padding-bottom: 18px; border-bottom: 1px solid var(--input-border); }
.cart-caption { color: var(--primary); font-size: 10px; font-weight: 700; letter-spacing: .14em; }.cart-header h2 { text-align: left; margin: 3px 0 0; font-size: 24px; }
.cart-close { width: 38px; height: 38px; border: 1px solid var(--input-border); border-radius: 50%; background: var(--card-bg); color: var(--text-secondary); font-size: 25px; cursor: pointer; }
.cart-content { flex: 1 1 auto; min-height: 0; padding-right: 4px; overflow-y: auto; overscroll-behavior: contain; }.cart-items { display: grid; align-content: start; gap: 14px; padding: 20px 0; }.cart-item { display: flex; gap: 14px; padding-bottom: 14px; border-bottom: 1px solid var(--input-border); }
.cart-item-image { width: 92px; height: 92px; flex: 0 0 92px; background: color-mix(in srgb, var(--product-accent) 12%, var(--input-bg)); border-radius: 10px; overflow: hidden; }.cart-item-image img { width: 100%; height: 100%; object-fit: contain; }
.cart-item-content { flex: 1; min-width: 0; }.cart-item-content h3 { font-size: 14px; margin-bottom: 3px; }.cart-item-content p { font-size: 11px; }.cart-item-content strong { display: block; margin-top: 7px; color: var(--text-primary); font-size: 15px; }
.item-actions { display: flex; align-items: center; gap: 8px; margin-top: 9px; }.quantity-label { font-size: 11px; }.quantity-control { display: inline-flex; align-items: center; border: 1px solid var(--input-border); border-radius: 7px; overflow: hidden; background: var(--input-bg); }.quantity-control button { width: 29px; height: 29px; border: 0; background: transparent; color: var(--text-primary); cursor: pointer; }.quantity-control button:hover:not(:disabled) { background: var(--primary-glow); color: var(--primary); }.quantity-control button:disabled { opacity: .35; cursor: not-allowed; }.quantity-control span { min-width: 26px; text-align: center; font-size: 12px; }.remove-item { margin-left: auto; border: 0; background: transparent; color: var(--text-secondary); text-decoration: underline; cursor: pointer; }.stock-limit { margin-top: 6px; color: var(--text-secondary); font-size: 10px !important; }
.empty-cart { display: grid; place-items: center; align-content: center; text-align: center; min-height: 270px; padding: 40px 20px; }.empty-cart svg { width: 58px; stroke: var(--primary); stroke-width: 1.4; margin-bottom: 16px; }.empty-cart h3 { font-size: 17px; }.empty-cart p { margin-top: 7px; font-size: 12px; }
.recommendations { display: grid; gap: 9px; padding: 18px 0 22px; border-top: 1px solid var(--input-border); }.recommendations h3 { margin-top: 3px; font-size: 15px; }.recommendation-item { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding: 11px 12px; border: 1px solid var(--input-border); border-radius: 9px; background: var(--input-bg); }.recommendation-item div { min-width: 0; }.recommendation-item strong { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 12px; }.recommendation-item span { display: block; margin-top: 3px; color: var(--text-secondary); font-size: 10px; }.recommendation-item button { flex: 0 0 auto; padding: 7px 9px; border: 1px solid var(--primary); border-radius: 7px; background: transparent; color: var(--primary); font: inherit; font-size: 10px; font-weight: 600; cursor: pointer; }.recommendation-item button:hover { background: var(--primary-glow); }
.cart-footer { flex: 0 0 auto; padding-top: 20px; border-top: 1px solid var(--input-border); background: transparent; }.cart-footer > div { display: flex; justify-content: space-between; align-items: baseline; }.cart-footer span { font-size: 13px; }.cart-footer strong { color: var(--text-primary); font-size: 23px; }.cart-footer .btn { margin-top: 18px; }.continue-button { width: 100%; margin-top: 10px; border: 0; background: transparent; color: var(--primary); padding: 9px; cursor: pointer; }
@media (max-width: 480px) { .cart-drawer { padding: 20px; }.cart-item-image { width: 78px; height: 78px; flex-basis: 78px; } }
</style>
