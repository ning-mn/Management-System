<template>
    <div id="app">
        <el-container v-if="!isLoginPage" style="min-height: 100vh;">
            <el-header style="background: #409eff; color: white; display: flex; align-items: center; justify-content: space-between; padding: 0 20px;">
                <div style="display: flex; align-items: center;">
                    <h2 style="margin: 0; margin-right: 40px;">会议室预定管理系统</h2>
                    <el-menu
                        :default-active="currentRoute"
                        mode="horizontal"
                        style="background: transparent; border-bottom: none;"
                        text-color="white"
                        active-text-color="#ffd04b"
                        @select="handleMenuSelect"
                    >
                        <el-menu-item index="/">会议室列表</el-menu-item>
                        <el-menu-item index="/my-bookings">我的预定</el-menu-item>
                        <el-menu-item v-if="userRole === 'admin'" index="/admin">管理员后台</el-menu-item>
                    </el-menu>
                </div>
                <div style="display: flex; align-items: center; gap: 15px;">
                    <span>欢迎, {{ userName }}</span>
                    <el-button type="warning" size="small" @click="logout">退出登录</el-button>
                </div>
            </el-header>
            <el-main style="background: #f5f7fa; padding: 20px;">
                <router-view />
            </el-main>
        </el-container>
        <router-view v-else />
    </div>
</template>

<script>
import { ref, computed, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'

export default {
    name: 'App',
    setup() {
        const router = useRouter()
        const route = useRoute()

        const userName = ref('')
        const userRole = ref('')
        const isLoginPage = computed(() => {
            return route.path === '/login' || route.path === '/register'
        })
        const currentRoute = computed(() => route.path)

        // 从 localStorage 读取用户信息
        const loadUserInfo = () => {
            const user = JSON.parse(localStorage.getItem('user') || '{}')
            userName.value = user.realName || user.username || ''
            userRole.value = user.role || ''
        }

        // 初始化加载
        loadUserInfo()

        // 监听路由变化，登录成功后重新读取用户信息
        watch(() => route.path, () => {
            loadUserInfo()
        })

        const handleMenuSelect = (index) => {
            router.push(index)
        }

        const logout = () => {
            localStorage.removeItem('token')
            localStorage.removeItem('user')
            router.push('/login')
        }

        return {
            userName,
            userRole,
            isLoginPage,
            currentRoute,
            handleMenuSelect,
            logout
        }
    }
}
</script>

<style>
body {
    margin: 0;
    padding: 0;
}
.el-menu--horizontal .el-menu-item {
    color: white !important;
}
.el-menu--horizontal .el-menu-item.is-active {
    color: #ffd04b !important;
    border-bottom-color: #ffd04b !important;
}
</style>