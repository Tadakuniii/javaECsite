<script setup lang="ts">
import { ref, watch } from 'vue'

const props = defineProps<{
  initialDebugMode?: boolean
}>()

const emit = defineEmits<{
  (e: 'change-view', view: 'register' | 'login' | 'success' | 'register-success'): void
  (e: 'login-success', username: string): void
}>()

const userid = ref('')
const password = ref('')
const error = ref('')
const isDebugMode = ref(props.initialDebugMode || false)

watch(() => props.initialDebugMode, (newVal) => {
  isDebugMode.value = newVal || false
})

const handleLogin = async () => {
  error.value = ''
  
  try {
    const params = new URLSearchParams()
    params.append('userid', userid.value)
    params.append('password', password.value)

    const response = await fetch('/api/login', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
      },
      body: params
    })

    if (!response.ok) {
      throw new Error('サーバーエラーが発生しました')
    }

    const data = await response.json()

    if (data.status === 'SUCCESS') {
      emit('login-success', data.username)
    } else if (data.status === 'EMPTY_ID') {
      // IDが空の場合は新規登録画面へ
      emit('change-view', 'register')
    } else if (data.status === 'DEBUG_LOGIN') {
      // デバッグモード：新規登録ボタンを隠してリロード
      isDebugMode.value = true
      userid.value = ''
      password.value = ''
      error.value = 'デバッグログイン：新規登録ボタンのないログイン画面に切り替えました。'
    } else if (data.status === 'RELOGIN') {
      error.value = data.message || 'ユーザーIDまたはパスワードが間違っています。'
    } else {
      error.value = '不明なエラーが発生しました。'
    }
  } catch (err: any) {
    error.value = err.message || '通信エラーが発生しました。'
  }
}

const goToRegister = () => {
  emit('change-view', 'register')
}
</script>

<template>
  <div class="card">
    <div class="logo-container">
      <img src="/himalaya.png" alt="ヒマラヤ" class="logo-image" />
    </div>
    
    <h1>Web ショップ販売 システム</h1>
    <h2>ログインしてください</h2>

    <div v-if="error" :class="['alert', isDebugMode ? 'alert-success' : 'alert-danger']">
      {{ error }}
    </div>

    <form @submit.prevent="handleLogin">
      <div class="form-group">
        <label for="userid">ユーザID</label>
        <input
          type="text"
          id="userid"
          v-model="userid"
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
          class="input-control"
          placeholder="パスワードを入力"
        />
      </div>

      <button type="submit" class="btn">ログイン</button>
    </form>

    <template v-if="!isDebugMode">
      <div class="divider"></div>
      <p class="text-center mb-4" style="font-size: 14px; font-weight: 500;">
        新規登録の方はユーザID登録ボタンをクリックしてください
      </p>
      <button @click="goToRegister" class="btn btn-secondary">ユーザID登録</button>
    </template>
  </div>
</template>
