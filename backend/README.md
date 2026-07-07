# pixelisle-backend

像素屿后端服务 — Spring Boot 2.7.6

## 本地运行

### 环境要求

- JDK 8+ / Maven 3.6+ / MySQL 8.0 / Redis 7.0

### 启动

```bash
# 创建数据库
mysql -u root -p < database/schema.sql

# 修改 application.yml 中的数据库/Redis 连接

# 启动
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

API 文档：http://localhost:8123/api/doc.html

## 项目结构

```
backend/
├── database/schema.sql          # 建表脚本
├── src/main/java/.../pixelisle/ # 源码
│   ├── controller/              # REST 控制器
│   ├── service/                 # 业务逻辑
│   ├── management/              # 鉴权 / 分表 / 协同引擎
│   └── model/                   # 实体 / DTO / VO
├── src/main/resources/
│   ├── application.yml          # 主配置
│   └── biz/                     # RBAC 配置
└── pom.xml
```

详见根目录 [README.md](../README.md)。
