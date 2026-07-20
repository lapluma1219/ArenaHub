# ArenaHub

ArenaHub 是一个基于 Spring Boot 的轻量级游戏匹配服务，提供用户认证、玩家匹配、房间管理、实时消息广播、对局结算和积分排行榜。

项目内置双玩家 Web 演示页，启动单个服务即可体验完整对局流程。

## 功能

- 用户注册、登录和 JWT 鉴权
- 玩家资料与胜负场次统计
- 双人匹配队列与房间创建
- 房间权限校验和 WebSocket 消息广播
- 对局结算与并发结算保护
- 积分更新、排行榜和对局记录
- Redis 排行榜缓存与数据库回退
- H2 文件数据库持久化
- Docker Compose 部署

## 技术栈

| 组件 | 技术 |
| --- | --- |
| 运行时 | Java 17 |
| Web 框架 | Spring Boot 4.1、Spring Web MVC |
| 数据访问 | Spring Data JPA、H2 |
| 缓存 | Spring Data Redis、Redis |
| 实时通信 | Spring WebSocket |
| 前端 | HTML、CSS、JavaScript |
| 构建 | Maven Wrapper |
| 部署 | Docker、Docker Compose |

## 快速开始

### 环境要求

- JDK 17 或更高版本
- Redis（可选）
- Docker 和 Docker Compose（可选）

### 本地运行

运行测试：

```bash
./mvnw clean test
```

启动服务：

```bash
./mvnw spring-boot:run
```

服务默认监听 `8080` 端口：

| 地址 | 用途 |
| --- | --- |
| `http://localhost:8080/` | 双玩家演示页 |
| `http://localhost:8080/api/ping` | 健康检查 |
| `http://localhost:8080/api/docs` | API 清单 |
| `http://localhost:8080/h2-console` | H2 控制台 |

健康检查：

```bash
curl http://localhost:8080/api/ping
```

Redis 不可用时，排行榜会自动回退到数据库查询。

### Web 演示

打开 `http://localhost:8080/`：

1. 分别注册或登录玩家 A 和玩家 B。
2. 任意玩家先进入匹配队列，另一名玩家加入后自动创建房间。
3. 任意一方点击“我获胜”完成结算。
4. 双方页面自动同步胜负、积分和排行榜。

### H2 控制台

```text
JDBC URL: jdbc:h2:file:./data/arenahub
Username: sa
Password: 留空
```

## Docker

构建并启动应用和 Redis：

```bash
docker compose up --build
```

停止服务：

```bash
docker compose down
```

删除服务及 H2 数据卷：

```bash
docker compose down -v
```

## 配置

默认配置位于 `src/main/resources/application.properties`。

| 环境变量 | 默认值 | 说明 |
| --- | --- | --- |
| `SERVER_PORT` | `8080` | HTTP 端口 |
| `SPRING_DATASOURCE_URL` | `jdbc:h2:file:./data/arenahub...` | 数据库连接地址 |
| `SPRING_DATA_REDIS_HOST` | `localhost` | Redis 主机 |
| `SPRING_DATA_REDIS_PORT` | `6379` | Redis 端口 |
| `ARENAHUB_JWT_SECRET` | 内置开发密钥 | JWT 签名密钥 |
| `ARENAHUB_JWT_EXPIRE_SECONDS` | `86400` | Token 有效期 |
| `ARENAHUB_CACHE_LEADERBOARD_TTL_SECONDS` | `60` | 排行榜缓存时间 |

公开部署前必须覆盖 JWT 密钥：

```bash
ARENAHUB_JWT_SECRET=replace-with-a-strong-secret ./mvnw spring-boot:run
```

## API

除注册、登录、健康检查和接口清单外，其他 `/api/**` 请求需要携带：

```text
Authorization: Bearer <token>
```

| 方法 | 路径 | 说明 | 鉴权 |
| --- | --- | --- | --- |
| `GET` | `/api/ping` | 健康检查 | 否 |
| `GET` | `/api/docs` | API 清单 | 否 |
| `POST` | `/api/auth/register` | 注册 | 否 |
| `POST` | `/api/auth/login` | 登录 | 否 |
| `GET` | `/api/players/me` | 当前玩家资料 | 是 |
| `POST` | `/api/matchmaking/join` | 加入匹配 | 是 |
| `GET` | `/api/matchmaking/status` | 查询匹配状态 | 是 |
| `POST` | `/api/matchmaking/leave` | 退出匹配 | 是 |
| `GET` | `/api/rooms/{roomId}` | 房间详情 | 是 |
| `POST` | `/api/rooms/{roomId}/finish` | 结算对局 | 是 |
| `GET` | `/api/leaderboard?limit=10` | 排行榜 | 是 |
| `GET` | `/api/matches/me` | 当前玩家对局记录 | 是 |

注册示例：

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"username":"alice","password":"password1","nickname":"Alice"}'
```

## WebSocket

房间连接地址：

```text
ws://localhost:8080/ws/rooms/{roomId}?token={jwt}
```

发送消息示例：

```json
{"action":"MOVE","x":10,"y":20}
```

房间内所有连接会收到广播：

```json
{"type":"ACTION","playerId":1,"payload":"{\"action\":\"MOVE\",\"x\":10,\"y\":20}"}
```

## 项目结构

```text
src
├── main
│   ├── java/com/arenahub
│   │   ├── config       # MVC 与 WebSocket 配置
│   │   ├── controller   # REST API
│   │   ├── dto          # 请求与响应模型
│   │   ├── entity       # JPA 实体
│   │   ├── exception    # 异常处理
│   │   ├── repository   # 数据访问
│   │   ├── security     # JWT 与请求鉴权
│   │   ├── service      # 核心业务逻辑
│   │   └── websocket    # 房间消息处理
│   └── resources
│       ├── static       # Web 演示页
│       └── application.properties
└── test                 # 集成测试
```

## 核心设计

- 匹配队列使用进程内并发队列，适用于单实例运行。
- 房间结算使用数据库悲观锁，避免重复结算。
- 胜者增加 `25` 分，败者扣除 `15` 分，积分最低为 `0`。
- 排行榜优先读取 Redis，缓存缺失或 Redis 不可用时查询数据库。
- JWT 和密码哈希使用轻量实现，公开部署时应替换默认密钥并采用生产级安全方案。

## 测试

```bash
./mvnw clean test
```

测试覆盖 Spring 上下文加载，以及注册、匹配、房间结算、重复结算保护、排行榜和对局记录等核心流程。

## 贡献

欢迎通过 Issue 报告问题或提交 Pull Request。提交代码前请确保测试全部通过。
