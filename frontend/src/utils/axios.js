import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

// 创建axios实例
const service = axios.create({
    baseURL: '/api',
    timeout: 15000
})

// 请求拦截器 - 自动添加token
service.interceptors.request.use(
    config => {
        const token = localStorage.getItem('token')
        if (token) {
            config.headers['Authorization'] = 'Bearer ' + token
        }
        return config
    },
    error => {
        return Promise.reject(error)
    }
)

// 响应拦截器 - 统一处理错误
service.interceptors.response.use(
    response => {
        const res = response.data
        if (res.code === 200) {
            return res
        }
        ElMessage.error(res.message || '请求失败')
        return Promise.reject(new Error(res.message || '请求失败'))
    },
    error => {
        if (error.response) {
            const status = error.response.status
            const data = error.response.data

            if (status === 401) {
                ElMessage.error('登录已过期，请重新登录')
                localStorage.removeItem('token')
                localStorage.removeItem('user')
                router.push('/login')
            } else if (status === 403) {
                ElMessage.error('权限不足')
            } else if (status === 409) {
                ElMessage.error(data.message || '时间冲突')
            } else {
                ElMessage.error(data.message || '服务器错误')
            }
        } else {
            ElMessage.error('网络错误，请检查连接')
        }
        return Promise.reject(error)
    }
)

export default service