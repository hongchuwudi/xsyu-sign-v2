import { reactive } from 'vue'

// 全局消息提示（配合 ToastHost 组件使用）
export const toastState = reactive({ list: [] })

let seq = 0

export function showMessage(msg, type = 'success') {
  const id = ++seq
  toastState.list.push({ id, msg, type })
  setTimeout(() => {
    const i = toastState.list.findIndex(t => t.id === id)
    if (i > -1) toastState.list.splice(i, 1)
  }, 3000)
}
