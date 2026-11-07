import request, { cleanParams } from './request'

export const queryLogPageApi = (params) =>
  request.get('/log/page', { params: cleanParams(params) })
