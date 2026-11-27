# 轻工具箱

微信小程序：**证件照 · 图片处理 · Office/PDF 转换与压缩** 一站式工具平台。

一期能力：智能证件照、图片五件套、Word→PDF / 图片↔PDF、Word/PDF 压图压缩、PDF 合并拆分。文件默认 **24 小时**清理，按用户日限流。

## 技术栈

| 层级 | 选型 |
|------|------|
| 小程序 | uni-app Vue3（`miniprogram/`，编译到微信） |
| 后端 | Java 17 / Spring Boot 3.3 / Security JWT / MyBatis-Plus / Flyway |
| 图片 Worker | Python FastAPI + Pillow（可选 rembg） |
| 文档 Worker | Python FastAPI + pypdf / zipfile 压图；LibreOffice 做转换 |
| 数据 | PostgreSQL + Redis |

## 目录

```
taobao-refund-assistant/          # 仓库目录名可保留
├── backend/                      # Spring Boot API
├── miniprogram/                  # uni-app 微信小程序
├── photo-worker/                 # 证件照 / 图片处理
├── doc-worker/                   # 文档转换与压缩
├── frontend/                     # 旧 Vben 后台（已非主路径，可忽略）
└── docker-compose.yml
```

## 快速启动

### 1. 基础设施

```bash
docker compose up -d postgres redis
# PostgreSQL → 本机 5433，库 light_toolbox / 用户 toolbox / 密码 toolbox123
# Redis → 6379
```

Workers 可选 Docker 或本机 Python：

```bash
# 本机
cd photo-worker && pip install -r requirements.txt && uvicorn main:app --port 8091
cd doc-worker && pip install -r requirements.txt && uvicorn main:app --port 8092
```

依赖说明：

- **证件照 / 换底**：建议 `pip install rembg onnxruntime`；未安装时会降级铺底（健康检查 `GET :8091/health` 的 `rembg` 字段）。
- **Word→PDF**：需 LibreOffice（`soffice` / `libreoffice`）；`doc-worker` Dockerfile 已含。
- **PDF→图片**：需 poppler 的 `pdftoppm`（Windows 安装后加入 PATH；Linux `poppler-utils`）。未安装会返回明确错误，不再生成占位图。

### 2. 后端

```bash
cd backend
mvn spring-boot:run
# http://127.0.0.1:8080/api
# 开发默认微信 mock 登录：POST /api/auth/wx-login
```

### 3. 小程序

```bash
cd miniprogram
npm install
npm run dev:mp-weixin
```

用微信开发者工具打开 `miniprogram/dist/dev/mp-weixin`（或 HBuilderX 直接运行到微信开发者工具）。

- 详情 → 本地设置：勾选「不校验合法域名」
- API 地址见 `miniprogram/utils/config.js`（默认 `http://127.0.0.1:8080/api`）

## 主要 API

- `POST /api/auth/wx-login` → `{ accessToken, user }`
- `GET /api/tools/catalog` → 分类 / 工具 / 证件照规格
- `GET /api/tools/recent`
- `POST /api/jobs/stage`（multipart 暂存额外文件，返回 `path`）
- `POST /api/jobs`（multipart：`toolCode` + `file` + 可选 `extraPaths` / 参数）
- `POST /api/jobs/estimate`（压缩预估）
- `GET /api/jobs` / `GET /api/jobs/{id}` / `GET /api/jobs/{id}/download`
- `GET /api/jobs/quota`

## 环境变量

| 变量 | 说明 |
|------|------|
| `SPRING_PROFILES_ACTIVE` | `dev` / `prod` |
| `DB_*` / `REDIS_*` | 数据源 |
| `APP_JWT_SECRET` | JWT 密钥（生产必改） |
| `WECHAT_MOCK` | `true` 时免真实 AppSecret |
| `WECHAT_APP_ID` / `WECHAT_APP_SECRET` | 正式小程序 |
| `PHOTO_WORKER_URL` / `DOC_WORKER_URL` | Worker 地址 |
| `QUOTA_DAILY_LIMIT` | 日限额，默认 10 |
| `UPLOAD_DIR` / `FILE_RETAIN_HOURS` | 上传目录与保留小时 |

## 文档

- [项目计划](docs/项目计划.md)
