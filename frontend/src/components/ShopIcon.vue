<script setup lang="ts">
defineProps<{ name: 'cart' | 'arrow' | 'trash' | 'leaf' | 'check' | 'bag' }>()
</script>

<template>
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.65" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
    <g v-if="name === 'cart'"><path d="M3 3h2l2.5 12h11L21 6H6" /><circle cx="9" cy="20" r="1" /><circle cx="18" cy="20" r="1" /></g>
    <path v-else-if="name === 'arrow'" d="M4 12h16m-6-6 6 6-6 6" />
    <g v-else-if="name === 'trash'"><path d="M3 6h18M9 6V3h6v3M5 6l1 15h12l1-15M10 10v7m4-7v7" /></g>
    <g v-else-if="name === 'leaf'"><path d="M20 3C9 2 3 7 5 14c2 7 15 7 15-11ZM4 21 15 9" /></g>
    <path v-else-if="name === 'check'" d="m5 12 4 4L19 6" />
    <g v-else><path d="M5 7h14l1 14H4L5 7Z" /><path d="M9 8V6a3 3 0 0 1 6 0v2" /></g>
  </svg>
</template>

<style scoped>
svg { width: 22px; height: 22px; flex-shrink: 0; }
</style>
