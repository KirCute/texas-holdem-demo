import { createRouter, createWebHashHistory } from 'vue-router';

const router = createRouter({
    history: createWebHashHistory(),
    routes: [
        {
            path: '',
            component: () => import('@/pages/LoginPage.vue')
        },
        {
            path: '/:room/:player',
            component: () => import('@/pages/GamePage.vue')
        },
        {
            path: '/:catchAll(.*)',
            redirect: "/"
        }
    ]
});

export default router;