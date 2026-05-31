import { computed, reactive } from 'vue'

export type FeedbackVariant = 'success' | 'error' | 'warning' | 'info'
export type ConfirmTone = 'default' | 'danger' | 'warning'

export type Toast = {
  id: number
  variant: FeedbackVariant
  title?: string
  message: string
}

export type NotificationItem = Toast & {
  createdAt: number
  read: boolean
}

type ConfirmState = {
  open: boolean
  title: string
  message: string
  confirmText: string
  cancelText: string
  tone: ConfirmTone
}

type NotifyOptions = {
  title?: string
  message: string
  variant?: FeedbackVariant
  duration?: number
  keepInFeed?: boolean
}

type ConfirmOptions = {
  title: string
  message: string
  confirmText?: string
  cancelText?: string
  tone?: ConfirmTone
}

let feedbackId = 0
let confirmResolver: ((confirmed: boolean) => void) | null = null

const emptyConfirm: ConfirmState = {
  open: false,
  title: '',
  message: '',
  confirmText: 'Confirmar',
  cancelText: 'Cancelar',
  tone: 'default',
}

const state = reactive({
  toasts: [] as Toast[],
  notifications: [] as NotificationItem[],
  confirm: { ...emptyConfirm },
})

function resetConfirm() {
  Object.assign(state.confirm, emptyConfirm)
}

function dismiss(id: number) {
  state.toasts = state.toasts.filter((toast) => toast.id !== id)
}

function notify(options: NotifyOptions) {
  const id = ++feedbackId
  const toast: Toast = {
    id,
    variant: options.variant ?? 'info',
    title: options.title,
    message: options.message,
  }

  state.toasts.push(toast)

  if (options.keepInFeed !== false) {
    state.notifications.unshift({
      ...toast,
      createdAt: Date.now(),
      read: false,
    })
    state.notifications = state.notifications.slice(0, 30)
  }

  window.setTimeout(() => dismiss(id), options.duration ?? 4500)
  return id
}

function success(message: string, title = 'Sucesso') {
  return notify({ message, title, variant: 'success' })
}

function error(message: string, title = 'Erro') {
  return notify({ message, title, variant: 'error', duration: 6500 })
}

function warning(message: string, title = 'Atenção') {
  return notify({ message, title, variant: 'warning' })
}

function info(message: string, title = 'Informação') {
  return notify({ message, title, variant: 'info' })
}

function markNotificationsRead() {
  state.notifications = state.notifications.map((item) => ({ ...item, read: true }))
}

function confirm(options: ConfirmOptions) {
  if (confirmResolver) confirmResolver(false)

  Object.assign(state.confirm, {
    open: true,
    title: options.title,
    message: options.message,
    confirmText: options.confirmText ?? 'Confirmar',
    cancelText: options.cancelText ?? 'Cancelar',
    tone: options.tone ?? 'default',
  })

  return new Promise<boolean>((resolve) => {
    confirmResolver = resolve
  })
}

function resolveConfirm(confirmed: boolean) {
  if (confirmResolver) confirmResolver(confirmed)
  confirmResolver = null
  resetConfirm()
}

export function useFeedback() {
  const unreadCount = computed(() => state.notifications.filter((item) => !item.read).length)

  return {
    state,
    unreadCount,
    notify,
    success,
    error,
    warning,
    info,
    dismiss,
    confirm,
    resolveConfirm,
    markNotificationsRead,
  }
}
