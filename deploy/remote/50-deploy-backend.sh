#!/bin/bash
# 50-deploy-backend.sh — 部署 jar 并以 systemd 双实例运行
set -euo pipefail
export LC_ALL=C

ROOT=/opt/meal-buddy
APP=$ROOT/app
cd "$ROOT"
set -a; . ./.env; set +a

step() { echo -e "\n===== $* ====="; }

step "1. 运行用户与目录权限"
if ! id meal >/dev/null 2>&1; then
  useradd -r -s /sbin/nologin -d "$APP" meal
  echo "已创建用户 meal"
fi
mkdir -p "$APP" "$ROOT/logs"
chown -R meal:meal "$APP" "$ROOT/logs"
chmod 755 "$ROOT"

step "2. 校验 jar 内激活的 profile"
unzip -p "$ROOT/src/target/distributed-base-1.0.0-SNAPSHOT.jar" BOOT-INF/classes/application.yml \
  | grep -A2 'profiles:'

step "3. 安装 jar"
install -m 640 -o meal -g meal "$ROOT/src/target/distributed-base-1.0.0-SNAPSHOT.jar" "$APP/distributed-base.jar"
ls -lh "$APP/distributed-base.jar"

step "4. 生成环境变量文件"
if [ ! -f "$APP/meal-buddy.env" ]; then
  cat > "$APP/meal-buddy.env" <<EOF
# 由部署脚本生成，勿提交版本库
MONGO_HOST=127.0.0.1
MONGO_PORT=27017
MONGO_USER=$MONGO_APP_USER
MONGO_PASSWORD=$MONGO_APP_PASSWORD
MONGO_AUTH_DB=distributed_base
REDIS_HOST=127.0.0.1
REDIS_PORT=6379
REDIS_PASSWORD=
JWT_SECRET=$(openssl rand -hex 32)
AES_KEY=$(openssl rand -hex 8)
LOG_DIR=/opt/meal-buddy/logs
# AI_API_KEY 由运维手动注入，勿写入本模板、勿提交版本库
EOF
  chown root:meal "$APP/meal-buddy.env"
  chmod 640 "$APP/meal-buddy.env"
  echo "已生成 meal-buddy.env"
else
  echo "meal-buddy.env 已存在，保留"
fi
sed -e 's/^MONGO_PASSWORD=.*/MONGO_PASSWORD=***/' \
    -e 's/^JWT_SECRET=.*/JWT_SECRET=***/' \
    -e 's/^AES_KEY=.*/AES_KEY=***/' \
    -e 's/^AI_API_KEY=.*/AI_API_KEY=***/' \
    "$APP/meal-buddy.env" | head -12

step "5. 安装 systemd 双实例单元"
sed -i 's/\r$//' /tmp/meal-buddy.service
for port in 8080 8081; do
  sed "s/__PORT__/$port/g" /tmp/meal-buddy.service > "/etc/systemd/system/meal-buddy-$port.service"
  chmod 644 "/etc/systemd/system/meal-buddy-$port.service"
  # 单元名 meal-buddy-8080 不是模板单元，%i 会展开为空导致两个实例抢同一端口
  if grep -qE '__PORT__|%i' "/etc/systemd/system/meal-buddy-$port.service"; then
    echo "ERROR: meal-buddy-$port.service 仍含未替换的端口占位符（应为 __PORT__）"
    exit 1
  fi
done
rm -f /etc/systemd/system/meal-buddy@.service
systemctl daemon-reload
systemctl reset-failed meal-buddy-8080 meal-buddy-8081 2>/dev/null || true

step "6. 启动双实例（已在运行也必须 restart，否则仍加载旧 jar）"
systemctl enable meal-buddy-8080 meal-buddy-8081
systemctl restart meal-buddy-8080 meal-buddy-8081
sleep 40
systemctl --no-pager --lines=0 status meal-buddy-8080 meal-buddy-8081 | sed -n '1,40p'

step "7. 端口监听"
ss -lntp | grep -E ':(8080|8081)' || echo "端口未监听"

step "8. 启动日志（8080）"
journalctl -u meal-buddy-8080 -n 30 --no-pager || true
