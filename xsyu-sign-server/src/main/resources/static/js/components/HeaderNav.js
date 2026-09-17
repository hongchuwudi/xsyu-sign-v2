// 顶部导航组件
Vue.component('header-nav', {
    props: ['userInfo', 'showUserMenu'],
    template: /*html*/ `
        <header class="bg-white/90 backdrop-blur-sm border-b border-pink-200 sticky top-0 z-10">
            <div class="container mx-auto px-4">
                <div class="flex items-center justify-between py-3">
                    <div class="flex items-center space-x-2">
                        <i class="fas fa-calendar-check text-pink-400 text-xl"></i>
                        <h1 class="text-lg font-semibold text-pink-800">签到系统</h1>
                    </div>

                    <div class="flex items-center space-x-3">
                        <!-- 用户信息 -->
                        <div class="hidden sm:flex items-center space-x-2">
                            <div class="w-8 h-8 bg-pink-100 rounded-full flex items-center justify-center">
                                <i class="fas fa-user text-pink-400 text-sm"></i>
                            </div>
                            <span class="text-sm font-medium text-pink-700">{{ userInfo.name || userInfo.username }}</span>
                        </div>

                        <!-- 菜单按钮 -->
                        <div class="relative">
                            <button @click="$emit('toggle-menu')"
                                    class="p-2 rounded-lg hover:bg-pink-50">
                                <i class="fas fa-ellipsis-v text-pink-400"></i>
                            </button>

                            <!-- 用户菜单 -->
                            <div v-if="showUserMenu"
                                 class="absolute right-0 mt-2 w-48 bg-white/95 backdrop-blur-sm rounded-lg shadow-lg border border-pink-200 py-2 z-20">
                                <button @click="$emit('edit-profile')"
                                        class="w-full text-left px-4 py-2 text-sm text-pink-700 hover:bg-pink-50 flex items-center">
                                    <i class="fas fa-user-edit mr-2 text-pink-400"></i>修改个人信息
                                </button>
                                <button @click="$emit('toggle-auto-sign')"
                                        class="w-full text-left px-4 py-2 text-sm text-pink-700 hover:bg-pink-50 flex items-center">
                                    <i class="fas fa-robot mr-2 text-pink-400"></i>
                                    自动签到: {{ userInfo.autoSign ? '开启' : '关闭' }}
                                </button>
                                <button v-if="userInfo.role !== 'ADMIN'" @click="$emit('bind-xsy')"
                                        class="w-full text-left px-4 py-2 text-sm text-pink-700 hover:bg-pink-50 flex items-center">
                                    <i class="fas fa-link mr-2 text-pink-400"></i>更新学校密码
                                </button>
                                <button @click="$emit('open-about')"
                                        class="w-full text-left px-4 py-2 text-sm text-pink-700 hover:bg-pink-50 flex items-center">
                                    <i class="fas fa-info-circle mr-2 text-pink-400"></i>关于软件
                                </button>
                                <div class="border-t border-pink-100 my-1"></div>
                                <button @click="$emit('logout')"
                                        class="w-full text-left px-4 py-2 text-sm text-pink-700 hover:bg-pink-50 flex items-center">
                                    <i class="fas fa-sign-out-alt mr-2 text-pink-400"></i>退出登录
                                </button>
                                <button @click="$emit('unregister-confirm')"
                                        class="w-full text-left px-4 py-2 text-sm text-rose-600 hover:bg-rose-50 flex items-center">
                                    <i class="fas fa-trash-alt mr-2"></i>注销信息
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </header>
    `
});
