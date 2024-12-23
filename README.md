# What is spring-dpl?
DPL，全称：Dynamic Plug-In Loader。这是一款基于Spring环境构建的简易版动态插件加载开发套件。 它的主要作用是支持在Spring环境中不重启主系统的情况下实现插件的热部署。

## 说明
- 插件：Plug-In，是一种基于某些约定接口或规范来实现的程序，它必须依赖主系统运行。
- 热部署：在不重启主系统的前提下，可以更新插件。

:exclamation: 仅支持插件中如下注解声明的类才会装载到SpringContext（`需要更多支持请自行扩展`）：
```
org.springframework.stereotype.Component.class
org.springframework.stereotype.Repository.class
org.springframework.stereotype.Service.class
org.springframework.stereotype.Controller.class
```

## 工作流程
1. 主系统定时（默认1分钟）扫描一次插件目录中（../plugins/）插件文件（xxx-plugin.jar）的变化；
2. 扫描发生变化的插件里的注解类，这些类通过 AnnotationConfigApplicationContext 加载到Spring中；
3. 加载完成后将插件注册到 PluginBus 容器里；
4. 主系统可以通过 PluginBus 容器查找到插件，调用插件里的类和方法。

# 快速开始
## 导入依赖
### Gradle
```angular2html
implementation 'io.github.big-mouth-cn:spring-boot-starter-dpl:1.0.0'
```

### Maven
```angular2html
<dependency>
    <groupId>io.github.big-mouth-cn</groupId>
    <artifactId>spring-boot-starter-dpl</artifactId>
    <version>1.0.0</version>
</dependency>
```
> 如果不在SpringBoot环境里使用，请修改成 spring-dpl 的依赖。

## 调用插件

```java
@Autowired
private PluginBus pluginBus;

Plugin plugin = pluginBus.lookup("demo-plugin");
MethodService methodService = plugin.getService(MethodService.class);
Object res = methodService.getMethodHandle();
```

# IDEA 示例

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