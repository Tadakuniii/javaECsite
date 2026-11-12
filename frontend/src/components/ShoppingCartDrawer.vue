<script setup lang="ts">
import { ref } from 'vue'
import { changeCartItemQuantity, removeCartItem, type ShoppingCart } from '../services/shoppingCartApi'
import ProductIllustration from './ProductIllustration.vue'

defineProps<{ cart: ShoppingCart }>()
const emit = defineEmits<{
  (e: 'close'): void
  (e: 'updated', cart: ShoppingCart): void
}>()
const updatingItemId = ref<number>()
const errorMessage = ref('')
const failedImageIds = ref<Set<number>>(new Set())
const yen = new Intl.NumberFormat('ja-JP', { style: 'currency', currency: 'JPY' })

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
    <aside class="cart-drawer" role="dialog" aria-modal="true" aria-labelledby="cart-title">
      <header class="cart-header">
        <div><span class="cart-caption">SHOPPING CART</span><h2 id="cart-title">買い物カゴ</h2></div>
        <button type="button" class="cart-close" aria-label="買い物カゴを閉じる" @click="emit('close')">×</button>
      </header>
      <div v-if="errorMessage" class="alert alert-danger" role="alert">{{ errorMessage }}</div>

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
              <label :for="`quantity-${item.id}`">数量</label>
              <select :id="`quantity-${item.id}`" :value="item.quantity" :disabled="updatingItemId === item.id"
                @change="updateQuantity(item.id, Number(($event.target as HTMLSelectElement).value))">
                <option v-for="quantity in Math.min(item.stockQuantity, 99)" :key="quantity" :value="quantity">{{ quantity }}</option>
              </select>
              <button type="button" :disabled="updatingItemId === item.id" @click="removeItem(item.id)">削除</button>
            </div>
          </div>
        </article>
      </div>
      <div v-else class="empty-cart">
        <svg viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M3 4h2l2.1 10.2a2 2 0 0 0 2 1.6h7.8a2 2 0 0 0 2-1.6L20 7H6"/><circle cx="10" cy="20" r="1"/><circle cx="17" cy="20" r="1"/></svg>
        <h3>買い物カゴは空です</h3><p>商品一覧から商品を追加してください。</p>
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
.cart-drawer { width: min(430px, 100%); height: 100%; display: flex; flex-direction: column; padding: 26px; background: var(--card-bg); box-shadow: -12px 0 36px rgba(15, 23, 42, .18); overflow-y: auto; }
.cart-header { display: flex; align-items: center; justify-content: space-between; padding-bottom: 18px; border-bottom: 1px solid var(--input-border); }
.cart-caption { color: var(--primary); font-size: 10px; font-weight: 700; letter-spacing: .14em; }.cart-header h2 { text-align: left; margin: 3px 0 0; font-size: 24px; }
.cart-close { width: 38px; height: 38px; border: 1px solid var(--input-border); border-radius: 50%; background: var(--card-bg); color: var(--text-secondary); font-size: 25px; cursor: pointer; }
.cart-items { display: grid; gap: 14px; padding: 20px 0; }.cart-item { display: flex; gap: 14px; padding-bottom: 14px; border-bottom: 1px solid var(--input-border); }
.cart-item-image { width: 92px; height: 92px; flex: 0 0 92px; background: color-mix(in srgb, var(--product-accent) 12%, var(--input-bg)); border-radius: 10px; overflow: hidden; }.cart-item-image img { width: 100%; height: 100%; object-fit: contain; }
.cart-item-content { flex: 1; min-width: 0; }.cart-item-content h3 { font-size: 14px; margin-bottom: 3px; }.cart-item-content p { font-size: 11px; }.cart-item-content strong { display: block; margin-top: 7px; color: var(--text-primary); font-size: 15px; }
.item-actions { display: flex; align-items: center; gap: 8px; margin-top: 9px; }.item-actions label { font-size: 11px; }.item-actions select { padding: 5px 8px; border: 1px solid var(--input-border); border-radius: 6px; background: var(--input-bg); color: var(--text-primary); }.item-actions button { margin-left: auto; border: 0; background: transparent; color: var(--text-secondary); text-decoration: underline; cursor: pointer; }
.empty-cart { display: grid; place-items: center; text-align: center; padding: 70px 20px; }.empty-cart svg { width: 58px; stroke: var(--primary); stroke-width: 1.4; margin-bottom: 16px; }.empty-cart h3 { font-size: 17px; }.empty-cart p { margin-top: 7px; font-size: 12px; }
.cart-footer { margin-top: auto; padding-top: 20px; border-top: 1px solid var(--input-border); }.cart-footer > div { display: flex; justify-content: space-between; align-items: baseline; }.cart-footer span { font-size: 13px; }.cart-footer strong { color: var(--text-primary); font-size: 23px; }.cart-footer .btn { margin-top: 18px; }.continue-button { width: 100%; margin-top: 10px; border: 0; background: transparent; color: var(--primary); padding: 9px; cursor: pointer; }
@media (max-width: 480px) { .cart-drawer { padding: 20px; }.cart-item-image { width: 78px; height: 78px; flex-basis: 78px; } }
</style>
