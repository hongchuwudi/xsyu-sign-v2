// 签到状态判定与时间格式化（列表卡片/详情弹窗/筛选共用）

export function getSignStatusDesc(sign) {
  // 有签到时间 = 真正签到成功
  if (sign.date) return '已签到'

  // signStatus=2 但无 date：签到周期已结束但未成功签到
  if (sign.signStatus === 2) {
    const now = new Date().getTime()
    if (sign.start && sign.end) {
      if (now < sign.start) return '未开始'
      if (now > sign.end) return '已过期'
    }
    return '已过期'
  }
  // signStatus=1: 未签到
  if (sign.signStatus === 1) {
    const now = new Date().getTime()
    if (sign.start && sign.end) {
      if (now < sign.start) return '未开始'
      if (now > sign.end) return '已过期'
    }
    return '待签到'
  }
  if (sign.signStatus === 3) return '已结束'
  return '未知'
}

export function getSignStatusClass(sign) {
  const classMap = {
    '待签到': 'bg-pink-100 text-pink-700',
    '已签到': 'bg-rose-100 text-rose-700',
    '未开始': 'bg-pink-50 text-pink-500',
    '已过期': 'bg-fuchsia-100 text-fuchsia-700',
    '已结束': 'bg-pink-50 text-pink-500'
  }
  return classMap[getSignStatusDesc(sign)] || 'bg-pink-50 text-pink-500'
}

export function isValidSign(sign) {
  // signStatus: 1=未签到(待签到), 2=已签到；只有未签到且未过期才能签到
  if (sign.signStatus !== 1 || sign.date) return false
  const now = new Date().getTime()
  if (sign.start && sign.end) {
    return now >= sign.start && now <= sign.end
  }
  return true
}

export function pendingCount(signs) {
  const now = new Date().getTime()
  return signs.filter(sign => {
    if (sign.signStatus !== 2 || sign.date) return false
    if (sign.start && sign.end) return now >= sign.start && now <= sign.end
    return false
  }).length
}

export function formatDateTime(timestamp) {
  if (!timestamp) return ''
  const date = new Date(timestamp)
  return date.toLocaleString('zh-CN', {
    month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit'
  })
}

export function formatTime(timestamp) {
  if (!timestamp) return ''
  const date = new Date(timestamp)
  return `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

export function formatDate(timestamp) {
  if (!timestamp) return ''
  const date = new Date(timestamp)
  return `${String(date.getMonth() + 1).padStart(2, '0')}月${String(date.getDate()).padStart(2, '0')}日`
}
