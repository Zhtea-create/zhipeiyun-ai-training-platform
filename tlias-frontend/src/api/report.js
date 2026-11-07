import request from './request'

export const getEmpJobDataApi = () => request.get('/report/empJobData')
export const getEmpGenderDataApi = () => request.get('/report/empGenderData')
export const getStudentDegreeDataApi = () => request.get('/report/studentDegreeData')
export const getStudentCountDataApi = () => request.get('/report/studentCountData')
