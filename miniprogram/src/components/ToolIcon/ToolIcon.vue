<template>
  <view class="wrap" :style="wrapStyle">
    <image class="img" :src="src" mode="aspectFit" />
    <view v-if="!enabled" class="badge">即将开放</view>
  </view>
</template>

<script setup>
import { computed } from 'vue'
import { toolIcon } from '../../utils/icons'

const props = defineProps({
  code: { type: String, default: '' },
  tone: { type: String, default: 'purple' },
  enabled: { type: Boolean, default: true },
  size: { type: Number, default: 88 },
})

const src = computed(() => toolIcon(props.code))

const wrapStyle = computed(() => ({
  width: props.size + 'rpx',
  height: props.size + 'rpx',
  opacity: props.enabled ? 1 : 0.72,
}))
</script>

<style>
.wrap {
  position: relative;
  display: inline-block;
  flex-shrink: 0;
  overflow: visible;
}
.img {
  width: 100%;
  height: 100%;
  display: block;
  border-radius: 22rpx;
}
.badge {
  position: absolute;
  top: -10rpx;
  right: -16rpx;
  padding: 2rpx 8rpx;
  border-radius: 8rpx;
  background: rgba(31, 35, 48, 0.75);
  color: #fff;
  font-size: 16rpx;
  line-height: 1.4;
  white-space: nowrap;
  z-index: 2;
}
</style>
