import request from './request'

/**
 * 获取笔记列表
 */
export function getNoteList(params) {
    return request({
        url: '/admin/note/list',
        method: 'get',
        params
    })
}

/**
 * 获取笔记统计
 */
export function getNoteStats() {
    return request({
        url: '/admin/note/stats',
        method: 'get'
    })
}

/**
 * 获取笔记详情
 */
export function getNoteDetail(id) {
    return request({
        url: `/admin/note/${id}`,
        method: 'get'
    })
}

/**
 * 审核通过
 */
export function approveNote(id) {
    return request({
        url: `/admin/note/${id}/approve`,
        method: 'put'
    })
}

/**
 * 审核拒绝
 */
export function rejectNote(id, reason) {
    return request({
        url: `/admin/note/${id}/reject`,
        method: 'put',
        params: { reason }
    })
}

/**
 * 置顶/取消置顶
 */
export function toggleSticky(id) {
    return request({
        url: `/admin/note/${id}/sticky`,
        method: 'put'
    })
}

/**
 * 推荐/取消推荐
 */
export function toggleRecommend(id) {
    return request({
        url: `/admin/note/${id}/recommend`,
        method: 'put'
    })
}

/**
 * 屏蔽/解除屏蔽
 */
export function toggleShield(id, reason) {
    return request({
        url: `/admin/note/${id}/shield`,
        method: 'put',
        params: reason ? { reason } : undefined
    })
}

/**
 * 管理员软删除内容
 */
export function softDeleteNote(id, reason) {
    const params = reason ? { reason } : undefined
    return request({
        url: `/admin/note/${id}/soft-delete`,
        method: 'put',
        params,
        silent404: true
    }).catch((err) => {
        if (err?.response?.status !== 404) {
            return Promise.reject(err)
        }
        return request({
            url: `/admin/note/${id}/softDelete`,
            method: 'put',
            params
        })
    })
}

/**
 * 批量操作
 */
export function batchNoteAction(action, ids) {
    return request({
        url: '/admin/note/batch',
        method: 'post',
        params: { action },
        data: ids
    })
}

/**
 * 获取评论列表
 */
export function getCommentList(params) {
    return request({
        url: '/admin/comment/list',
        method: 'get',
        params
    })
}

/**
 * 删除评论
 */
export function deleteComment(id) {
    return request({
        url: `/admin/comment/${id}`,
        method: 'delete'
    })
}
