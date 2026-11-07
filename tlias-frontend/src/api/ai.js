import request from './request'

export const getAiStatusApi = () => request.get('/ai/status')

export const aiChatApi = (data) => request.post('/ai/chat', data)

export const aiReportApi = (data) => request.post('/ai/report', data)
