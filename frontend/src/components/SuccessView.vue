<script setup lang="ts">
import { ref } from 'vue'
import { submitAuthenticationForm } from '../services/authenticationApi'
const error = ref('')
const isSubmitting = ref(false)
const props = defineProps<{
  username: string
}>()

const emit = defineEmits<{
  (e: 'logout-success'): void
}>()

/** サーバーのセッションを破棄した後にログアウト完了を通知する。 */
const handleLogout = async () => {
  error.value = ''
  isSubmitting.value = true
  try {
    await submitAuthenticationForm('/api/logout')
    emit('logout-success')
  } catch (errorResponse: unknown) {
    error.value = errorResponse instanceof Error ? errorResponse.message : 'ログアウトに失敗しました'
  } finally {
    isSubmitting.value = false
  }
}
</script>

<template>
  <div class="card text-center">
    <div class="success-icon-container">
      <svg class="success-icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
        <path d="M12 2C6.48 2 2 6.48 2 12C2 17.52 6.48 22 12 22C17.52 22 22 17.52 22 12C22 6.48 17.52 2 12 2ZM10 17L5 12L6.41 10.59L10 14.17L17.59 6.58L19 8L10 17Z" fill="currentColor"/>
      </svg>
    </div>
    
    <h1>ログイン成功</h1>
    <h2 class="mt-4">
      <span>{{ props.username }} 様</span>
    </h2>
    <p class="mb-4">Web Shop にご来店ありがとうございます</p>
    
    <div class="divider"></div>
    <div v-if="error" class="alert alert-danger">{{ error }}</div>
    <button @click="handleLogout" class="btn" :disabled="isSubmitting">{{ isSubmitting ? 'ログアウト中…' : 'ログアウト' }}</button>
  </div>
</template>

<style scoped>
.success-icon-container {
  display: flex;
  justify-content: center;
  margin-bottom: 20px;
  color: var(--success);
}
.success-icon {
  width: 64px;
  height: 64px;
}
</style>
