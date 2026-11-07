import request, { cleanParams } from './request'

export const queryEmpPageApi = (params) =>
  request.get('/emps', { params: cleanParams(params) })

export const queryAllEmpsApi = () => request.get('/emps/list')

export const queryEmpByIdApi = (id) => request.get(`/emps/${id}`)

export const addEmpApi = (emp) => request.post('/emps', emp)

export const updateEmpApi = (emp) => request.put('/emps', emp)

export const deleteEmpsApi = (ids) => {
  const value = Array.isArray(ids) ? ids.join(',') : ids
  return request.delete('/emps', { params: { ids: value } })
}
