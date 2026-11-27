<template>
  <button class="primary-btn" :disabled="disabled || loading" @tap="onTap">
    <text v-if="loading">处理中...</text>
    <text v-else>{{ text }}</text>
  </button>
</template>

<script setup>
const props = defineProps({
  text: { type: String, default: '开始处理' },
  loading: Boolean,
  disabled: Boolean,
})
const emit = defineEmits(['click'])
function onTap() {
  if (!props.disabled && !props.loading) emit('click')
}
</script>

<style lang="scss" scoped>

.primary-btn {
  @include primary-btn;
  width: 100%;
  height: 96rpx;
  line-height: 96rpx;
  font-size: 32rpx;
  border: none;
  &::after {
    border: none;
  }
  &[disabled] {
    opacity: 0.55;
  }
}
</style>

