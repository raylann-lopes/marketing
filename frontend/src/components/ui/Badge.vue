<script setup lang="ts">
import { cn } from '@/lib/utils'

type BadgeVariant = 'default' | 'secondary' | 'outline' | 'success' | 'warning' | 'destructive' | 'purple'

const props = withDefaults(
  defineProps<{
    variant?: BadgeVariant
    class?: string | string[] | Record<string, boolean>
  }>(),
  {
    variant: 'default',
  },
)

const variants: Record<BadgeVariant, string> = {
  default: 'bg-primary text-primary-foreground',
  secondary: 'bg-secondary text-secondary-foreground',
  outline: 'border border-input text-foreground',
  success: 'bg-green-100 text-green-700',
  warning: 'bg-orange-100 text-orange-700',
  destructive: 'bg-red-100 text-red-700',
  purple: 'bg-purple-100 text-purple-700',
}
</script>

<template>
  <span
    :class="cn(
      'inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-semibold',
      variants[variant],
      props.class
    )"
  >
    <slot />
  </span>
</template>
