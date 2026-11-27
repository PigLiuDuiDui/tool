<template>
  <view class="page">
    <view class="hero">
      <view class="hero-top">
        <view class="brand-wrap">
          <text class="brand">轻工具箱</text>
          <text class="sub">照片、文件处理，一站轻松搞定</text>
        </view>
        <view class="avatar" @click="goMine">
          <text>{{ avatarText }}</text>
        </view>
      </view>
      <view class="search">
        <view class="search-ico">
          <view class="search-ring" />
          <view class="search-handle" />
        </view>
        <input
          class="search-input"
          v-model="keyword"
          confirm-type="search"
          placeholder="搜索工具，如证件照、压缩、转PDF"
          placeholder-class="search-ph"
          @confirm="submitSearch"
        />
      </view>
    </view>

    <view class="content">
      <!-- 主推：证件照吸引，不喧宾夺主 -->
      <view class="id-card">
        <view class="id-left">
          <text class="id-title">智能证件照</text>
          <text class="id-sub">一寸、考试、护照</text>
          <view class="id-btn" @click="openTool('idphoto_smart')">立即制作</view>
        </view>
        <view class="samples">
          <image class="sample s-blue" src="/static/id-sample-blue.png" mode="aspectFill" />
          <image class="sample s-white" src="/static/id-sample-white.png" mode="aspectFill" />
          <image class="sample s-red" src="/static/id-sample-red.png" mode="aspectFill" />
        </view>
      </view>

      <!-- 高频快捷：文件 / 图片，形成日常使用入口 -->
      <view class="quick-row">
        <view class="quick" @click="openTool('word_to_pdf')">
          <image class="quick-icon" :src="toolIcon('word_to_pdf')" mode="aspectFit" />
          <view class="quick-meta">
            <text class="quick-title">Word转PDF</text>
            <text class="quick-sub">日常转换</text>
          </view>
        </view>
        <view class="quick" @click="openTool('image_compress')">
          <image class="quick-icon" :src="toolIcon('image_compress')" mode="aspectFit" />
          <view class="quick-meta">
            <text class="quick-title">图片压缩</text>
            <text class="quick-sub">发图更轻便</text>
          </view>
        </view>
      </view>

      <view v-for="cat in homeCategories" :key="cat.code" class="section">
        <view class="section-head">
          <text class="section-title">{{ cat.name }}</text>
          <text class="more" @click="goTools(cat.code)">全部</text>
        </view>
        <view class="grid">
          <view
            v-for="tool in cat.tools"
            :key="tool.code"
            class="tool"
            @click="openTool(tool.code, tool)"
          >
            <view class="tool-icon-wrap">
              <image
                class="tool-icon"
                :class="{ dim: tool.enabled === false }"
                :src="toolIcon(tool.code)"
                mode="aspectFit"
              />
              <text v-if="tool.enabled === false" class="soon">即将开放</text>
            </view>
            <text class="tool-name">{{ tool.name }}</text>
          </view>
        </view>
      </view>

      <view v-if="recent.length" class="section">
        <view class="section-head">
          <text class="section-title">最近使用</text>
        </view>
        <scroll-view scroll-x class="recent-scroll" :show-scrollbar="false">
          <view
            v-for="item in recent"
            :key="item.code"
            class="recent-item"
            @click="openTool(item.code, item)"
          >
            {{ item.name }}
          </view>
        </scroll-view>
      </view>
    </view>

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
import { ref, computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { request } from '../../utils/request'
import { getUser } from '../../utils/auth'
import { getRecent, pushRecent } from '../../utils/recent'
import { toolIcon } from '../../utils/icons'
import { sheetShow, sheetTitle, sheetContent, openSheet, closeSheet } from '../../utils/sheet'

/** 首页只展示高频入口，完整目录在「工具」页 */
const HOME_PICK = {
  idphoto: ['idphoto_smart', 'idphoto_1inch', 'idphoto_2inch', 'idphoto_exam', 'idphoto_passport'],
  convert: ['word_to_pdf', 'pdf_to_word', 'image_to_pdf', 'pdf_to_image', 'excel_to_pdf'],
  image: ['image_compress', 'image_crop', 'image_resize', 'image_format', 'image_bg'],
  doc: ['pdf_compress', 'word_compress', 'pdf_merge', 'pdf_split'],
}

const categories = ref([])
const recent = ref([])
const avatarText = ref('我')
const keyword = ref('')

const CAT_NAME = {
  idphoto: '证件照',
  convert: '文件转换',
  image: '图片处理',
  doc: '文档处理',
}

const homeCategories = computed(() => {
  return (categories.value || [])
    .map((cat) => {
      const pick = HOME_PICK[cat.code]
      if (!pick) return null
      const map = Object.fromEntries((cat.tools || []).map((t) => [t.code, t]))
      const tools = pick.map((code) => map[code]).filter(Boolean)
      if (!tools.length) return null
      return {
        ...cat,
        name: CAT_NAME[cat.code] || cat.name,
        tools: tools.slice(0, 5),
      }
    })
    .filter(Boolean)
})

onShow(async () => {
  const user = getUser()
  avatarText.value = (user.nickName || '我').slice(0, 1)
  recent.value = getRecent()
  try {
    const data = await request({ url: '/tools/catalog' })
    categories.value = data.categories || []
  } catch (e) {
    uni.showToast({ title: e.message || '加载失败', icon: 'none' })
  }
})

function goMine() {
  uni.switchTab({ url: '/pages/mine/index' })
}

function goTools(category) {
  uni.removeStorageSync('tools_keyword')
  if (category) {
    uni.setStorageSync('tools_filter', category)
  } else {
    uni.removeStorageSync('tools_filter')
  }
  uni.switchTab({ url: '/pages/tools/index' })
}

function submitSearch() {
  const kw = keyword.value.trim()
  if (kw) uni.setStorageSync('tools_keyword', kw)
  else uni.removeStorageSync('tools_keyword')
  uni.removeStorageSync('tools_filter')
  uni.switchTab({ url: '/pages/tools/index' })
}

function openTool(code, tool) {
  if (tool && tool.enabled === false) {
    openSheet('即将开放', '这个功能还在准备，可以先用已开放的工具。')
    return
  }
  if (tool) pushRecent(tool)
  else pushRecent({ code, name: code })
  uni.navigateTo({ url: `/pages/tool-detail/index?code=${code}` })
}
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  padding-bottom: calc(24rpx + env(safe-area-inset-bottom));
  background: $bg-page;
}

.hero {
  position: relative;
  padding: 96rpx 32rpx 36rpx;
  background: linear-gradient(180deg, #5c73f2 0%, #6d78ef 70%, #f7f8fc 100%);
  color: #fff;
}
.hero-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}
.brand {
  display: block;
  font-size: 40rpx;
  font-weight: 600;
  letter-spacing: 1rpx;
}
.sub {
  display: block;
  margin-top: 10rpx;
  opacity: 0.88;
  font-size: 24rpx;
  font-weight: 400;
  letter-spacing: 0.5rpx;
}
.avatar {
  width: 68rpx;
  height: 68rpx;
  border-radius: 34rpx;
  background: rgba(255, 255, 255, 0.22);
  border: 1rpx solid rgba(255, 255, 255, 0.28);
  text-align: center;
  line-height: 68rpx;
  font-weight: 600;
  font-size: 26rpx;
  backdrop-filter: blur(8px);
}
.search {
  margin-top: 28rpx;
  display: flex;
  align-items: center;
  gap: 16rpx;
  height: 80rpx;
  padding: 0 28rpx;
  box-sizing: border-box;
  background: rgba(255, 255, 255, 0.88);
  border-radius: 40rpx;
  box-shadow: 0 8rpx 24rpx rgba(40, 55, 140, 0.12);
  color: $text-secondary;
}
.search-ico {
  width: 32rpx;
  height: 32rpx;
  position: relative;
  flex-shrink: 0;
}
.search-ring {
  width: 22rpx;
  height: 22rpx;
  border: 3rpx solid $primary-from;
  border-radius: 50%;
  box-sizing: border-box;
}
.search-handle {
  position: absolute;
  right: 2rpx;
  bottom: 2rpx;
  width: 10rpx;
  height: 3rpx;
  background: $primary-from;
  border-radius: 2rpx;
  transform: rotate(45deg);
  transform-origin: left center;
}
.search-input {
  flex: 1;
  height: 80rpx;
  font-size: 26rpx;
  color: $text-primary;
}
.search-ph {
  font-size: 26rpx;
  color: #9aa1b2;
}

.content {
  margin-top: -16rpx;
  padding: 0 28rpx 8rpx;
  position: relative;
  z-index: 2;
}

.id-card {
  background: #fff;
  border-radius: 20rpx;
  padding: 36rpx 32rpx;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16rpx;
}
.id-title {
  display: block;
  font-size: 34rpx;
  font-weight: 600;
  color: $text-primary;
}
.id-sub {
  display: block;
  margin-top: 8rpx;
  color: $text-secondary;
  font-size: 24rpx;
}
.id-btn {
  display: inline-block;
  margin-top: 24rpx;
  padding: 12rpx 28rpx;
  font-size: 26rpx;
  font-weight: 600;
  color: #fff;
  background: $primary-from;
  border-radius: 12rpx;
}
.samples {
  width: 196rpx;
  height: 168rpx;
  position: relative;
  flex-shrink: 0;
}
.sample {
  position: absolute;
  width: 78rpx;
  height: 108rpx;
  border-radius: 8rpx;
  border: 2rpx solid #fff;
  box-shadow: 0 8rpx 18rpx rgba(31, 35, 48, 0.16);
}
.s-blue {
  left: 0;
  top: 28rpx;
  transform: rotate(-8deg);
}
.s-white {
  left: 56rpx;
  top: 8rpx;
  z-index: 2;
}
.s-red {
  right: 0;
  top: 32rpx;
  transform: rotate(8deg);
}

.quick-row {
  margin-top: 24rpx;
  display: flex;
  gap: 16rpx;
}
.quick {
  flex: 1;
  background: #fff;
  border-radius: 20rpx;
  padding: 22rpx 20rpx;
  display: flex;
  align-items: center;
  gap: 16rpx;
}
.quick-title {
  display: block;
  font-size: 26rpx;
  font-weight: 600;
  color: $text-primary;
}
.quick-sub {
  display: block;
  margin-top: 4rpx;
  font-size: 20rpx;
  color: $text-secondary;
}

.section {
  margin-top: 32rpx;
}
.section-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16rpx;
  padding: 0 4rpx;
}
.section-title {
  font-size: 30rpx;
  font-weight: 600;
  color: $text-primary;
}
.more {
  color: $text-secondary;
  font-size: 24rpx;
}
.grid {
  background: #fff;
  border-radius: 20rpx;
  padding: 28rpx 8rpx 8rpx;
  display: flex;
  flex-wrap: wrap;
}
.tool {
  width: 25%;
  box-sizing: border-box;
  padding: 8rpx 6rpx 28rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.tool-icon-wrap {
  position: relative;
  width: 96rpx;
  height: 96rpx;
}
.tool-icon {
  width: 96rpx;
  height: 96rpx;
  border-radius: 24rpx;
  display: block;
}
.tool-icon.dim {
  opacity: 0.4;
}
.soon {
  position: absolute;
  left: 50%;
  bottom: -6rpx;
  transform: translateX(-50%);
  padding: 0 8rpx;
  border-radius: 8rpx;
  background: #eef0f5;
  color: #8a90a0;
  font-size: 16rpx;
  line-height: 28rpx;
  white-space: nowrap;
}
.quick-icon {
  width: 72rpx;
  height: 72rpx;
  border-radius: 20rpx;
  flex-shrink: 0;
}
.tool-name {
  margin-top: 14rpx;
  font-size: 24rpx;
  line-height: 1.35;
  text-align: center;
  color: $text-primary;
}
.recent-scroll {
  white-space: nowrap;
}
.recent-item {
  display: inline-block;
  margin-right: 12rpx;
  padding: 14rpx 24rpx;
  background: #fff;
  border-radius: 12rpx;
  font-size: 24rpx;
  color: $text-primary;
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
