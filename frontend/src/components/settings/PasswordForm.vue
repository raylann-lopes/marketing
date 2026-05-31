<script setup lang="ts">
import { Shield } from 'lucide-vue-next'
import Card from '@/components/ui/Card.vue'
import Input from '@/components/ui/Input.vue'
import Button from '@/components/ui/Button.vue'

interface PasswordForm {
  current: string
  new: string
  confirm: string
}

interface Props {
  passwordForm: PasswordForm
  savingPassword: boolean
}

const props = defineProps<Props>()
const emit = defineEmits<{
  (e: 'save'): void
  (e: 'update:passwordForm', val: PasswordForm): void
}>()

function updateField(key: keyof PasswordForm, value: string) {
  emit('update:passwordForm', { ...props.passwordForm, [key]: value })
}
</script>

<template>
  <Card class="p-6">
    <h2 class="text-xl font-semibold mb-4 text-gray-900 flex items-center gap-2">
      <Shield class="w-5 h-5 text-gray-400" />
      Segurança
    </h2>
    <div class="space-y-4">
      <div class="space-y-1.5">
        <label class="text-xs font-semibold text-gray-500 uppercase">Senha Atual</label>
        <Input 
          :model-value="passwordForm.current" 
          @update:model-value="v => updateField('current', v as string)" 
          type="password" 
          placeholder="••••••••" 
        />
      </div>
      <div class="grid grid-cols-2 gap-4">
        <div class="space-y-1.5">
          <label class="text-xs font-semibold text-gray-500 uppercase">Nova Senha</label>
          <Input 
            :model-value="passwordForm.new" 
            @update:model-value="v => updateField('new', v as string)" 
            type="password" 
            placeholder="Mínimo 6 caracteres" 
          />
        </div>
        <div class="space-y-1.5">
          <label class="text-xs font-semibold text-gray-500 uppercase">Confirmar Senha</label>
          <Input 
            :model-value="passwordForm.confirm" 
            @update:model-value="v => updateField('confirm', v as string)" 
            type="password" 
            placeholder="Repita a senha" 
          />
        </div>
      </div>
      <Button variant="outline" class="w-full" :disabled="savingPassword" @click="$emit('save')">
        {{ savingPassword ? 'Salvando...' : 'Atualizar Senha' }}
      </Button>
    </div>
  </Card>
</template>
