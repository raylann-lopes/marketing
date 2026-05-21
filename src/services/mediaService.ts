import { apiFetch } from '@/lib/api'

export type PresignedUploadResponse = {
  uploadUrl: string
  s3Key: string
}

export type UploadCompleteResponse = {
  postId: number
  postStatus: string
  webhookDispatched: boolean
}

export type MediaUrlResponseDTO = {
  id: number | null
  postId: number
  mediaUrl: string
  caption: string
  igUserId: string | null
  accessToken: string | null
}

export const mediaService = {
  /**
   * Solicita ao backend uma URL presigned para upload direto ao S3.
   * Retorna uploadUrl (para PUT) e s3Key (para salvar no ApproveDTO).
   */
  async getUploadUrl(
    postId: string | number,
    filename: string,
    contentType: string
  ): Promise<PresignedUploadResponse> {
    const params = new URLSearchParams({
      postId: String(postId),
      filename,
      contentType
    })
    return apiFetch<PresignedUploadResponse>(`/api/media/upload-url?${params}`)
  },

  async getReferenceUploadUrl(
    postId: string | number,
    filename: string,
    contentType: string
  ): Promise<PresignedUploadResponse> {
    const params = new URLSearchParams({
      postId: String(postId),
      filename,
      contentType
    })
    return apiFetch<PresignedUploadResponse>(`/api/media/reference-url?${params}`)
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
  },

  async completeUpload(
    postId: string | number,
    s3Key: string,
    artName: string,
  ): Promise<UploadCompleteResponse> {
    return apiFetch<UploadCompleteResponse>('/api/media/upload-complete', {
      method: 'POST',
      body: JSON.stringify({ postId, s3Key, artName })
    })
  },

  async getArtPreviewUrl(postId: string | number): Promise<string> {
    const res = await apiFetch<MediaUrlResponseDTO>(`/api/media/preview/${postId}`)
    return res.mediaUrl
  },

  async getReferencePreviewUrl(postId: string | number): Promise<string> {
    const res = await apiFetch<MediaUrlResponseDTO>(`/api/media/reference-preview/${postId}`)
    return res.mediaUrl
  }
}
