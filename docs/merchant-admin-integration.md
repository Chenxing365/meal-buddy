# 外接商户端对接规约（v1）

> 面向对象：以独立技术栈（自有服务端、自有 MySQL、自有登录与路由守卫）外接的商户端团队。
> 目标：**保留现有 UniApp 商户端不变**，让另一套商户端以最小侵入方式接入，共用同一后端与数据。
> 本文只描述契约与边界，不涉及主站代码改动；文中标注「待排期」的能力需双方确认后再实施。

---

## 1. 边界：共享什么，不共享什么

| 项目 | 是否共享 | 说明 |
|---|---|---|
| 后端服务 | 共享 | 同一 `backend` 实例，不新建第二套写入服务 |
| 数据库 | 共享 | MongoDB 是**唯一写入口** |
| 外接端 MySQL | 不共享 | 仅存本地会话、菜单配置、审计、账号映射 |
| 前端代码与构建产物 | 不共享 | 各自独立构建、独立目录部署 |
| 领域逻辑（入驻、核销、状态机） | 共享 | 必须调主站接口，不自行实现 |

一句话：**换界面可以，换数据源不行。**

---

## 2. 三个契约

### 2.1 身份契约

主站只认一个请求头：

```
Authorization: Bearer <accessToken>
```

Token 为 JWT，业务字段固定为：

| claim | 含义 | 示例 |
|---|---|---|
| `sub` | 主站用户 ID（= `users._id`） | `<24 位 ObjectId>` |
| `username` | 登录名 | `shop_owner_01` |
| `role` | 角色码 | `2` |
| `type` | `access`（2 小时）/ `refresh`（7 天） | `access` |

> 严禁自行签发 Token。伪造 `role=9` 等同获取平台管理权限，主站不为此背书，且共享签名密钥方需承担全部安全责任。

### 2.2 数据契约（只读字段）

`users`：

| 字段 | 类型 | 说明 |
|---|---|---|
| `_id` | string | 主键，外接端映射的唯一依据 |
| `username` | string | 唯一索引 |
| `role` | int | 0 用户 / 1 骑手 / 2 商家 / 9 平台管理员 |
| `status` | int | 0 禁用 / 1 正常 |

`merchants`：

| 字段 | 类型 | 说明 |
|---|---|---|
| `_id` | string | 门店主键 |
| `user_id` | string | **唯一索引**，店主账号 ID，即两端合并键 |
| `name` / `logo` / `address` | string | 门店展示信息 |
| `location` | GeoJSON Point | `[lng, lat]` |
| `category` | string | 经营分类 |
| `commission_rate` | double | 平台抽成，默认 0.05 |
| `status` | int | 0 待审核 / 1 营业 / 2 歇业 / 3 驳回 |
| `reject_reason` | string | 驳回理由 |

`orders.status`：0 待支付 / 1 已支付 / 2 配送中 / 3 已核销 / 4 已完成 / 5 已取消。
**状态只能由主站接口推进**，外接端不得直接改库。

> 现有集合无通用扩展字段。外接端的个性化数据请存**自己的 MySQL**，不要直连 Mongo 给 `merchants` 加字段。

### 2.3 接口契约

统一响应结构：

```json
{ "success": true, "code": 200, "message": "success", "data": {}, "timestamp": 1762560000000 }
```

错误码：`400` 参数错误 / `401` 未登录或凭证失效 / `403` 无权限 / `404` 资源不存在。

---

## 3. 身份接入方式

### 方案 A｜复用主站登录（现状可用，推荐起步）

外接端调用主站登录接口，将返回的 Token 存入自己的会话：

```bash
curl -X POST http://<主站地址>/api/user/login \
  -H "Content-Type: application/json" \
  -d '{"username":"shop_owner_01","password":"******"}'
```

响应含 `accessToken`、`refreshToken`、`user` 三项，`user.role` 决定该账号可见范围。

### 方案 B｜网关代签（外接端侧即可实现，不改主站）

在其网关中完成身份翻译：把自有登录态转换为携带 `sub` / `role` 的 Token，再转发至主站。

- 适用：外接端已有完整 token 与路由守卫体系，只想在边缘做一次转换。
- 前提：签名密钥由**双方运维共同保管**，仅按商家身份签发，不得签发 `role=9`。
- 现状：主站无需改动即可接受此类 Token，前提是 claim 结构一致。

### 方案 C｜令牌交换端点（待排期，当前未提供）

由主站新增 `POST /api/integration/token`，外接端服务端持集成密钥、按主站 `userId` 换取短期 Token，Token 不下发浏览器。

- 适用：不愿共享签名密钥、希望 Token 生命周期由主站掌控。
- 现状：**该端点尚未实现**，需要后端排期；实施时须满足：默认关闭、密钥走环境变量、拒绝签发管理员令牌、账号禁用即拒签。

> 三种方案都不允许把 `security.jwt.secret` 直接交给外接端。

---

## 4. 可用接口清单

| 方法 | 路径 | 鉴权 | 说明 |
|---|---|---|---|
| POST | `/api/user/login` | 公开 | 登录，返回双 Token |
| POST | `/api/user/register` | 公开 | 注册 |
| GET | `/api/user/info` | 登录 | 当前用户信息 |
| GET | `/api/merchant/list` | 公开 | 门店列表（附近 / 品类 / 分页） |
| GET | `/api/merchant/{id}` | 公开 | 门店详情 + 菜品清单 |
| GET | `/api/dish/{id}` | 公开 | 菜品详情 |
| GET | `/api/dish/merchant/{merchantId}` | 公开 | 门店菜品分页 |
| POST | `/api/merchant/apply` | 登录 | 提交入驻申请 |
| PUT | `/api/merchant/info/{id}` | 登录 + 归属校验 | 改门店信息，仅限本店 |
| GET | `/api/merchant/dashboard` | 登录 | 经营大盘 |
| GET | `/api/merchant/orders` | 登录 | 门店订单分页 |
| POST | `/api/merchant/verify` | 登录 | 核销自提 / 堂食 |
| POST / PUT / DELETE | `/api/dish/**` | 登录 | 菜品新增、修改、上下架、软删 |
| GET | `/api/order/{id}` | 登录 | 订单详情 |
| GET | `/api/order/list` | 登录 | 我的订单 |
| GET | `/api/admin/merchants` | `role=9` | 商家审核列表 |
| POST | `/api/admin/merchant/{id}/approve` | `role=9` | 审核通过（店主角色升为 2） |
| POST | `/api/admin/merchant/{id}/reject` | `role=9` | 驳回申请 |

### 鉴权现状与注意点

当前拦截器对 `GET /api/merchant/**` 采用「**黑名单**」方式放行：仅 `dashboard`、`orders`、`verify` 被判定为私有路径，其余 GET 匿名可读。

- 外接端**不要**依赖这一特性设计新接口。若需新增商家侧只读接口，应先与主站确认后再上线。
- 商家数据隔离依赖服务层的归属校验（门店 `user_id` 与当前 Token 的 `sub` 比对），不是路径白名单。外接端调用写接口时务必携带**店主本人**的 Token。

---

## 5. 外接端 MySQL 侧的边界

建议只做账号映射与本地配置：

```sql
CREATE TABLE mb_user_map (
  id           BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
  ext_user_id  VARCHAR(64) NOT NULL COMMENT '主站 users._id',
  merchant_id  VARCHAR(64) NULL     COMMENT '主站 merchants._id',
  created_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_ext_user (ext_user_id)
) COMMENT='主站账号映射';
```

四条红线：

1. 不直连 MongoDB，不共享 Mongo 账号与连接串。
2. 不把 `merchants`、`orders` 落成 MySQL 主表，不做双向同步。
3. 不回写 `role`、`status`：`role` 只在主站审核通过时升级，门店状态只走主站状态机。
4. 时间统一按 `Asia/Shanghai` 解释；跨端传 ISO-8601 或毫秒时间戳，不传裸字符串。

---

## 6. 部署与跨域

### 6.1 前端产物分流

```nginx
# 原有 UniApp 商户端保持不动
location / {
    root /usr/share/nginx/html;
    try_files $uri $uri/ /index.html;
}

# 外接端独立产物，独立回退，互不影响
location /m/ {
    alias /usr/share/nginx/html-merchant/;
    try_files $uri $uri/ /m/index.html;
}
```

### 6.2 跨域白名单

主站 Nginx 的 `$cors_origin` map 需显式追加外接端域名（当前只放行本地开发地址与占位域名）：

```nginx
~^https?://merchant-admin\.example\.com$  $http_origin;
```

若外接端由**服务端**代为调用主站，则只需放行其出口 IP，无需开启浏览器跨域。

### 6.3 容器接入

外接端服务加入主站 compose 网络后，可直接访问 `mongodb` 与 `redis` 服务名；生产环境不得将这两个端口映射到公网。

---

## 7. 合并验收清单

- [ ] 登录后 `GET /api/user/info` 返回本人信息
- [ ] `role=2` 时 `GET /api/merchant/dashboard` 返回 200
- [ ] `role=0` 调用商家接口返回 403
- [ ] `role=9` 才能调 `/api/admin/**`
- [ ] 修改非本店门店返回 403
- [ ] 未登录访问 `/api/merchant/orders` 返回 401
- [ ] 外接端 MySQL 中不存在 `merchants`、`orders` 主表
- [ ] 关闭登录态后刷新页面，路由守卫生效并回到登录页

---

## 8. 待确认事项

| 序号 | 事项 | 责任方 |
|---|---|---|
| 1 | 是否启用令牌交换端点（方案 C） | 主站后端 |
| 2 | 外接端正式域名，用于 CORS 白名单 | 外接端 |
| 3 | 商家身份由谁签发、密钥如何保管 | 双方运维 |
| 4 | 外接端是否需要新增商家侧只读接口 | 双方 |

---

## 9. 变更记录

| 版本 | 内容 |
|---|---|
| v1 | 首版：身份三方案、字段字典、接口清单、部署与验收 |
