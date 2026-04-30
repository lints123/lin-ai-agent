# Lin AI Agent - 项目规范

## 项目信息
- Java 21 + Maven + Spring Boot 3.4.4
- AI 框架：Spring AI Alibaba 1.1.2.2（通义千问/DashScope）
- 数据库：MySQL + Spring Data JPA
- 工具库：Hutool 5.8.34、Lombok
- API 文档：Knife4j 4.5.0（OpenAPI 3 + Jakarta）

## 编码规范

### 接口字段说明
所有 DTO、VO、Entity 类的字段**必须**使用 `@Schema` 注解标注说明：
```java
@Schema(description = "字段说明", example = "示例值", requiredMode = Schema.RequiredMode.REQUIRED)
private String fieldName;
```
- 类级别加 `@Schema(description = "xxx")`
- 每个字段加 `@Schema(description = "...")` 和 `example`
- 必填字段加 `requiredMode = Schema.RequiredMode.REQUIRED`

### 接口定义
- Controller 方法加 `@Operation(summary = "...", description = "...")`
- Controller 类加 `@Tag(name = "xxx")`
- 统一使用 `Result<T>` 封装响应

### 项目结构
- `common/` - 通用类（Result 等）
- `config/` - 配置类
- `controller/` - REST 控制器
- `service/` - 服务接口
- `service/impl/` - 服务实现
- `model/dto/` - 请求对象
- `model/vo/` - 响应对象
- `model/entity/` - JPA 实体

## Git 提交规范
- 提交注释**必须使用中文**
- 格式：`<类型>: <中文描述>`
- 类型：feat(新功能) / fix(修复) / chore(构建/配置) / docs(文档) / refactor(重构)
- 示例：`feat: 新增 ResearchTool 组合调研工具`
