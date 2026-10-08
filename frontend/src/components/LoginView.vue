<script setup lang="ts">
import { ref } from 'vue'
import { loginCustomer } from '../services/authenticationApi'

const emit = defineEmits<{
  (e: 'change-view', view: 'register'): void
  (e: 'login-success', username: string): void
}>()
const userid = ref('')
const password = ref('')
const error = ref('')
const isSubmitting = ref(false)

/** 入力を確認してログインし、成功した表示名を親画面へ通知する。 */
const handleLogin = async () => {
  error.value = ''
  if (!userid.value.trim() || !password.value) {
    error.value = 'ユーザIDとパスワードを入力してください'
    return
  }
  isSubmitting.value = true
  try {
    const displayName = await loginCustomer(userid.value, password.value)
    emit('login-success', displayName)
  } catch (errorResponse: unknown) {
    error.value = errorResponse instanceof Error ? errorResponse.message : '通信エラーが発生しました'
  } finally {
    isSubmitting.value = false
  }
}
/** 新規登録画面への切り替えを親画面へ通知する。 */
const goToRegister = () => emit('change-view', 'register')
</script>

<template>
  <div class="card">
    <div class="logo-container">
      <img src="/himalaya.png" alt="ヒマラヤ" class="logo-image" />
    </div>
    
    <h1>Web ショップ販売 システム</h1>
    <h2>ログインしてください</h2>

    <div v-if="error" class="alert alert-danger">
      {{ error }}
    </div>

    <form @submit.prevent="handleLogin">
      <div class="form-group">
        <label for="userid">ユーザID</label>
        <input
          type="text"
          id="userid"
          v-model="userid"
          autocomplete="username"
          class="input-control"
          placeholder="ユーザーIDを入力"
        />
      </div>

      <div class="form-group">
        <label for="password">パスワード</label>
        <input
          type="password"
          id="password"
          v-model="password"
          autocomplete="current-password"
          class="input-control"
          placeholder="パスワードを入力"
        />
      </div>

      <button type="submit" class="btn" :disabled="isSubmitting">{{ isSubmitting ? 'ログイン中…' : 'ログイン' }}</button>
    </form>

    <div class="divider"></div>
    <p class="text-center mb-4" style="font-size: 14px; font-weight: 500;">
      新規登録の方はユーザID登録ボタンをクリックしてください
    </p>
    <button type="button" @click="goToRegister" class="btn btn-secondary">ユーザID登録</button>
  </div>
</template>
