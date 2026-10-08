<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { getAuthenticatedCustomer } from './services/authenticationApi'
import LoginView from './components/LoginView.vue'
import RegisterView from './components/RegisterView.vue'
import SuccessView from './components/SuccessView.vue'
import RegisterSuccessView from './components/RegisterSuccessView.vue'

type ViewState = 'login' | 'register' | 'success' | 'register-success'

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

/** ログイン中の表示名を保存し、成功画面を表示する。 */
const onLoginSuccess = (username: string) => {
  loggedInUser.value = username
  currentView.value = 'success'
}

/** 画面上のログイン情報を消去し、ログイン画面へ戻る。 */
const onLogoutSuccess = () => {
  loggedInUser.value = ''
  currentView.value = 'login'
}
</script>

<template>
  <main id="app-container">
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
        <SuccessView
          v-else-if="currentView === 'success'"
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
  min-height: 100vh;
}
</style>
