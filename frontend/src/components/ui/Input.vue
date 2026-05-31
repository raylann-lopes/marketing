<script setup lang="ts">
import { cn } from '@/lib/utils'

type InputType = 'text' | 'email' | 'password' | 'search' | 'number'
const props = withDefaults(
  defineProps<{
    type?: InputType
    placeholder?: string
    modelValue?: string
    class?: string | string[] | Record<string, boolean>
    disabled?: boolean
  }>(),
  {
    type: 'text',
    modelValue: '',
  },
)

const emit = defineEmits<{
  (event: 'update:modelValue', value: string): void
}>()

function onInput(event: Event) {
  const target = event.target as HTMLInputElement
  emit('update:modelValue', target.value)
}
</script>

<template>
  <input
    :type="type"
    :placeholder="placeholder"
    :value="modelValue"
    :disabled="disabled"
    :class="cn(
      'flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background file:border-0 file:bg-transparent file:text-sm file:font-medium placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50',
      props.class
    )"
    @input="onInput"
  />
</template>
