// 获取签到状态描述
function getSignStatusDesc(sign) {
    if (sign.signStatus === 2 && !sign.date) {
        const currentTime = new Date().getTime();
        if (sign.start && sign.end) {
            if (currentTime < sign.start) return '未开始';
            if (currentTime > sign.end) return '已过期';
            return '待签到';
        }
        return '待签到';
    }
    if (sign.date) return '已签到';
    if (sign.signStatus === 1) return '未开始';
    if (sign.signStatus === 3) return '已结束';
    return '未知';
}
