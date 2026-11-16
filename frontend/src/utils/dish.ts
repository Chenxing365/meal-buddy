/**
 * 菜品展示工具：首页推荐与门店详情共用同一套图标映射，避免同一道菜两处长相不一致
 */
export function getDishEmoji(name?: string): string {
  const text = name || ''
  if (text.includes('沙拉') || text.includes('轻食')) return '🥗'
  if (text.includes('鸡') || text.includes('肉')) return '🍗'
  if (text.includes('牛')) return '🥩'
  if (text.includes('虾') || text.includes('海鲜')) return '🍤'
  if (text.includes('面') || text.includes('粉')) return '🍜'
  if (text.includes('堡')) return '🍔'
  if (text.includes('饭') || text.includes('套餐')) return '🍱'
  return '🍱'
}
