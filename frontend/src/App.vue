<script setup lang="ts">
import { ref } from 'vue'
import LoginView from './components/LoginView.vue'
import RegisterView from './components/RegisterView.vue'
import SuccessView from './components/SuccessView.vue'
import RegisterSuccessView from './components/RegisterSuccessView.vue'

type ViewState = 'login' | 'register' | 'success' | 'register-success'

const currentView = ref<ViewState>('login')
const loggedInUser = ref('')
const loginDebugMode = ref(false)

const changeView = (view: ViewState) => {
  if (view !== 'login') {
    loginDebugMode.value = false
  }
  currentView.value = view
}

const onLoginSuccess = (username: string) => {
  loggedInUser.value = username
  currentView.value = 'success'
}

const onReturnDebug = () => {
  loginDebugMode.value = true
  currentView.value = 'login'
}
</script>

<template>
  <main id="app-container">
    <Transition name="fade" mode="out-in">
      <LoginView
        v-if="currentView === 'login'"
        :initial-debug-mode="loginDebugMode"
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
        @change-view="changeView"
      />
      <RegisterSuccessView
        v-else-if="currentView === 'register-success'"
        @change-view="changeView"
        @change-view-debug="onReturnDebug"
      />
    </Transition>
  </main>
</template>

<style scoped>
#app-container {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
  min-height: 100vh;
}
</style>
