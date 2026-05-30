import request from '../utils/axios'

export function getRoomList(params) {
    return request.get('/room/list', { params })
}

export function getRoomById(id) {
    if (id === undefined || id === null || id === '') {
        return Promise.reject(new Error('会议室ID不能为空'))
    }
    return request.get('/room/' + id)
}

export function addRoom(data) {
    return request.post('/room', data)
}

export function updateRoom(id, data) {
    if (id === undefined || id === null || id === '') {
        return Promise.reject(new Error('会议室ID不能为空'))
    }
    return request.put('/room/' + id, data)
}

export function deleteRoom(id) {
    if (id === undefined || id === null || id === '') {
        return Promise.reject(new Error('会议室ID不能为空'))
    }
    return request.delete('/room/' + id)
}

export function getRoomBookings(id, params) {
    if (id === undefined || id === null || id === '') {
        return Promise.reject(new Error('会议室ID不能为空'))
    }
    return request.get('/room/' + id + '/bookings', { params })
}