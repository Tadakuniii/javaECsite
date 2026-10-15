<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { getManagedProducts, saveProduct, type ManagedProduct, type ProductInput } from '../services/productManagementApi'
import type { ProductCategory } from '../services/productApi'

const emit = defineEmits<{ (e: 'back'): void }>()
const categories: ProductCategory[] = ['サプリメント', '食品・飲料', '健康グッズ']
const products = ref<ManagedProduct[]>([])
const editingProductId = ref<number>()
const isLoading = ref(true)
const isSaving = ref(false)
const errorMessage = ref('')
const successMessage = ref('')
const emptyForm = (): ProductInput => ({ name: '', category: 'サプリメント', description: '', price: 0, stockQuantity: 0, imageUrl: '', active: true })
const form = reactive<ProductInput>(emptyForm())

onMounted(async () => {
  try { products.value = await getManagedProducts() }
  catch (error: unknown) { errorMessage.value = error instanceof Error ? error.message : '商品情報を取得できませんでした' }
  finally { isLoading.value = false }
})

const resetForm = () => {
  Object.assign(form, emptyForm())
  editingProductId.value = undefined
  errorMessage.value = ''
  successMessage.value = ''
}

const editProduct = (product: ManagedProduct) => {
  editingProductId.value = product.id
  Object.assign(form, { name: product.name, category: product.category, description: product.description, price: product.price, stockQuantity: product.stockQuantity, imageUrl: product.imageUrl || '', active: product.active })
  errorMessage.value = ''
  successMessage.value = ''
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

const submitProduct = async () => {
  errorMessage.value = ''
  successMessage.value = ''
  isSaving.value = true
  try {
    const isEditing = editingProductId.value !== undefined
    const saved = await saveProduct({ ...form }, editingProductId.value)
    const index = products.value.findIndex(product => product.id === saved.id)
    if (index >= 0) products.value[index] = saved
    else products.value.push(saved)
    successMessage.value = isEditing ? '商品情報を更新しました' : '商品を登録しました'
    Object.assign(form, emptyForm())
    editingProductId.value = undefined
  } catch (error: unknown) {
    errorMessage.value = error instanceof Error ? error.message : '商品情報を保存できませんでした'
  } finally { isSaving.value = false }
}

const yen = new Intl.NumberFormat('ja-JP', { style: 'currency', currency: 'JPY' })
</script>

<template>
  <div class="management-page">
    <header class="management-header">
      <div><span class="eyebrow">ADMINISTRATION</span><h1>商品管理</h1><p>商品の登録と販売情報の更新</p></div>
      <button type="button" class="back-button" @click="emit('back')">← 商品一覧へ戻る</button>
    </header>

    <section class="editor-card card" aria-labelledby="editor-title">
      <div class="editor-heading">
        <div><span class="eyebrow">PRODUCT EDITOR</span><h2 id="editor-title">{{ editingProductId ? '商品情報を編集' : '新しい商品を登録' }}</h2></div>
        <button v-if="editingProductId" type="button" class="text-button" @click="resetForm">新規登録に戻す</button>
      </div>
      <div v-if="errorMessage" class="alert alert-danger" role="alert">{{ errorMessage }}</div>
      <div v-if="successMessage" class="alert alert-success" role="status">{{ successMessage }}</div>
      <form class="product-form" @submit.prevent="submitProduct">
        <div class="form-group wide"><label for="product-name">商品名</label><input id="product-name" v-model.trim="form.name" class="input-control" maxlength="200" required /></div>
        <div class="form-group"><label for="category">カテゴリ</label><select id="category" v-model="form.category" class="input-control"><option v-for="category in categories" :key="category">{{ category }}</option></select></div>
        <div class="form-group"><label for="stock">数量（在庫数）</label><input id="stock" v-model.number="form.stockQuantity" class="input-control" type="number" min="0" required /></div>
        <div class="form-group"><label for="price">価格（税込・円）</label><input id="price" v-model.number="form.price" class="input-control" type="number" min="0" required /></div>
        <div class="form-group"><label for="image-url">画像URL（任意）</label><input id="image-url" v-model.trim="form.imageUrl" class="input-control" type="url" maxlength="500" placeholder="未設定でも登録できます" /></div>
        <div class="form-group wide"><label for="description">説明</label><textarea id="description" v-model.trim="form.description" class="input-control" rows="4" maxlength="2000" required></textarea></div>
        <label class="sale-switch wide"><input v-model="form.active" type="checkbox" /><span><strong>販売する</strong><small>オフにすると商品一覧に表示されません</small></span></label>
        <div class="form-actions wide"><button type="submit" class="btn" :disabled="isSaving">{{ isSaving ? '保存中…' : editingProductId ? '変更を保存' : '商品を登録' }}</button></div>
      </form>
    </section>

    <section class="inventory" aria-labelledby="inventory-title">
      <div class="inventory-heading"><div><span class="eyebrow">INVENTORY</span><h2 id="inventory-title">登録済みの商品</h2></div><span>{{ products.length }}件</span></div>
      <p v-if="isLoading" role="status">商品を読み込んでいます…</p>
      <div v-else class="product-rows">
        <article v-for="product in products" :key="product.id" class="product-row card">
          <div class="thumbnail"><img v-if="product.imageUrl" :src="product.imageUrl" :alt="product.name" /><span v-else>画像なし</span></div>
          <div class="product-summary"><span class="category">{{ product.category }}</span><h3>{{ product.name }}</h3><p>{{ yen.format(product.price) }} · 在庫 {{ product.stockQuantity }}</p></div>
          <span :class="['status', { inactive: !product.active }]">{{ product.active ? '販売中' : '販売停止' }}</span>
          <button type="button" class="edit-button" @click="editProduct(product)">編集</button>
        </article>
      </div>
    </section>
  </div>
</template>

<style scoped>
.management-page { width: 100%; padding-top: 12px; }
.management-header, .editor-heading, .inventory-heading, .product-row { display: flex; align-items: center; justify-content: space-between; gap: 20px; }
.management-header { margin-bottom: 28px; }.management-header h1 { text-align: left; margin: 2px 0; font-size: 32px; }.management-header p { font-size: 13px; }
.eyebrow { color: var(--primary); font-size: 10px; font-weight: 700; letter-spacing: .14em; }
.back-button, .text-button, .edit-button { border: 1px solid var(--input-border); background: var(--card-bg); color: var(--primary); border-radius: var(--radius-md); padding: 10px 14px; font: inherit; cursor: pointer; }
.editor-card { padding: 30px; }.editor-card:hover, .product-row:hover { transform: none; }.editor-heading { margin-bottom: 24px; }.editor-heading h2, .inventory-heading h2 { color: var(--text-primary); text-align: left; margin: 4px 0 0; font-size: 21px; font-weight: 700; }
.product-form { display: grid; grid-template-columns: 1fr 1fr; gap: 0 22px; }.wide { grid-column: 1 / -1; }.form-group textarea { resize: vertical; }.sale-switch { display: flex; align-items: center; gap: 12px; padding: 14px 16px; border: 1px solid var(--input-border); border-radius: var(--radius-md); background: var(--input-bg); cursor: pointer; }.sale-switch input { width: 20px; height: 20px; accent-color: var(--primary); }.sale-switch span { display: flex; flex-direction: column; gap: 2px; }.sale-switch small { color: var(--text-secondary); }.form-actions { display: flex; justify-content: flex-end; margin-top: 22px; }.form-actions .btn { width: min(260px, 100%); }
.inventory { margin-top: 36px; }.inventory-heading { margin-bottom: 16px; }.inventory-heading > span { color: var(--text-secondary); font-size: 13px; }.product-rows { display: grid; gap: 12px; }.product-row { padding: 16px 18px; }.thumbnail { width: 72px; height: 64px; flex: 0 0 72px; display: grid; place-items: center; background: var(--input-bg); border: 1px solid var(--input-border); border-radius: 10px; color: var(--text-secondary); font-size: 10px; }.thumbnail img { width: 100%; height: 100%; object-fit: contain; border-radius: 10px; }.product-summary { flex: 1; min-width: 0; }.product-summary h3 { margin: 3px 0; font-size: 16px; }.product-summary p, .category { color: var(--text-secondary); font-size: 11px; }.category { color: var(--primary); }.status { color: var(--success); background: var(--success-bg); border-radius: 20px; padding: 6px 10px; font-size: 11px; white-space: nowrap; }.status.inactive { color: var(--text-secondary); background: var(--input-bg); }.edit-button { min-width: 72px; }
@media (max-width: 650px) { .management-header { align-items: flex-start; flex-direction: column; }.product-form { grid-template-columns: 1fr; }.wide { grid-column: auto; }.editor-card { padding: 24px 20px; }.product-row { flex-wrap: wrap; }.product-summary { min-width: calc(100% - 100px); }.status { margin-left: 92px; }.edit-button { flex: 1; }.editor-heading { align-items: flex-start; } }
</style>
