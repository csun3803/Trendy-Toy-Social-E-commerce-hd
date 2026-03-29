

# 潮玩玩具社交电商平台 (Trendy Toy Social E-commerce Platform)

基于 Spring Boot 3 + MyBatis-Plus 开发的潮玩玩具社交电商平台后端系统。

## 项目简介

本项目是一个集社交、电商于一体的潮玩玩具销售平台，支持用户浏览商品、加入购物车、下单购买、管理收货地址等功能，同时提供商家管理和平台管理功能。

## 技术栈

### 后端框架
- **Java 21**
- **Spring Boot 3.2.2**
- **MyBatis-Plus 3.5.5**
- **Spring Security + JWT** (认证与授权)
- **SpringDoc** (API 文档)

### 数据库与缓存
- **MySQL** (主数据库)
- **Redis** (缓存)

### 其他依赖
- Lombok - 简化代码
- Spring Boot DevTools - 开发热部署
- JJWT - JWT 令牌生成与验证

## 项目结构

```
src/
├── main/
│   ├── java/com/example/trendytoysocialecommercehd/
│   │   ├── common/           # 通用类
│   │   │   └── Result.java   # 统一响应结果
│   │   ├── config/           # 配置类
│   │   │   ├── CorsConfig.java          # 跨域配置
│   │   │   ├── MyBatisPlusConfig.java   # MyBatis-Plus 配置
│   │   │   ├── SecurityConfig.java      # Spring Security 配置
│   │   │   ├── SpringDocConfig.java     # API 文档配置
│   │   │   └── WebConfig.java           # Web 配置
│   │   ├── controller/       # 控制器层
│   │   │   ├── AddressController.java   # 地址管理
│   │   │   ├── AlbumController.java     # 相册管理
│   │   │   ├── CartController.java      # 购物车
│   │   │   ├── OrderController.java     # 订单管理
│   │   │   ├── UserController.java      # 用户管理
│   │   │   ├── MerchantController.java  # 商家管理
│   │   │   └── ...
│   │   ├── dto/              # 数据传输对象
│   │   ├── entity/           # 实体类
│   │   ├── mapper/           # MyBatis-Plus Mapper
│   │   ├── service/          # 业务逻辑层
│   │   ├── util/             # 工具类
│   │   │   └── JwtUtil.java  # JWT 工具
│   │   └── TrendyToySocialECommerceHdApplication.java  # 启动类
│   └── resources/
│       ├── static/images/    # 静态资源
│       ├── application.properties  # 配置文件
│       └── ...
└── test/                     # 测试代码
```

## 快速开始

### 环境要求
- JDK 21+
- Maven 3.6+
- MySQL 8.0+
- Redis (可选)

### 安装步骤

1. **克隆项目**
   ```bash
   git clone <repository-url>
   cd Trendy-Toy-Social-E-commerce-hd
   ```

2. **配置数据库**

   创建 MySQL 数据库 `chaowan_platform`，然后修改 `src/main/resources/application.properties`：
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/chaowan_platform?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
   spring.datasource.username=your_username
   spring.datasource.password=your_password
   ```

3. **配置 JWT 密钥**

   在 `application.properties` 中修改 JWT 密钥（确保至少 256 位）：
   ```properties
   jwt.secret=your-secret-key-must-be-at-least-256-bits-long
   ```

4. **构建项目**
   ```bash
   ./mvnw clean install
   ```

5. **运行项目**
   ```bash
   ./mvnw spring-boot:run
   ```
   应用将在 `http://localhost:8080` 启动。

## API 文档

项目集成 SpringDoc，启动后访问以下地址查看 API 文档：
```
http://localhost:8080/swagger-ui.html
```

## 安全认证

项目使用 JWT 进行身份验证，需要在请求头中携带 Token：
```
Authorization: Bearer <your-jwt-token>
```

## 主要功能模块

### 用户端
- 用户注册/登录
- 商品浏览（系列、单品）
- 购物车管理
- 订单管理
- 收货地址管理
- 相册功能

### 商家端
- 商家注册/登录
- 商品管理
- 订单处理

### 平台管理
- 平台管理员登录
- 全局管理功能

##  配置说明

### 主要配置项 (`application.properties`)

```properties
# 服务器端口
server.port=8080

# 文件上传限制
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB

# 上传路径
upload.path=src/main/resources/static/images/avatar

# JWT 配置
jwt.expiration=1800000  # 30 分钟
jwt.refreshExpiration=604800000  # 7 天
```

## 测试

运行所有测试：
```bash
./mvnw test
```

