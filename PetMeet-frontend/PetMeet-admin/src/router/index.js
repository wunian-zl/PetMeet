import { createRouter, createWebHistory } from 'vue-router'
import { useAdminStore } from '@/store/admin'

const routes = [
    {
        path: '/admin/login',
        name: 'Login',
        component: () => import('@/views/Login.vue'),
        meta: { title: '登录' }
    },
    {
        path: '/admin',
        component: () => import('@/layout/AdminLayout.vue'),
        redirect: '/admin/dashboard',
        children: [
            {
                path: 'dashboard',
                name: 'Dashboard',
                component: () => import('@/views/dashboard/index.vue'),
                meta: { title: '仪表盘', icon: 'Odometer' }
            },
            {
                path: 'content',
                name: 'Content',
                component: () => import('@/views/content/index.vue'),
                meta: { title: '内容管理', icon: 'DocumentChecked' }
            },            {
                path: 'complaint',
                name: 'Complaint',
                component: () => import('@/views/complaint/index.vue'),
                meta: { title: '投诉管理', icon: 'Warning' }
            },

            {
                path: 'user',
                name: 'User',
                component: () => import('@/views/user/index.vue'),
                meta: { title: '用户管理', icon: 'User' }
            },
            {
                path: 'product',
                name: 'Product',
                component: () => import('@/views/product/index.vue'),
                meta: { title: '商品管理', icon: 'Goods' }
            },
            {
                path: 'category',
                name: 'Category',
                component: () => import('@/views/product/category/index.vue'),
                meta: { title: '分类管理', icon: 'Menu' }
            },
            {
                path: 'order',
                name: 'Order',
                component: () => import('@/views/order/index.vue'),
                meta: { title: '订单管理', icon: 'List' }
            },
            {
                path: 'after-sale',
                name: 'AfterSale',
                component: () => import('@/views/afterSale/index.vue'),
                meta: { title: '售后管理', icon: 'Service' }
            },
            {
                path: 'banner',
                name: 'Banner',
                component: () => import('@/views/banner/index.vue'),
                meta: { title: '广告管理', icon: 'Picture' }
            }
        ]
    },
    {
        path: '/:pathMatch(.*)*',
        redirect: '/admin/login'
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

router.beforeEach((to, from, next) => {
    const adminStore = useAdminStore()
    if (to.path.startsWith('/admin')) {
        if (to.path === '/admin/login') {
            next()
        } else {
            if (adminStore.isLoggedIn) {
                next()
            } else {
                next('/admin/login')
            }
        }
    } else {
        next() // Should not happen given config, but safe fallback
    }
})

export default router
