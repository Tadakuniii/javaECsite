<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import { submitAuthenticationForm } from '../services/authenticationApi'
import { getAvailableProducts, type HealthProduct, type ProductCategory } from '../services/productApi'
import ProductIllustration from './ProductIllustration.vue'
import ShopHeader from './ShopHeader.vue'
import ShopIcon from './ShopIcon.vue'
import CartView from './CartView.vue'
import { addCartProduct, cartQuantity, setCartQuantity, type CartLine } from '../services/cart'

defineProps<{ username: string }>()
const emit = defineEmits<{ (e: 'logout-success'): void }>()
const categories: ('すべて' | ProductCategory)[] = ['すべて', 'サプリメント', '食品・飲料', '健康グッズ']
const selectedCategory = ref<(typeof categories)[number]>('すべて')
const products = ref<HealthProduct[]>([])
const productLoadingError = ref('')
const isLoadingProducts = ref(true)
const searchQuery = ref('')
const selectedProduct = ref<HealthProduct | null>(null)
const productDialog = ref<HTMLDialogElement | null>(null)
const logoutError = ref('')
const isLoggingOut = ref(false)
const isCart = ref(false)
const cartLines = ref<CartLine[]>([])
const cartCount = computed(() => cartQuantity(cartLines.value))
const cartMessage = ref('')
const priceFormatter = new Intl.NumberFormat('ja-JP', { style: 'currency', currency: 'JPY' })

const syncShopLocation = async () => {
  isCart.value = window.location.hash === '#cart'
  cartMessage.value = ''
  await nextTick()
  const headingId = isCart.value ? 'cart-title' : window.location.hash === '#product-list' ? 'products-title' : 'menu-title'
  document.getElementById(headingId)?.focus({ preventScroll: true })
  if (window.location.hash === '#product-list') document.getElementById('product-list')?.scrollIntoView()
  else window.scrollTo({ top: 0 })
}

const availableToAdd = (product: HealthProduct) =>
  (cartLines.value.find(line => line.product.id === product.id)?.quantity ?? 0) < product.stockQuantity

const addToCart = async (product: HealthProduct) => {
  if (!availableToAdd(product)) return
  cartLines.value = addCartProduct(cartLines.value, product)
  cartMessage.value = `${product.name}をカートに追加しました。`
  if (selectedProduct.value) closeProductDetails()
  if (isCart.value) {
    await nextTick()
    document.getElementById('cart-title')?.focus({ preventScroll: true })
  }
}

const updateQuantity = (productId: number, quantity: number) => {
  cartLines.value = setCartQuantity(cartLines.value, productId, quantity)
  cartMessage.value = `カートの数量を更新しました。現在${cartCount.value}点です。`
}

const removeFromCart = async (productId: number) => {
  const product = cartLines.value.find(line => line.product.id === productId)?.product
  cartLines.value = cartLines.value.filter(line => line.product.id !== productId)
  cartMessage.value = `${product?.name ?? '商品'}をカートから削除しました。`
  await nextTick()
  document.getElementById('cart-title')?.focus({ preventScroll: true })
}

const visibleProducts = computed(() => {
  const query = searchQuery.value.trim().toLocaleLowerCase('ja-JP')
  return products.value.filter(product =>
    (selectedCategory.value === 'すべて' || product.category === selectedCategory.value)
    && (!query || `${product.name} ${product.category} ${product.description}`.toLocaleLowerCase('ja-JP').includes(query)),
  )
})

/** ログイン後にDBから販売中の商品を読み込む。 */
onMounted(async () => {
  isCart.value = window.location.hash === '#cart'
  window.addEventListener('hashchange', syncShopLocation)
  try {
    products.value = await getAvailableProducts()
  } catch (error: unknown) {
    productLoadingError.value = error instanceof Error ? error.message : '商品情報を取得できませんでした'
  } finally {
    isLoadingProducts.value = false
  }
})

onUnmounted(() => window.removeEventListener('hashchange', syncShopLocation))

/** 選択商品の情報を表示し、ダイアログ内へフォーカスを移す。 */
const openProductDetails = async (product: HealthProduct) => {
  selectedProduct.value = product
  await nextTick()
  productDialog.value?.showModal()
}

/** 商品情報を閉じ、元の商品ボタンへフォーカスを戻す。 */
const closeProductDetails = () => { productDialog.value?.close() }

/** 検索語とカテゴリの絞り込みを解除する。 */
const resetProductFilters = () => {
  searchQuery.value = ''
  selectedCategory.value = 'すべて'
}

/** サーバーのログイン状態を破棄してログイン画面へ戻る。 */
const handleLogout = async () => {
  logoutError.value = ''
  isLoggingOut.value = true
  try {
    await submitAuthenticationForm('/api/logout')
    emit('logout-success')
  } catch (error: unknown) {
    logoutError.value = error instanceof Error ? error.message : 'ログアウトに失敗しました'
  } finally {
    isLoggingOut.value = false
  }
}
</script>

<template>
  <div class="main-menu">
    <ShopHeader :username="username" :count="cartCount" :is-cart="isCart" :is-logging-out="isLoggingOut" @logout="handleLogout" />
    <div v-if="logoutError" class="alert alert-danger" role="alert">{{ logoutError }}</div>
    <div class="cart-feedback" :class="{ visible: cartMessage }">
      <div class="cart-announcement" role="status" aria-atomic="true"><span v-if="cartMessage"><ShopIcon name="check" />{{ cartMessage }}</span></div>
      <div v-if="cartMessage" class="cart-toast">
        <a v-if="!isCart" href="#cart">カートを見る <ShopIcon name="arrow" /></a>
        <button type="button" aria-label="通知を閉じる" @click="cartMessage = ''">×</button>
      </div>
    </div>

    <CartView v-if="isCart" :lines="cartLines" :products="products" @add="addToCart" @update-quantity="updateQuantity" @remove="removeFromCart" />
    <template v-else>

    <section class="welcome-card card" aria-labelledby="menu-title">
      <div class="welcome-copy">
        <span class="section-label">毎日の健康と暮らしに</span>
        <h1 id="menu-title" tabindex="-1">健やかな毎日を、<br />あなたらしく。</h1>
        <p>サプリメントから、暮らしに寄り添う健康グッズまで。<br class="desktop-break" />日々の習慣に取り入れたいアイテムを見つけてみませんか。</p>
        <a href="#product-list" class="browse-products">商品を見る <span aria-hidden="true">↓</span></a>
      </div>
      <div class="welcome-art" aria-hidden="true">
        <div class="art-circle"></div>
        <div class="hero-tea"><ProductIllustration kind="tea" label="HERBAL TEA" accent-color="#689b87" /></div>
        <div class="hero-bottle"><ProductIllustration kind="bottle" label="VITAMIN C" accent-color="#7881b9" /></div>
        <span class="art-sparkle sparkle-one">✦</span><span class="art-sparkle sparkle-two">✦</span>
      </div>
    </section>

    <section id="product-list" class="products-section" aria-labelledby="products-title">
      <div class="products-heading">
        <div><span class="section-label">お気に入りのアイテムを探す</span><h2 id="products-title" tabindex="-1">商品一覧</h2></div>
        <span class="sample-label">サンプル商品</span>
      </div>
      <div class="product-toolbar">
        <div class="category-filters" role="group" aria-label="商品カテゴリ">
          <button v-for="category in categories" :key="category" type="button"
            :class="['category-button', { selected: selectedCategory === category }]"
            :aria-pressed="selectedCategory === category" @click="selectedCategory = category">{{ category }}</button>
        </div>
        <div class="search-field">
          <svg viewBox="0 0 24 24" fill="none" aria-hidden="true"><circle cx="10.5" cy="10.5" r="6.5" /><path d="m16 16 4 4" /></svg>
          <input v-model="searchQuery" type="search" aria-label="商品を検索" placeholder="商品を検索" />
        </div>
      </div>
      <p v-if="isLoadingProducts" class="product-count" role="status">商品を読み込んでいます…</p>
      <div v-else-if="productLoadingError" class="alert alert-danger" role="alert">{{ productLoadingError }}</div>
      <template v-else>
      <p class="product-count" role="status">{{ visibleProducts.length }}件の商品</p>
      <div v-if="visibleProducts.length" class="product-grid">
        <article v-for="product in visibleProducts" :key="product.productCode" class="product-card card">
          <div class="product-art" :style="{ '--product-accent': product.accentColor }">
            <ProductIllustration :kind="product.illustration" :label="product.label" :accent-color="product.accentColor" />
          </div>
          <div class="product-content">
            <span class="product-category">{{ product.category }}</span>
            <h3>{{ product.name }}</h3>
            <p class="product-size">{{ product.size }}</p>
            <div class="product-bottom"><p class="product-price">{{ priceFormatter.format(product.price) }}<span>（税込）</span></p></div>
            <button type="button" class="product-details-button" :aria-label="`${product.name}の詳細を見る`" @click="openProductDetails(product)">
              詳細を見る <span aria-hidden="true">→</span>
            </button>
            <button type="button" class="btn add-cart-button" :disabled="!availableToAdd(product)" :aria-label="`${product.name}をカートに追加`" @click="addToCart(product)">
              <ShopIcon name="cart" />{{ product.stockQuantity < 1 ? '在庫なし' : !availableToAdd(product) ? '在庫数の上限です' : 'カートに追加' }}
            </button>
          </div>
        </article>
      </div>
      <div v-else class="empty-products card">
        <h3>条件に合う商品が見つかりませんでした</h3>
        <p>別のキーワードやカテゴリでお試しください。</p>
        <button type="button" class="btn btn-secondary" @click="resetProductFilters">絞り込みを解除</button>
      </div>
      </template>
      <p class="sample-note">掲載商品・価格はサンプルです。カートへの追加をお試しいただけます。購入機能は準備中です。</p>
    </section>
    </template>
    <footer class="menu-footer">Web Shop <span>毎日の暮らしに、健やかな選択を。</span></footer>

    <dialog ref="productDialog" class="product-dialog" aria-labelledby="product-detail-title" @close="selectedProduct = null">
      <div v-if="selectedProduct" class="dialog-content">
        <button type="button" class="dialog-close" aria-label="商品詳細を閉じる" autofocus @click="closeProductDetails">×</button>
        <div class="detail-art" :style="{ '--product-accent': selectedProduct.accentColor }">
          <ProductIllustration :kind="selectedProduct.illustration" :label="selectedProduct.label" :accent-color="selectedProduct.accentColor" />
        </div>
        <span class="product-category">{{ selectedProduct.category }}</span>
        <h2 id="product-detail-title">{{ selectedProduct.name }}</h2>
        <p class="detail-description">{{ selectedProduct.description }}</p>
        <dl class="product-specifications"><div><dt>内容量・サイズ</dt><dd>{{ selectedProduct.size }}</dd></div><div><dt>価格（税込）</dt><dd>{{ priceFormatter.format(selectedProduct.price) }}</dd></div></dl>
        <p class="sample-note">商品情報はサンプルです。</p>
        <button type="button" class="btn add-cart-button" :disabled="!availableToAdd(selectedProduct)" @click="addToCart(selectedProduct)"><ShopIcon name="cart" />{{ selectedProduct.stockQuantity < 1 ? '在庫なし' : !availableToAdd(selectedProduct) ? '在庫数の上限です' : 'カートに追加' }}</button>
        <button type="button" class="btn btn-secondary" @click="closeProductDetails">商品一覧に戻る</button>
      </div>
    </dialog>
  </div>
</template>

<style scoped>
.main-menu { width: 100%; padding-top: 96px; }
.add-cart-button { display: flex; align-items: center; justify-content: center; gap: 10px; margin-top: 10px; font-size: 12px; padding: 12px; }.add-cart-button svg { width: 18px; height: 18px; }.add-cart-button:disabled { cursor: not-allowed; }
.cart-feedback { position: fixed; z-index: 40; bottom: 24px; left: 50%; transform: translateX(-50%); width: max-content; max-width: calc(100% - 32px); display: flex; align-items: center; gap: 20px; padding: 15px 18px; border: 1px solid var(--input-border); border-radius: 12px; background: var(--input-bg); backdrop-filter: blur(20px); box-shadow: var(--shadow-lg); opacity: 0; pointer-events: none; }.cart-feedback.visible { opacity: 1; pointer-events: auto; }
.cart-announcement { font-size: 12px; }.cart-announcement span { display: flex; align-items: center; gap: 10px; }.cart-announcement svg { color: var(--primary); width: 18px; height: 18px; }
.cart-toast { display: flex; align-items: center; gap: 16px; flex-shrink: 0; }.cart-toast a { display: inline-flex; align-items: center; gap: 7px; font-size: 12px; color: var(--primary); text-decoration: none; }.cart-toast svg { width: 17px; }.cart-toast button { border: 1px solid var(--input-border); background: var(--card-bg); color: var(--text-secondary); border-radius: 50%; width: 26px; height: 26px; cursor: pointer; font-size: 18px; }
#menu-title:focus, #products-title:focus { outline: none; }
.welcome-card { display: grid; grid-template-columns: 1.4fr 1fr; align-items: center; padding: 44px 48px; overflow: hidden; background: linear-gradient(115deg, var(--card-bg), var(--primary-glow)); }
.welcome-card:hover { transform: none; }
.section-label { display: block; color: var(--primary); font-size: 12px; font-weight: 600; letter-spacing: 0.08em; margin-bottom: 12px; }
.welcome-copy h1 { font-size: clamp(29px, 3.8vw, 44px); text-align: left; line-height: 1.45; letter-spacing: 0.025em; margin-bottom: 16px; }
.welcome-copy p { font-size: 13px; line-height: 1.9; }
.browse-products { display: inline-flex; gap: 32px; align-items: center; margin-top: 24px; padding: 12px 20px; background: var(--primary); color: white; text-decoration: none; border-radius: var(--radius-md); font-size: 13px; font-weight: 600; box-shadow: 0 6px 16px var(--primary-glow); }
.browse-products:hover { background: var(--primary-hover); }
.welcome-art { height: 260px; position: relative; }
.art-circle { position: absolute; width: 240px; height: 240px; border-radius: 50%; background: var(--card-bg); left: 50%; top: 50%; transform: translate(-50%, -50%); border: 1px solid var(--card-border); }
.hero-tea { position: absolute; width: 260px; height: 240px; left: calc(50% - 180px); top: 0; transform: rotate(-10deg); }
.hero-bottle { position: absolute; width: 245px; height: 230px; left: calc(50% - 65px); top: 45px; transform: rotate(12deg); }
.art-sparkle { position: absolute; color: var(--primary); opacity: 0.6; font-size: 24px; }
.sparkle-one { left: 18%; top: 16px; }.sparkle-two { right: 8%; bottom: 10px; font-size: 16px; }
.products-section { margin-top: 40px; scroll-margin-top: 112px; }
.products-heading { display: flex; justify-content: space-between; align-items: center; margin-bottom: 22px; gap: 16px; }
.products-heading .section-label { margin-bottom: 6px; }
.products-heading h2 { text-align: left; color: var(--text-primary); font-size: 25px; font-weight: 700; margin: 0; }
.sample-label { background: var(--card-bg); border: 1px solid var(--card-border); padding: 5px 10px; border-radius: 20px; font-size: 11px; color: var(--text-secondary); white-space: nowrap; }
.product-toolbar { display: flex; justify-content: space-between; align-items: center; gap: 20px; flex-wrap: wrap; }
.category-filters { display: flex; flex-wrap: wrap; gap: 8px; }
.category-button { padding: 10px 16px; border: 1px solid var(--input-border); background: var(--card-bg); color: var(--text-secondary); border-radius: 24px; font: inherit; font-size: 12px; font-weight: 500; cursor: pointer; }
.category-button.selected { color: white; background: var(--primary); border-color: var(--primary); }
.category-button:hover:not(.selected) { color: var(--primary); border-color: var(--primary); }
.search-field { display: flex; gap: 8px; align-items: center; padding: 10px 12px; background: var(--input-bg); border: 1px solid var(--input-border); border-radius: var(--radius-md); width: 230px; }
.search-field:focus-within { border-color: var(--primary); box-shadow: 0 0 0 3px var(--primary-glow); }
.search-field svg { width: 18px; height: 18px; stroke: var(--text-secondary); stroke-width: 1.7; flex-shrink: 0; }
.search-field input { border: 0; outline: 0; color: var(--text-primary); background: transparent; font: inherit; font-size: 13px; width: 100%; min-width: 0; }
.product-count { margin: 16px 0 12px; font-size: 12px; }
.product-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 22px; }
.product-card { padding: 0; overflow: hidden; }
.product-art, .detail-art { background: color-mix(in srgb, var(--product-accent) 12%, var(--input-bg)); }
.product-art { height: 210px; padding: 6px 20px; border-bottom: 1px solid var(--card-border); }
.product-content { padding: 22px; }
.product-category { display: block; font-size: 11px; color: var(--primary); margin-bottom: 9px; font-weight: 600; }
.product-content h3 { color: var(--text-primary); font-size: 17px; font-weight: 600; line-height: 1.5; }
.product-size { font-size: 12px; margin-top: 6px; }
.product-price { font-size: 22px; color: var(--text-primary); font-weight: 600; margin-top: 18px; }
.product-price span { font-size: 10px; color: var(--text-secondary); font-weight: 400; margin-left: 6px; }
.product-details-button { display: flex; justify-content: space-between; align-items: center; width: 100%; margin-top: 16px; padding: 11px 14px; border: 1px solid var(--input-border); border-radius: var(--radius-md); color: var(--primary); background: transparent; font: inherit; font-size: 12px; font-weight: 600; cursor: pointer; }
.product-details-button:hover { background: var(--primary-glow); border-color: var(--primary); }
.empty-products { text-align: center; padding: 40px 20px; }
.empty-products h3 { font-size: 17px; margin-bottom: 12px; }.empty-products p { font-size: 13px; }
.empty-products .btn { max-width: 220px; margin-top: 22px; }
.sample-note { font-size: 11px; text-align: center; margin-top: 22px; }
.menu-footer { display: flex; justify-content: space-between; gap: 20px; padding: 24px 0 12px; margin-top: 24px; border-top: 1px solid var(--input-border); color: var(--text-secondary); font-size: 12px; }
.menu-footer span { font-size: 11px; }
.product-dialog { margin: auto; padding: 0; width: min(440px, calc(100% - 32px)); max-height: calc(100dvh - 40px); border: 1px solid var(--card-border); border-radius: var(--radius-lg); background: var(--input-bg); color: var(--text-primary); box-shadow: var(--shadow-lg); }
.product-dialog::backdrop { background: rgba(15, 23, 42, 0.5); backdrop-filter: blur(4px); }
.dialog-content { padding: 28px; position: relative; }
.dialog-close { position: absolute; top: 12px; right: 12px; width: 34px; height: 34px; font-size: 25px; color: var(--text-secondary); background: var(--card-bg); border: 1px solid var(--input-border); border-radius: 50%; cursor: pointer; }
.detail-art { height: 210px; border-radius: 10px; margin-bottom: 22px; }
.dialog-content h2 { color: var(--text-primary); text-align: left; font-size: 22px; margin-bottom: 14px; }
.detail-description { font-size: 13px; }
.product-specifications { margin-top: 20px; font-size: 13px; }
.product-specifications div { display: flex; justify-content: space-between; gap: 12px; border-bottom: 1px solid var(--input-border); padding: 12px 0; }
.product-specifications dt { color: var(--text-secondary); }
.dialog-content .btn { margin-top: 20px; font-size: 13px; }
button:focus-visible, a:focus-visible { outline: 3px solid var(--primary); outline-offset: 3px; }
@media (max-width: 900px) {
  .welcome-card { padding: 32px; grid-template-columns: 1.4fr 1fr; }
  .welcome-art { transform: scale(0.85); }
  .product-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
@media (max-width: 600px) {
  .main-menu { padding-top: 80px; }
  .cart-feedback { bottom: 16px; padding: 13px; gap: 12px; flex-wrap: wrap; }.cart-announcement { font-size: 11px; }.cart-toast { margin-left: auto; }
  .welcome-card { grid-template-columns: 1fr; padding: 28px 24px 12px; }
  .welcome-copy h1 { font-size: 30px; }.welcome-copy p { font-size: 12px; }
  .welcome-art { height: 230px; transform: scale(0.75); margin: 0; }
  .desktop-break { display: none; }.browse-products { margin-top: 20px; }
  .products-section { margin-top: 30px; }.products-heading h2 { font-size: 23px; }
  .product-toolbar { gap: 14px; }.search-field { width: 100%; }
  .category-button { padding: 9px 12px; font-size: 11px; }
  .product-grid { grid-template-columns: 1fr; gap: 18px; }
  .product-art { height: 210px; }.product-content { padding: 20px; }
  .menu-footer { flex-direction: column; gap: 6px; }.sample-note { line-height: 1.8; }
}
@media (prefers-reduced-motion: reduce) { .product-card { transition: none; }.product-card:hover { transform: none; } }
</style>
