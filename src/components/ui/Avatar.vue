<script setup lang="ts">
import { cn } from '@/lib/utils'

type AvatarSize = 'sm' | 'md' | 'lg'

const props = withDefaults(
  defineProps<{
    name?: string
    src?: string
    size?: AvatarSize
    class?: string | string[] | Record<string, boolean>
  }>(),
  {
    name: '',
    size: 'md',
  },
)

const sizes: Record<AvatarSize, string> = {
  sm: 'w-7 h-7 text-xs',
  md: 'w-9 h-9 text-sm',
  lg: 'w-12 h-12 text-base',
}

const initials = (name: string): string => {
  if (!name) return '?'
  return name
    .split(' ')
    .map((segment) => segment[0])
    .slice(0, 2)
    .join('')
    .toUpperCase()
}

const colors = ['bg-purple-200 text-purple-700', 'bg-blue-200 text-blue-700', 'bg-pink-200 text-pink-700', 'bg-green-200 text-green-700']
const colorIndex = (props.name?.charCodeAt(0) ?? 0) % colors.length
</script>

<template>
  <div
    :class="cn(
      'rounded-full flex items-center justify-center font-semibold shrink-0',
      sizes[size],
      !src ? colors[colorIndex] : '',
      props.class
    )"
  >
    <img v-if="src" :src="src" :alt="name" class="w-full h-full rounded-full object-cover" />
    <span v-else>{{ initials(name) }}</span>
  </div>
</template>
