# Mindustry Mod Loading Callchain

## Overview
本页整理从 `Vars.mods` 到 Mod 实际进入运行态的源码调用链。目标是把“发现文件”到“内容注册”“资源打包”“脚本运行”之间的顺序讲清楚。

## Core Startup Chain
### 1. Vars 初始化阶段
`Vars` 持有全局入口：
- `Vars.mods`
- `Vars.tree`
- `Vars.content`
- `Vars.platform`
- `Vars.state`

其中与 Mod 最直接相关的是：
- `Vars.mods`：Mod 调度器
- `Vars.tree`：Mod 文件树/资源树

### 2. Mods.load()
`mindustry.mod.Mods.load()` 是 Mod 发现与解析的主入口。

典型流程：
1. 扫描 `modDirectory`
2. 收集 jar/zip/目录候选
3. 读取 `mod.json` / `mod.hjson` / `plugin.json` / `plugin.hjson`
4. 构造 `ModMeta`
5. 解析依赖并排序
6. 调用 `loadMod(...)`
7. 生成 `LoadedMod`
8. 更新依赖状态
9. `sortMods()`
10. `buildFiles()`

### 3. loadMod(...)
`loadMod` 负责把一个文件真正变成可用 Mod。

虽然实现细节在源码中更长，但从调用层面可归纳为：
- 识别 Mod 类型
- 构建类加载器或脚本环境
- 绑定 `LoadedMod`
- 记录元数据
- 准备内容与资源入口

### 4. LoadedMod 进入列表
成功加载后，Mod 会加入：
- `mods`
- `newImports`

并且可能写入：
- `Core.settings.put("mod-<name>-enabled", true)`

### 5. Dependency Update
`mods.each(this::updateDependencies)` 会进一步修正状态：
- enabled
- disabled
- unsupported

### 6. sortMods()
对最终启用顺序做排序，确保依赖先于依赖者。

### 7. buildFiles()
把 Mod 的文件结构纳入运行时文件树，供 `Vars.tree` 和资源系统使用。

---

## Resource and Runtime Chain
### loadAsync()
异步资源链路主要负责：
- 扫描 `sprites/`
- 扫描 `sprites-override/`
- 并行读取图片
- 生成 `PixmapRegion`
- `MultiPacker.add(...)`
- `Core.atlas.flush(...)`
- `AtlasPackEvent`

### loadSync()
同步资源链路主要负责：
- `loadIcons()`
- 读取 `icon.png`
- 为 Mod 设置 `iconTexture`

---

## Java/Kotlin Mod Callchain
### 入口
Java/Kotlin Mod 通常继承 `mindustry.mod.Mod`。

### 生命周期顺序
可理解为：
1. 类加载
2. Mod 实例化
3. `loadContent()`
4. `init()`
5. 命令注册
6. 事件监听与运行时逻辑

### Config Access
Mod 调用：
- `getConfigFolder()`
- `getConfig()`

时，最终会落到 `Vars.mods.getConfigFolder(this)` / `getConfig(this)`。

---

## JS Mod Callchain
### 入口
`mindustry.mod.Scripts` 初始化 Rhino：
1. `Vars.platform.getScriptContext()`
2. `new ImporterTopLevel(context)`
3. `RequireBuilder` 安装模块系统
4. 运行 `scripts/global.js`

### 执行单个脚本
`run(LoadedMod mod, Fi file)`：
- 设置 `currentMod`
- 注入 `modName` / `scriptName`
- 执行脚本内容
- 默认包装成 strict mode IIFE

### 子模块引用
`ScriptModuleProvider` 支持：
- `modname/path`
- 当前 Mod 内 `scripts/` 子目录加载
- 跨 Mod require 解析

---

## Vars.tree In Callchain
`Vars.tree` 不是“普通数据结构”，而是 Mod/资源文件解析的重要入口：
- `FileTree.get(path)` 负责寻找文件
- `ContentParser` 用它找 schematic
- `FileTree.loadSound/loadMusic` 用它找音频
- Mod 资源被 buildFiles 后也会进入这条路径

这使得 Mod 的资源访问统一收束到一个文件树抽象里。

---

## DataPatch Callchain
虽然它不是普通 Mod 加载主线，但和内容热修补有关：
- `DataPatcher.apply(...)`
- 使用 `ContentParser`
- 动态修改内容字段
- 可能影响 Mod 内容的后处理逻辑

因此它可以视作“内容补丁链”的旁路入口。

---

## Representative End-to-End Chain
一个典型 Mod 的完整主线可以概括为：

1. `Vars` 完成全局初始化
2. `Vars.mods.load()` 扫描文件
3. 读取元数据并构建 `ModMeta`
4. `resolveDependencies()` 排序
5. `loadMod()` 生成 `LoadedMod`
6. `Mod.loadContent()` / `Scripts.run()` 执行内容与脚本
7. `loadAsync()` 打包资源
8. `loadSync()` 装载 icon
9. `init()` 和事件钩子进入运行态
10. Mod 参与游戏生命周期

## Important Observations
- Mod 的“发现”“加载”“内容注册”“资源打包”是分阶段的，不是一个步骤完成
- JS Mod 和 Java/Kotlin Mod 共享总体框架，但执行器不同
- `Vars.tree` 是贯穿资源加载、脚本加载、内容解析的重要中枢
- 异常通常被局部捕获，避免拖垮全局加载

## Related
- [Mindustry Mod Loading System](mod-loading-system.md)
- [Mindustry Mod Meta Format](mod-meta-format.md)
- [Mindustry Mod Lifecycle](mod-lifecycle.md)
- [Mindustry Mod Resource Packing](mod-resource-packing.md)
