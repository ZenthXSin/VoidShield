# Mindustry Mod Java/Kotlin Loading

## 目标
本页整理 Java/Kotlin Mod 的加载链，重点是：
- 类加载器如何组装
- 主类如何实例化
- `Mod` / `Plugin` 如何识别
- 生命周期回调何时触发

## 1. ClassLoader 结构
`ModClassLoader` 是一个“主加载器 + 子加载器链”的聚合器：
- 继承 `ClassLoader`
- 持有多个 child loader
- `findClass()` 会顺序向 child 询问
- 使用 `ThreadLocal` 防止 child 再反向委托导致循环

这意味着：
- Mod 类可以从各自独立 jar 中加载
- 主加载器统一对外暴露
- 子加载失败时继续尝试下一个 child

## 2. Mod 类型
源码中公开了两个核心类型：
- `Mod`：普通 Mod 基类
- `Plugin`：隐藏型 Mod 子类

`Plugin` 本身不增加新逻辑，只是语义层面的特殊 Mod。

## 3. 入口模型
`Mods.load()` 先完成：
1. 候选文件扫描
2. 元数据读取
3. 依赖排序
4. `loadMod(file, false, enabled)` 实例化
5. 收集到 `mods`

Java/Kotlin Mod 的真正实例化逻辑在 `loadMod()` 内部完成，随后再进入后续阶段。

## 4. 生命周期顺序
从公开 API 与注释可确认：
- `loadContent()`：内容注册阶段
- `registerServerCommands()`：服务端命令注册
- `registerClientCommands()`：客户端命令注册
- `init()`：所有插件创建并完成命令注册后调用

因此生命周期可理解为：
- 构造/装载
- 内容注册
- 命令注册
- init 收尾

## 5. Config 绑定
`Mod` 的配置访问统一走 `Vars.mods`：
- `getConfigFolder()`
- `getConfig()`

它们依赖“当前 Mod 已加载”，否则会抛出非法状态。

## 6. Load State
加载后，Mod 会处于不同状态：
- enabled
- disabled
- unsupported

这些状态由依赖与兼容性共同决定，随后参与排序与运行。

## 7. Error Surface
典型加载错误包括：
- 主类不存在
- 旧版 `mindustry.plugin.Plugin` 继承链
- 依赖缺失
- 类加载异常

旧插件会收到明确迁移提示：
- 需要改为继承 `mindustry.mod.Plugin`

## Related
- [Mindustry Mod System Complete Outline](mod-system-complete-outline.md)
- [Mindustry Mod Loading Callchain](mod-loading-callchain.md)
- [Mindustry Mod Lifecycle](mod-lifecycle.md)
