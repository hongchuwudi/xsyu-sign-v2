// 注销信息确认模态框组件（删除数据库）
Vue.component('unregister-confirm-modal', {
    props: ['visible'],
    template: /*html*/ `
        <base-modal :visible="visible" title="确认注销" icon="fas fa-exclamation-triangle"
                    theme="red" :closable="false" @close="$emit('close')">
            <div class="text-center">
                <div class="w-16 h-16 bg-red-100 rounded-full flex items-center justify-center mx-auto mb-4">
                    <i class="fas fa-trash-alt text-red-500 text-2xl"></i>
                </div>
                <h4 class="text-lg font-semibold text-gray-800 mb-2">确认要注销吗？</h4>
                <p class="text-gray-600">
                    注销后，您的个人信息将从系统中移除，不会再自动签到。<br>
                    下次使用需要重新登录。
                </p>
            </div>
            <template v-slot:footer>
                <div class="flex justify-center gap-4">
                    <button @click="$emit('close')"
                            class="px-6 py-2 border border-gray-300 rounded-lg text-gray-700 hover:bg-gray-50 transition-colors">
                        取消
                    </button>
                    <button @click="$emit('confirm')"
                            class="bg-red-500 text-white px-6 py-2 rounded-lg font-medium hover:bg-red-600 transition-colors flex items-center">
                        <i class="fas fa-trash-alt mr-2"></i>确认注销
                    </button>
                </div>
            </template>
        </base-modal>
    `
});
