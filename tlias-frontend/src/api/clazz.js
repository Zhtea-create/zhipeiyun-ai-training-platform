import request, { cleanParams } from './request'

export const queryClazzPageApi = (params) =>
  request.get('/clazzs', { params: cleanParams(params) })

export const queryAllClazzApi = () => request.get('/clazzs/list')

export const queryClazzByIdApi = (id) => request.get(`/clazzs/${id}`)

export const addClazzApi = (clazz) => request.post('/clazzs', clazz)

export const updateClazzApi = (clazz) => request.put('/clazzs', clazz)

export const deleteClazzApi = (id) => request.delete(`/clazzs/${id}`)
