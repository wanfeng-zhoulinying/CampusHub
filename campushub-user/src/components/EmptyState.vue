<template>
  <div class="empty">
    <van-empty :description="text" />
  </div>
</template>

<script setup lang="ts">
defineProps<{ text: string }>()
</script>
