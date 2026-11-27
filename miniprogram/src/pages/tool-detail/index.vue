<template>
  <view class="page">
    <view v-if="done" class="card done">
      <image
        v-if="resultPreview"
        class="done-img"
        :class="{ portrait: isIdPhoto }"
        :src="resultPreview"
        mode="aspectFit"
        @click="previewResult"
      />
      <text class="done-meta">{{ result.outputName }} · {{ formatSize(result.outputSize) }}</text>
      <text class="preview-caption">制作完成，可保存或转发</text>
    </view>

    <view v-else-if="(isIdPhoto || toolCode === 'image_bg') && fileName && thumb" class="card picked">
      <view class="preview-frame" :style="previewBox">
        <image v-if="mattePreview" class="matte" :src="mattePreview" mode="aspectFit" />
        <image v-else class="matte raw" :src="thumb" mode="aspectFill" />
      </view>
      <text v-if="previewing" class="preview-caption">预览生成中</text>
      <text v-else-if="mattePreview" class="preview-caption">预览</text>
      <view class="picked-row">
        <view class="picked-meta">
          <text class="upload-name">{{ fileName }}</text>
          <text class="upload-size">{{ sizeText }}{{ extraHint ? ' · ' + extraHint : '' }}</text>
        </view>
        <text class="upload-change" @click="chooseFile">更换</text>
      </view>
    </view>

    <view v-else-if="fileName && thumb" class="card picked" @click="chooseFile">
      <image class="picked-img" :src="thumb" mode="aspectFill" />
      <view class="picked-row">
        <view class="picked-meta">
          <text class="upload-name">{{ fileName }}</text>
          <text class="upload-size">{{ sizeText }}{{ extraHint ? ' · ' + extraHint : '' }}</text>
        </view>
        <text class="upload-change">更换</text>
      </view>
    </view>

    <view v-else class="upload" :class="{ filled: !!fileName }" @click="chooseFile">
      <view v-if="!fileName" class="upload-empty">
        <text class="upload-icon">↑</text>
        <text class="upload-title">{{ uploadTitle }}</text>
        <text class="upload-hint">{{ uploadHint }}</text>
      </view>
      <view v-else class="upload-filled">
        <image v-if="thumb" class="upload-thumb" :src="thumb" mode="aspectFill" />
        <view v-else class="upload-file-badge">文</view>
        <view class="upload-meta">
          <text class="upload-name">{{ fileName }}</text>
          <text class="upload-size">{{ sizeText }}</text>
          <text v-if="extraHint" class="upload-extra">{{ extraHint }}</text>
        </view>
        <text class="upload-change">更换</text>
      </view>
    </view>

    <!-- 多图预览 -->
    <view v-if="filePath && (toolCode === 'image_to_pdf' || toolCode === 'pdf_merge')" class="card thumbs">
      <view class="thumbs-head">
        <text class="label">已选 {{ 1 + extraPaths.length }} 个</text>
        <text class="upload-change" @click="addMore">再加</text>
      </view>
      <view class="file-line">
        <text class="file-line-name">{{ fileName }}</text>
        <text class="file-line-x" @click="removeAt(0)">删除</text>
      </view>
      <view v-for="(name, i) in extraNames" :key="i" class="file-line">
        <text class="file-line-name">{{ name }}</text>
        <text class="file-line-x" @click="removeAt(i + 1)">删除</text>
      </view>
    </view>

    <view class="card params">
      <!-- 证件照 -->
      <view v-if="isIdPhoto">
        <text class="label">规格</text>
        <view class="chips">
          <view
            v-for="s in sizes"
            :key="s.code"
            class="chip"
            :class="{ active: sizeCode === s.code }"
            @click="onSize(s)"
          >
            <text class="chip-name">{{ s.name }}</text>
            <text v-if="s.width" class="chip-size">{{ s.width }}×{{ s.height }}</text>
            <text v-else class="chip-size">{{ s.tip || '自定义' }}</text>
          </view>
        </view>
        <view v-if="sizeCode === 'custom'" class="row-inputs">
          <input v-model="width" type="number" class="input" placeholder="宽(px)" @blur="refreshPreview" />
          <input v-model="height" type="number" class="input" placeholder="高(px)" @blur="refreshPreview" />
        </view>
        <text class="label">背景色</text>
        <view class="colors">
          <view
            v-for="c in colors"
            :key="c.value"
            class="color"
            :class="{ active: bgColor === c.value }"
            :style="{ background: c.value }"
            @click="bgColor = c.value"
          >
            <text class="color-name" :class="{ dark: c.value === '#FFFFFF' }">{{ c.name }}</text>
          </view>
          <view
            class="color more"
            :class="{ active: customActive }"
            :style="{ background: customActive ? bgColor : '' }"
            @click="pickerOn = !pickerOn"
          >
            <text class="color-name" :class="{ dark: customActive && colorIsLight }">其他</text>
          </view>
        </view>
        <view v-if="pickerOn" class="picker">
          <view class="picker-chip" :style="{ background: bgColor }" />
          <text class="label">色相</text>
          <slider :value="hue" min="0" max="360" activeColor="#536DFE" block-size="18" @changing="onHue" @change="onHue" />
          <text class="label">明暗</text>
          <slider :value="lightness" min="18" max="88" activeColor="#536DFE" block-size="18" @changing="onLight" @change="onLight" />
        </view>
      </view>

      <!-- 压缩：质量和尺寸跟桌面压缩工具同一套 -->
      <view v-else-if="isCompress">
        <text class="label">常用</text>
        <view class="chips">
          <view class="chip" :class="{ active: mode === 'standard' }" @click="setMode('standard')">更清晰</view>
          <view class="chip" :class="{ active: mode === 'enhanced' }" @click="setMode('enhanced')">更小</view>
          <view class="chip" :class="{ active: mode === 'custom' }">自定义</view>
        </view>
        <text class="label">图片质量 {{ quality }}</text>
        <slider
          :value="quality"
          min="40"
          max="95"
          activeColor="#536DFE"
          block-size="18"
          @changing="onQualityLive"
          @change="onQuality"
        />
        <template v-if="toolCode !== 'pdf_compress'">
          <text class="label">最长边</text>
          <view class="chips">
            <view
              v-for="edge in edgeOptions"
              :key="edge.v"
              class="chip"
              :class="{ active: maxEdge === edge.v }"
              @click="setEdge(edge.v)"
            >{{ edge.label }}</view>
          </view>
          <text class="label">缩放</text>
          <view class="chips">
            <view
              v-for="item in scaleOptions"
              :key="item.v"
              class="chip"
              :class="{ active: scalePct === item.v }"
              @click="setScale(item.v)"
            >{{ item.label }}</view>
          </view>
          <text class="label">输出格式</text>
          <view class="chips">
            <view
              v-for="item in formatOptions"
              :key="item.v"
              class="chip"
              :class="{ active: outputFormat === item.v }"
              @click="setFormat(item.v)"
            >{{ item.label }}</view>
          </view>
        </template>
        <text v-else class="hint-line">PDF 按质量压体积，页面里的图片格式保持不变</text>
        <view v-if="fileName" class="estimate">
          原文件 {{ sizeText || '-' }}<text v-if="estimatedSize">，大约 {{ formatSize(estimatedSize) }}{{ savedHint }}</text>
        </view>
      </view>

      <!-- 裁剪 -->
      <view v-else-if="toolCode === 'image_crop'">
        <view v-if="cropMode === 'inset'">
          <text class="label">从四周裁掉 {{ cropInset }}%</text>
          <slider :value="cropInset" min="0" max="40" activeColor="#536DFE" @change="onCropInset" />
          <text class="hint-line">保留中心区域。选图时会先尝试直接裁剪。</text>
        </view>
        <view v-else class="hint-line">选图后直接裁剪。若当前环境不支持，会改为从四周裁掉。</view>
        <view v-if="filePath" class="link-btn" @click="openCrop">重新裁剪</view>
      </view>

      <!-- 其它图片工具 -->
      <view v-else-if="isImageTool">
        <text v-if="fileName && toolCode === 'image_resize'" class="hint-line">将保存为 {{ width || '-' }} × {{ height || '-' }} 像素</text>
        <text v-if="fileName && toolCode === 'image_format'" class="hint-line">将保存为 {{ outFormat.toUpperCase() }}</text>
        <text v-if="toolCode === 'image_format'" class="label">目标格式</text>
        <view v-if="toolCode === 'image_format'" class="chips">
          <view
            v-for="f in ['jpeg', 'png', 'webp']"
            :key="f"
            class="chip"
            :class="{ active: outFormat === f }"
            @click="outFormat = f"
          >
            {{ f.toUpperCase() }}
          </view>
        </view>
        <text v-if="toolCode === 'image_bg'" class="label">背景色</text>
        <view v-if="toolCode === 'image_bg'" class="colors">
          <view
            v-for="c in colors"
            :key="c.value"
            class="color"
            :class="{ active: bgColor === c.value }"
            :style="{ background: c.value }"
            @click="bgColor = c.value"
          >
            <text class="color-name" :class="{ dark: c.value === '#FFFFFF' }">{{ c.name }}</text>
          </view>
          <view
            class="color more"
            :class="{ active: customActive }"
            :style="{ background: customActive ? bgColor : '' }"
            @click="pickerOn = !pickerOn"
          >
            <text class="color-name" :class="{ dark: customActive && colorIsLight }">其他</text>
          </view>
        </view>
        <view v-if="pickerOn && toolCode === 'image_bg'" class="picker">
          <view class="picker-chip" :style="{ background: bgColor }" />
          <text class="label">色相</text>
          <slider :value="hue" min="0" max="360" activeColor="#536DFE" block-size="18" @changing="onHue" @change="onHue" />
          <text class="label">明暗</text>
          <slider :value="lightness" min="18" max="88" activeColor="#536DFE" block-size="18" @changing="onLight" @change="onLight" />
        </view>
        <text v-if="toolCode === 'image_resize'" class="label">宽度 / 高度</text>
        <view v-if="toolCode === 'image_resize'" class="row-inputs">
          <input v-model="width" type="number" class="input" placeholder="宽" />
          <input v-model="height" type="number" class="input" placeholder="高" />
        </view>
      </view>

      <!-- PDF 拆分 / 转图片：页码 -->
      <view v-else-if="needPage">
        <text class="label">{{ toolCode === 'pdf_split' ? '导出第几页' : '转出第几页' }}</text>
        <view class="stepper">
          <view class="step-btn" @click="stepPage(-1)">－</view>
          <text class="step-num">{{ pageNo }}</text>
          <view class="step-btn" @click="stepPage(1)">＋</view>
        </view>
        <text class="hint-line">{{ toolCode === 'pdf_split' ? '保存这一页为单独的 PDF' : '保存这一页为图片' }}</text>
      </view>

      <view v-else-if="toolCode === 'pdf_merge'" class="plain">
        <text class="desc">从聊天记录选择至少 2 个 PDF，可以再追加或删掉。</text>
      </view>

      <view v-else class="plain">
        <text class="desc">选好文件后，底部按钮会保存结果。</text>
      </view>
      <view class="fav-line" @click="onFav">{{ favored ? '已收藏' : '收藏这个工具' }}</view>
    </view>

    <view class="footer">
      <text v-if="footerError" class="footer-error">{{ footerError }}</text>
      <text v-else-if="remaining <= 0" class="quota-hint">今天 10 次已用完，明天 0 点恢复</text>
      <button
        class="primary-btn"
        :class="{ exhausted: remaining <= 0 && !loading && !done }"
        :disabled="loading"
        @click="onPrimary"
      >
        {{ buttonText }}
      </button>
      <text v-if="done" class="again" @click="reset">{{ againText }}</text>
      <text v-else-if="remaining > 0" class="remain">今日剩余 {{ remaining }} 次 · 文件 24 小时后清理</text>
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
import { onLoad } from '@dcloudio/uni-app'
import { request, uploadJob, estimateJob, previewJob } from '../../utils/request'
import { ensureLogin } from '../../utils/auth'
import { pushRecent } from '../../utils/recent'
import { BASE_URL } from '../../utils/config'
import { sheetShow, sheetTitle, sheetContent, openSheet, closeSheet } from '../../utils/sheet'
import { isFavorite, toggleFavorite } from '../../utils/favorites'
import { openResult } from '../../utils/open-result'

const toolCode = ref('')
const tool = ref({})
const sizes = ref([])
const filePath = ref('')
const fileName = ref('')
const fileSize = ref(0)
const thumb = ref('')
const mattePreview = ref('')
const previewSize = ref({ w: 0, h: 0 })
const previewing = ref(false)
let previewSeq = 0
const extraPaths = ref([])
const extraThumbs = ref([])
const extraNames = ref([])
const loading = ref(false)
const status = ref('')
const result = ref(null)
const resultPreview = ref('')
const remaining = ref(10)
const footerError = ref('')
const favTick = ref(0)
const estimatedSize = ref(0)

const sizeCode = ref('1inch')
const bgColor = ref('#FFFFFF')
const mode = ref('standard')
const quality = ref(85)
const maxEdge = ref(1920)
const scalePct = ref(100)
const outputFormat = ref('jpeg')
const edgeOptions = [
  { v: 0, label: '原尺寸' },
  { v: 2560, label: '2560' },
  { v: 1920, label: '1920' },
  { v: 1280, label: '1280' },
  { v: 800, label: '800' },
]
const scaleOptions = [
  { v: 100, label: '100%' },
  { v: 75, label: '75%' },
  { v: 50, label: '50%' },
]
const outFormat = ref('jpeg')
const width = ref('800')
const height = ref('800')
const pageNo = ref('1')
const cropMode = ref('system')
const cropInset = ref(10)
const cropBox = ref(null) // {x,y,w,h} 相对原图像素
const imgNatural = ref({ w: 0, h: 0 })

const colors = [
  { value: '#FFFFFF', name: '白' },
  { value: '#438EDB', name: '蓝' },
  { value: '#FF0000', name: '红' },
]
const pickerOn = ref(false)
const hue = ref(200)
const lightness = ref(52)

const isIdPhoto = computed(() => toolCode.value.startsWith('idphoto'))
const isCompress = computed(() =>
  ['image_compress', 'pdf_compress', 'word_compress'].includes(toolCode.value),
)
const formatOptions = computed(() => {
  if (toolCode.value === 'image_compress') {
    return [
      { v: 'jpeg', label: 'JPEG' },
      { v: 'png', label: 'PNG' },
      { v: 'webp', label: 'WebP' },
      { v: 'original', label: '原格式' },
    ]
  }
  return [
    { v: 'jpeg', label: 'JPEG' },
    { v: 'png', label: 'PNG' },
    { v: 'original', label: '原格式' },
  ]
})
const savedHint = computed(() => {
  const before = Number(fileSize.value || 0)
  const after = Number(estimatedSize.value || 0)
  if (!before || !after) return ''
  const ratio = Math.round((1 - after / before) * 100)
  return ratio > 0 ? `，大约省 ${ratio}%` : ''
})
const isImageTool = computed(() => toolCode.value.startsWith('image_'))
const needImagePick = computed(
  () => isIdPhoto.value || isImageTool.value || toolCode.value === 'image_to_pdf',
)
const needPage = computed(() => ['pdf_split', 'pdf_to_image'].includes(toolCode.value))
const canSaveAlbum = computed(
  () =>
    isIdPhoto.value ||
    isImageTool.value ||
    toolCode.value === 'pdf_to_image' ||
    (resultPreview.value && /\.(png|jpe?g|webp)$/i.test(result.value?.outputName || '')),
)
const sizeText = computed(() => (fileSize.value ? formatSize(fileSize.value) : ''))
const extraHint = computed(() => {
  if (toolCode.value === 'image_to_pdf' && extraPaths.value.length) {
    return `另含 ${extraPaths.value.length} 张`
  }
  if (toolCode.value === 'pdf_merge' && extraPaths.value.length) {
    return `另含 ${extraPaths.value.length} 个文件`
  }
  return ''
})
const uploadTitle = computed(() => {
  if (isIdPhoto.value) return '选择人像照片'
  if (toolCode.value === 'image_to_pdf') return '选择图片，可多选'
  if (toolCode.value === 'pdf_merge') return '从聊天记录选择 PDF'
  if (toolCode.value.includes('word')) return '从聊天记录选择 Word'
  if (toolCode.value.includes('pdf')) return '从聊天记录选择 PDF'
  return '选择图片'
})
const uploadHint = computed(() => {
  if (isIdPhoto.value || isImageTool.value) return '拍照或从相册选择'
  if (toolCode.value === 'image_to_pdf') return '最多 9 张，按顺序合成'
  if (toolCode.value === 'pdf_merge') return '至少 2 个，可再追加'
  return '点这里打开聊天记录'
})
const primaryText = computed(() => {
  if (done.value) return canSaveAlbum.value ? '保存到相册' : '打开并转发'
  if (isCompress.value) return '开始压缩'
  if (toolCode.value === 'pdf_to_image') return '保存图片'
  if (isIdPhoto.value || isImageTool.value) return '保存到相册'
  if (String(toolCode.value).includes('pdf') || toolCode.value === 'word_to_pdf' || toolCode.value === 'image_to_pdf') {
    return '保存 PDF'
  }
  return '保存文件'
})
const previewBox = computed(() => {
  if (toolCode.value === 'image_bg') {
    return { width: '100%', height: '420rpx', background: bgColor.value }
  }
  const w = previewSize.value.w || Number(width.value) || 295
  const h = previewSize.value.h || Number(height.value) || 413
  const frameW = 420
  const frameH = Math.max(120, Math.round((frameW * h) / w))
  return {
    width: frameW + 'rpx',
    height: frameH + 'rpx',
    background: bgColor.value,
  }
})
const customActive = computed(
  () => !colors.some((c) => c.value.toUpperCase() === String(bgColor.value).toUpperCase()),
)
const colorIsLight = computed(() => {
  const hex = String(bgColor.value || '#FFFFFF').replace('#', '')
  if (hex.length !== 6) return true
  const r = parseInt(hex.slice(0, 2), 16)
  const g = parseInt(hex.slice(2, 4), 16)
  const b = parseInt(hex.slice(4, 6), 16)
  return (r * 299 + g * 587 + b * 114) / 1000 > 170
})
const againText = computed(() =>
  isIdPhoto.value || isImageTool.value || toolCode.value === 'image_to_pdf' ? '换一张' : '换一个文件',
)
const favored = computed(() => {
  favTick.value
  return isFavorite(toolCode.value)
})
const buttonText = computed(() => {
  if (loading.value) return '处理中...'
  if (remaining.value <= 0) return '明天再来'
  return primaryText.value
})
const done = computed(() => result.value && result.value.status === 'SUCCESS')
const canRun = computed(() => {
  if (!filePath.value) return false
  if (toolCode.value === 'pdf_merge' && extraPaths.value.length < 1) return false
  return true
})
function showNotice(message) {
  const text = message || '处理失败，请稍后重试'
  if (text.includes('次数')) {
    openSheet('今天的次数用完了', '每天可处理 10 次，明天 0 点自动恢复。')
    return
  }
  footerError.value = text
}

const CODE_TO_SIZE = {
  idphoto_1inch: '1inch',
  idphoto_2inch: '2inch',
  idphoto_exam: 'exam',
  idphoto_passport: 'passport',
  idphoto_custom: 'custom',
  idphoto_smart: '1inch',
}

onLoad(async (query) => {
  toolCode.value = query.code || 'idphoto_smart'
  await ensureLogin()
  const data = await request({ url: '/tools/catalog' })
  sizes.value = data.idSizes || []
  const found = (data.tools || []).find((t) => t.code === toolCode.value)
  tool.value = found || { name: toolCode.value, desc: '' }
  uni.setNavigationBarTitle({ title: tool.value.name || '工具' })
  pushRecent(tool.value)

  if (isIdPhoto.value) {
    const preset = CODE_TO_SIZE[toolCode.value] || '1inch'
    sizeCode.value = preset
    const s = sizes.value.find((x) => x.code === preset)
    if (s && s.width > 0) {
      width.value = String(s.width)
      height.value = String(s.height)
    }
  }

  try {
    const q = await request({ url: '/jobs/quota' })
    remaining.value = q.remaining
  } catch (_) {}
})

function onSize(s) {
  sizeCode.value = s.code
  if (s.width > 0) {
    width.value = String(s.width)
    height.value = String(s.height)
  }
  refreshPreview()
}

function hslToHex(h, l) {
  const s = 0.72
  const ll = l / 100
  const a = s * Math.min(ll, 1 - ll)
  const f = (n) => {
    const k = (n + h / 30) % 12
    return ll - a * Math.max(-1, Math.min(k - 3, Math.min(9 - k, 1)))
  }
  const hex = (x) => Math.round(255 * f(x)).toString(16).padStart(2, '0')
  return `#${hex(0)}${hex(8)}${hex(4)}`.toUpperCase()
}

function applyCustomColor() {
  bgColor.value = hslToHex(Number(hue.value), Number(lightness.value))
}

function onHue(e) {
  hue.value = Number(e.detail.value)
  applyCustomColor()
}

function onLight(e) {
  lightness.value = Number(e.detail.value)
  applyCustomColor()
}

function setMode(next) {
  mode.value = next
  if (next === 'enhanced') {
    quality.value = 55
    maxEdge.value = 1280
    scalePct.value = 75
    outputFormat.value = 'jpeg'
  } else {
    quality.value = 85
    maxEdge.value = 1920
    scalePct.value = 100
    outputFormat.value = 'jpeg'
  }
  doEstimate()
}

function onQualityLive(e) {
  quality.value = Number(e.detail.value)
}

function onQuality(e) {
  quality.value = Number(e.detail.value)
  mode.value = 'custom'
  doEstimate()
}

function setEdge(value) {
  maxEdge.value = value
  mode.value = 'custom'
  doEstimate()
}

function setScale(value) {
  scalePct.value = value
  mode.value = 'custom'
  doEstimate()
}

function setFormat(value) {
  outputFormat.value = value
  mode.value = 'custom'
  doEstimate()
}

function stepPage(delta) {
  const n = Math.max(1, Number(pageNo.value || 1) + delta)
  pageNo.value = String(n)
}

function onFav() {
  const on = toggleFavorite({
    code: toolCode.value,
    name: tool.value.name || toolCode.value,
    desc: tool.value.desc || '',
    category: tool.value.category || '',
  })
  favTick.value += 1
  uni.showToast({ title: on ? '已收藏' : '已取消收藏', icon: 'none' })
}

function onCropInset(e) {
  cropInset.value = Number(e.detail.value)
  applyInsetCrop()
}

function pickImage(source, append) {
  const count = toolCode.value === 'image_to_pdf' ? 9 : 1
  uni.chooseImage({
    count,
    sourceType: source === 'camera' ? ['camera'] : source === 'album' ? ['album'] : ['album', 'camera'],
    success: (res) => (append ? appendFiles(res.tempFilePaths || [], res.tempFiles || []) : applyImages(res)),
  })
}

function chooseFile(append) {
  if (needImagePick.value) {
    uni.showActionSheet({
      itemList: ['拍照', '从相册选择'],
      success: (res) => {
        if (res.tapIndex === 0) pickImage('camera', append === true)
        else if (res.tapIndex === 1) pickImage('album', append === true)
      },
    })
    return
  }
  const count = toolCode.value === 'pdf_merge' ? 9 : 1
  uni.chooseMessageFile({
    count,
    type: 'file',
    success: (res) => {
      const files = res.tempFiles || []
      if (!files.length) return
      if (append === true && filePath.value) {
        appendFiles(
          files.map((f) => f.path),
          files,
        )
        return
      }
      filePath.value = files[0].path
      fileName.value = files[0].name
      fileSize.value = files[0].size || 0
      thumb.value = ''
      extraPaths.value = files.slice(1).map((f) => f.path)
      extraNames.value = files.slice(1).map((f) => f.name)
      extraThumbs.value = []
      result.value = null
      resultPreview.value = ''
      status.value = ''
      if (isCompress.value) doEstimate()
    },
    fail: () => uni.showToast({ title: '请从聊天记录选择文件', icon: 'none' }),
  })
}

function appendFiles(paths, files) {
  const incoming = (paths || []).filter(Boolean)
  if (!incoming.length) return
  if (!filePath.value) {
    filePath.value = incoming[0]
    fileName.value = files[0]?.name || 'file'
    thumb.value = toolCode.value === 'image_to_pdf' ? incoming[0] : ''
    extraPaths.value = incoming.slice(1)
    extraNames.value = incoming.slice(1).map((p, i) => files[i + 1]?.name || `文件${i + 2}`)
    extraThumbs.value = toolCode.value === 'image_to_pdf' ? incoming.slice(1) : []
    return
  }
  const room = 8 - extraPaths.value.length
  const more = incoming.slice(0, Math.max(0, room))
  extraPaths.value = extraPaths.value.concat(more)
  extraNames.value = extraNames.value.concat(more.map((p, i) => files[i]?.name || `文件${extraNames.value.length + i + 2}`))
  if (toolCode.value === 'image_to_pdf') extraThumbs.value = extraThumbs.value.concat(more)
}

function applyImages(res) {
  const paths = res.tempFilePaths || []
  const files = res.tempFiles || []
  if (!paths.length) return
  filePath.value = paths[0]
  fileName.value = files[0]?.name || `photo_${paths.length}.jpg`
  fileSize.value = files.reduce((s, f) => s + (f?.size || 0), 0)
  thumb.value = paths[0]
  extraPaths.value = paths.slice(1)
  extraThumbs.value = paths.slice(1)
  extraNames.value = files.slice(1).map((f, i) => f?.name || `img_${i + 2}.jpg`)
  result.value = null
  resultPreview.value = ''
  status.value = ''
  cropBox.value = null
  mattePreview.value = ''
  previewSize.value = { w: 0, h: 0 }
  if (isIdPhoto.value || toolCode.value === 'image_bg') refreshPreview()
  uni.getImageInfo({
    src: paths[0],
    success: (info) => {
      imgNatural.value = { w: info.width, h: info.height }
      if (toolCode.value === 'image_crop') {
        if (cropMode.value === 'system') openCrop()
        else applyInsetCrop()
      }
    },
  })
  if (isCompress.value) doEstimate()
}

function applyInsetCrop() {
  const { w, h } = imgNatural.value
  if (!w || !h) return
  const inset = Math.max(0, Math.min(40, cropInset.value)) / 100
  const x = Math.round(w * inset)
  const y = Math.round(h * inset)
  const cw = Math.max(1, Math.round(w * (1 - 2 * inset)))
  const ch = Math.max(1, Math.round(h * (1 - 2 * inset)))
  cropBox.value = { x, y, w: cw, h: ch }
}

function openCrop() {
  if (!filePath.value) return
  if (cropMode.value === 'inset') {
    applyInsetCrop()
    uni.showToast({ title: '已应用边距裁剪', icon: 'none' })
    return
  }
  if (typeof uni.cropImage === 'function') {
    uni.cropImage({
      src: filePath.value,
      success: (res) => {
        filePath.value = res.tempFilePath
        thumb.value = res.tempFilePath
        cropBox.value = null
        uni.getImageInfo({
          src: res.tempFilePath,
          success: (info) => {
            imgNatural.value = { w: info.width, h: info.height }
          },
        })
      },
      fail: () => {
        cropMode.value = 'inset'
        applyInsetCrop()
        uni.showToast({ title: '已切换为边距裁剪', icon: 'none' })
      },
    })
  } else {
    cropMode.value = 'inset'
    applyInsetCrop()
    uni.showToast({ title: '当前基础库不支持系统裁剪', icon: 'none' })
  }
}

function buildForm() {
  const form = {}
  if (isIdPhoto.value) {
    form.sizeCode = sizeCode.value
    form.bgColor = bgColor.value
    if (sizeCode.value === 'custom') {
      form.width = width.value
      form.height = height.value
    }
  }
  if (isCompress.value) {
    form.quality = String(quality.value)
    form.maxDimension = String(maxEdge.value)
    form.scale = String(scalePct.value / 100)
    form.outputFormat = outputFormat.value
  }
  if (toolCode.value === 'image_format') form.format = outFormat.value
  if (toolCode.value === 'image_bg') form.bgColor = bgColor.value
  if (toolCode.value === 'image_resize') {
    form.width = width.value
    form.height = height.value
  }
  if (toolCode.value === 'image_crop' && cropBox.value) {
    form.cropX = String(cropBox.value.x)
    form.cropY = String(cropBox.value.y)
    form.cropW = String(cropBox.value.w)
    form.cropH = String(cropBox.value.h)
  }
  if (needPage.value) {
    form.pages = String(pageNo.value || '1')
  }
  return form
}

function removeAt(index) {
  const paths = [filePath.value, ...extraPaths.value]
  const names = [fileName.value, ...extraNames.value]
  const thumbs = [thumb.value, ...extraThumbs.value]
  paths.splice(index, 1)
  names.splice(index, 1)
  thumbs.splice(index, 1)
  if (!paths.length) {
    filePath.value = ''
    fileName.value = ''
    fileSize.value = 0
    thumb.value = ''
    extraPaths.value = []
    extraNames.value = []
    extraThumbs.value = []
    return
  }
  filePath.value = paths[0]
  fileName.value = names[0]
  thumb.value = thumbs[0] || ''
  extraPaths.value = paths.slice(1)
  extraNames.value = names.slice(1)
  extraThumbs.value = thumbs.slice(1).filter(Boolean)
}

function addMore() {
  chooseFile(true)
}

async function refreshPreview() {
  if (!(isIdPhoto.value || toolCode.value === 'image_bg') || !filePath.value) return
  const seq = ++previewSeq
  previewing.value = true
  try {
    const data = await previewJob(toolCode.value, filePath.value, buildForm())
    if (seq !== previewSeq || !data?.image) return
    const fsm = uni.getFileSystemManager()
    const path = `${uni.env.USER_DATA_PATH}/id-preview-${seq}.png`
    await new Promise((resolve, reject) => {
      fsm.writeFile({
        filePath: path,
        data: data.image,
        encoding: 'base64',
        success: resolve,
        fail: reject,
      })
    })
    if (seq !== previewSeq) return
    if (data.width && data.height) previewSize.value = { w: Number(data.width), h: Number(data.height) }
    mattePreview.value = path
  } catch (_) {
    // 预览失败时仍显示原图，正式生成不受影响
  } finally {
    if (seq === previewSeq) previewing.value = false
  }
}

async function doEstimate() {
  if (!filePath.value || !isCompress.value) return
  try {
    const data = await estimateJob(toolCode.value, filePath.value, buildForm())
    estimatedSize.value = data.estimatedSize
  } catch (_) {}
}

function onPrimary() {
  footerError.value = ''
  if (done.value) {
    downloadResult()
    return
  }
  run()
}

async function run() {
  footerError.value = ''
  if (remaining.value <= 0) {
    showNotice('今日次数已用完')
    return
  }
  if (!canRun.value) {
    if (toolCode.value === 'pdf_merge') {
      footerError.value = '至少再选 1 个 PDF'
    } else {
      footerError.value = '先选择文件'
    }
    return
  }
  if (toolCode.value === 'image_crop' && cropMode.value === 'inset' && !cropBox.value) {
    applyInsetCrop()
  }
  loading.value = true
  status.value = 'uploading'
  result.value = null
  try {
    await ensureLogin()
    status.value = 'processing'
    const data = await uploadJob(toolCode.value, filePath.value, buildForm(), extraPaths.value)
    result.value = data
    remaining.value = data.remaining ?? remaining.value
    if (data.status === 'SUCCESS') {
      status.value = ''
      uni.showToast({ title: '完成', icon: 'success' })
      if (canSaveAlbum.value || isIdPhoto.value || isImageTool.value || toolCode.value === 'pdf_to_image') {
        loadResultPreview(data.id, true)
      }
    } else {
      status.value = ''
      showNotice(data.errorMessage || '处理失败，请换一张图再试')
    }
  } catch (e) {
    status.value = ''
    showNotice(e.message || '处理失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

function loadResultPreview(id, saveAfter) {
  const token = uni.getStorageSync('accessToken')
  uni.downloadFile({
    url: `${BASE_URL}/jobs/${id}/download`,
    header: { Authorization: `Bearer ${token}` },
    success: (res) => {
      if (res.statusCode === 200) {
        resultPreview.value = res.tempFilePath
        if (saveAfter && canSaveAlbum.value) {
          uni.saveImageToPhotosAlbum({
            filePath: res.tempFilePath,
            success: () => uni.showToast({ title: '已保存到相册' }),
            fail: () => {},
          })
        }
      }
    },
  })
}

function previewResult() {
  if (resultPreview.value) {
    uni.previewImage({ urls: [resultPreview.value] })
  }
}

function reset() {
  result.value = null
  resultPreview.value = ''
  status.value = ''
  footerError.value = ''
  if ((isIdPhoto.value || toolCode.value === 'image_bg') && filePath.value) refreshPreview()
}

function downloadResult() {
  if (!result.value?.id) return
  const token = uni.getStorageSync('accessToken')
  const openLocal = (path) => {
    openResult(path, result.value?.outputName || '')
  }
  if (resultPreview.value) {
    openLocal(resultPreview.value)
    return
  }
  uni.downloadFile({
    url: `${BASE_URL}/jobs/${result.value.id}/download`,
    header: { Authorization: `Bearer ${token}` },
    success: (res) => {
      if (res.statusCode !== 200) {
        uni.showToast({ title: '下载失败', icon: 'none' })
        return
      }
      resultPreview.value = res.tempFilePath
      openLocal(res.tempFilePath)
    },
  })
}

function formatSize(n) {
  if (!n) return '-'
  if (n < 1024) return n + 'B'
  if (n < 1024 * 1024) return (n / 1024).toFixed(1) + 'KB'
  return (n / 1024 / 1024).toFixed(2) + 'MB'
}
</script>

<style lang="scss" scoped>
.page {
  padding: 24rpx 32rpx 200rpx;
  background: $bg-page;
  min-height: 100vh;
  box-sizing: border-box;
}
.card {
  background: #fff;
  border-radius: 16rpx;
  padding: 28rpx;
  margin-bottom: 20rpx;
}
.title {
  display: block;
  font-size: 34rpx;
  font-weight: 700;
}
.desc {
  display: block;
  margin-top: 10rpx;
  color: $text-secondary;
  font-size: 24rpx;
}
.label {
  display: block;
  margin: 12rpx 0;
  font-weight: 600;
}
.hint-line {
  display: block;
  margin-top: 8rpx;
  color: $text-secondary;
  font-size: 22rpx;
}
.chips {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
  margin-bottom: 12rpx;
}
.chip {
  padding: 12rpx 22rpx;
  border-radius: 12rpx;
  background: #f2f4fa;
  font-size: 24rpx;
}
.chip-name,
.chip-size {
  display: block;
  text-align: center;
}
.chip-size {
  margin-top: 4rpx;
  font-size: 20rpx;
  opacity: 0.75;
}
.chip.active {
  background: $primary-from;
  color: #fff;
}
.stepper {
  display: flex;
  align-items: center;
  gap: 20rpx;
}
.step-btn {
  width: 64rpx;
  height: 64rpx;
  line-height: 64rpx;
  text-align: center;
  border-radius: 12rpx;
  background: #f2f4fa;
  font-size: 32rpx;
}
.step-num {
  min-width: 48rpx;
  text-align: center;
  font-size: 32rpx;
  font-weight: 600;
}
.thumbs-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.file-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16rpx 0;
  border-top: 1rpx solid #f2f4f8;
}
.file-line-name {
  flex: 1;
  min-width: 0;
  font-size: 26rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.file-line-x {
  margin-left: 16rpx;
  color: #d94b4b;
  font-size: 24rpx;
}
.fav-line {
  margin-top: 20rpx;
  color: $primary-from;
  font-size: 24rpx;
}
.colors {
  display: flex;
  gap: 16rpx;
}
.color.more {
  background: conic-gradient(#ff4d4f, #faad14, #52c41a, #1677ff, #722ed1, #ff4d4f);
}
.picker {
  margin-top: 16rpx;
}
.picker-chip {
  width: 72rpx;
  height: 36rpx;
  border-radius: 8rpx;
  border: 1rpx solid #e5e7ef;
}
.color {
  width: 72rpx;
  height: 72rpx;
  border-radius: 12rpx;
  border: 4rpx solid transparent;
  box-shadow: inset 0 0 0 1rpx #e5e7ef;
  display: flex;
  align-items: center;
  justify-content: center;
}
.color-name {
  font-size: 22rpx;
  color: #fff;
}
.color-name.dark {
  color: #1f2330;
}
.color.active {
  border-color: $primary-from;
}
.estimate {
  margin-top: 8rpx;
  color: $tone-orange;
  font-size: 24rpx;
}
.link-btn {
  margin-top: 8rpx;
  color: $primary-from;
  font-size: 24rpx;
}
.row-inputs {
  display: flex;
  gap: 16rpx;
  margin-bottom: 12rpx;
}
.input {
  flex: 1;
  background: #f2f4fa;
  border-radius: 16rpx;
  padding: 16rpx;
}
.result-img {
  display: block;
  width: 100%;
  margin-top: 20rpx;
  border-radius: 16rpx;
  background: #f2f4fa;
}
.result-actions {
  display: flex;
  gap: 16rpx;
  margin-top: 20rpx;
}
.ghost {
  flex: 1;
  text-align: center;
  padding: 18rpx 0;
  border-radius: 12rpx;
  background: #f3f4f8;
  color: $primary-from;
  font-weight: 600;
}
.thumb-row {
  white-space: nowrap;
  margin-top: 12rpx;
}
.mini {
  display: inline-block;
  width: 96rpx;
  height: 96rpx;
  border-radius: 12rpx;
  margin-right: 12rpx;
  background: #eef1f8;
}
.footer {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 20rpx 32rpx calc(20rpx + env(safe-area-inset-bottom));
  background: rgba(247, 248, 252, 0.96);
  box-shadow: 0 -8rpx 24rpx rgba(31, 35, 48, 0.04);
}
.primary-btn {
  width: 100%;
  height: 88rpx;
  line-height: 88rpx;
  font-size: 30rpx;
  font-weight: 600;
  color: #fff;
  background: $primary-from;
  border-radius: 12rpx;
  border: none;
  padding: 0;
}
.primary-btn::after {
  border: none;
}
.primary-btn[disabled] {
  opacity: 0.45;
}
.primary-btn.exhausted {
  background: #c5cad6;
  color: #fff;
}
.footer-error {
  display: block;
  margin-bottom: 12rpx;
  text-align: center;
  color: #8a90a0;
  font-size: 24rpx;
  line-height: 1.4;
}
.again {
  display: block;
  margin-top: 16rpx;
  text-align: center;
  color: $primary-from;
  font-size: 26rpx;
}
.remain {
  display: block;
  text-align: center;
  margin-top: 12rpx;
  color: $text-secondary;
  font-size: 22rpx;
}
.plain .desc {
  margin-top: 0;
}

.upload {
  background: #fff;
  border-radius: 16rpx;
  border: 1rpx dashed #cfd3de;
  min-height: 220rpx;
  padding: 28rpx;
  margin-bottom: 20rpx;
  box-sizing: border-box;
}
.upload.filled {
  border-style: solid;
  border-color: #e6e8ef;
}
.upload-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 36rpx 8rpx;
}
.upload-icon {
  width: 64rpx;
  height: 64rpx;
  border-radius: 12rpx;
  background: #f3f4f8;
  color: #6b7280;
  font-size: 34rpx;
  text-align: center;
  line-height: 64rpx;
  margin-bottom: 16rpx;
}
.upload-title {
  font-size: 28rpx;
  font-weight: 600;
  color: $text-primary;
}
.upload-hint {
  margin-top: 8rpx;
  font-size: 22rpx;
  color: $text-secondary;
  text-align: center;
}
.upload-filled {
  display: flex;
  align-items: center;
  gap: 20rpx;
}
.upload-thumb {
  width: 96rpx;
  height: 96rpx;
  border-radius: 10rpx;
  background: #f3f4f8;
  flex-shrink: 0;
}
.upload-file-badge {
  width: 96rpx;
  height: 96rpx;
  border-radius: 10rpx;
  background: #f3f4f8;
  color: #6b7280;
  font-size: 28rpx;
  font-weight: 600;
  text-align: center;
  line-height: 96rpx;
  flex-shrink: 0;
}
.upload-meta {
  flex: 1;
  min-width: 0;
}
.upload-name {
  display: block;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.upload-size {
  display: block;
  margin-top: 8rpx;
  color: $text-secondary;
  font-size: 24rpx;
}
.upload-extra {
  display: block;
  margin-top: 6rpx;
  color: $primary-from;
  font-size: 22rpx;
}
.upload-change {
  color: $primary-from;
  font-size: 26rpx;
}
.preview-frame {
  margin: 0 auto;
  border-radius: 8rpx;
  overflow: hidden;
  position: relative;
}
.preview-frame .matte {
  width: 100%;
  height: 100%;
  display: block;
}
.preview-frame .raw {
  background: #f3f4f8;
}
.preview-caption {
  display: block;
  margin-top: 12rpx;
  text-align: center;
  color: $text-secondary;
  font-size: 22rpx;
}
.picked-img {
  width: 100%;
  height: 360rpx;
  border-radius: 12rpx;
  background: #f3f4f8;
}
.picked-img.portrait {
  width: 240rpx;
  height: 336rpx;
  margin: 0 auto;
  display: block;
}
.picked-row {
  margin-top: 16rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.done-img {
  width: 100%;
  height: 360rpx;
  border-radius: 12rpx;
  background: #f3f4f8;
}
.done-img.portrait {
  width: 240rpx;
  height: 336rpx;
  margin: 0 auto;
  display: block;
}
.done-meta {
  display: block;
  margin-top: 16rpx;
  text-align: center;
  color: $text-secondary;
  font-size: 24rpx;
}
.done-actions {
  display: flex;
  gap: 16rpx;
  margin-top: 20rpx;
}
.quota-hint {
  display: block;
  text-align: center;
  margin-bottom: 12rpx;
  color: $text-secondary;
  font-size: 24rpx;
}
.primary-btn.exhausted {
  background: #c5cad6;
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
  color: $text-primary;
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
  font-size: 30rpx;
  font-weight: 600;
}
</style>
