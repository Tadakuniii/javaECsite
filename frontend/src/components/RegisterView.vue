<script setup lang="ts">
import { ref } from 'vue'

const emit = defineEmits<{
  (e: 'change-view', view: 'register' | 'login' | 'success' | 'register-success'): void
}>()

const userid = ref('')
const password1 = ref('')
const password2 = ref('')
const username = ref('')
const error = ref('')

const handleRegister = async () => {
  error.value = ''

  // クライアント側簡易チェック
  if (!userid.value || !password1.value || !password2.value) {
    error.value = '必須項目を入力してください。'
    return
  }

  try {
    const params = new URLSearchParams()
    params.append('userid', userid.value)
    params.append('password1', password1.value)
    params.append('password2', password2.value)
    params.append('username', username.value)

    const response = await fetch('/api/register', {
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
      emit('change-view', 'register-success')
    } else if (data.status === 'PASSWORD_MISMATCH') {
      error.value = data.message || '異なるパスワードが入力されました'
    } else if (data.status === 'EXISTING_ID') {
      error.value = data.message || '入力されたユーザIDは登録済です'
    } else {
      error.value = data.message || '登録に失敗しました。'
    }
  } catch (err: any) {
    error.value = err.message || '通信エラーが発生しました。'
  }
}

const goToLogin = () => {
  emit('change-view', 'login')
}
</script>

<template>
  <div class="card">
    <h1>新規会員登録</h1>
    <h2>情報を入力してください</h2>

    <div v-if="error" class="alert alert-danger">
      {{ error }}
    </div>

    <form @submit.prevent="handleRegister">
      <div class="form-group">
        <label for="userid">ご希望のユーザID *</label>
        <input
          type="text"
          id="userid"
          v-model="userid"
          class="input-control"
          placeholder="ユーザーID"
          required
        />
      </div>

      <div class="form-group">
        <label for="password1">パスワード *</label>
        <input
          type="password"
          id="password1"
          v-model="password1"
          class="input-control"
          placeholder="パスワード"
          required
        />
      </div>

      <div class="form-group">
        <label for="password2">同じパスワードを入力してください *</label>
        <input
          type="password"
          id="password2"
          v-model="password2"
          class="input-control"
          placeholder="パスワードの確認"
          required
        />
      </div>

      <div class="form-group">
        <label for="username">お名前（姓名）</label>
        <input
          type="text"
          id="username"
          v-model="username"
          class="input-control"
          placeholder="例：山田 太郎"
        />
      </div>

      <button type="submit" class="btn">登録</button>
    </form>

    <div class="divider"></div>
    <button @click="goToLogin" class="btn btn-secondary">ログイン画面に戻る</button>
  </div>
</template>
