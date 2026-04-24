# Mindustry Mod Meta Format

## Overview
Mod 元数据是加载系统的入口描述文件，决定一个 Mod 是否能被发现、如何显示、是否启用、主类是什么、依赖哪些 Mod，以及是否兼容当前版本。

## Meta Files
源码中 Mod 加载器识别的元数据文件为：
- `mod.json`
- `mod.hjson`
- `plugin.json`
- `plugin.hjson`

对应源码常量：
```java
private static final String[] metaFiles = {"mod.json", "mod.hjson", "plugin.json", "plugin.hjson"};
```

## Core Meta Fields
从 `Mods`、`ModListing` 与实际加载流程可归纳出核心字段：
- `name`：显示名称
- `internalName`：内部唯一名
- `author`：作者
- `version`：版本
- `main`：主入口类
- `description`：简介
- `minGameVersion`：最低游戏版本
- `dependencies`：前置依赖
- `hidden`：是否隐藏（插件型）
- `mix` / `clientside` / `shaders` / `texturescale` / `pregenerated` 等资源或行为相关字段

> 具体字段数量会随版本变化，最终应以源码 `ModMeta` 定义为准。

## How Metadata Is Used
### Discovery
加载器会先扫描文件，再读取元数据，只有元数据有效的候选项才会进入下一步。

### Identification
`name` / `internalName` 用于：
- UI 中显示
- `Core.settings` 启用开关键
- 依赖解析
- 文件映射

### Dependency Resolution
元数据中的依赖声明会进入排序系统，用来决定：
- 加载顺序
- 是否启用
- 是否支持当前版本

### Resource Behavior
`texturescale`、`pregenerated` 这类字段会影响：
- sprite 打包
- atlas 重建
- 贴图缩放策略
- 是否跳过生成图标/贴图

## Plugin vs Mod
- `mod.json`：一般 Mod
- `plugin.json`：插件型 Mod

插件在源码里对应：
```java
public abstract class Plugin extends Mod{}
```

这意味着插件仍然走 Mod 生命周期，但语义上偏隐藏/系统化。

## Loading Rules
### 1. Metadata must be parseable
如果元数据损坏或缺字段，候选项直接被跳过。

### 2. Name must exist
`meta == null || meta.name == null` 的候选会被忽略。

### 3. Internal name maps to file
加载阶段会用 `internalName -> file` 建立映射，便于依赖解析。

### 4. Workshop entries are treated similarly
Steam Workshop 来源也会走元数据解析与映射。

## ClassMap Relevance
很多 JSON/HJSON 内容会在解析时通过 `ClassMap` 将字符串映射为具体类名。例如：
- AI
- BulletType
- Ability
- DrawBlock
- StatusEffect
- UnitType
- Block 相关实现

这使得 Mod 元数据不仅描述“入口”，还间接影响后续内容解析行为。

## Common Pitfalls
- `name` 缺失导致整个候选被跳过
- `main` 写错导致主类无法实例化
- `internalName` 冲突导致依赖映射混乱
- 旧版字段迁移后未更新，导致兼容性问题
- `plugin.json` 误当作普通 Mod 使用

## Related
- [Mindustry Mod Loading System](mod-loading-system.md)
- [Mindustry Mod Dependency Resolution](mod-dependency-resolution.md)
- [Mindustry Mod Lifecycle](mod-lifecycle.md)
- [Mindustry Mod Content Registration](mod-content-registration.md)
