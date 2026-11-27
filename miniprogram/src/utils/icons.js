const MAP = {
  idphoto_smart: '/static/icons/idphoto.png',
  idphoto_1inch: '/static/icons/idphoto.png',
  idphoto_2inch: '/static/icons/idphoto.png',
  idphoto_exam: '/static/icons/idphoto.png',
  idphoto_passport: '/static/icons/idphoto.png',
  idphoto_custom: '/static/icons/idphoto.png',
  word_to_pdf: '/static/icons/word.png',
  pdf_to_word: '/static/icons/pdf.png',
  image_to_pdf: '/static/icons/img2pdf.png',
  pdf_to_image: '/static/icons/pdf.png',
  excel_to_pdf: '/static/icons/excel.png',
  ppt_to_pdf: '/static/icons/ppt.png',
  image_compress: '/static/icons/compress.png',
  image_crop: '/static/icons/crop.png',
  image_resize: '/static/icons/resize.png',
  image_format: '/static/icons/format.png',
  image_bg: '/static/icons/bg.png',
  pdf_compress: '/static/icons/pdf_c.png',
  word_compress: '/static/icons/word_c.png',
  pdf_merge: '/static/icons/merge.png',
  pdf_split: '/static/icons/split.png',
}

export function toolIcon(code) {
  if (MAP[code]) return MAP[code]
  if (code && code.startsWith('idphoto')) return '/static/icons/idphoto.png'
  return '/static/icons/word.png'
}
