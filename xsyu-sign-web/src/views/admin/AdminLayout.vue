<script setup>
import AdminBottomNav from '@/components/AdminBottomNav.vue'
</script>

<template>
  <div class="min-h-screen bg-gradient-to-br from-pink-50 via-white to-rose-50 pb-20">
    <router-view />
    <AdminBottomNav />
  </div>
</template>
