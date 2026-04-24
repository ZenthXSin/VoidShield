# Mindustry Mod Lifecycle

## Overview
本页整理 `Mod` / `Plugin` 的生命周期、回调时机，以及它们在 `Mods` 管理器中的位置。

## Base Classes
### Mod
源码定义：
```java
public abstract class Mod{
    public Fi getConfigFolder(){...}
    public Fi getConfig(){...}
    public void init(){ }
    public void loadContent(){ }
    public void registerServerCommands(CommandHandler handler){ }
    public void registerClientCommands(CommandHandler handler){ }
}
```

### Plugin
源码定义：
```java
public abstract class Plugin extends Mod{}
```

结论：`Plugin` 只是一个语义更强的 `Mod` 子类，表示“始终隐藏”的特殊 Mod。

---

## Lifecycle Phases
### 1. Discover
Mod 被扫描到后进入候选列表，尚未实例化。

### 2. Parse Meta
读取元数据，构造 `ModMeta`。

### 3. Load Class or Script
- Java/Kotlin Mod：实例化 `Mod`
- JS Mod：进入 `Scripts` 执行链

### 4. Content Loading
对应回调：
- `loadContent()`

这一步用于注册内容、执行资源绑定、初始化静态定义。

### 5. Command Registration
对应回调：
- `registerServerCommands(...)`
- `registerClientCommands(...)`

说明：命令注册是显式回调，不是自动发生的内容加载的一部分。

### 6. Init
对应回调：
- `init()`

这是内容和命令准备完成后进入运行态的阶段。

---

## Practical Order
从源码行为和注释可归纳为一个常见顺序：

1. Mod 被创建
2. `loadContent()`
3. 命令注册
4. `init()`
5. 游戏进入运行中生命周期

> 实际顺序会受 Mod 类型和加载器细节影响，但这个顺序最符合源码注释与整体结构。

---

## Config Access
`Mod` 提供两个便捷入口：
- `getConfigFolder()`
- `getConfig()`

它们最终委托给 `Vars.mods`：
- `Vars.mods.getConfigFolder(this)`
- `Vars.mods.getConfig(this)`

这意味着配置目录不是 Mod 自己决定的，而是由 Mod 管理器统一分配。

---

## Hidden Behavior of Plugin
`Plugin` 的源码虽然为空，但在 `Mods` / 元数据层面通常表现为：
- 隐藏展示
- 更偏向系统扩展或服务器功能
- 常用于不需要向普通玩家展示的功能

---

## Notes from Source
- `init()` 的注释明确写的是“所有插件创建并完成命令注册后调用”
- `loadContent()` 的注释明确写的是“clientside mods. Load content here.”
- 命令注册是单独接口，说明命令系统与内容系统是分开的两个阶段

---

## Related
- [Mindustry Mod Loading Callchain](mod-loading-callchain.md)
- [Mindustry Mod Meta Format](mod-meta-format.md)
- [Mindustry Mod Loading System](mod-loading-system.md)
- [Mindustry JS Mod Loading](mod-js-loading.md)
- [Mindustry Java Mod Loading](mod-java-loading.md)
