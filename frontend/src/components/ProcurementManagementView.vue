<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { getManagedProducts, type ManagedProduct } from '../services/productManagementApi'
import { createPurchaseOrder, getPurchaseOrders, receivePurchaseOrder, type PurchaseOrder } from '../services/procurementApi'

defineEmits<{ (e: 'back'): void; (e: 'open-products'): void }>()
const products = ref<ManagedProduct[]>([])
const orders = ref<PurchaseOrder[]>([])
const isLoading = ref(true)
const isSaving = ref(false)
const receivingOrderId = ref<number>()
const errorMessage = ref('')
const successMessage = ref('')
const localDate = (date: Date) => {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}
const tomorrow = new Date()
tomorrow.setDate(tomorrow.getDate() + 1)
const form = reactive({ productId: 0, supplierName: '', expectedDeliveryDate: localDate(tomorrow), unitCost: 0, quantity: 1 })
const yen = new Intl.NumberFormat('ja-JP', { style: 'currency', currency: 'JPY' })
const dateTime = new Intl.DateTimeFormat('ja-JP', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
const selectedProduct = computed(() => products.value.find(product => product.id === form.productId))
const estimatedTotal = computed(() => form.unitCost * form.quantity)

onMounted(async () => {
  try {
    const [loadedProducts, loadedOrders] = await Promise.all([getManagedProducts(), getPurchaseOrders()])
    products.value = loadedProducts
    orders.value = loadedOrders
    if (loadedProducts.length) form.productId = loadedProducts[0]!.id
  } catch (error: unknown) {
    errorMessage.value = error instanceof Error ? error.message : '発注情報を取得できませんでした'
  } finally { isLoading.value = false }
})

const submitOrder = async () => {
  if (isSaving.value || receivingOrderId.value !== undefined) return
  errorMessage.value = ''
  successMessage.value = ''
  isSaving.value = true
  try {
    const created = await createPurchaseOrder({ ...form })
    orders.value.unshift(created)
    successMessage.value = `${created.productName}を発注しました`
    form.supplierName = ''
    form.quantity = 1
    form.unitCost = 0
  } catch (error: unknown) {
    errorMessage.value = error instanceof Error ? error.message : '発注を登録できませんでした'
  } finally { isSaving.value = false }
}

const receiveOrder = async (order: PurchaseOrder) => {
  if (isSaving.value || receivingOrderId.value !== undefined) return
  errorMessage.value = ''
  successMessage.value = ''
  receivingOrderId.value = order.id
  try {
    const received = await receivePurchaseOrder(order.id)
    const orderIndex = orders.value.findIndex(item => item.id === received.id)
    if (orderIndex >= 0) orders.value[orderIndex] = received
    successMessage.value = `${received.productName}の入荷を在庫へ反映しました`
    try {
      const [latestProducts, latestOrders] = await Promise.all([getManagedProducts(), getPurchaseOrders()])
      products.value = latestProducts
      orders.value = latestOrders
    } catch {
      errorMessage.value = '入荷は反映されましたが、最新の商品情報と発注履歴を再取得できませんでした'
    }
  } catch (error: unknown) {
    errorMessage.value = error instanceof Error ? error.message : '入荷を登録できませんでした'
  } finally { receivingOrderId.value = undefined }
}
</script>

<template>
  <div class="procurement-page">
    <header class="page-header">
      <div><span class="eyebrow">PROCUREMENT</span><h1>商品発注管理</h1><p>商品の発注登録、自動発注、入荷履歴を管理します。</p></div>
      <div class="header-actions"><button type="button" @click="$emit('open-products')">商品・自動発注設定</button><button type="button" @click="$emit('back')">商品一覧へ戻る</button></div>
    </header>
    <div v-if="errorMessage" class="alert alert-danger" role="alert">{{ errorMessage }}</div>
    <div v-if="successMessage" class="alert alert-success" role="status">{{ successMessage }}</div>

    <section class="order-editor card" aria-labelledby="order-editor-title">
      <div class="section-heading"><div><span class="eyebrow">NEW ORDER</span><h2 id="order-editor-title">商品を発注する</h2></div><span v-if="selectedProduct">現在庫 {{ selectedProduct.stockQuantity }}</span></div>
      <form class="order-form" @submit.prevent="submitOrder">
        <div class="form-group wide"><label for="order-product">商品</label><select id="order-product" v-model.number="form.productId" class="input-control" required><option v-for="product in products" :key="product.id" :value="product.id">{{ product.name }}（在庫 {{ product.stockQuantity }}）</option></select></div>
        <div class="form-group"><label for="supplier">発注先</label><input id="supplier" v-model.trim="form.supplierName" class="input-control" maxlength="200" required /></div>
        <div class="form-group"><label for="delivery-date">納期</label><input id="delivery-date" v-model="form.expectedDeliveryDate" class="input-control" type="date" :min="localDate(new Date())" required /></div>
        <div class="form-group"><label for="unit-cost">単価（円）</label><input id="unit-cost" v-model.number="form.unitCost" class="input-control" type="number" min="0" required /></div>
        <div class="form-group"><label for="order-quantity">発注数</label><input id="order-quantity" v-model.number="form.quantity" class="input-control" type="number" min="1" required /></div>
        <div class="order-summary wide"><span>発注金額</span><strong>{{ yen.format(estimatedTotal) }}</strong><button type="submit" class="btn" :disabled="isSaving || receivingOrderId !== undefined || !products.length">{{ isSaving ? '登録中…' : '発注を登録' }}</button></div>
      </form>
    </section>

    <section class="history" aria-labelledby="procurement-history-title">
      <div class="section-heading"><div><span class="eyebrow">ORDER HISTORY</span><h2 id="procurement-history-title">発注履歴</h2></div><span>{{ orders.length }}件</span></div>
      <p v-if="isLoading" role="status">発注履歴を読み込んでいます…</p>
      <div v-else-if="!orders.length" class="empty-history card">発注履歴はまだありません。</div>
      <div v-else class="history-list">
        <article v-for="order in orders" :key="order.id" class="history-card card">
          <header><div><span class="order-number">発注番号 #{{ order.id }}</span><h3>{{ order.productName }}</h3></div><div class="badges"><span v-if="order.automatic" class="auto-badge">自動発注</span><span :class="['status', { received: order.status === 'RECEIVED' }]">{{ order.statusLabel }}</span></div></header>
          <dl><div><dt>発注先</dt><dd>{{ order.supplierName }}</dd></div><div><dt>発注日時</dt><dd>{{ dateTime.format(new Date(order.orderedAt)) }}</dd></div><div><dt>納期</dt><dd>{{ order.expectedDeliveryDate }}</dd></div><div><dt>単価</dt><dd>{{ yen.format(order.unitCost) }}</dd></div><div><dt>発注数</dt><dd>{{ order.quantity }}</dd></div><div><dt>発注金額</dt><dd>{{ yen.format(order.totalCost) }}</dd></div></dl>
          <button v-if="order.status === 'ORDERED'" type="button" class="receive-button" :disabled="isSaving || receivingOrderId !== undefined" @click="receiveOrder(order)">{{ receivingOrderId === order.id ? '反映中…' : '入荷済みにして在庫へ反映' }}</button>
        </article>
      </div>
    </section>
  </div>
</template>

<style scoped>
.procurement-page { width: 100%; padding: 12px 0 36px; }.page-header, .section-heading, .history-card header { display: flex; align-items: center; justify-content: space-between; gap: 20px; }.page-header { margin-bottom: 28px; }.page-header h1 { margin: 3px 0; text-align: left; font-size: 31px; }.page-header p { font-size: 13px; }.eyebrow { color: var(--primary); font-size: 10px; font-weight: 700; letter-spacing: .14em; }.header-actions { display: flex; gap: 8px; }.header-actions button, .receive-button { padding: 10px 13px; border: 1px solid var(--input-border); border-radius: var(--radius-md); background: var(--card-bg); color: var(--primary); font: inherit; font-size: 11px; cursor: pointer; }.order-editor { padding: 28px; }.order-editor:hover, .history-card:hover { transform: none; }.section-heading { margin-bottom: 22px; }.section-heading h2 { margin-top: 4px; text-align: left; font-size: 21px; }.section-heading > span { color: var(--text-secondary); font-size: 12px; }.order-form { display: grid; grid-template-columns: 1fr 1fr; gap: 0 20px; }.wide { grid-column: 1 / -1; }.order-summary { display: flex; align-items: center; justify-content: flex-end; gap: 18px; margin-top: 18px; padding-top: 18px; border-top: 1px solid var(--input-border); }.order-summary span { font-size: 12px; }.order-summary strong { margin-right: auto; font-size: 22px; }.order-summary .btn { width: 220px; }.history { margin-top: 36px; }.history-list { display: grid; gap: 14px; }.history-card { padding: 20px; }.history-card h3 { margin-top: 4px; font-size: 17px; }.order-number { color: var(--text-secondary); font-size: 10px; }.badges { display: flex; gap: 7px; }.auto-badge, .status { padding: 6px 9px; border-radius: 16px; font-size: 10px; }.auto-badge { color: var(--primary); background: var(--primary-glow); }.status { color: #9a6700; background: #fff3cd; }.status.received { color: var(--success); background: var(--success-bg); }.history-card dl { display: grid; grid-template-columns: repeat(3, 1fr); gap: 14px 24px; margin-top: 18px; padding-top: 16px; border-top: 1px solid var(--input-border); }.history-card dl div { display: flex; justify-content: space-between; gap: 10px; }.history-card dt { color: var(--text-secondary); font-size: 10px; }.history-card dd { font-size: 11px; font-weight: 600; }.receive-button { display: block; margin: 18px 0 0 auto; }.receive-button:disabled { opacity: .55; cursor: wait; }.empty-history { padding: 42px; text-align: center; color: var(--text-secondary); }
@media (max-width: 700px) { .page-header { align-items: flex-start; flex-direction: column; }.header-actions { flex-wrap: wrap; }.order-form { grid-template-columns: 1fr; }.wide { grid-column: auto; }.order-summary { flex-wrap: wrap; }.order-summary .btn { width: 100%; }.history-card dl { grid-template-columns: 1fr; }.history-card header { align-items: flex-start; }.badges { flex-direction: column; } }
</style>
