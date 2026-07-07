# pixelisle-frontend

像素屿前端应用 — Vue 3 + Vite + Ant Design Vue

## 本地运行

```bash
npm install
npm run dev
# 开发服务器默认代理 API 到 localhost:8123
```

### 生产构建

```bash
npm run build
```

## 项目结构

```
frontend/
├── src/
│   ├── pages/       # 页面组件
│   ├── components/  # 通用组件
│   ├── api/         # API 调用
│   ├── stores/      # Pinia 状态管理
│   └── router/      # 路由配置
├── package.json
└── vite.config.ts
```

详见根目录 [README.md](../README.md)。
