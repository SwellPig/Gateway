# 开放银行网关（Java MVP 工程）

这是一个可直接运行的 Java 版本“开放银行风格网关”MVP 工程，包含：

- **网关服务（gateway-server）**：Spring Boot + WebFlux，提供规则管理 API、规则热生效、反向代理转发能力。
- **管理台前端**：内置静态页面（无需 npm），可视化增删改查路由规则并发布生效。

> 当前版本为 MVP，后续可对接 Apollo/Nacos、完善鉴权/限流/熔断插件化能力。

## 功能特性

- **路由转发**：按 path + method 匹配规则转发请求
- **规则热生效**：更新规则后无需重启，自动刷新内存规则
- **管理台**：规则列表、创建、删除、更新
- **可扩展设计**：以“规则快照”作为配置载体，便于接入配置中心

## 项目结构

```
.
└── gateway-server
    ├── src/main/java        # 网关服务
    ├── src/main/resources   # 管理台静态资源 + 默认规则快照
    └── pom.xml
```

## 快速开始

### 1. 启动网关服务

```bash
cd gateway-server
mvn spring-boot:run
```

默认端口：`http://localhost:8080`

### 2. 打开管理台

浏览器访问：`http://localhost:8080`

## 网关规则快照示例

规则存储在 `./data/routes.json`（启动时自动创建），示例：

```json
{
  "version": "2025-01-01T12:00:00Z",
  "routes": [
    {
      "id": "sample-account",
      "path": "/api/account/**",
      "methods": ["GET"],
      "target": "http://localhost:9001",
      "stripPrefix": 1,
      "rewrite": "/v1/account/$1"
    }
  ]
}
```

## 管理 API

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/admin/routes` | 获取规则快照 |
| POST | `/admin/routes` | 新增规则 |
| PUT | `/admin/routes/:id` | 更新规则 |
| DELETE | `/admin/routes/:id` | 删除规则 |

## 下一步计划

- 接入 **Apollo/Nacos** 配置中心并实现版本回滚
- 增加鉴权、限流、熔断插件
- 丰富管理台功能（灰度、审计、发布历史）
