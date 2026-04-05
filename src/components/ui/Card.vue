<script setup lang="ts">
import { cn } from '@/lib/utils'
const props = defineProps<{ class?: string | string[] | Record<string, boolean> }>()
</script>

<template>
  <div :class="cn('rounded-xl border bg-card text-card-foreground shadow-sm', props.class)">
    <slot />
  </div>
</template>
