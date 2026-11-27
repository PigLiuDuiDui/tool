<template>
  <view class="page">
    <view class="search-wrap">
      <view class="search-ico">
        <view class="search-ring" />
        <view class="search-handle" />
      </view>
      <input
        class="search"
        v-model="keyword"
        placeholder="搜索全部工具"
        confirm-type="search"
        @confirm="load"
      />
    </view>
    <scroll-view scroll-x class="chips-scroll" :show-scrollbar="false">
      <view class="chips">
        <view
          v-for="c in chipList"
          :key="c.code"
          class="chip"
          :class="{ active: filter === c.code }"
          @click="setFilter(c.code)"
        >
          {{ c.name }}
        </view>
      </view>
    </scroll-view>
    <view v-for="group in groups" :key="group.code" class="group">
      <text class="group-title">{{ group.name }}</text>
      <view class="list">
        <view
          v-for="tool in group.tools"
          :key="tool.code"
          class="row"
          :class="{ soon: tool.enabled === false }"
          @click="open(tool)"
        >
          <view class="icon-wrap">
            <image class="row-icon" :class="{ dim: tool.enabled === false }" :src="toolIcon(tool.code)" mode="aspectFit" />
          </view>
          <view class="meta">
            <view class="name-row">
              <text class="name">{{ tool.name }}</text>
              <text v-if="tool.enabled === false" class="soon-tag">即将开放</text>
            </view>
            <text class="desc">{{ tool.enabled === false ? '还在准备，可先用已开放的工具' : tool.desc }}</text>
          </view>
          <text
            v-if="tool.enabled !== false"
            class="star"
            :class="{ on: favOn(tool.code) }"
            @click.stop="onStar(tool)"
          >{{ favOn(tool.code) ? '★' : '☆' }}</text>
          <text v-else class="arrow">›</text>
        </view>
      </view>
    </view>
    <view v-if="!groups.length" class="empty">暂无匹配工具</view>

    <view v-if="sheetShow" class="sheet-mask" @click="closeSheet">
      <view class="sheet" @click.stop>
        <text class="sheet-title">{{ sheetTitle }}</text>
        <text class="sheet-body">{{ sheetContent }}</text>
        <view class="sheet-ok" @click="closeSheet">知道了</view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { request } from '../../utils/request'
import { pushRecent } from '../../utils/recent'
import { toolIcon } from '../../utils/icons'
import { getFavorites, toggleFavorite } from '../../utils/favorites'
import { sheetShow, sheetTitle, sheetContent, openSheet, closeSheet } from '../../utils/sheet'

const tools = ref([])
const keyword = ref('')
const filter = ref('all')
const favTick = ref(0)
const GROUP_NAME = {
  idphoto: '证件照',
  convert: '文件转换',
  image: '图片处理',
  doc: '文档处理',
}
const chipList = [
  { code: 'all', name: '全部' },
  { code: 'idphoto', name: '证件照' },
  { code: 'convert', name: '文件转换' },
  { code: 'image', name: '图片处理' },
  { code: 'doc', name: '文档处理' },
]

const filtered = computed(() => {
  favTick.value
  return tools.value.filter((t) => {
    if (filter.value !== 'all' && t.category !== filter.value) return false
    if (!keyword.value) return true
    const kw = keyword.value.toLowerCase()
    return (
      String(t.name).toLowerCase().includes(kw) ||
      String(t.desc).toLowerCase().includes(kw)
    )
  })
})

const groups = computed(() => {
  const order = filter.value === 'all' ? ['idphoto', 'convert', 'image', 'doc'] : [filter.value]
  return order
    .map((code) => ({
      code,
      name: GROUP_NAME[code] || '工具',
      tools: filtered.value.filter((t) => t.category === code),
    }))
    .filter((g) => g.tools.length)
})

onShow(async () => {
  const saved = uni.getStorageSync('tools_filter')
  if (saved) {
    filter.value = saved
    uni.removeStorageSync('tools_filter')
  }
  const kw = uni.getStorageSync('tools_keyword')
  if (kw) {
    keyword.value = kw
    uni.removeStorageSync('tools_keyword')
  }
  favTick.value += 1
  await load()
})

function favOn(code) {
  favTick.value
  return getFavorites().some((t) => t.code === code)
}

function onStar(tool) {
  const on = toggleFavorite(tool)
  favTick.value += 1
  uni.showToast({ title: on ? '已收藏' : '已取消收藏', icon: 'none' })
}

async function load() {
  try {
    const data = await request({
      url: '/tools/catalog',
      data: keyword.value ? { keyword: keyword.value } : {},
    })
    tools.value = data.tools || []
  } catch (e) {
    uni.showToast({ title: e.message || '加载失败', icon: 'none' })
  }
}

function setFilter(code) {
  filter.value = code
}

function open(tool) {
  if (!tool.enabled) {
    openSheet('即将开放', '这个功能还在准备，可以先用已开放的工具。')
    return
  }
  pushRecent(tool)
  uni.navigateTo({ url: `/pages/tool-detail/index?code=${tool.code}` })
}
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  padding: 24rpx 28rpx calc(24rpx + env(safe-area-inset-bottom));
  background: $bg-page;
  box-sizing: border-box;
}
.search-wrap {
  @include card;
  padding: 0 24rpx;
  height: 80rpx;
  display: flex;
  align-items: center;
  gap: 14rpx;
}
.search-ico {
  width: 28rpx;
  height: 28rpx;
  position: relative;
  flex-shrink: 0;
}
.search-ring {
  width: 18rpx;
  height: 18rpx;
  border: 3rpx solid $primary-from;
  border-radius: 50%;
  box-sizing: border-box;
}
.search-handle {
  position: absolute;
  right: 0;
  bottom: 0;
  width: 9rpx;
  height: 3rpx;
  background: $primary-from;
  border-radius: 2rpx;
  transform: rotate(45deg);
}
.search {
  flex: 1;
  height: 80rpx;
  font-size: 28rpx;
}
.chips-scroll {
  margin: 20rpx 0 8rpx;
  white-space: nowrap;
}
.chips {
  display: inline-flex;
  gap: 14rpx;
  padding-bottom: 8rpx;
}
.chip {
  display: inline-block;
  padding: 12rpx 26rpx;
  border-radius: 999rpx;
  background: #fff;
  color: $text-secondary;
  font-size: 24rpx;
  box-shadow: 0 4rpx 12rpx rgba(83, 109, 254, 0.05);
}
.chip.active {
  background: $primary-from;
  color: #fff;
}
.group {
  margin-top: 28rpx;
}
.group-title {
  display: block;
  margin: 0 8rpx 12rpx;
  font-size: 28rpx;
  font-weight: 600;
  color: $text-primary;
}
.list {
  @include card;
  overflow: visible;
}
.row {
  display: flex;
  align-items: center;
  gap: 22rpx;
  padding: 24rpx 28rpx;
  border-bottom: 1rpx solid #f2f4f8;
}
.row:last-child {
  border-bottom: none;
}
.row.soon {
  opacity: 0.88;
}
.icon-wrap {
  width: 84rpx;
  height: 84rpx;
  flex-shrink: 0;
}
.row-icon {
  width: 84rpx;
  height: 84rpx;
  border-radius: 20rpx;
  display: block;
}
.row-icon.dim {
  opacity: 0.4;
}
.star {
  color: #c5cad6;
  font-size: 36rpx;
  padding: 8rpx;
}
.star.on {
  color: #e0a106;
}
.meta {
  flex: 1;
  min-width: 0;
}
.name-row {
  display: flex;
  align-items: center;
  gap: 12rpx;
}
.name {
  font-weight: 600;
  font-size: 28rpx;
  color: $text-primary;
}
.soon-tag {
  font-size: 18rpx;
  line-height: 1.3;
  padding: 2rpx 10rpx;
  border-radius: 8rpx;
  color: #8a90a0;
  background: #f0f2f7;
}
.desc {
  display: block;
  margin-top: 8rpx;
  color: $text-secondary;
  font-size: 22rpx;
}
.arrow {
  color: #c5cad6;
  font-size: 36rpx;
  line-height: 1;
}
.empty {
  padding: 64rpx 24rpx;
  text-align: center;
  color: $text-secondary;
  font-size: 26rpx;
}
.sheet-mask {
  position: fixed;
  left: 0;
  right: 0;
  top: 0;
  bottom: 0;
  background: rgba(31, 35, 48, 0.45);
  z-index: 30;
  display: flex;
  align-items: flex-end;
}
.sheet {
  width: 100%;
  background: #fff;
  border-radius: 24rpx 24rpx 0 0;
  padding: 36rpx 32rpx calc(28rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
}
.sheet-title {
  display: block;
  font-size: 32rpx;
  font-weight: 600;
}
.sheet-body {
  display: block;
  margin-top: 12rpx;
  font-size: 26rpx;
  line-height: 1.5;
  color: $text-secondary;
}
.sheet-ok {
  margin-top: 28rpx;
  height: 88rpx;
  line-height: 88rpx;
  text-align: center;
  border-radius: 12rpx;
  background: $primary-from;
  color: #fff;
  font-weight: 600;
}
</style>
