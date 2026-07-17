import type { PostApproval } from '@/services/approvalService'

/**
 * Helpers de aprovação compartilhados entre ApprovalsView, ApprovalCard e
 * ApprovalDetailModal — antes duplicados nos três, o que já obrigou a
 * corrigir o mesmo bug (REJECT→REJECTED) em cada cópia.
 */

/** A API retorna postId plano; post aninhado cobre consumidores antigos. */
export function postIdOf(approval: PostApproval): string | number | undefined {
  return approval.postId ?? approval.post?.id
}

export function getDemandTitle(approval: PostApproval): string {
  return approval.post?.title || `Demanda #${postIdOf(approval) ?? '-'}`
}

export function statusLabel(s: string): string {
  return { PENDING: 'Pendente', APPROVE: 'Aprovado', REJECTED: 'Rejeitado' }[s] ?? s
}

export function statusVariant(s: string): 'warning' | 'success' | 'destructive' | 'secondary' {
  return (
    ({ PENDING: 'warning', APPROVE: 'success', REJECTED: 'destructive' } as const)[s] ?? 'secondary'
  )
}
