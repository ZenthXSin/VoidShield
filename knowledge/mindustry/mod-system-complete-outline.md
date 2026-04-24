# Mindustry Mod System Complete Outline

## Scope
本页作为 Mod 系统总补完页，覆盖：
- Mod 发现
- 元数据
- 依赖解析
- 生命周期
- Java/Kotlin Mod
- JS Mod
- 资源与图标加载
- 配置与文件树
- 卸载与重载
- 脚本模块系统

## 1. Discovery
`Mods.load()` 会扫描两类来源：
- 本地 `modDirectory`
- Steam Workshop 内容

候选必须满足：
- 是 `jar` / `zip`
- 或目录中存在 `mod.json` / `mod.hjson` / `plugin.json` / `plugin.hjson`

## 2. Metadata Parsing
源码识别的元数据文件：
- `mod.json`
- `mod.hjson`
- `plugin.json`
- `plugin.hjson`

元数据至少需要：
- `name`
- `internalName`

否则会被跳过。

## 3. Dependency Resolution
`resolveDependencies(metas)` 会先把元数据排序，再进入实际加载。

在结果阶段：
- `ModState.enabled`：可启用
- `ModState.disabled`：手动关闭
- `ModState.unsupported`：版本或环境不兼容

## 4. Instantiation
`loadMod(file, false, enabled)` 创建 `LoadedMod`，并加入：
- `mods`
- `newImports`（仅导入场景）

如果是 Workshop 内容，还会记录 Steam ID。

## 5. Lifecycle
`Mod` 的公开生命周期接口：
- `loadContent()`
- `registerServerCommands(...)`
- `registerClientCommands(...)`
- `init()`

`Plugin` 只是一个隐藏型 `Mod` 子类。

## 6. Script Engine
`Scripts` 做了三件事：
1. 创建 Rhino context
2. 安装 `RequireBuilder`
3. 执行 `scripts/global.js`

单个脚本执行时：
- 注入 `modName`
- 注入 `scriptName`
- `use strict`
- 支持跨 Mod `require`

## 7. Resource Packing
`loadAsync()` 负责：
- 扫描 `sprites/` 与 `sprites-override/`
- 并行读取 PNG
- 通过 `MultiPacker` 打包
- 生成新的 atlas
- 触发 `AtlasPackEvent`

`loadSync()` 负责：
- 加载 `icon.png`
- 设置 `iconTexture`

## 8. File Tree
`Vars.tree` / `FileTree` 是 Mod 资源统一入口：
- `get(path)`
- `resolve(path)`
- `loadSound()`
- `loadMusic()`

## 9. Config
`Mod` 的配置访问统一通过：
- `getConfigFolder()`
- `getConfig()`

最终落在 `Vars.mods` 管理的 mod 目录下。

## 10. Removal
`removeMod()` 会：
- 尝试关闭类加载器
- 删除 zip/directory
- 从 `mods` / `newImports` 移除
- `dispose()`
- 必要时标记 `requiresReload`

## 11. Error Handling
加载失败时：
- 普通 mod 走普通错误日志
- Workshop mod 走专门错误日志
- 旧插件主类 `mindustry.plugin.Plugin` 会提示迁移到 `mindustry.mod.Plugin`

## 12. Key Source Relationships
- `Vars.mods` 是主调度器
- `Vars.tree` 是资源文件树
- `ModClassLoader` 负责子类加载
- `ContentParser` 负责内容数据解析
- `Scripts` 负责 JS mod 环境
- `DataPatcher` 负责内容补丁

## 13. Final Loading Model
一个 Mod 从文件到运行态的完整模型可概括为：

1. 扫描文件
2. 读取元数据
3. 解析依赖
4. 创建 `LoadedMod`
5. 绑定类加载器/脚本环境
6. 加载内容
7. 注册命令
8. 加载资源与图标
9. 初始化运行态
10. 参与游戏生命周期

## Related
- [Mindustry Mod Loading System](mod-loading-system.md)
- [Mindustry Mod Meta Format](mod-meta-format.md)
- [Mindustry Mod Loading Callchain](mod-loading-callchain.md)
- [Mindustry Mod Lifecycle](mod-lifecycle.md)
