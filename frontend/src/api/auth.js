import request from './request'

export const login = (data) => request.post('/auth/login', data)

export const register = (data) => request.post('/auth/register', data)

export const sendEmailCode = (email) => request.post('/email/send-code', null, { params: { email } })

export const sendResetCode = (email) => request.post('/email/send-reset-code', null, { params: { email } })

export const sendBindCode = (email) => request.post('/email/send-bind-code', null, { params: { email } })

export const getUserInfo = () => request.get('/auth/info')

export const updateUserInfo = (data) => request.put('/auth/info', data)

export const changePassword = (data) => request.put('/auth/change-password', data)

export const resetPassword = (data) => request.post('/auth/reset-password', data)

export const changeEmail = (data) => request.put('/auth/change-email', data)

export const getUserStats = (userId) => request.get(`/auth/user-stats/${userId}`)
