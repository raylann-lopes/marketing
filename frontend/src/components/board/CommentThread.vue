<script setup lang="ts">
import { ref, watch, nextTick, onUnmounted } from 'vue'
import { Send, MessageSquare } from 'lucide-vue-next'
import Avatar from '@/components/ui/Avatar.vue'
import { commentService, type Comment } from '@/services/commentService'
import { getErrorMessage } from '@/lib/errors'
import { useFeedback } from '@/lib/feedback'
import { getCurrentUserId } from '@/lib/api'

const props = defineProps<{ postId: string | number | null | undefined }>()
const emit = defineEmits<{ (e: 'sent'): void }>()

const feedback = useFeedback()
const comments = ref<Comment[]>([])
const newText = ref('')
const isLoading = ref(false)
const isSending = ref(false)
const scrollContainer = ref<HTMLDivElement | null>(null)

function isOwnComment(comment: Comment): boolean {
  return String(comment.authorId) === String(getCurrentUserId())
}

// Polling: enquanto o chat está aberto, busca a lista de novo a cada 6s
// pra simular atualização em tempo real sem WebSocket
const POLL_INTERVAL_MS = 6000
let pollTimer: ReturnType<typeof setInterval> | null = null

function stopPolling() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

function startPolling() {
  stopPolling()
  pollTimer = setInterval(refreshComments, POLL_INTERVAL_MS)
}

function isNearBottom(): boolean {
  const el = scrollContainer.value
  if (!el) return true
  return el.scrollHeight - el.scrollTop - el.clientHeight < 60
}

// Busca silenciosa em segundo plano: sem spinner, sem toast de erro (uma
// falha de poll não deve incomodar quem só está lendo), e só re-renderiza
// se a contagem mudou de verdade — evita rolagem/flicker à toa
async function refreshComments() {
  if (!props.postId) return
  try {
    const fresh = await commentService.getByPostId(props.postId)
    if (fresh.length === comments.value.length) return
    const wasNearBottom = isNearBottom()
    comments.value = fresh
    if (wasNearBottom) scrollToBottom()
  } catch {
    // silencioso — tenta de novo no próximo ciclo
  }
}

// Rola pra última mensagem depois que o DOM atualiza com a lista nova —
// sem nextTick, scrollHeight ainda reflete o conteúdo antigo
function scrollToBottom() {
  nextTick(() => {
    if (scrollContainer.value) {
      scrollContainer.value.scrollTop = scrollContainer.value.scrollHeight
    }
  })
}

function formatDateTime(iso: string): string {
  const date = new Date(iso)
  if (Number.isNaN(date.getTime())) return iso
  return date.toLocaleString('pt-BR', {
    day: '2-digit',
    month: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

async function fetchComments() {
  stopPolling()
  if (!props.postId) {
    comments.value = []
    return
  }
  isLoading.value = true
  try {
    comments.value = await commentService.getByPostId(props.postId)
  } catch (e: unknown) {
    feedback.error(getErrorMessage(e, 'Erro ao carregar comentários'))
  } finally {
    // isLoading precisa virar false ANTES do scroll — senão o nextTick
    // pode medir a altura ainda com o spinner no DOM, não a lista real
    isLoading.value = false
    scrollToBottom()
    startPolling()
  }
}

// O modal reutiliza a mesma instância entre posts diferentes — recarrega
// a thread sempre que o postId mudar (inclusive na primeira abertura)
watch(() => props.postId, fetchComments, { immediate: true })

// Navegador pausa/atrasa setInterval em abas em segundo plano — ao voltar
// o foco pra essa aba, busca na hora em vez de esperar o próximo ciclo
function handleVisibilityChange() {
  if (document.visibilityState === 'visible') {
    refreshComments()
  }
}
document.addEventListener('visibilitychange', handleVisibilityChange)

// Chat fechado (modal com v-if desmonta o componente) — para o timer e
// remove o listener pra não vazar entre aberturas
onUnmounted(() => {
  stopPolling()
  document.removeEventListener('visibilitychange', handleVisibilityChange)
})

async function sendComment() {
  const text = newText.value.trim()
  if (!text || !props.postId || isSending.value) return

  isSending.value = true
  try {
    const created = await commentService.create(props.postId, text)
    comments.value.push(created)
    newText.value = ''
    scrollToBottom()
    emit('sent')
  } catch (e: unknown) {
    feedback.error(getErrorMessage(e, 'Erro ao enviar comentário'))
  } finally {
    isSending.value = false
  }
}

function handleKeydown(event: KeyboardEvent) {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    sendComment()
  }
}
</script>

<template>
  <div class="rounded-2xl border border-gray-100 bg-gray-50/50 overflow-hidden">
    <div class="flex items-center gap-2 px-5 pt-4 pb-2 text-gray-400">
      <MessageSquare class="w-3.5 h-3.5" />
      <span class="text-[10px] font-bold uppercase tracking-widest">Comentários</span>
    </div>

    <div ref="scrollContainer" class="max-h-72 overflow-y-auto px-5 py-2 space-y-3 scroll-smooth">
      <div v-if="isLoading" class="flex justify-center py-4">
        <div class="w-5 h-5 border-2 border-gray-200 border-t-primary rounded-full animate-spin"></div>
      </div>

      <p v-else-if="comments.length === 0" class="text-xs text-gray-400 italic py-2">
        Nenhum comentário ainda. Seja o primeiro a comentar.
      </p>

      <div
        v-else
        v-for="comment in comments"
        :key="comment.id"
        :class="['flex gap-2', isOwnComment(comment) ? 'flex-row-reverse' : 'flex-row']"
      >
        <Avatar
          v-if="!isOwnComment(comment)"
          :name="comment.authorName"
          size="sm"
          class="w-7 h-7 text-[10px] shrink-0"
        />
        <div :class="['max-w-[75%] flex flex-col', isOwnComment(comment) ? 'items-end' : 'items-start']">
          <div class="flex items-baseline gap-2 px-1">
            <span v-if="!isOwnComment(comment)" class="text-[11px] font-bold text-gray-700">{{ comment.authorName }}</span>
            <span class="text-[10px] text-gray-400">{{ formatDateTime(comment.createdAt) }}</span>
          </div>
          <div
            :class="[
              'mt-0.5 px-3 py-2 rounded-2xl text-xs leading-relaxed whitespace-pre-line',
              isOwnComment(comment)
                ? 'bg-primary text-white rounded-tr-sm'
                : 'bg-white border border-gray-100 text-gray-700 rounded-tl-sm',
            ]"
          >
            {{ comment.text }}
          </div>
        </div>
      </div>
    </div>

    <div class="flex items-end gap-2 px-4 py-3 border-t border-gray-100 bg-white">
      <textarea
        v-model="newText"
        rows="1"
        placeholder="Escreva um comentário..."
        class="flex-1 resize-none text-xs px-3 py-2 rounded-xl border border-gray-200 focus:outline-none focus:ring-2 focus:ring-primary/10 focus:border-primary/30"
        @keydown="handleKeydown"
      ></textarea>
      <button
        type="button"
        :disabled="!newText.trim() || isSending"
        class="w-9 h-9 shrink-0 rounded-xl bg-primary text-white flex items-center justify-center disabled:opacity-40 disabled:cursor-not-allowed transition-opacity"
        title="Enviar comentário"
        @click="sendComment"
      >
        <Send class="w-4 h-4" />
      </button>
    </div>
  </div>
</template>
