import dayjs from 'dayjs'

export const formatTime = (time) => {
    if (!time) return ''

    const date = dayjs(time)
    const now = dayjs()
    const diffInMinutes = now.diff(date, 'minute')
    const diffInHours = now.diff(date, 'hour')

    if (diffInMinutes < 60) {
        return `${diffInMinutes}分钟前`
    }

    if (diffInHours < 24) {
        return `${diffInHours}小时前`
    }

    return date.format('YYYY-MM-DD')
}
