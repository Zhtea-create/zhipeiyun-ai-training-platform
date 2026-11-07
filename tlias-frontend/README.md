# 智培云前端

Vue 3 + Vite + Element Plus + Pinia + ECharts + markdown-it 构建的培训管理后台。

## 开发

```bash
npm install
npm run dev
```

访问 `http://localhost:5173`，开发服务器会把 `/api` 代理到 `http://localhost:8080`。

## 构建

```bash
npm run build
```

构建产物直接输出到 `../nginx/html/`，随后通过 nginx 访问 `http://localhost:90`。

完整的项目说明、后端启动方式、AI Key 配置请见仓库根目录的 `README.md`。
