<script setup lang="ts">
import { ref } from 'vue'
import { submitAuthenticationForm } from '../services/authenticationApi'

const emit = defineEmits<{
  (e: 'change-view', view: 'register' | 'login' | 'success' | 'register-success'): void
}>()

const userid = ref('')
const password1 = ref('')
const password2 = ref('')
const username = ref('')
const error = ref('')
const isSubmitting = ref(false)

/** 会員情報を送信し、登録結果または入力エラーを表示する。 */
const handleRegister = async () => {
  error.value = ''

  // クライアント側簡易チェック
  if (!userid.value || !password1.value || !password2.value) {
    error.value = '必須項目を入力してください。'
    return
  }

  isSubmitting.value = true
  try {
    const data = await submitAuthenticationForm('/api/register', {
      userid: userid.value,
      password1: password1.value,
      password2: password2.value,
      username: username.value,
    })

    if (data.status === 'SUCCESS') {
      emit('change-view', 'register-success')
    } else if (data.status === 'PASSWORD_MISMATCH') {
      error.value = data.message || '異なるパスワードが入力されました'
    } else if (data.status === 'EXISTING_ID') {
      error.value = data.message || '入力されたユーザIDは登録済です'
    } else {
      error.value = data.message || '登録に失敗しました。'
    }
  } catch (err: unknown) {
    error.value = err instanceof Error ? err.message : '通信エラーが発生しました。'
  } finally {
    isSubmitting.value = false
  }
}

/** ログイン画面への切り替えを親画面へ通知する。 */
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
          placeholder="例：user-01_test"
          autocomplete="username"
          minlength="3" maxlength="64" pattern="[A-Za-z0-9_-]{3,64}"
          required
        />
        <p class="input-hint">半角英数字・「-」・「_」で3〜64文字</p>
      </div>

      <div class="form-group">
        <label for="password1">パスワード *</label>
        <input
          type="password"
          id="password1"
          v-model="password1"
          autocomplete="new-password" minlength="8" maxlength="72"
          class="input-control"
          placeholder="8文字以上で入力してください"
          required
        />
      </div>

      <div class="form-group">
        <label for="password2">同じパスワードを入力してください *</label>
        <input
          type="password"
          id="password2"
          v-model="password2"
          autocomplete="new-password" minlength="8" maxlength="72"
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
          autocomplete="name" maxlength="100"
          class="input-control"
          placeholder="例：山田 太郎"
        />
      </div>

      <button type="submit" class="btn" :disabled="isSubmitting">{{ isSubmitting ? '登録中…' : '登録' }}</button>
    </form>

    <div class="divider"></div>
    <button @click="goToLogin" class="btn btn-secondary">ログイン画面に戻る</button>
  </div>
</template>

<style scoped>
.input-hint {
  margin-top: 6px;
  font-size: 13px;
}
</style>
