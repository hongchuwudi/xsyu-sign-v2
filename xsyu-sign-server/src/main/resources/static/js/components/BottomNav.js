// 用户端底部导航组件
Vue.component('bottom-nav', {
    props: ['currentPage'],
    template: /*html*/ `
        <nav class="fixed bottom-0 left-0 right-0 bg-white/95 backdrop-blur-sm border-t border-pink-200 z-20 safe-area-bottom">
            <div class="max-w-lg mx-auto flex justify-around py-2">
                <button @click="$emit('go-home')"
                    :class="currentPage==='home' ? 'text-pink-500' : 'text-gray-400'"
                    class="flex flex-col items-center px-4 py-1 transition-colors">
                    <i class="fas fa-home text-xl"></i>
                    <span class="text-xs mt-1 font-medium">首页</span>
                </button>
                <button @click="$emit('go-signs')"
                    :class="currentPage==='user' ? 'text-pink-500' : 'text-gray-400'"
                    class="flex flex-col items-center px-4 py-1 transition-colors">
                    <i class="fas fa-calendar-check text-xl"></i>
                    <span class="text-xs mt-1 font-medium">签到</span>
                </button>
            </div>
        </nav>
    `
});
