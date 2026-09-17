<script setup>
// 关于软件（原弹窗改为独立路由页面）
import { useRouter } from 'vue-router'

const router = useRouter()

function goBack() {
  if (window.history.length > 1) router.back()
  else router.push('/')
}
</script>

<template>
  <div class="min-h-screen bg-gradient-to-br from-pink-50 to-rose-50">
    <!-- 顶部返回栏 -->
    <div class="bg-white shadow-sm border-b border-pink-100 sticky top-0 z-10">
      <div class="max-w-2xl mx-auto px-4 py-2 flex items-center gap-2">
        <button @click="goBack" class="p-2 rounded-lg hover:bg-pink-50 text-pink-500 transition-colors">
          <i class="fas fa-chevron-left"></i>
        </button>
        <h1 class="text-lg font-bold text-gray-800">关于软件</h1>
      </div>
    </div>

    <div class="max-w-2xl mx-auto p-4">
      <div class="bg-white rounded-xl border border-pink-200 shadow-sm p-6">
        <!-- 软件信息 -->
        <div class="text-center mb-8">
          <div class="w-20 h-20 bg-gradient-to-r from-pink-400 to-rose-400 rounded-full flex items-center justify-center mx-auto mb-4">
            <i class="fas fa-calendar-check text-white text-3xl"></i>
          </div>
          <h2 class="text-2xl font-bold text-pink-800">校园签到系统</h2>
          <p class="text-pink-500 mt-2">版本 1.0.0</p>
        </div>

        <!-- 使用说明 -->
        <div class="bg-pink-50 rounded-lg p-6 border border-pink-100 mb-6">
          <h4 class="font-bold text-pink-800 mb-3 text-lg">使用说明</h4>
          <div class="space-y-2 text-pink-700">
            <p class="flex items-start">
              <i class="fas fa-check text-rose-400 mr-2 mt-1 flex-shrink-0"></i>
              <span>本系统为西安石油大学校园签到辅助工具</span>
            </p>
            <p class="flex items-start">
              <i class="fas fa-check text-rose-400 mr-2 mt-1 flex-shrink-0"></i>
              <span>支持自动签到和手动签到两种模式</span>
            </p>
            <p class="flex items-start">
              <i class="fas fa-check text-rose-400 mr-2 mt-1 flex-shrink-0"></i>
              <span>请遵守学校规定，诚信使用签到系统</span>
            </p>
          </div>
        </div>

        <!-- 作者信息 -->
        <div class="text-center">
          <p class="text-sm text-pink-500">
            <i class="far fa-copyright mr-1"></i> --@hongchu@--
          </p>
          <p class="text-xs text-pink-400 mt-2">陕ICP备2026004528号-1</p>
        </div>
      </div>
    </div>
  </div>
</template>
