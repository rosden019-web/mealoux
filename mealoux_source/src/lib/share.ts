export type ShareResult = 'shared' | 'copied' | 'cancelled' | 'failed'

/** Web Share API si disponible, sinon copie du lien dans le presse-papiers. */
export async function shareLink(data: { title: string; text: string; url: string }): Promise<ShareResult> {
  try {
    if (navigator.share) {
      await navigator.share(data)
      return 'shared'
    }
    await navigator.clipboard.writeText(data.url)
    return 'copied'
  } catch (error) {
    return error instanceof DOMException && error.name === 'AbortError' ? 'cancelled' : 'failed'
  }
}

/** Partage le code d'une session : Web Share API si disponible, sinon lien WhatsApp (wa.me). */
export async function shareSession(session: { code: string; name: string }): Promise<ShareResult | 'whatsapp'> {
  const url = `${window.location.origin}/s/${session.code}`
  const text = `Rejoins ma session MEALOUX « ${session.name} » avec le code ${session.code} : ${url}`
  if (navigator.share) {
    try {
      await navigator.share({ title: 'MEALOUX', text, url })
      return 'shared'
    } catch (error) {
      if (error instanceof DOMException && error.name === 'AbortError') return 'cancelled'
    }
  }
  window.open(`https://wa.me/?text=${encodeURIComponent(text)}`, '_blank', 'noopener,noreferrer')
  return 'whatsapp'
}
