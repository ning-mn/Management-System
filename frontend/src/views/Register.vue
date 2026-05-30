<template>
    <div class="register-container">
        <el-card class="register-card">
            <h2 style="text-align: center; margin-bottom: 20px;">会议室预定管理系统 - 注册</h2>
            <el-form :model="form" :rules="rules" ref="formRef" label-width="80px">
                <el-form-item label="用户名" prop="username">
                    <el-input v-model="form.username" placeholder="请输入用户名" />
                </el-form-item>
                <el-form-item label="密码" prop="password">
                    <el-input v-model="form.password" type="password" placeholder="请输入密码（至少6位）" show-password />
                </el-form-item>
                <el-form-item label="真实姓名" prop="realName">
                    <el-input v-model="form.realName" placeholder="请输入真实姓名" />
                </el-form-item>
                <el-form-item>
                    <el-button type="primary" @click="handleRegister" style="width: 100%;" :loading="loading">注册</el-button>
                </el-form-item>
                <el-form-item>
                    <span>已有账号？<el-link type="primary" @click="$router.push('/login')">立即登录</el-link></span>
                </el-form-item>
            </el-form>
        </el-card>
    </div>
</template>

<script>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { register } from '../api/user'
import { ElMessage } from 'element-plus'

export default {
    name: 'Register',
    setup() {
        const router = useRouter()
        const formRef = ref(null)
        const loading = ref(false)
        const form = reactive({
            username: '',
            password: '',
            realName: ''
        })
        const rules = {
            username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
            password: [
                { required: true, message: '请输入密码', trigger: 'blur' },
                { min: 6, message: '密码至少6位', trigger: 'blur' }
            ]
        }

        const handleRegister = async () => {
            const valid = await formRef.value.validate().catch(() => false)
            if (!valid) return

            loading.value = true
            try {
                await register(form)
                ElMessage.success('注册成功，请登录')
                router.push('/login')
            } catch (e) {
                // 错误已在拦截器中处理
            } finally {
                loading.value = false
            }
        }

        return { form, formRef, rules, loading, handleRegister }
    }
}
</script>

<style scoped>
.register-container {
    display: flex;
    justify-content: center;
    align-items: center;
    min-height: 100vh;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
.register-card {
    width: 400px;
    padding: 20px;
}
</style>