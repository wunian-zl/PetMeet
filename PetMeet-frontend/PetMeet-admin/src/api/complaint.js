import request from './request'

export function getComplaintList(params) {
    return request({
        url: '/admin/complaint/list',
        method: 'get',
        params
    })
}

export function updateComplaintStatus(id, status, remark) {
    const params = { status }
    const trimmed = typeof remark === 'string' ? remark.trim() : remark
    if (trimmed) {
        params.remark = trimmed
    }
    return request({
        url: `/admin/complaint/${id}/status`,
        method: 'put',
        params
    })
}

export function updateComplaintStatusWithRemark(id, status, remark) {
    return updateComplaintStatus(id, status, remark)
}

export function deleteComplaint(id) {
    return request({
        url: `/admin/complaint/${id}`,
        method: 'delete'
    })
}

export function batchDeleteComplaints(ids) {
    return request({
        url: '/admin/complaint/batch-delete',
        method: 'post',
        data: ids
    })
}
