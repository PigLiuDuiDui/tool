<template>
  <view class="upload">
    <view v-if="!fileName" class="empty" @click="$emit('choose')">
      <view class="plus">＋</view>
      <text class="title">{{ emptyTitle }}</text>
      <text class="hint">{{ hint }}</text>
      <view v-if="showMediaActions" class="actions" @click.stop>
        <view class="action" @click="$emit('camera')">拍照</view>
        <view class="action primary" @click="$emit('album')">相册</view>
      </view>
    </view>
    <view v-else class="filled" @click="$emit('choose')">
      <image v-if="thumb" class="thumb" :src="thumb" mode="aspectFill" />
      <view v-else class="file-badge">文件</view>
      <view class="meta">
        <text class="name">{{ fileName }}</text>
        <text class="size">{{ sizeText }}</text>
        <text v-if="extraText" class="extra">{{ extraText }}</text>
      </view>
      <text class="change">更换</text>
    </view>
  </view>
</template>

<script setup>
defineProps({
  hint: { type: String, default: '支持常见格式' },
  emptyTitle: { type: String, default: '点击上传文件' },
  fileName: String,
  sizeText: String,
  thumb: String,
  extraText: String,
  showMediaActions: { type: Boolean, default: false },
})
defineEmits(['choose', 'camera', 'album'])
</script>

<style lang="scss" scoped>
.upload {
  @include card;
  min-height: 260rpx;
  border: 2rpx dashed rgba(83, 109, 254, 0.35);
  padding: 32rpx;
  box-sizing: border-box;
}
.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 28rpx 0 12rpx;
}
.plus {
  width: 88rpx;
  height: 88rpx;
  border-radius: 44rpx;
  background: linear-gradient(135deg, rgba(83, 109, 254, 0.12), rgba(120, 103, 248, 0.12));
  color: $primary-from;
  font-size: 48rpx;
  text-align: center;
  line-height: 88rpx;
  margin-bottom: 20rpx;
}
.title {
  font-size: 30rpx;
  font-weight: 600;
  color: $text-primary;
}
.hint {
  margin-top: 10rpx;
  font-size: 24rpx;
  color: $text-secondary;
  text-align: center;
  padding: 0 24rpx;
}
.actions {
  margin-top: 28rpx;
  display: flex;
  gap: 16rpx;
}
.action {
  min-width: 160rpx;
  text-align: center;
  padding: 14rpx 28rpx;
  border-radius: 999rpx;
  background: #f2f4fa;
  color: $text-primary;
  font-size: 26rpx;
  font-weight: 600;
}
.action.primary {
  background: linear-gradient(135deg, $primary-from, $primary-to);
  color: #fff;
}
.filled {
  display: flex;
  align-items: center;
  gap: 20rpx;
}
.thumb {
  width: 112rpx;
  height: 112rpx;
  border-radius: 16rpx;
  background: #eef1f8;
  flex-shrink: 0;
}
.file-badge {
  width: 112rpx;
  height: 112rpx;
  border-radius: 16rpx;
  background: #eef1ff;
  color: $primary-from;
  font-size: 26rpx;
  font-weight: 600;
  text-align: center;
  line-height: 112rpx;
  flex-shrink: 0;
}
.meta {
  flex: 1;
  min-width: 0;
}
.name {
  display: block;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.size {
  display: block;
  margin-top: 8rpx;
  color: $text-secondary;
  font-size: 24rpx;
}
.extra {
  display: block;
  margin-top: 6rpx;
  color: $primary-from;
  font-size: 22rpx;
}
.change {
  color: $primary-from;
  font-size: 26rpx;
}
</style>
