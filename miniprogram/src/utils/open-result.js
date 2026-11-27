const IMAGE_EXT = ['jpg', 'jpeg', 'png', 'webp', 'gif', 'bmp']
const DOC_EXT = {
  pdf: 'pdf',
  doc: 'doc',
  docx: 'docx',
  xls: 'xls',
  xlsx: 'xlsx',
  ppt: 'ppt',
  pptx: 'pptx',
}

function extOf(name) {
  const match = String(name || '').toLowerCase().match(/\.([a-z0-9]+)$/)
  return match ? match[1] : ''
}

function withSuffix(tempPath, fileName) {
  const ext = extOf(fileName)
  if (!ext || String(tempPath).toLowerCase().endsWith('.' + ext)) return tempPath
  const dest = `${uni.env.USER_DATA_PATH}/lt-${Date.now()}.${ext}`
  try {
    uni.getFileSystemManager().copyFileSync(tempPath, dest)
    return dest
  } catch (_) {
    return tempPath
  }
}

function toast(title) {
  uni.showToast({ title, icon: 'none' })
}

/** 按真实后缀打开。图片进相册或预览，文档必须带 fileType，避免 openDocument 报 filetype not supported。 */
export function openResult(tempPath, fileName) {
  if (!tempPath) return
  const ext = extOf(fileName)
  const fileType = DOC_EXT[ext]
  if (fileType) {
    const path = withSuffix(tempPath, fileName)
    uni.openDocument({
      filePath: path,
      fileType,
      showMenu: true,
      fail: () => toast('这个文件打不开'),
    })
    return
  }
  if (IMAGE_EXT.includes(ext) || !ext) {
    uni.saveImageToPhotosAlbum({
      filePath: tempPath,
      success: () => toast('已保存到相册'),
      fail: () => {
        uni.previewImage({
          urls: [tempPath],
          fail: () => toast('这个文件打不开'),
        })
      },
    })
    return
  }
  toast('这个文件打不开')
}
