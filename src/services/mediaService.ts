import { apiFetch } from '@/lib/api'

export type PresignedUploadResponse = {
  uploadUrl: string
  s3Key: string
}

export const mediaService = {
  /**
   * Solicita ao backend uma URL presigned para upload direto ao S3.
   * Retorna uploadUrl (para PUT) e s3Key (para salvar no ApproveDTO).
   */
  async getUploadUrl(
    clientId: string | number,
    postId: string | number,
    filename: string,
    contentType: string
  ): Promise<PresignedUploadResponse> {
    const params = new URLSearchParams({
      clientId: String(clientId),
      postId: String(postId),
      filename,
      contentType
    })
    return apiFetch<PresignedUploadResponse>(`/api/media/upload-url?${params}`, {
      method: 'POST'
    })
  },

  /**
   * Faz o upload do arquivo diretamente para o S3 via URL presigned.
   * Não passa pelo backend — o browser faz PUT diretamente.
   */
  async uploadToS3(uploadUrl: string, file: File): Promise<void> {
    const response = await fetch(uploadUrl, {
      method: 'PUT',
      headers: { 'Content-Type': file.type },
      body: file
    })
    if (!response.ok) {
      throw new Error('Falha no upload para o S3. Tente novamente.')
    }
  }
}
