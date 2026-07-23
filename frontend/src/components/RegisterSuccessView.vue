<script setup lang="ts">
import { ref } from 'vue'

const emit = defineEmits<{
  (e: 'change-view-debug'): void
  (e: 'change-view', view: 'login'): void
}>()

const error = ref('')

const handleReturnToLogin = async () => {
  try {
    const params = new URLSearchParams()
    params.append('userid', 'LOgIN0000')
    params.append('password', '00000000')

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

    if (data.status === 'DEBUG_LOGIN') {
      emit('change-view-debug')
    } else {
      // 想定外の場合は通常のログイン画面へ
      emit('change-view', 'login')
    }
  } catch (err: any) {
    error.value = 'ログイン画面の切り替えに失敗しました。通常のログイン画面に戻ります。'
    setTimeout(() => {
      emit('change-view', 'login')
    }, 2000)
  }
}
</script>

<template>
  <div class="card text-center">
    <div class="success-icon-container">
      <svg class="success-icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
        <path d="M12 2C6.48 2 2 6.48 2 12C2 17.52 6.48 22 12 22C17.52 22 22 17.52 22 12C22 6.48 17.52 2 12 2ZM12 17H10V15H12V17ZM12 13H10V7H12V13Z" fill="currentColor"/>
      </svg>
    </div>
    
    <h1>登録完了</h1>
    <h2 class="mt-4">ユーザIDが登録されました</h2>
    
    <div v-if="error" class="alert alert-danger">
      {{ error }}
    </div>

    <p class="mb-4" style="font-size: 14px; font-weight: 500;">
      新規登録の方はユーザID登録ボタンをクリックしてください
    </p>

    <div class="divider"></div>
    <button @click="handleReturnToLogin" class="btn">ログイン画面に戻る</button>
  </div>
</template>

<style scoped>
.success-icon-container {
  display: flex;
  justify-content: center;
  margin-bottom: 20px;
  color: var(--primary);
}
.success-icon {
  width: 64px;
  height: 64px;
}
</style>
