<script setup lang="ts">
import { computed } from 'vue'
import { cartQuantity, cartSubtotal, type CartLine } from '../services/cart'
import type { HealthProduct } from '../services/productApi'
import ProductIllustration from './ProductIllustration.vue'
import ShopIcon from './ShopIcon.vue'

const props = defineProps<{ lines: CartLine[]; products: HealthProduct[] }>()
defineEmits<{
  'update-quantity': [productId: number, quantity: number]
  remove: [productId: number]
  add: [product: HealthProduct]
}>()
const count = computed(() => cartQuantity(props.lines))
const subtotal = computed(() => cartSubtotal(props.lines))
const recommendations = computed(() => props.products
  .filter(product => product.stockQuantity > 0 && !props.lines.some(line => line.product.id === product.id)).slice(0, 3))
const money = (value: number) => new Intl.NumberFormat('ja-JP', { style: 'currency', currency: 'JPY' }).format(value)
</script>

<template>
  <section class="cart-page" aria-labelledby="cart-title">
    <nav class="breadcrumbs" aria-label="パンくずリスト"><a href="#shop">ホーム</a><span aria-hidden="true">/</span><span aria-current="page">カート</span></nav>
    <div class="cart-heading">
      <div><span class="eyebrow">YOUR WELLNESS BAG</span><h1 id="cart-title" tabindex="-1">ショッピングカート<span class="heading-count">{{ count }}点</span></h1><p>毎日の習慣に、ちょっといい選択を。</p></div>
      <a href="#product-list" class="continue-link">お買い物を続ける <ShopIcon name="arrow" /></a>
    </div>

    <div v-if="lines.length" class="cart-layout">
      <div class="cart-main">
        <div class="wellness-note"><span class="note-icon"><ShopIcon name="leaf" /></span><div><strong>あなたらしい、健やかな毎日へ。</strong><p>お気に入りのアイテムを、いつもの暮らしに。</p></div><span class="note-sparkle" aria-hidden="true">✦</span></div>
        <section class="cart-items card" aria-label="カートの商品">
          <div class="items-heading"><h2>選んだアイテム</h2><span>{{ lines.length }}種類 / {{ count }}点</span></div>
          <article v-for="line in lines" :key="line.product.id" class="cart-item">
            <div class="item-art" :style="{ '--product-accent': line.product.accentColor }"><ProductIllustration :kind="line.product.illustration" :label="line.product.label" :accent-color="line.product.accentColor" /></div>
            <div class="item-details"><span class="item-category">{{ line.product.category }}</span><h3>{{ line.product.name }}</h3><p class="item-size">{{ line.product.size }}</p><p class="unit-price">{{ money(line.product.price) }} <span> / 点（税込）</span></p>
              <div class="item-controls"><div class="quantity-control" role="group" :aria-label="`${line.product.name}の数量`">
                <button type="button" :disabled="line.quantity <= 1" :aria-label="`${line.product.name}を1点減らす`" @click="$emit('update-quantity', line.product.id, line.quantity - 1)">−</button>
                <span :aria-label="`${line.quantity}点`">{{ line.quantity }}</span>
                <button type="button" :disabled="line.quantity >= line.product.stockQuantity" :aria-label="`${line.product.name}を1点増やす`" @click="$emit('update-quantity', line.product.id, line.quantity + 1)">＋</button>
              </div><button type="button" class="remove-button" :aria-label="`${line.product.name}をカートから削除`" @click="$emit('remove', line.product.id)"><ShopIcon name="trash" /><span>削除</span></button></div>
              <p v-if="line.quantity >= line.product.stockQuantity" class="stock-note">在庫数の上限です</p>
            </div>
            <p class="line-price">{{ money(line.product.price * line.quantity) }}<span>税込</span></p>
          </article>
        </section>
        <p class="cart-footnote">掲載商品・価格はサンプルです。カートは再読み込み・ログアウトでリセットされます。</p>
      </div>

      <aside class="order-summary card" aria-labelledby="summary-title">
        <span class="eyebrow">ORDER SUMMARY</span><h2 id="summary-title">ご注文の概要</h2>
        <dl class="summary-lines"><div><dt>商品小計（{{ count }}点）</dt><dd>{{ money(subtotal) }}</dd></div><div><dt>送料</dt><dd class="shipping-note">お手続き時に確定</dd></div></dl>
        <div class="summary-total"><div><span>商品合計</span><small>税込・送料別</small></div><strong>{{ money(subtotal) }}</strong></div>
        <div class="checkout-note"><ShopIcon name="bag" /><p>選んだアイテムを確認して、<br />毎日の新しい習慣の準備を。</p></div>
        <button type="button" class="btn checkout-button" disabled aria-describedby="checkout-availability">購入手続きへ <ShopIcon name="arrow" /></button>
        <p id="checkout-availability" class="checkout-availability">購入機能は準備中です。<br />現在はカートへの追加・編集をお試しいただけます。</p>
        <a href="#product-list" class="summary-continue">商品一覧に戻る</a>
      </aside>
    </div>

    <div v-else class="empty-cart card">
      <div class="empty-art" aria-hidden="true"><ShopIcon name="bag" /><span>✦</span></div>
      <span class="eyebrow">A LITTLE CARE, EVERY DAY</span><h2>毎日を整える、お気に入りを。</h2><p>カートにはまだ商品が入っていません。<br />あなたの暮らしに合うアイテムを探してみませんか。</p>
      <a href="#product-list" class="btn empty-browse">商品を探す <ShopIcon name="arrow" /></a>
    </div>

    <section v-if="recommendations.length" class="recommendations" aria-labelledby="recommendations-title">
      <div class="recommendation-heading"><div><span class="eyebrow">MAKE IT A DAILY HABIT</span><h2 id="recommendations-title">暮らしに、もうひとつの健やかさ。</h2></div><a href="#product-list" class="continue-link">すべての商品 <ShopIcon name="arrow" /></a></div>
      <div class="recommendation-grid"><article v-for="product in recommendations" :key="product.id" class="recommendation-card card">
        <div class="recommendation-art" :style="{ '--product-accent': product.accentColor }"><ProductIllustration :kind="product.illustration" :label="product.label" :accent-color="product.accentColor" /></div>
        <div class="recommendation-copy"><span class="item-category">{{ product.category }}</span><h3>{{ product.name }}</h3><p>{{ product.size }}</p><div class="recommendation-bottom"><strong>{{ money(product.price) }}<small>税込</small></strong><button type="button" class="quick-add" :aria-label="`${product.name}をカートに追加`" @click="$emit('add', product)"><ShopIcon name="cart" /><span>追加</span></button></div></div>
      </article></div>
    </section>
  </section>
</template>

<style scoped>
.breadcrumbs { display: flex; gap: 12px; align-items: center; font-size: 11px; color: var(--text-secondary); margin: 2px 0 30px; }
.breadcrumbs a { text-decoration: none; color: var(--text-secondary); }.breadcrumbs a:hover { color: var(--primary); }
.cart-heading { display: flex; justify-content: space-between; align-items: center; gap: 20px; margin-bottom: 30px; }
.eyebrow { display: block; font-size: 10px; font-weight: 600; letter-spacing: 0.15em; color: var(--primary); margin-bottom: 10px; }
.cart-heading h1 { display: flex; flex-wrap: wrap; align-items: center; gap: 14px; font-size: clamp(25px, 3vw, 34px); line-height: 1.5; text-align: left; background: none; -webkit-text-fill-color: currentColor; letter-spacing: 0.01em; }
.heading-count { background: var(--primary-glow); color: var(--primary); padding: 4px 11px; border-radius: 20px; font-size: 12px; font-weight: 500; white-space: nowrap; }
.cart-heading p { font-size: 12px; margin-top: 6px; }
.continue-link { display: inline-flex; align-items: center; justify-content: center; gap: 10px; font-size: 12px; font-weight: 500; color: var(--primary); text-decoration: none; flex-shrink: 0; }.continue-link svg { width: 17px; }
.cart-layout { display: grid; grid-template-columns: minmax(0, 1fr) 330px; align-items: start; gap: 28px; }
.wellness-note { display: flex; align-items: center; gap: 15px; padding: 19px 22px; margin-bottom: 20px; background: linear-gradient(105deg, var(--primary-glow), var(--card-bg)); border: 1px solid var(--card-border); border-radius: var(--radius-lg); }
.note-icon { display: grid; place-items: center; width: 42px; height: 42px; background: var(--card-bg); border-radius: 13px; color: var(--primary); flex-shrink: 0; }.wellness-note strong { font-size: 13px; font-weight: 600; }.wellness-note p { font-size: 11px; margin-top: 5px; }.note-sparkle { margin-left: auto; color: var(--primary); font-size: 25px; opacity: 0.6; }
.cart-items { padding: 0 24px; box-shadow: 0 8px 30px #0f172a05; }.cart-items:hover, .order-summary:hover, .empty-cart:hover { transform: none; }
.items-heading { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding: 23px 0; border-bottom: 1px solid var(--input-border); }
.items-heading h2 { color: var(--text-primary); font-size: 14px; font-weight: 600; margin: 0; }.items-heading > span { color: var(--text-secondary); font-size: 11px; }
.cart-item { display: grid; grid-template-columns: 112px minmax(0, 1fr) auto; gap: 20px; padding: 28px 0; align-items: start; }.cart-item + .cart-item { border-top: 1px solid var(--input-border); }
.item-art { width: 112px; height: 128px; display: flex; align-items: center; border: 1px solid var(--card-border); border-radius: 12px; background: color-mix(in srgb, var(--product-accent) 12%, var(--input-bg)); }
.item-category { display: block; color: var(--primary); font-weight: 500; font-size: 10px; margin-bottom: 7px; }
.item-details h3 { font-size: 15px; line-height: 1.6; font-weight: 600; }.item-size { font-size: 11px; margin-top: 3px; }.unit-price { font-size: 12px; color: var(--text-primary); margin-top: 10px; }.unit-price span { color: var(--text-secondary); font-size: 10px; }
.item-controls { display: flex; flex-wrap: wrap; align-items: center; gap: 14px; margin-top: 16px; }
.quantity-control { display: inline-flex; align-items: center; border: 1px solid var(--input-border); border-radius: 8px; overflow: hidden; background: var(--input-bg); }
.quantity-control button { width: 34px; height: 34px; background: transparent; border: 0; color: var(--text-primary); cursor: pointer; font: inherit; font-size: 16px; }.quantity-control span { min-width: 30px; text-align: center; font-size: 13px; font-variant-numeric: tabular-nums; }.quantity-control button:hover:not(:disabled) { background: var(--primary-glow); color: var(--primary); }.quantity-control button:disabled { opacity: 0.3; cursor: not-allowed; }
.remove-button { display: inline-flex; align-items: center; gap: 5px; min-height: 34px; padding: 4px; background: none; border: 0; font: inherit; font-size: 10px; color: var(--text-secondary); cursor: pointer; }.remove-button svg { width: 15px; height: 15px; }.remove-button:hover { color: var(--danger); }
.line-price { text-align: right; white-space: nowrap; font-size: 19px; font-weight: 600; color: var(--text-primary); line-height: 1.2; padding-top: 22px; font-variant-numeric: tabular-nums; }.line-price span { display: block; font-size: 9px; color: var(--text-secondary); margin-top: 8px; font-weight: 400; }
.stock-note { font-size: 10px; margin-top: 6px; }.cart-footnote { margin-top: 16px; font-size: 10px; line-height: 1.8; }
.order-summary { position: sticky; top: 112px; padding: 28px; box-shadow: 0 12px 35px #0f172a09; }.order-summary h2 { text-align: left; color: var(--text-primary); font-size: 21px; font-weight: 600; margin-bottom: 26px; }.order-summary .eyebrow { font-size: 9px; }
.summary-lines { display: grid; gap: 18px; padding-bottom: 25px; border-bottom: 1px solid var(--input-border); font-size: 12px; }.summary-lines div { display: flex; justify-content: space-between; gap: 10px; }.summary-lines dt { color: var(--text-secondary); }.summary-lines dd { font-weight: 500; }.summary-lines .shipping-note { color: var(--text-secondary); font-size: 11px; }
.summary-total { display: flex; justify-content: space-between; gap: 10px; align-items: center; margin-top: 24px; }.summary-total span { font-size: 14px; font-weight: 600; }.summary-total small { display: block; color: var(--text-secondary); font-size: 9px; margin-top: 4px; }.summary-total strong { font-size: 30px; letter-spacing: -1px; font-weight: 600; }
.checkout-note { display: flex; align-items: center; gap: 11px; background: var(--primary-glow); padding: 13px; border-radius: 10px; margin: 25px 0 18px; color: var(--primary); }.checkout-note p { color: var(--text-primary); font-size: 10px; line-height: 1.8; }
.checkout-button { display: flex; align-items: center; justify-content: space-between; font-size: 13px; padding: 15px 18px; }.checkout-button:disabled { cursor: not-allowed; box-shadow: none; opacity: 0.65; }.checkout-button svg { width: 18px; }
.checkout-availability { margin-top: 12px; text-align: center; font-size: 10px; line-height: 1.9; }.summary-continue { display: block; text-align: center; margin-top: 22px; color: var(--primary); font-size: 11px; text-underline-offset: 4px; }
.recommendations { margin-top: 46px; }.recommendation-heading { display: flex; justify-content: space-between; align-items: center; gap: 16px; margin-bottom: 20px; }.recommendation-heading h2 { font-size: 20px; font-weight: 600; margin: 0; color: var(--text-primary); text-align: left; }
.recommendation-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 20px; }.recommendation-card { padding: 0; overflow: hidden; box-shadow: 0 5px 20px #0f172a05; }.recommendation-art { height: 160px; padding: 4px 25px; background: color-mix(in srgb, var(--product-accent) 12%, var(--input-bg)); }.recommendation-copy { padding: 20px; }.recommendation-copy h3 { font-size: 14px; font-weight: 600; }.recommendation-copy > p { font-size: 11px; margin-top: 5px; }.recommendation-bottom { display: flex; justify-content: space-between; align-items: center; gap: 10px; margin-top: 17px; }.recommendation-bottom strong { font-size: 19px; font-weight: 600; }.recommendation-bottom small { color: var(--text-secondary); font-size: 9px; font-weight: 400; margin-left: 6px; }
.quick-add { display: flex; gap: 7px; align-items: center; padding: 8px 12px; background: var(--primary-glow); border: 1px solid transparent; color: var(--primary); font: inherit; font-size: 11px; border-radius: 8px; cursor: pointer; }.quick-add svg { width: 17px; height: 17px; }.quick-add:hover { border-color: var(--primary); }
.empty-cart { text-align: center; padding: 45px 24px; background: linear-gradient(120deg, var(--card-bg), var(--primary-glow)); }.empty-art { position: relative; display: grid; place-items: center; height: 90px; width: 90px; border-radius: 50%; margin: 0 auto 26px; background: var(--card-bg); color: var(--primary); }.empty-art svg { width: 40px; height: 40px; }.empty-art span { position: absolute; top: 0; right: 0; font-size: 23px; }.empty-cart h2 { color: var(--text-primary); font-size: 24px; font-weight: 600; margin-bottom: 14px; }.empty-cart p { font-size: 12px; line-height: 1.9; }.empty-browse { display: inline-flex; justify-content: center; align-items: center; gap: 25px; width: auto; font-size: 13px; text-decoration: none; margin-top: 26px; padding: 13px 26px; }
button:focus-visible, a:focus-visible { outline: 3px solid var(--primary); outline-offset: 3px; }h1:focus { outline: none; }
@media (max-width: 950px) { .cart-layout { grid-template-columns: minmax(0, 1fr) 290px; gap: 20px; }.cart-items { padding: 0 20px; }.cart-item { grid-template-columns: 80px minmax(0, 1fr); gap: 14px; }.item-art { width: 80px; height: 104px; }.line-price { grid-column: 2; padding: 0; text-align: left; }.line-price span { display: inline; margin-left: 6px; }.order-summary { padding: 24px; } }
@media (max-width: 760px) { .cart-layout { grid-template-columns: 1fr; }.order-summary { position: static; }.cart-heading { align-items: flex-start; }.cart-heading > .continue-link { display: none; }.recommendation-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }.recommendation-heading { align-items: flex-start; }.recommendation-heading h2 { font-size: 18px; }.recommendation-heading .continue-link { font-size: 10px; gap: 5px; }.cart-item { grid-template-columns: 100px minmax(0, 1fr) auto; }.item-art { width: 100px; height: 120px; }.line-price { grid-column: auto; padding-top: 20px; text-align: right; }.line-price span { display: block; }.order-summary h2 { margin-bottom: 20px; } }
@media (max-width: 520px) { .breadcrumbs { margin-bottom: 24px; }.cart-heading h1 { font-size: 24px; gap: 8px; }.heading-count { font-size: 10px; }.cart-heading { margin-bottom: 24px; }.wellness-note { padding: 16px; gap: 10px; }.wellness-note strong { font-size: 12px; }.wellness-note p { font-size: 10px; }.note-sparkle { display: none; }.cart-items { padding: 0 16px; }.cart-item { grid-template-columns: 76px minmax(0, 1fr); gap: 12px; padding: 22px 0; }.item-art { width: 76px; height: 100px; }.item-details h3 { font-size: 14px; }.line-price { grid-column: 2; padding-top: 0; text-align: left; }.line-price span { display: inline; margin-left: 8px; }.cart-footnote { font-size: 9px; }.recommendations { margin-top: 34px; }.recommendation-heading { flex-direction: column; }.recommendation-grid { grid-template-columns: 1fr; gap: 14px; }.recommendation-card { display: grid; grid-template-columns: 100px minmax(0, 1fr); }.recommendation-art { height: 100%; min-height: 150px; padding: 4px; display: flex; align-items: center; }.recommendation-copy { padding: 16px; }.recommendation-copy h3 { font-size: 13px; }.recommendation-bottom { margin-top: 12px; }.recommendation-bottom strong { font-size: 17px; }.quick-add { padding: 8px; }.empty-cart h2 { font-size: 20px; } }
@media (prefers-reduced-motion: reduce) { .card { transition: none; }.card:hover { transform: none; } }
</style>
