export const resolveImageUrl = (url) => {
    if (!url) return ''
    if (url.startsWith('http://') || url.startsWith('https://')) return url
    if (url.startsWith('/api/')) return url
    if (url.startsWith('/')) return `/api${url}`
    return url
}
