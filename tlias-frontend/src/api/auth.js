import request from './request'

export const loginApi = (data) => request.post('/login', data)
