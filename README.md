## 测试插件热部署

### 一、启动 container
首先通过 IDEA 启动 spring-dpl-example-container 项目的 ContainerApplication 类。

### 二、请求测试接口
GET http://localhost:8080/get?pluginKey=demo-plugin

### 三、插件成功执行
接口返回：`Hello, I'm DemoMethodService, get method handle.`，表示插件被成功执行。

### 四、删除插件
删除 plugins 目录下的 spring-dpl-example-plugin-demo-1.0.0.jar。
再次请求测试接口（第二步）。此时接口返回：`plugin not found`，表示插件已经卸载了。

### 五、添加/替换插件
重新将 spring-dpl-example-plugin-demo-1.0.0.jar 放入 plugins 目录下。
再次请求测试接口（第二步）。此时接口返回：`Hello, I'm DemoMethodService, get method handle.`，表示插件被成功执行。