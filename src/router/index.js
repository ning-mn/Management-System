import { createRouter, createWebHistory } from 'vue-router'

const routes = [
    {
        path: '/login',
        name: 'Login',
        component: () => import('../views/Login.vue'),
        meta: { requiresAuth: false }
    },
    {
        path: '/register',
        name: 'Register',
        component: () => import('../views/Register.vue'),
        meta: { requiresAuth: false }
    },
    {
        path: '/',
        name: 'Home',
        component: () => import('../views/RoomList.vue'),
        meta: { requiresAuth: true }
    },
    {
        path: '/booking',
        name: 'BookingForm',
        component: () => import('../views/BookingForm.vue'),
        meta: { requiresAuth: true }
    },
    {
        path: '/my-bookings',
        name: 'MyBookings',
        component: () => import('../views/MyBookings.vue'),
        meta: { requiresAuth: true }
    },
    {
        path: '/admin',
        name: 'Admin',
        component: () => import('../views/Admin.vue'),
        meta: { requiresAuth: true, requiresAdmin: true }
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
    const token = localStorage.getItem('token')
    const user = JSON.parse(localStorage.getItem('user') || '{}')

    if (to.meta.requiresAuth && !token) {
        next('/login')
    } else if (to.meta.requiresAdmin && user.role !== 'admin') {
        next('/')
    } else if ((to.path === '/login' || to.path === '/register') && token) {
        next('/')
    } else {
        next()
    }
})

export default router