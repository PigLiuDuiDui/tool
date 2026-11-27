<template>
  <view class="page">
    <view class="profile card">
      <view class="avatar">{{ avatarText }}</view>
      <view class="profile-meta">
        <text class="name">{{ displayName }}</text>
        <view class="quota-row">
          <view class="quota-pill">
            <view class="quota-dot" />
            <text>今日剩余 {{ quota.remaining }}/{{ quota.dailyLimit }} 次</text>
          </view>
        </view>
      </view>
    </view>

    <view class="card menu">
      <view class="item" @click="openPanel('records')">
        <view class="item-left">
          <view class="menu-ico tone-blue">
            <view class="ico-clock" />
          </view>
          <text class="item-text">最近处理记录</text>
        </view>
        <text class="arrow">›</text>
      </view>
      <view class="item" @click="openPanel('files')">
        <view class="item-left">
          <view class="menu-ico tone-teal">
            <view class="ico-folder" />
          </view>
          <text class="item-text">我的文件</text>
        </view>
        <text class="arrow">›</text>
      </view>
      <view class="item" @click="openPanel('fav')">
        <view class="item-left">
          <view class="menu-ico tone-orange">
            <view class="ico-star" />
          </view>
          <text class="item-text">我的收藏</text>
        </view>
        <text class="arrow">›</text>
      </view>
    </view>

    <view class="card menu">
      <view class="item" @click="openSheet('使用帮助', '证件照：选照片，看预览，保存到相册。图片：选图后直接看效果，再保存。文件：从聊天记录选择，处理完可以打开或转发。')">
        <view class="item-left">
          <view class="menu-ico tone-purple">
            <view class="ico-help" />
          </view>
          <text class="item-text">使用帮助</text>
        </view>
        <text class="arrow">›</text>
      </view>
      <view class="item" @click="feedbackOpen = true">
        <view class="item-left">
          <view class="menu-ico tone-blue">
            <view class="ico-chat" />
          </view>
          <text class="item-text">意见反馈</text>
        </view>
        <text class="arrow">›</text>
      </view>
      <view class="item" @click="openSheet('关于我们', '轻工具箱用于证件照、图片和文件处理。上传的文件默认保留 24 小时。')">
        <view class="item-left">
          <view class="menu-ico tone-teal">
            <view class="ico-info" />
          </view>
          <text class="item-text">关于我们</text>
        </view>
        <text class="arrow">›</text>
      </view>
    </view>

    <view v-if="panel === 'records'" class="card jobs">
      <view class="jobs-head">
        <text class="block-title">最近处理记录</text>
        <text class="jobs-count">{{ jobs.length }} 条</text>
      </view>
      <view v-if="!jobs.length" class="panel-empty">还没有记录。处理完成后会出现在这里。</view>
      <view v-for="j in jobs" :key="j.id" class="job">
        <image class="job-ico" :src="toolIcon(j.toolCode)" mode="aspectFit" />
        <view class="job-meta">
          <text class="j-name">{{ toolLabel(j.toolCode) }}</text>
          <view class="j-sub">
            <text class="status-tag" :class="statusClass(j.status)">{{ statusLabel(j.status) }}</text>
            <text class="j-size">{{ formatSize(j.outputSize || j.inputSize) }}</text>
          </view>
        </view>
        <view
          v-if="j.status === 'SUCCESS'"
          class="dl-btn"
          @click.stop="download(j)"
        >打开</view>
      </view>
    </view>

    <view v-if="panel === 'files'" class="card jobs">
      <view class="jobs-head">
        <text class="block-title">我的文件</text>
        <text class="jobs-count">{{ files.length }} 个</text>
      </view>
      <view v-if="!files.length" class="panel-empty">还没有可打开的文件。成功的结果会在这里保留到过期。</view>
      <view v-for="j in files" :key="'f-' + j.id" class="job">
        <image class="job-ico" :src="toolIcon(j.toolCode)" mode="aspectFit" />
        <view class="job-meta">
          <text class="j-name">{{ j.outputName || toolLabel(j.toolCode) }}</text>
          <text class="j-size">{{ formatSize(j.outputSize) }}</text>
        </view>
        <view class="dl-btn" @click.stop="download(j)">打开</view>
      </view>
    </view>

    <view v-if="panel === 'fav'" class="card jobs">
      <view class="jobs-head">
        <text class="block-title">我的收藏</text>
        <text class="jobs-count">{{ favorites.length }} 个</text>
      </view>
      <view v-if="!favorites.length" class="panel-empty">在工具列表里点星标，收藏的工具会出现在这里。</view>
      <view v-for="t in favorites" :key="t.code" class="job" @click="openTool(t)">
        <image class="job-ico" :src="toolIcon(t.code)" mode="aspectFit" />
        <view class="job-meta">
          <text class="j-name">{{ t.name }}</text>
          <text class="j-size">{{ t.desc || '已收藏' }}</text>
        </view>
        <text class="arrow">›</text>
      </view>
    </view>

    <view v-if="feedbackOpen" class="sheet-mask" @click="feedbackOpen = false">
      <view class="sheet" @click.stop>
        <text class="sheet-title">意见反馈</text>
        <textarea
          v-model="feedbackText"
          class="feedback-input"
          maxlength="200"
          placeholder="写下结果不对的地方，或想改的操作"
        />
        <view class="sheet-ok" @click="submitFeedback">留下</view>
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
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { request } from '../../utils/request'
import { getUser, ensureLogin } from '../../utils/auth'
import { BASE_URL } from '../../utils/config'
import { toolIcon } from '../../utils/icons'
import { getFavorites } from '../../utils/favorites'
import { saveFeedback } from '../../utils/feedback'
import { openResult } from '../../utils/open-result'
import { sheetShow, sheetTitle, sheetContent, openSheet, closeSheet } from '../../utils/sheet'

const LABEL_MAP = {
  word_compress: 'Word压缩',
  pdf_compress: 'PDF压缩',
  image_compress: '图片压缩',
  word_to_pdf: 'Word转PDF',
  pdf_to_word: 'PDF转Word',
  image_to_pdf: '图片转PDF',
  pdf_to_image: 'PDF转图片',
  excel_to_pdf: 'Excel转PDF',
  ppt_to_pdf: 'PPT转PDF',
  image_crop: '图片裁剪',
  image_resize: '图片尺寸调整',
  image_format: '图片格式转换',
  image_bg: '图片背景修改',
  pdf_merge: 'PDF合并',
  pdf_split: 'PDF拆分',
  idphoto_smart: '智能证件照',
  idphoto_1inch: '一寸照片',
  idphoto_2inch: '二寸照片',
  idphoto_exam: '考试报名照',
  idphoto_passport: '护照照片',
}

const user = ref({})
const quota = ref({ remaining: 0, dailyLimit: 10 })
const jobs = ref([])
const favorites = ref([])
const panel = ref('')
const feedbackOpen = ref(false)
const feedbackText = ref('')
const avatarText = ref('我')

const files = computed(() => {
  const now = Date.now()
  return jobs.value.filter((j) => {
    if (j.status !== 'SUCCESS' || !j.outputName) return false
    if (!j.expireAt) return true
    const ts = Date.parse(String(j.expireAt).replace(' ', 'T'))
    return Number.isNaN(ts) || ts > now
  })
})

const displayName = computed(() => {
  const n = user.value.nickName || ''
  if (!n || /[?]{2,}/.test(n) || /�/.test(n)) return '微信用户'
  return n
})

onShow(async () => {
  try {
    await ensureLogin()
    user.value = getUser()
    const name = displayName.value
    avatarText.value = name.slice(0, 1)
    quota.value = (await request({ url: '/jobs/quota' })) || quota.value
    jobs.value = (await request({ url: '/jobs', data: { limit: 20 } })) || []
    favorites.value = getFavorites()
  } catch (e) {
    console.warn(e)
  }
})

function toolLabel(code) {
  return LABEL_MAP[code] || code
}

function toolTone(code) {
  if (!code) return 'purple'
  if (code.startsWith('idphoto')) return 'purple'
  if (code.startsWith('image_')) return 'teal'
  if (code.includes('compress') || code.includes('merge') || code.includes('split')) return 'orange'
  return 'blue'
}

function statusLabel(s) {
  if (s === 'SUCCESS') return '成功'
  if (s === 'FAILED') return '失败'
  if (s === 'RUNNING') return '处理中'
  if (s === 'PENDING') return '排队中'
  return s || '-'
}

function statusClass(s) {
  if (s === 'SUCCESS') return 'ok'
  if (s === 'FAILED') return 'fail'
  return 'pending'
}

function toast(title) {
  uni.showToast({ title, icon: 'none' })
}

function openPanel(name) {
  panel.value = panel.value === name ? '' : name
  if (name === 'fav') favorites.value = getFavorites()
}

function openTool(tool) {
  uni.navigateTo({ url: `/pages/tool-detail/index?code=${tool.code}` })
}

function submitFeedback() {
  if (!saveFeedback(feedbackText.value)) {
    uni.showToast({ title: '先写一点内容', icon: 'none' })
    return
  }
  feedbackText.value = ''
  feedbackOpen.value = false
  uni.showToast({ title: '已留下', icon: 'none' })
}

function formatSize(n) {
  if (!n) return '-'
  if (n < 1024) return n + 'B'
  if (n < 1024 * 1024) return (n / 1024).toFixed(1) + 'KB'
  return (n / 1024 / 1024).toFixed(2) + 'MB'
}

function download(job) {
  const token = uni.getStorageSync('accessToken')
  uni.downloadFile({
    url: `${BASE_URL}/jobs/${job.id}/download`,
    header: { Authorization: `Bearer ${token}` },
    success: (res) => {
      if (res.statusCode === 200) {
        openResult(res.tempFilePath, job.outputName || job.inputName)
      } else toast('下载失败')
    },
    fail: () => toast('下载失败'),
  })
}
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  padding: 24rpx 28rpx calc(24rpx + env(safe-area-inset-bottom));
  background: $bg-page;
  box-sizing: border-box;
}
.card {
  @include card;
  margin-bottom: 20rpx;
}
.profile {
  display: flex;
  align-items: center;
  gap: 24rpx;
  padding: 36rpx 32rpx;
}
.avatar {
  width: 96rpx;
  height: 96rpx;
  border-radius: 48rpx;
  background: $primary-from;
  color: #fff;
  text-align: center;
  line-height: 96rpx;
  font-size: 36rpx;
  font-weight: 600;
  flex-shrink: 0;
}
.profile-meta {
  flex: 1;
  min-width: 0;
}
.name {
  display: block;
  font-size: 34rpx;
  font-weight: 600;
  color: $text-primary;
}
.quota-row {
  margin-top: 12rpx;
}
.quota-pill {
  display: inline-flex;
  align-items: center;
  gap: 8rpx;
  padding: 6rpx 16rpx;
  border-radius: 999rpx;
  background: rgba(83, 109, 254, 0.08);
  color: $primary-from;
  font-size: 22rpx;
}
.quota-dot {
  width: 10rpx;
  height: 10rpx;
  border-radius: 50%;
  background: $primary-from;
}

.menu .item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 26rpx 28rpx;
  border-bottom: 1rpx solid #f2f4f8;
}
.menu .item:last-child {
  border-bottom: none;
}
.item-left {
  display: flex;
  align-items: center;
  gap: 20rpx;
}
.item-text {
  font-size: 28rpx;
  color: $text-primary;
}
.arrow {
  color: #c5cad6;
  font-size: 34rpx;
  line-height: 1;
}
.menu-ico {
  width: 64rpx;
  height: 64rpx;
  border-radius: 18rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.menu-ico.tone-purple {
  background: rgba(122, 108, 240, 0.12);
}
.menu-ico.tone-blue {
  background: rgba(76, 125, 255, 0.12);
}
.menu-ico.tone-teal {
  background: rgba(44, 184, 162, 0.12);
}
.menu-ico.tone-orange {
  background: rgba(240, 138, 60, 0.14);
}

.ico-clock {
  width: 22rpx;
  height: 22rpx;
  border: 3rpx solid #4c7dff;
  border-radius: 50%;
  position: relative;
  box-sizing: border-box;
}
.ico-clock::before {
  content: '';
  position: absolute;
  left: 8rpx;
  top: 3rpx;
  width: 2rpx;
  height: 8rpx;
  background: #4c7dff;
}
.ico-clock::after {
  content: '';
  position: absolute;
  left: 8rpx;
  top: 9rpx;
  width: 7rpx;
  height: 2rpx;
  background: #4c7dff;
}
.ico-folder {
  width: 26rpx;
  height: 20rpx;
  border-radius: 3rpx;
  background: #2cb8a2;
  position: relative;
}
.ico-folder::before {
  content: '';
  position: absolute;
  left: 0;
  top: -6rpx;
  width: 12rpx;
  height: 8rpx;
  border-radius: 3rpx 3rpx 0 0;
  background: #2cb8a2;
}
.ico-star {
  width: 0;
  height: 0;
  border-left: 10rpx solid transparent;
  border-right: 10rpx solid transparent;
  border-bottom: 7rpx solid #f08a3c;
  position: relative;
  top: -4rpx;
}
.ico-star::after {
  content: '';
  position: absolute;
  left: -10rpx;
  top: 5rpx;
  border-left: 10rpx solid transparent;
  border-right: 10rpx solid transparent;
  border-top: 7rpx solid #f08a3c;
}
.ico-help {
  width: 24rpx;
  height: 24rpx;
  border-radius: 50%;
  border: 3rpx solid #7c6cf0;
  box-sizing: border-box;
  position: relative;
}
.ico-help::before {
  content: '?';
  position: absolute;
  left: 0;
  right: 0;
  text-align: center;
  top: -2rpx;
  color: #7c6cf0;
  font-size: 18rpx;
  font-weight: 700;
  line-height: 24rpx;
}
.ico-chat {
  width: 24rpx;
  height: 18rpx;
  border-radius: 6rpx;
  background: #4c7dff;
  position: relative;
}
.ico-chat::after {
  content: '';
  position: absolute;
  left: 6rpx;
  bottom: -6rpx;
  border-left: 6rpx solid transparent;
  border-right: 6rpx solid transparent;
  border-top: 8rpx solid #4c7dff;
}
.ico-info {
  width: 24rpx;
  height: 24rpx;
  border-radius: 50%;
  border: 3rpx solid #2cb8a2;
  box-sizing: border-box;
  position: relative;
}
.ico-info::before {
  content: 'i';
  position: absolute;
  left: 0;
  right: 0;
  text-align: center;
  color: #2cb8a2;
  font-size: 18rpx;
  font-weight: 700;
  line-height: 20rpx;
  font-style: italic;
}

.jobs {
  padding: 28rpx 24rpx 12rpx;
}
.jobs-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 8rpx 8rpx;
}
.block-title {
  font-size: 30rpx;
  font-weight: 600;
  color: $text-primary;
}
.jobs-count {
  font-size: 22rpx;
  color: $text-secondary;
}
.job {
  display: flex;
  align-items: center;
  gap: 18rpx;
  padding: 20rpx 8rpx;
  border-top: 1rpx solid #f2f4f8;
}
.job-ico {
  width: 64rpx;
  height: 64rpx;
  border-radius: 18rpx;
  overflow: hidden;
  flex-shrink: 0;
}
.job-meta {
  flex: 1;
  min-width: 0;
}
.j-name {
  display: block;
  font-size: 28rpx;
  font-weight: 600;
  color: $text-primary;
}
.j-sub {
  margin-top: 8rpx;
  display: flex;
  align-items: center;
  gap: 12rpx;
}
.status-tag {
  font-size: 20rpx;
  padding: 2rpx 10rpx;
  border-radius: 8rpx;
  line-height: 1.4;
}
.status-tag.ok {
  color: #1a9f78;
  background: rgba(44, 184, 162, 0.12);
}
.status-tag.fail {
  color: #d94b4b;
  background: rgba(217, 75, 75, 0.1);
}
.status-tag.pending {
  color: #8a90a0;
  background: rgba(138, 144, 160, 0.12);
}
.j-size {
  font-size: 22rpx;
  color: $text-secondary;
}
.dl-btn {
  flex-shrink: 0;
  padding: 10rpx 22rpx;
  border-radius: 12rpx;
  background: rgba(83, 109, 254, 0.1);
  color: $primary-from;
  font-size: 24rpx;
  font-weight: 600;
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
.panel-empty {
  padding: 28rpx 12rpx 16rpx;
  color: $text-secondary;
  font-size: 24rpx;
  line-height: 1.5;
}
.feedback-input {
  width: 100%;
  height: 180rpx;
  margin-top: 20rpx;
  padding: 16rpx;
  box-sizing: border-box;
  background: #f7f8fc;
  border-radius: 12rpx;
  font-size: 26rpx;
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
