/** Detecta vídeo pela extensão da URL (ignorando query string) ou do nome do arquivo. */
export function isVideo(url: string, filename?: string): boolean {
  const check = (value: string) => {
    if (!value) return false
    const clean = (value.split('?')[0] ?? '').toLowerCase()
    return clean.endsWith('.mp4') || clean.endsWith('.webm') || clean.endsWith('.mov')
  }
  return check(url) || (filename ? check(filename) : false)
}
