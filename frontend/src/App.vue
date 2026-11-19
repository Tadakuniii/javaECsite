<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { getAuthenticatedCustomer } from './services/authenticationApi'
import LoginView from './components/LoginView.vue'
import RegisterView from './components/RegisterView.vue'
import MainMenuView from './components/MainMenuView.vue'
import ProductManagementView from './components/ProductManagementView.vue'
import RegisterSuccessView from './components/RegisterSuccessView.vue'
import OrderHistoryView from './components/OrderHistoryView.vue'

type ViewState = 'login' | 'register' | 'main-menu' | 'product-management' | 'order-history' | 'register-success'

const currentView = ref<ViewState>('login')
const loggedInUser = ref('')
const isAdministrator = ref(false)
const isRestoringSession = ref(true)
const sessionError = ref('')
const purchaseCompleted = ref(false)
/** 指定された認証画面へ切り替える。 */
const changeView = (view: ViewState) => {
  currentView.value = view
}

onMounted(async () => {
  try {
    const customer = await getAuthenticatedCustomer()
    if (customer?.username) onLoginSuccess(customer.username, customer.administrator === true)
  } catch {
    sessionError.value = 'ログイン状態を確認できませんでした。接続を確認して再読み込みしてください。'
  } finally {
    isRestoringSession.value = false
  }
})

/** ログイン中の表示名を保存し、メインメニューを表示する。 */
const onLoginSuccess = (username: string, administrator = false) => {
  loggedInUser.value = username
  isAdministrator.value = administrator
  currentView.value = 'main-menu'
}

/** 画面上のログイン情報を消去し、ログイン画面へ戻る。 */
const onLogoutSuccess = () => {
  loggedInUser.value = ''
  isAdministrator.value = false
  currentView.value = 'login'
}

const openOrderHistory = (purchased: boolean) => {
  purchaseCompleted.value = purchased
  currentView.value = 'order-history'
}
</script>

<template>
  <main id="app-container" :class="{ 'wide-layout': currentView === 'main-menu' || currentView === 'product-management' || currentView === 'order-history' }">
    <p v-if="isRestoringSession" role="status">ログイン状態を確認中…</p>
    <template v-else>
      <div v-if="sessionError" class="alert alert-danger">{{ sessionError }}</div>
      <Transition name="fade" mode="out-in">
        <LoginView
          v-if="currentView === 'login'"
          @change-view="changeView"
          @login-success="onLoginSuccess"
        />
        <RegisterView
          v-else-if="currentView === 'register'"
          @change-view="changeView"
        />
        <MainMenuView
          v-else-if="currentView === 'main-menu'"
          :username="loggedInUser"
          :administrator="isAdministrator"
          @open-product-management="currentView = 'product-management'"
          @open-order-history="openOrderHistory"
          @logout-success="onLogoutSuccess"
        />
        <ProductManagementView
          v-else-if="currentView === 'product-management' && isAdministrator"
          @back="currentView = 'main-menu'"
        />
        <div v-else-if="currentView === 'order-history'" class="order-history-wrapper">
          <div v-if="purchaseCompleted" class="alert purchase-complete" role="status">購入が完了しました。ご注文ありがとうございます。</div>
          <OrderHistoryView @back="purchaseCompleted = false; currentView = 'main-menu'" />
        </div>
        <RegisterSuccessView
          v-else-if="currentView === 'register-success'"
          @change-view="changeView"
        />
      </Transition>
    </template>
  </main>
</template>

<style scoped>
#app-container {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  width: 100%;
  max-width: 440px;
  margin: 0 auto;
  min-height: calc(100dvh - 40px);
}

#app-container.wide-layout {
  max-width: 1120px;
  justify-content: flex-start;
}

.order-history-wrapper { width: 100%; }
.purchase-complete { margin-top: 12px; color: var(--success); background: var(--success-bg); border: 1px solid color-mix(in srgb, var(--success) 30%, transparent); }
</style>
