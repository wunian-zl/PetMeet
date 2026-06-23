import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/store/user'
import { ElMessage } from 'element-plus'

const routes = [
    {
        path: '/login',
        name: 'Login',
        component: () => import('@/views/Login.vue')
    },
    {
        path: '/',
        component: () => import('@/layout/BasicLayout.vue'),
        children: [
            {
                path: '',
                name: 'Home',
                component: () => import('@/views/Home.vue')
            },
            {
                path: 'note/detail/:id',
                name: 'NoteDetail',
                component: () => import('@/views/NoteDetail.vue')
            },
            {
                path: 'publish',
                name: 'Publish',
                component: () => import('@/views/Publish.vue')
            },
            {
                path: 'shop',
                name: 'Shop',
                component: () => import('@/views/Shop.vue')
            },
            {
                path: 'mall/list',
                name: 'ShopList',
                component: () => import('@/views/ShopList.vue')
            },
            {
                path: 'product/:id',
                name: 'ProductDetail',
                component: () => import('@/views/ProductDetail.vue')
            },
            {
                path: 'cart',
                name: 'Cart',
                component: () => import('@/views/Cart.vue')
            },
            {
                path: 'checkout',
                name: 'Checkout',
                component: () => import('@/views/Checkout.vue')
            },
            {
                path: 'pay',
                name: 'Pay',
                component: () => import('@/views/Pay.vue')
            },
            {
                path: 'pay/result',
                name: 'PayResult',
                component: () => import('@/views/PayResult.vue')
            },
            {
                path: 'profile',
                name: 'Profile',
                component: () => import('@/views/Profile.vue')
            },
            {
                path: 'notification',
                name: 'Notification',
                component: () => import('@/views/Notifications.vue')
            },
            {
                path: 'follows',
                name: 'FollowList',
                component: () => import('@/views/FollowList.vue')
            }
        ]
    }
]

const normalizeBase = (rawBase) => {
    if (typeof rawBase !== 'string') return '/'
    const raw = rawBase.trim()
    if (!raw || /^undefined\/?$/i.test(raw) || /^null\/?$/i.test(raw)) {
        return '/'
    }
    const withLeadingSlash = raw.startsWith('/') ? raw : `/${raw}`
    return withLeadingSlash.endsWith('/') ? withLeadingSlash : `${withLeadingSlash}/`
}

const isInvalidPath = (path) => {
    if (typeof path !== 'string') return false
    const normalized = path.trim().toLowerCase()
    return normalized === 'undefined'
        || normalized === 'undefined/'
        || normalized.startsWith('undefined?')
        || normalized.startsWith('undefined/')
}

const envBase = normalizeBase(import.meta.env.BASE_URL)
const getRuntimeBase = () => {
    if (typeof window === 'undefined') {
        return envBase
    }
    const pathname = window.location?.pathname || '/'
    return pathname.startsWith(envBase) ? envBase : '/'
}

const appBase = getRuntimeBase()

const router = createRouter({
    history: createWebHistory(appBase),
    routes,
    // 路由切换时滚动行为
    scrollBehavior(to, from, savedPosition) {
        if (savedPosition) {
            // 浏览器后退/前进时恢复位置
            return { ...savedPosition, behavior: 'instant' }
        } else {
            // 新页面滚动到顶部
            return { top: 0, behavior: 'instant' }
        }
    }
})

const wrapNavigationMethod = (methodName) => {
    const original = router[methodName].bind(router)
    router[methodName] = (to, ...rest) => {
        if (typeof to === 'string' && isInvalidPath(to)) {
            return original('/', ...rest)
        }
        if (to && typeof to === 'object' && typeof to.path === 'string' && isInvalidPath(to.path)) {
            return original({ ...to, path: '/' }, ...rest)
        }
        return original(to, ...rest)
    }
}

wrapNavigationMethod('push')
wrapNavigationMethod('replace')

// 白名单路由
const whiteList = ['/login', '/', '/shop', '/mall/list']
const directAuthRenderList = ['/cart', '/publish', '/profile']

// 前端路由守卫
router.beforeEach((to, from, next) => {
    // 兜底处理非法路径，避免跳到 undefined 页面
    if (isInvalidPath(to?.fullPath) || isInvalidPath(to?.path)) {
        next('/')
        return
    }

    const userStore = useUserStore()
    const token = userStore.token || localStorage.getItem('token')

    // 判断当前页面是不是白名单页面
    const isWhiteList = whiteList.includes(to.path)
        || to.path.startsWith('/note/detail/')
        || to.path.startsWith('/product/')
        || to.path === '/'
    const isDirectAuthEntry = from.matched.length === 0 && directAuthRenderList.includes(to.path)

    if (isWhiteList || token || isDirectAuthEntry) {
        // 已登录用户再访问登录页时，直接回首页
        if (to.path === '/login' && token) {
            next('/')
        } else {
            if (!token && isDirectAuthEntry) {
                ElMessage.warning('请先登录后继续操作')
                userStore.showLogin(to.fullPath)
            }
            next()
        }
    } else {
        // 未登录时，弹出登录框并阻止进入受保护页面
        ElMessage.warning('请先登录后继续操作')
        userStore.showLogin(to.fullPath)
        next(false)
    }
})

export default router
