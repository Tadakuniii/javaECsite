<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { getOrderHistory, type CustomerOrder } from '../services/orderApi'
import ProductIllustration from './ProductIllustration.vue'

defineEmits<{ (e: 'back'): void }>()
const orders = ref<CustomerOrder[]>([])
const isLoading = ref(true)
const errorMessage = ref('')
const failedImages = ref<Set<string>>(new Set())
const yen = new Intl.NumberFormat('ja-JP', { style: 'currency', currency: 'JPY' })
const dateTime = new Intl.DateTimeFormat('ja-JP', {
  year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit',
})

onMounted(async () => {
  try {
    orders.value = await getOrderHistory()
  } catch (error: unknown) {
    errorMessage.value = error instanceof Error ? error.message : '注文履歴を取得できませんでした'
  } finally {
    isLoading.value = false
  }
})

const imageKey = (orderId: number, productId: number) => `${orderId}-${productId}`
</script>

<template>
  <div class="order-history">
    <header class="history-header">
      <div><span class="section-label">ORDER HISTORY</span><h1>注文履歴</h1><p>これまでに購入した商品を確認できます。</p></div>
      <button type="button" class="back-button" @click="$emit('back')">商品一覧に戻る</button>
    </header>
    <div v-if="isLoading" class="history-state card" role="status">注文履歴を読み込んでいます…</div>
    <div v-else-if="errorMessage" class="alert alert-danger" role="alert">{{ errorMessage }}</div>
    <div v-else-if="!orders.length" class="history-state card">
      <h2>注文履歴はまだありません</h2><p>商品をカゴへ追加して購入すると、こちらに表示されます。</p>
      <button type="button" class="btn" @click="$emit('back')">商品を見る</button>
    </div>
    <div v-else class="order-list">
      <article v-for="order in orders" :key="order.id" class="order-card card">
        <header class="order-heading">
          <div><span>注文番号</span><strong>#{{ order.id }}</strong></div>
          <div><span>注文日時</span><strong>{{ dateTime.format(new Date(order.orderedAt)) }}</strong></div>
          <span class="status-badge">{{ order.statusLabel }}</span>
        </header>
        <div class="order-items">
          <div v-for="item in order.items" :key="item.productId" class="order-item">
            <div class="item-image" :style="{ '--product-accent': item.accentColor }">
              <img v-if="item.imageUrl && !failedImages.has(imageKey(order.id, item.productId))" :src="item.imageUrl" :alt="item.productName"
                @error="failedImages = new Set([...failedImages, imageKey(order.id, item.productId)])" />
              <ProductIllustration v-else :kind="item.illustration" :label="item.label" :accent-color="item.accentColor" />
            </div>
            <div class="item-description"><strong>{{ item.productName }}</strong><span>{{ item.size }}</span><span>{{ yen.format(item.unitPrice) }} × {{ item.quantity }}点</span></div>
            <strong class="subtotal">{{ yen.format(item.subtotal) }}</strong>
          </div>
        </div>
        <footer class="order-total"><span>合計</span><strong>{{ yen.format(order.totalAmount) }}</strong></footer>
      </article>
    </div>
  </div>
</template>

<style scoped>
.order-history { width: 100%; padding: 12px 0 32px; }.history-header { display: flex; align-items: flex-end; justify-content: space-between; gap: 24px; margin-bottom: 28px; }.section-label { display: block; color: var(--primary); font-size: 11px; font-weight: 700; letter-spacing: .14em; }.history-header h1 { margin: 5px 0 7px; text-align: left; font-size: 30px; }.history-header p, .history-state p { font-size: 13px; }.back-button { padding: 10px 15px; border: 1px solid var(--input-border); border-radius: var(--radius-md); background: var(--card-bg); color: var(--text-primary); font: inherit; font-size: 12px; cursor: pointer; }.back-button:hover { border-color: var(--primary); color: var(--primary); }.history-state { padding: 56px 24px; text-align: center; }.history-state h2 { margin-bottom: 10px; font-size: 20px; }.history-state .btn { max-width: 220px; margin-top: 24px; }.order-list { display: grid; gap: 20px; }.order-card { padding: 0; overflow: hidden; }.order-card:hover { transform: none; }.order-heading { display: flex; align-items: center; gap: 36px; padding: 20px 24px; border-bottom: 1px solid var(--input-border); background: var(--input-bg); }.order-heading div span { display: block; color: var(--text-secondary); font-size: 10px; }.order-heading div strong { display: block; margin-top: 4px; font-size: 13px; }.status-badge { margin-left: auto; padding: 6px 11px; border-radius: 18px; background: var(--success-bg); color: var(--success); font-size: 11px; font-weight: 700; }.order-items { padding: 6px 24px; }.order-item { display: grid; grid-template-columns: 72px 1fr auto; align-items: center; gap: 16px; padding: 16px 0; border-bottom: 1px solid var(--input-border); }.order-item:last-child { border-bottom: 0; }.item-image { width: 72px; height: 72px; overflow: hidden; border-radius: 9px; background: color-mix(in srgb, var(--product-accent) 12%, var(--input-bg)); }.item-image img { width: 100%; height: 100%; object-fit: contain; }.item-description strong, .item-description span { display: block; }.item-description strong { margin-bottom: 5px; font-size: 14px; }.item-description span { color: var(--text-secondary); font-size: 11px; line-height: 1.7; }.subtotal { font-size: 15px; }.order-total { display: flex; justify-content: flex-end; align-items: baseline; gap: 24px; padding: 17px 24px; border-top: 1px solid var(--input-border); background: var(--input-bg); }.order-total span { font-size: 12px; }.order-total strong { font-size: 21px; }
@media (max-width: 600px) { .history-header { align-items: flex-start; flex-direction: column; }.order-heading { align-items: flex-start; gap: 16px; flex-wrap: wrap; }.status-badge { margin-left: 0; }.order-items { padding: 4px 18px; }.order-item { grid-template-columns: 60px 1fr; gap: 12px; }.item-image { width: 60px; height: 60px; }.subtotal { grid-column: 2; }.order-total { padding: 15px 18px; } }
</style>
