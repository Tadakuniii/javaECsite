<script setup lang="ts">
import type { ProductIllustrationKind } from '../services/productApi'
defineProps<{ kind: ProductIllustrationKind; label: string; accentColor: string }>()
</script>

<template>
  <svg class="product-illustration" viewBox="0 0 260 220" fill="none" aria-hidden="true">
    <ellipse cx="130" cy="193" rx="65" ry="9" fill="currentColor" opacity="0.08" />
    <g v-if="kind === 'bottle'">
      <rect x="88" y="47" width="84" height="145" rx="23" fill="#fafbf9" stroke="#dce1e5" />
      <rect x="96" y="30" width="68" height="31" rx="7" :fill="accentColor" />
      <path d="M104 37V54M115 37V54M126 37V54M137 37V54M148 37V54M158 37V54" stroke="white" opacity="0.22" />
      <rect x="88" y="97" width="84" height="64" :fill="accentColor" opacity="0.18" />
      <path d="M112 116H148M112 144H148" :stroke="accentColor" opacity="0.5" />
      <text x="130" y="133" text-anchor="middle" :fill="accentColor" font-size="10" font-weight="700">{{ label }}</text>
      <path d="M98 75V91" stroke="white" stroke-width="7" stroke-linecap="round" />
    </g>
    <g v-else-if="kind === 'pouch'">
      <path d="M81 39H179L190 192H70L81 39Z" fill="#faf8f3" stroke="#ddd7cc" />
      <path d="M83 48H177M83 56H177" stroke="#d3ccc0" stroke-width="3" />
      <path d="M76 109H184L187 164H73L76 109Z" :fill="accentColor" opacity="0.22" />
      <text x="130" y="138" text-anchor="middle" :fill="accentColor" font-size="12" font-weight="700">{{ label }}</text>
      <path d="M117 87Q129 68 143 81Q130 97 117 87Z" :fill="accentColor" opacity="0.6" />
    </g>
    <g v-else-if="kind === 'tea'">
      <rect x="72" y="52" width="105" height="139" rx="5" fill="#f7faf5" stroke="#d4dfd3" />
      <path d="M72 52L87 38H192L177 52M177 52L192 38V177L177 191" :fill="accentColor" opacity="0.3" />
      <rect x="72" y="109" width="105" height="60" :fill="accentColor" opacity="0.18" />
      <path d="M125 92V70M125 80Q101 60 110 86Q116 91 125 90M125 84Q150 59 143 86Q134 94 125 92" :stroke="accentColor" stroke-width="3" stroke-linecap="round" />
      <text x="125" y="142" text-anchor="middle" :fill="accentColor" font-size="11" font-weight="700">{{ label }}</text>
    </g>
    <g v-else-if="kind === 'water-bottle'">
      <rect x="104" y="24" width="52" height="26" rx="7" :fill="accentColor" />
      <path d="M111 50V62Q89 67 89 89V173Q89 193 109 193H151Q171 193 171 173V89Q171 67 149 62V50" fill="#eef6fa" stroke="#c2d6e0" stroke-width="2" />
      <path d="M92 116H168V171Q168 189 150 189H110Q92 189 92 171V116Z" :fill="accentColor" opacity="0.22" />
      <path d="M146 83H158M150 101H158M146 119H158M150 137H158M146 155H158" :stroke="accentColor" stroke-width="2" />
      <text x="125" y="146" text-anchor="middle" :fill="accentColor" font-size="9" font-weight="700">DAILY</text>
      <path d="M100 82V103" stroke="white" stroke-width="6" stroke-linecap="round" />
    </g>
    <g v-else>
      <path d="M85 118L176 172L208 138L116 86Z" :fill="accentColor" opacity="0.25" />
      <path d="M71 158L157 192Q186 203 198 179L208 151L118 103Z" :fill="accentColor" opacity="0.7" />
      <ellipse cx="91" cy="131" rx="30" ry="43" transform="rotate(-33 91 131)" :fill="accentColor" />
      <ellipse cx="91" cy="131" rx="21" ry="32" transform="rotate(-33 91 131)" fill="white" opacity="0.5" />
      <ellipse cx="91" cy="131" rx="11" ry="19" transform="rotate(-33 91 131)" :fill="accentColor" opacity="0.8" />
      <path d="M130 160L178 187" stroke="white" stroke-width="3" opacity="0.3" />
    </g>
  </svg>
</template>

<style scoped>
.product-illustration { display: block; width: 100%; height: 100%; }
</style>
