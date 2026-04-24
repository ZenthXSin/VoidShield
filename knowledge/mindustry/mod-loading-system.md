# Mindustry Mod Loading System

## Overview
Mindustry 的 Mod 加载系统不是单纯“读取一个脚本/一个 JAR”，而是一个贯穿**发现→解析→依赖排序→类加载/脚本初始化→内容注册→资源打包→运行时钩子**的完整流水线。其核心目标是：让 Mod 安全地并入游戏内容系统，同时尽量隔离单个 Mod 的错误。

## Core Runtime Entrypoints
源码中最关键的入口类是：
- [Mods](mod-loading-system.md#mods-class) — 总调度器
- [Mod](mod-loading-system.md#mod-base-class) — Java/Kotlin Mod 基类
- [Plugin](mod-loading-system.md#plugin-class) — 隐藏型 Mod/插件基类
- [Scripts](mod-loading-system.md#scripts-class) — JS Mod 运行环境
- [ContentParser](mod-loading-system.md#contentparser-class) — 内容 JSON/HJSON 解析器

## Loading Pipeline
### 1. Discovery
`Mods.load()` 扫描候选 Mod：
- 本地 `modDirectory`
- jar/zip 文件
- 包含 `mod.json` / `mod.hjson` / `plugin.json` / `plugin.hjson` 的目录
- 平台提供的 Workshop 内容

源码要点：
- `metaFiles = {"mod.json", "mod.hjson", "plugin.json", "plugin.hjson"}`
- 候选文件会过滤扩展名与目录元数据存在性

### 2. Metadata Parse
通过 `findMeta(...)` 读取 `ModMeta`，用于提取：
- `name`
- `internalName`
- `author`
- `version`
- `main` 主入口
- `dependencies`
- `minGameVersion`
- `texturcale/pregenerated` 等资源行为参数

### 3. Dependency Resolution
`resolveDependencies(metas)` 决定加载顺序与启用状态。流程中会：
- 解析 Mod 间依赖
- 标记 enabled/disabled/unsupported
- 处理缺失依赖或版本不兼容

### 4. Mod Instantiation
`loadMod(file, ...)` 创建 `LoadedMod`：
- JAR/Kotlin/Java Mod 走类加载器
- JS Mod 走 `Scripts`
- 插件类通常继承 `Plugin`

### 5. Content Registration
`Mod.loadContent()` 进入内容注册阶段，常用于：
- 方块
- 单位
- 液体
- 弹药
- 状态效果
- 科技树

注意：内容注册必须在合适阶段进行，否则会出现引用未就绪、ID 错乱或 atlas 不完整。

### 6. Resource Packing
`Mods.loadAsync()` 会重打包 sprite：
- 扫描 `sprites/` 与 `sprites-override/`
- 并行读取 PNG
- `Pixmap` 预处理（如 bleed）
- `MultiPacker` 按页打包进 atlas
- 触发 `AtlasPackEvent`

### 7. Icon Loading
`loadSync()` 调用 `loadIcons()`：
- 读取 `icon.png`
- 作为 Mod 图标加载到 `LoadedMod.iconTexture`

### 8. Runtime Activation
Mod 被启用后参与：
- 事件系统
- 命令系统
- UI 扩展
- 网络/服务端逻辑
- 内容生成与图标生成

---

## Mods Class
`mindustry.mod.Mods` 是总调度器，负责：
- 扫描、加载、卸载 Mod
- 管理 `LoadedMod`
- 处理依赖与启用状态
- 管理脚本引擎
- 资源重打包
- 配置目录
- mod 导入/删除

### Important fields
源码中可见的重要字段：
- `mainLoader`：主 Mod 类加载器
- `mods`：当前已加载 Mod 列表
- `newImports`：新导入 Mod 记录
- `scripts`：JS 脚本引擎
- `parser`：内容解析器
- `requiresReload`：是否需要重启/重载

### Key methods
- `load()`：发现并解析 Mod
- `loadAsync()`：异步打包 sprite
- `loadSync()`：加载 icon
- `importMod(Fi file)`：导入外部 Mod
- `removeMod(LoadedMod mod)`：删除 Mod
- `getConfigFolder(Mod mod)` / `getConfig(Mod mod)`：Mod 配置目录
- `getScripts()`：获取脚本引擎
- `skipModLoading()`：根据崩溃保护决定是否跳过加载

---

## Mod Base Class
`mindustry.mod.Mod` 定义了最基础的 Mod 生命周期：

```java
public abstract class Mod{
    public void init(){}
    public void loadContent(){}
    public void registerServerCommands(CommandHandler handler){}
    public void registerClientCommands(CommandHandler handler){}
}
```

### 生命周期语义
- `init()`：内容注册之后、命令注册前后依实现而定，用于初始化逻辑
- `loadContent()`：注册内容对象
- `registerServerCommands()`：服务端命令
- `registerClientCommands()`：客户端命令

### Config Access
- `getConfigFolder()` → `Vars.mods.getConfigFolder(this)`
- `getConfig()` → `Vars.mods.getConfig(this)`

---

## Plugin Class
`mindustry.mod.Plugin` 只是 `Mod` 的特殊子类：
- 默认隐藏
- 仍然走 Mod 体系
- 常用于插件式功能注入

源码非常短：
```java
public abstract class Plugin extends Mod{}
```

---

## Scripts Class
`mindustry.mod.Scripts` 是 JS Mod 的运行时：
- 使用 Rhino `Context`
- 通过 `RequireBuilder` 构建模块系统
- 载入 `scripts/global.js`
- 提供 `run(LoadedMod mod, Fi file)` 执行单个脚本
- 支持模块引用与跨 Mod 脚本加载

### Script execution flow
1. 初始化 Rhino context
2. 安装 sandbox require
3. 载入 global.js
4. 为每个 Mod 执行主脚本/子脚本
5. 注入 `modName`、`scriptName`

### Notable behavior
- `currentMod` 决定当前脚本上下文
- `ScriptModuleProvider` 允许按 `modname/path` 解析依赖脚本
- `run()` 默认包裹为 strict mode IIFE

---

## ContentParser Class
`mindustry.mod.ContentParser` 负责把文本内容配置转换成真实对象。它是 Mod 内容系统的“反序列化核心”。

### Typical responsibilities
- 解析 block / item / unit / liquid / planet 定义
- 解析 `Effect`、`BulletType`、`DrawBlock`、`StatusEffect`、`AmmoType` 等复杂字段
- 通过 `Vars.tree` 读取 schematics
- 通过 `Core.atlas` 读取纹理

### Important design point
它使用大量 `classParsers` 来处理特殊字段类型，这意味着：
- HJSON 不是纯数据，很多字段会被动态映射成对象
- 内容定义可很简洁，但背后有复杂的类型转换

---

## Resource Packing Details
`Mods.loadAsync()` 处理资源时有几个关键点：
- `sprites/` 与 `sprites-override/` 都会扫描 PNG
- `textureScale` 会影响最终 atlas 中 region 的 scale
- `bleed` 仅在线性过滤开启时启用
- `PageType` 决定进入主图集、UI 图集、环境图集或 rubble 图集
- 在主线程替换 `Core.atlas`，确保后续内容使用新 atlas

### Why this matters
Mod 的图标、块贴图、UI 元素都依赖 atlas。若资源打包时机不对，内容就会出现：
- 找不到贴图
- atlas 旧数据残留
- icon 与实际 region 不一致

---

## Dependency and State Resolution
加载完成后还会处理：
- `updateDependencies(mod)`
- `shouldBeEnabled()`
- `isSupported()`
- `state = enabled/disabled/unsupported`

这一步决定一个 Mod 最终在 UI 中的状态，而不只是“是否被发现”。

---

## Error Handling Strategy
源码中明显体现了“尽量不中断整体启动”的策略：
- 单个 Mod 加载失败会被捕获并记录
- Workshop 文件和本地文件分别给出错误日志
- JS 运行失败会保存错误信息并返回
- 内容解析中允许忽略未知字段（`ignoreUnknownFields = true`）

---

## Common Pitfalls
- **主类继承错误**：旧版 `mindustry.plugin.Plugin` 已过时，应继承 `mindustry.mod.Plugin`
- **内容注册过晚**：导致 `Core.atlas` 或内容引用未准备好
- **脚本模块路径错误**：JS require 依赖解析失败
- **贴图目录命名不规范**：导致打包到错误 page
- **依赖循环**：导致加载排序失败
- **忘记设置 mod metadata**：Mod 不会被识别

---

## Source-Level Call Chain Summary
一个典型 Java/Kotlin Mod 的核心链路可以概括为：
1. `Mods.load()` 扫描候选文件
2. `findMeta()` 解析元数据
3. `resolveDependencies()` 排序与状态判定
4. `loadMod()` 构建 `LoadedMod`
5. `Mod.loadContent()` 注册内容
6. `Mods.loadAsync()` 打包资源
7. `Mods.loadSync()` 加载 icon
8. `Mod.init()` / 事件 / 命令 / 运行时逻辑生效

JS Mod 则在 `Scripts` 中走：
1. 初始化 Rhino
2. 加载 `global.js`
3. 执行 Mod 脚本
4. 通过 module provider 加载子脚本

---

## Suggested Next Pages
要把这个系统补成真正“完整深入”的知识库，下一步建议拆成这些页：
- `mod-meta-format.md`：mod.json/hjson 全字段
- `mod-loading-callchain.md`：源码调用链细化
- `mod-lifecycle.md`：生命周期与阶段职责
- `mod-js-loading.md`：JS Mod 全流程
- `mod-java-loading.md`：Java/Kotlin Mod 全流程
- `mod-content-registration.md`：内容注册顺序与依赖
- `mod-resource-packing.md`：贴图、atlas、icon 机制
- `mod-dependency-resolution.md`：依赖与状态机
- `mod-error-handling.md`：异常、兼容性、崩溃保护

## Related
- [Mindustry Project Structure](project-structure.md)
- [Content System](content-system.md)
- [Vars System](vars-system.md)
- [Events System](events-system.md)
- [Save and Load System](save-load-system.md)
