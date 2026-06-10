# PetMeet - 宠遇 (前端端项目)

PetMeet 是一个基于 Vue 3 + Vite 构建的宠物社交与电商平台前端应用。

## 🛠 技术栈

- **框架**: Vue 3 (Composition API + Script Setup)
- **构建工具**: Vite 5.x
- **UI 组件库**: Element Plus
- **路由**: Vue Router 4
- **状态管理**: Pinia
- **工具库**: Axios (网络请求), Day.js (时间格式化), Sass (预处理器)

## 🚀 快速开始

### 1. 安装依赖

确保你的环境已安装 Node.js (推荐 v18+)。

```bash
npm install
```

### 2. 启动开发服务器

```bash
npm run dev
```

启动后，访问 `http://localhost:5173` 即可预览项目。

### 3. 构建生产版本

```bash
npm run build
```

## 🔌 后端接口配置

项目默认配置了 API 代理，指向本地后端的 `8080` 端口。

**`vite.config.js` 配置说明**:
- `/api` -> `http://localhost:8080` (会自动去除 `/api` 前缀)
- `/files` -> `http://localhost:8080` (用于访问静态资源/上传的文件)

**注意**: 如果没有运行后端服务，除了页面 UI 结构外，登录、数据列表等涉及 API 的功能将无法正常通过网络请求获取数据（会报 Network Error 或 404）。

## 📂 项目结构

```
src/
├── layout/
│   └── BasicLayout.vue    # 通用布局 (Header + RouterView)
├── router/
│   └── index.js           # 路由配置 (含权限拦截守卫)
├── store/
│   └── user.js            # 用户状态管理 (Pinia)
├── utils/
│   ├── request.js         # Axios 封装 (拦截器、Token处理)
│   └── format.js          # 工具函数
├── views/
│   ├── Login.vue          # 登录/注册页
│   ├── Home.vue           # 首页 (瀑布流笔记)
│   ├── NoteDetail.vue     # 笔记详情 (图文、商品、评论)
│   ├── Publish.vue        # 发布笔记 (图片上传、关联商品)
│   ├── Cart.vue           # 购物车 (结算流程)
│   └── Profile.vue        # 个人中心 (订单、地址、我的笔记)
└── main.js                # 入口文件
```

## ✨ 核心功能

1.  **用户认证**: 登录/注册/退出，基于 Token 的路由拦截。
2.  **内容社区**: 
    - 首页瀑布流展示笔记。
    - 笔记详情页支持轮播图、富文本展示及评论互动。
    - 发布笔记支持图片上传和商品关联。
3.  **电商闭环**:
    - 笔记内直接将关联商品加入购物车。
    - 购物车支持数量管理与移除。
    -完整的订单结算流程（地址选择 -> 支付 -> 订单列表）。

## 🤝 贡献
仅供学习参考。
