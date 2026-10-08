<script setup lang="ts">
const emit = defineEmits<{ (e: 'change-view', view: 'login'): void }>()
/** 登録後に通常のログイン画面へ戻る。 */
const handleReturnToLogin = () => emit('change-view', 'login')
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
    
    <p class="mb-4">登録したユーザIDとパスワードでログインしてください</p>

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
