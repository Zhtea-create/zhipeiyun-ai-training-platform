import request, { cleanParams } from './request'

export const queryStudentPageApi = (params) =>
  request.get('/students', { params: cleanParams(params) })

export const queryStudentByIdApi = (id) => request.get(`/students/${id}`)

export const addStudentApi = (student) => request.post('/students', student)

export const updateStudentApi = (student) => request.put('/students', student)

export const handleViolationApi = (id, score) =>
  request.put(`/students/violation/${id}/${score}`)

export const deleteStudentsApi = (ids) => {
  const value = Array.isArray(ids) ? ids.join(',') : ids
  return request.delete(`/students/${value}`)
}
