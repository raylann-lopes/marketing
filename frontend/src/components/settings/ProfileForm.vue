<script setup lang="ts">
import { User } from 'lucide-vue-next'
import Card from '@/components/ui/Card.vue'
import Input from '@/components/ui/Input.vue'
import Button from '@/components/ui/Button.vue'

interface ProfileForm {
  name: string
  email: string
  role: string
}

interface Props {
  profile: ProfileForm
  savingProfile: boolean
}

const props = defineProps<Props>()
const emit = defineEmits<{
  (e: 'save'): void
  (e: 'update:profile', val: ProfileForm): void
}>()

const roleLabel: Record<string, string> = {
  ADMIN: 'Administrador',
  USER: 'Colaborador'
}

function updateField(key: keyof ProfileForm, value: string) {
  emit('update:profile', { ...props.profile, [key]: value })
}
</script>

<template>
  <Card class="p-6">
    <h2 class="text-xl font-semibold mb-4 text-gray-900 flex items-center gap-2">
      <User class="w-5 h-5 text-gray-400" />
      Meu Perfil
    </h2>

    <div class="space-y-4">
      <div class="bg-gray-50 rounded-lg p-4 space-y-3">
        <p class="text-xs font-semibold text-gray-400 uppercase">Informações da Conta</p>
        <div class="flex items-center justify-between text-sm">
          <span class="text-gray-500">Função</span>
          <span class="font-semibold text-gray-800">{{ roleLabel[profile.role] || profile.role }}</span>
        </div>
      </div>

      <div class="grid grid-cols-2 gap-4">
        <div class="space-y-1.5">
          <label class="text-xs font-semibold text-gray-500 uppercase">Nome</label>
          <Input 
            :model-value="profile.name" 
            @update:model-value="v => updateField('name', v as string)" 
            placeholder="Seu nome" 
          />
        </div>
        <div class="space-y-1.5">
          <label class="text-xs font-semibold text-gray-500 uppercase">E-mail</label>
          <Input 
            :model-value="profile.email" 
            @update:model-value="v => updateField('email', v as string)" 
            type="email" 
            placeholder="seu@email.com" 
          />
        </div>
      </div>

      <Button class="w-full" :disabled="savingProfile" @click="$emit('save')">
        {{ savingProfile ? 'Salvando...' : 'Salvar Alterações' }}
      </Button>
    </div>
  </Card>
</template>
