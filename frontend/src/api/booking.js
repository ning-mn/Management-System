import request from '../utils/axios'

export function createBooking(data) {
    return request.post('/booking', data)
}

export function cancelBooking(id) {
    if (id === undefined || id === null || id === '') {
        return Promise.reject(new Error('预定ID不能为空'))
    }
    return request.delete('/booking/' + id)
}

export function getMyBookings(params) {
    return request.get('/booking/my', { params })
}

export function getAllBookings(params) {
    return request.get('/booking/all', { params })
}

export function updateBookingStatus(id, data) {
    if (id === undefined || id === null || id === '') {
        return Promise.reject(new Error('预定ID不能为空'))
    }
    return request.put('/booking/' + id + '/status', data)
}