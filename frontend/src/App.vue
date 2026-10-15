<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { getAuthenticatedCustomer } from './services/authenticationApi'
import LoginView from './components/LoginView.vue'
import RegisterView from './components/RegisterView.vue'
import MainMenuView from './components/MainMenuView.vue'
import RegisterSuccessView from './components/RegisterSuccessView.vue'

type ViewState = 'login' | 'register' | 'main-menu' | 'register-success'

const currentView = ref<ViewState>('login')
const loggedInUser = ref('')
const isRestoringSession = ref(true)
const sessionError = ref('')
/** 指定された認証画面へ切り替える。 */
const changeView = (view: ViewState) => {
  currentView.value = view
}

onMounted(async () => {
  try {
    const customer = await getAuthenticatedCustomer()
    if (customer?.username) onLoginSuccess(customer.username)
  } catch {
    sessionError.value = 'ログイン状態を確認できませんでした。接続を確認して再読み込みしてください。'
  } finally {
    isRestoringSession.value = false
  }
})

/** ログイン中の表示名を保存し、メインメニューを表示する。 */
const onLoginSuccess = (username: string) => {
  loggedInUser.value = username
  currentView.value = 'main-menu'
}

/** 画面上のログイン情報を消去し、ログイン画面へ戻る。 */
const onLogoutSuccess = () => {
  loggedInUser.value = ''
  currentView.value = 'login'
}
</script>

<template>
  <main id="app-container" :class="{ 'main-menu-layout': currentView === 'main-menu' }">
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
          @logout-success="onLogoutSuccess"
        />
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

#app-container.main-menu-layout {
  max-width: 1120px;
  justify-content: flex-start;
}
</style>
