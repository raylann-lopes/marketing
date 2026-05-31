<script setup lang="ts">
import { X, ChevronDown, RefreshCw } from 'lucide-vue-next'
import Button from '@/components/ui/Button.vue'
import { type Client } from '@/services/clientService'

interface TransactionForm {
  client: string
  description: string
  value: number
  status: string
  expirationDate: string
  type: string
}

interface Props {
  isOpen: boolean
  isEdit: boolean
  transaction: TransactionForm
  clients: Client[]
  isSubmitting: boolean
  fieldErrors: Record<string, string>
}

const TYPES = [
  { value: 'FIXED_REVENUE',    label: 'Receita Fixa',      color: 'text-green-700 bg-green-50 border-green-200' },
  { value: 'VARIABLE_REVENUE', label: 'Receita Variável',  color: 'text-emerald-700 bg-emerald-50 border-emerald-200' },
  { value: 'FIXED_EXPENSE',    label: 'Despesa Fixa',      color: 'text-red-700 bg-red-50 border-red-200' },
  { value: 'VARIABLE_EXPENSE', label: 'Despesa Variável',  color: 'text-orange-700 bg-orange-50 border-orange-200' },
]

const props = defineProps<Props>()
const emit = defineEmits<{
  (e: 'close'): void
  (e: 'save'): void
  (e: 'update:transaction', val: TransactionForm): void
}>()

function updateField(key: keyof TransactionForm, value: string | number) {
  emit('update:transaction', { ...props.transaction, [key]: value })
}

function isFixed(type: string) {
  return type === 'FIXED_EXPENSE' || type === 'FIXED_REVENUE'
}
</script>

<template>
  <div v-if="isOpen" class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm">
    <div class="bg-white rounded-2xl shadow-xl w-full max-w-lg overflow-hidden animate-in fade-in zoom-in duration-200">
      <div class="flex items-center justify-between p-6 border-b border-gray-100">
        <h2 class="text-xl font-bold text-gray-900">{{ isEdit ? 'Editar Transação' : 'Nova Transação' }}</h2>
        <button @click="$emit('close')" class="p-2 hover:bg-gray-100 rounded-lg text-gray-400"><X class="w-5 h-5" /></button>
      </div>
      <div class="p-6 space-y-4">

        <!-- Tipo -->
        <div class="space-y-1.5">
          <label class="text-xs font-semibold text-gray-500 uppercase">Tipo</label>
          <div class="grid grid-cols-2 gap-2">
            <button
              v-for="t in TYPES"
              :key="t.value"
              type="button"
              @click="updateField('type', t.value)"
              :class="[
                'py-2 px-3 rounded-lg text-xs font-semibold border transition-all text-left',
                transaction.type === t.value ? t.color + ' border-current shadow-sm' : 'bg-gray-50 text-gray-500 border-gray-200 hover:bg-gray-100'
              ]"
            >
              {{ t.label }}
            </button>
          </div>
          <p v-if="transaction.type && isFixed(transaction.type) && !isEdit" class="flex items-center gap-1.5 text-[10px] text-blue-600 font-medium bg-blue-50 rounded-md px-2 py-1.5">
            <RefreshCw class="w-3 h-3 flex-shrink-0" />
            Será replicado automaticamente por 12 meses
          </p>
        </div>

        <!-- Cliente -->
        <div class="space-y-1.5">
          <label class="text-xs font-semibold text-gray-500 uppercase">Cliente</label>
          <div class="relative">
            <select
              :value="transaction.client"
              @change="updateField('client', ($event.target as HTMLSelectElement).value)"
              :class="['w-full p-2.5 pr-10 rounded-lg border bg-gray-50 text-sm focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary appearance-none cursor-pointer', fieldErrors.client ? 'border-red-500' : 'border-gray-200']"
            >
              <option value="">Selecione um cliente</option>
              <option v-for="c in clients" :key="c.id" :value="c.id">{{ c.name }}</option>
            </select>
            <ChevronDown class="absolute right-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400 pointer-events-none" />
          </div>
          <p v-if="fieldErrors.client" class="text-[10px] text-red-500 font-medium">{{ fieldErrors.client }}</p>
        </div>

        <!-- Descrição -->
        <div class="space-y-1.5">
          <label class="text-xs font-semibold text-gray-500 uppercase">Descrição</label>
          <input
            :value="transaction.description"
            @input="updateField('description', ($event.target as HTMLInputElement).value)"
            type="text"
            :class="['w-full p-2.5 rounded-lg border bg-gray-50 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary text-sm', fieldErrors.description ? 'border-red-500' : 'border-gray-200']"
            placeholder="Ex: Mensalidade Abril"
          />
          <p v-if="fieldErrors.description" class="text-[10px] text-red-500 font-medium">{{ fieldErrors.description }}</p>
        </div>

        <!-- Valor + Vencimento -->
        <div class="grid grid-cols-2 gap-4">
          <div class="space-y-1.5">
            <label class="text-xs font-semibold text-gray-500 uppercase">Valor (R$)</label>
            <input
              :value="transaction.value"
              @input="updateField('value', Number(($event.target as HTMLInputElement).value))"
              type="number"
              step="0.01"
              :class="['w-full p-2.5 rounded-lg border bg-gray-50 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary text-sm', fieldErrors.value ? 'border-red-500' : 'border-gray-200']"
              placeholder="0.00"
            />
            <p v-if="fieldErrors.value" class="text-[10px] text-red-500 font-medium">{{ fieldErrors.value }}</p>
          </div>
          <div class="space-y-1.5">
            <label class="text-xs font-semibold text-gray-500 uppercase">Vencimento</label>
            <input
              :value="transaction.expirationDate"
              @input="updateField('expirationDate', ($event.target as HTMLInputElement).value)"
              type="date"
              :class="['w-full p-2.5 rounded-lg border bg-gray-50 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary text-sm', fieldErrors.expirationDate ? 'border-red-500' : 'border-gray-200']"
            />
            <p v-if="fieldErrors.expirationDate" class="text-[10px] text-red-500 font-medium">{{ fieldErrors.expirationDate }}</p>
          </div>
        </div>

        <!-- Status -->
        <div class="space-y-1.5">
          <label class="text-xs font-semibold text-gray-500 uppercase">Status Inicial</label>
          <div class="flex gap-2">
            <button
              v-for="s in ['PENDING', 'PAY']"
              :key="s"
              type="button"
              @click="updateField('status', s)"
              :class="['flex-1 py-2 rounded-lg text-xs font-bold border transition-all', transaction.status === s ? 'bg-primary text-white border-primary' : 'bg-gray-50 text-gray-500 border-gray-200 hover:bg-gray-100']"
            >
              {{ s === 'PENDING' ? 'PENDENTE' : 'PAGO' }}
            </button>
          </div>
        </div>

      </div>
      <div class="p-6 bg-gray-50 border-t border-gray-100 flex gap-3">
        <Button variant="outline" class="flex-1" @click="$emit('close')">Cancelar</Button>
        <Button class="flex-1" :disabled="isSubmitting" @click="$emit('save')">
          {{ isSubmitting ? 'Salvando...' : (isEdit ? 'Atualizar Transação' : 'Salvar Transação') }}
        </Button>
      </div>
    </div>
  </div>
</template>
