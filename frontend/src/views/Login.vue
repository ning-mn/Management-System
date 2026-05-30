<template>
    <div class="login-container">
        <el-card class="login-card">
            <h2 style="text-align: center; margin-bottom: 20px;">会议室预定管理系统 - 登录</h2>
            <el-form :model="form" :rules="rules" ref="formRef" label-width="80px">
                <el-form-item label="用户名" prop="username">
                    <el-input v-model="form.username" placeholder="请输入用户名" />
                </el-form-item>
                <el-form-item label="密码" prop="password">
                    <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password />
                </el-form-item>
                <el-form-item>
                    <el-button type="primary" @click="handleLogin" style="width: 100%;" :loading="loading">登录</el-button>
                </el-form-item>
                <el-form-item>
                    <span>还没有账号？<el-link type="primary" @click="$router.push('/register')">立即注册</el-link></span>
                </el-form-item>
            </el-form>
        </el-card>
    </div>
</template>

<script>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { login } from '../api/user'
import { ElMessage } from 'element-plus'

export default {
    name: 'Login',
    setup() {
        const router = useRouter()
        const formRef = ref(null)
        const loading = ref(false)
        const form = reactive({
            username: '',
            password: ''
        })
        const rules = {
            username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
            password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
        }

        const handleLogin = async () => {
            const valid = await formRef.value.validate().catch(() => false)
            if (!valid) return

            loading.value = true
            try {
                const res = await login(form)
                localStorage.setItem('token', res.data.token)
                localStorage.setItem('user', JSON.stringify({
                    userId: res.data.userId,
                    username: res.data.username,
                    role: res.data.role,
                    realName: res.data.realName
                }))
                ElMessage.success('登录成功')
                router.push('/')
            } catch (e) {
                // 错误已在拦截器中处理
            } finally {
                loading.value = false
            }
        }

        return { form, formRef, rules, loading, handleLogin }
    }
}
</script>

<style scoped>
.login-container {
    display: flex;
    justify-content: center;
    align-items: center;
    min-height: 100vh;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
.login-card {
    width: 400px;
    padding: 20px;
}
</style>