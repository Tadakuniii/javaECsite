<script setup lang="ts">
import ShopIcon from './ShopIcon.vue'

defineProps<{ username: string; administrator: boolean; count: number; isCart: boolean; isLoggingOut: boolean }>()
defineEmits<{ logout: []; 'open-product-management': [] }>()
</script>

<template>
  <header class="shop-header">
    <div class="header-inner">
      <a class="shop-brand" href="#shop" aria-label="Web Shop 商品一覧へ">
        <img src="/himalaya.png" alt="" class="brand-logo" />
        <span><span class="brand-name">Web Shop<span class="brand-dot">.</span></span><span class="brand-caption">サプリメントと健康雑貨</span></span>
      </a>
      <nav class="header-actions" aria-label="ショップメニュー">
        <a class="catalog-link" href="#product-list" :aria-current="!isCart ? 'page' : undefined">商品一覧</a>
        <span class="welcome-message"><span>ようこそ</span><strong>{{ username }} 様</strong></span>
        <button v-if="administrator" type="button" class="logout-button" @click="$emit('open-product-management')">商品管理</button>
        <button type="button" class="logout-button" :disabled="isLoggingOut" @click="$emit('logout')">{{ isLoggingOut ? 'ログアウト中…' : 'ログアウト' }}</button>
        <a class="cart-link" :class="{ active: isCart }" href="#cart" :aria-current="isCart ? 'page' : undefined" :aria-label="`カート、${count}点の商品`">
          <span class="cart-icon"><ShopIcon name="cart" /><span class="cart-badge">{{ count > 99 ? '99+' : count }}</span></span>
          <span class="cart-label">カート</span>
        </a>
      </nav>
    </div>
  </header>
</template>

<style scoped>
.shop-header { position: fixed; inset: 0 0 auto; z-index: 30; background: var(--input-bg); border-bottom: 1px solid var(--input-border); backdrop-filter: blur(20px); box-shadow: 0 4px 24px #0f172a06; }
.header-inner { max-width: 1160px; min-height: 88px; padding: 14px 20px; margin: auto; display: flex; align-items: center; justify-content: space-between; gap: 24px; }
.shop-brand { display: flex; align-items: center; gap: 12px; text-decoration: none; color: var(--text-primary); flex-shrink: 0; }
.brand-logo { width: 58px; height: 46px; object-fit: contain; border-radius: 8px; }
.brand-name { display: block; font-size: 24px; letter-spacing: -0.7px; font-weight: 700; }
.brand-dot { color: var(--primary); }
.brand-caption { display: block; margin-top: 2px; font-size: 10px; color: var(--text-secondary); }
.header-actions { display: flex; align-items: center; gap: 24px; }
.catalog-link { text-decoration: none; color: var(--text-secondary); font-size: 13px; white-space: nowrap; }
.catalog-link:hover, .catalog-link[aria-current] { color: var(--primary); }
.welcome-message { display: flex; flex-direction: column; gap: 3px; font-size: 10px; color: var(--text-secondary); }
.welcome-message strong { font-size: 12px; color: var(--text-primary); max-width: 150px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.logout-button { padding: 9px 12px; border: 1px solid var(--input-border); border-radius: var(--radius-md); background: transparent; color: var(--text-secondary); font: inherit; font-size: 11px; white-space: nowrap; cursor: pointer; }
.logout-button:hover { color: var(--primary); border-color: var(--primary); }
.logout-button:disabled { opacity: 0.6; cursor: wait; }
.cart-link { display: flex; align-items: center; gap: 14px; min-height: 46px; padding: 10px 17px; border: 1px solid var(--input-border); border-radius: 12px; color: var(--primary); background: var(--card-bg); text-decoration: none; font-size: 12px; font-weight: 600; }
.cart-link.active, .cart-link:hover { background: var(--primary); border-color: var(--primary); color: white; }
.cart-icon { position: relative; display: flex; }
.cart-badge { position: absolute; top: -9px; right: -10px; min-width: 18px; height: 18px; padding: 0 4px; border: 2px solid var(--input-bg); border-radius: 12px; display: grid; place-items: center; background: var(--primary); color: white; font-size: 9px; line-height: 1; }
.cart-link.active .cart-badge { color: var(--primary); background: white; }
a:focus-visible, button:focus-visible { outline: 3px solid var(--primary); outline-offset: 4px; }
@media (max-width: 800px) { .header-actions { gap: 12px; }.welcome-message { display: none; } }
@media (max-width: 520px) {
  .header-inner { min-height: 76px; gap: 8px; padding: 12px 16px; }
  .shop-brand { gap: 6px; }.brand-logo { width: 35px; height: 32px; }.brand-name { font-size: 20px; }.brand-caption { font-size: 8px; }
  .header-actions { gap: 10px; }.catalog-link, .cart-label { display: none; }.cart-link { padding: 11px; min-width: 46px; }
  .logout-button { border: 0; padding: 8px 0; font-size: 10px; }
}
</style>
