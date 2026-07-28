# 自定义建筑分类 JSON 配置文档

## 概述

`CategoryExtenderJsonParse` 用于通过 JSON 配置向 Mindustry 的建筑分类（`Category`）中追加自定义分类，并将指定建筑移动到这些分类下。

配置文件需要放置在 Mod 的 `content/config` 文件夹中，文件名必须为 `CategoryExtenderConfig.json`。

---

## 配置文件位置

```text
你的Mod/
├── content/
│   └── config/
│       └── CategoryExtenderConfig.json
```

参数是 Mod 的内部名称，应与 Mod 元数据中的名称一致。

---

## 基础配置格式

```json
{
  "catList": [
    {
      "name": "custom-category",
      "block": [
        "example-block"
      ]
    }
  ]
}
```

### 字段说明

| 字段名 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `catList` | Array | `[]` | 自定义建筑分类配置列表 |
| `catList[].name` | String | `""` | 新分类的名称，同时也是分类的唯一标识 |
| `catList[].block` | Array | `[]` | 需要移动到该分类下的建筑名称列表 |

`block` 中的建筑名称应使用建筑的内部名称，而不是显示名称。例如：

```json
{
  "catList": [
    {
      "name": "defense-custom",
      "block": [
        "copper-wall",
        "titanium-wall"
      ]
    }
  ]
}
```

---

## 多个自定义分类

可以在 `catList` 中同时定义多个分类：

```json
{
  "catList": [
    {
      "name": "defense-custom",
      "block": [
        "copper-wall",
        "titanium-wall"
      ]
    },
    {
      "name": "production-custom",
      "block": [
        "mechanical-drill",
        "pneumatic-drill"
      ]
    }
  ]
}
```

每个建筑只能在最后一次配置赋值中确定分类。如果同一个建筑出现在多个分类的 `block` 列表中，后执行的配置会覆盖先前的分类。

---

## 图标配置

自定义分类的图标名称会由代码自动生成：

```text
${VsVars.modName}-${cat.name}
```

其中：

- `VsVars.modName` 是 Mod 的名称前缀，由程序自动设置，不需要在 JSON 中填写。
- `cat.name` 是 `CategoryExtenderConfig.json` 中配置的分类名称。

例如，分类配置如下：

```json
{
  "catList": [
    {
      "name": "defense-custom",
      "block": ["copper-wall"]
    }
  ]
}
```

如果 `VsVars.modName` 为 `voidshield`，程序会优先查找以下图集名称：

```text
voidshield-defense-custom
```

对应的资源文件应放在 Mod 的资源目录中，例如：

```text
sprites/defense-custom.png
```

如果找不到带 Mod 名称前缀的图标，程序还会依次尝试查找：

1. `cat.name`，即 `defense-custom`
2. `error`

因此也可以提供以下备用图标：

```text
assets/defense-custom.png
```

如果前两种图标都不存在，界面会使用 `error` 图标作为回退图标。

---

## 加载流程

调用 `loadJson` 后，解析器会按以下顺序处理配置：

1. 查找指定 Mod。
2. 查找 `content/config/CategoryExtenderConfig.json`。
3. 读取并解析 JSON。
4. 为 `catList` 中的每项追加一个新的 `Category`。
5. 注册分类图标并刷新建筑选择栏 UI。
6. 将 `block` 列表中的建筑设置为对应的分类。

如果找不到 Mod 或配置文件，解析器会输出日志并结束加载，不会创建分类。

---

## 使用要求

- `name` 不能与已有分类名称冲突。
- `block` 中的建筑必须已经注册到 Mindustry 内容系统中。
- 分类名称建议使用小写字母、数字和连字符，例如 `defense-custom`。
- JSON 文件名和路径区分大小写，必须为 `CategoryExtenderConfig.json`。
- 修改建筑分类后，分类 UI 会在加载时刷新。

加载失败时，日志会输出以下前缀，便于定位问题：

```text
[CategoryExtenderJsonParse]
```

---

## 完整示例

```json
{
  "catList": [
    {
      "name": "defense-custom",
      "block": [
        "copper-wall",
        "titanium-wall",
        "plastanium-wall"
      ]
    },
    {
      "name": "production-custom",
      "block": [
        "mechanical-drill",
        "pneumatic-drill",
        "steam-generator"
      ]
    }
  ]
}
```
