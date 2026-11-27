import { ref } from 'vue'

export const sheetShow = ref(false)
export const sheetTitle = ref('')
export const sheetContent = ref('')

export function openSheet(title, content) {
  sheetTitle.value = title
  sheetContent.value = content
  sheetShow.value = true
}

export function closeSheet() {
  sheetShow.value = false
}
