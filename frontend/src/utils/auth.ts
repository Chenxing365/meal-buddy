/**
 * src/utils/auth.ts
 *
 * 演示环境自动登录：打开页面即进入登录态（免输密码）。
 *
 * ⚠️ 演示专用：账号口令会随前端产物下发，任何人可在 JS 中找到。
 *    正式环境请删除 App.vue 中的 onLaunch 调用与 DEMO_ACCOUNT 常量。
 */
import { login, getUserInfo } from '@/api/user'
import { getToken, clearToken } from '@/utils/request'

/** 演示账号（普通用户），改为 admin / demo_merchant 可切换演示角色 */
const DEMO_ACCOUNT = {
  username: 'demo_buyer',
  password: 'Demo@123456'
}

/**
 * 确保处于登录态：
 * 1. 已有 token 时先校验有效性（调 /api/user/info），失效则清除并重新登录；
 * 2. 无 token 时用演示账号登录一次并写入 storage；
 * 3. 任何失败都静默处理，不阻塞页面。
 *
 * 注意：token 有效期 2 小时，隔天打开页面必须走「校验失败 → 重登」分支，
 * 否则会带着过期 token 请求，页面永远停在未登录状态。
 */
export async function autoLogin(): Promise<void> {
  if (getToken()) {
    try {
      const res = await getUserInfo()
      if (res.data) return
    } catch {
      // 忽略，走重新登录
    }
    clearToken()
  }

  try {
    await login(DEMO_ACCOUNT)
  } catch (e) {
    console.warn('[auth] 自动登录失败:', (e as Error)?.message)
  }
}
