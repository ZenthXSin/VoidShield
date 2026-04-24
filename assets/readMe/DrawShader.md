# DrawShader JSON 配置文档

## 概述
`DrawShader` 是一个自定义着色器绘制器，用于在 Mindustry 中为建筑应用 GPU 着色器效果。支持多种几何形状（圆形、四边形、多边形、环形等）和纹理区域。

**注意**：v2.0+ 版本已将多个 ShaderData 子类合并为统一的 `ShaderData` 类，通过 `type` 字符串字段区分形状类型。

---

## 基础配置格式

### 主类信息
- **类型**: `voidshield.other.drawers.DrawShader`
- **适用场景**: 用于 Block 的 `drawer` 字段或复合绘制器的子绘制器

### 基础字段

| 字段名 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `type` | String | 必填 | 完整类路径：`voidshield.other.drawers.DrawShader` |
| `shaderName` | String | `"HeatShader"` | 着色器名称，对应 `VsVars.shaders` 中的键 |
| `data` | Object | `{}` | 着色器数据对象，包含形状类型和几何参数 |

---

## ShaderData 配置详解

`data` 对象使用统一的类结构，通过 `type` 字段指定渲染形状：

### 通用字段说明

| 字段 | 类型 | 默认值 | 适用形状 | 说明 |
|------|------|--------|----------|------|
| `type` | String | `""` | 所有 | **必填**，形状类型标识符 |
| `r` | Float | `0.0` | Circle, Polygon | 半径（游戏内像素单位） |
| `seg` | Int | `48` | Circle, Ring | 分段数（平滑度，建议 32-64） |
| `width` | Float | `0.0` | Quad, ScreenQuad, TexturedRegion | 宽度 |
| `height` | Float | `0.0` | Quad, ScreenQuad, TexturedRegion | 高度 |
| `sides` | Int | `0` | Polygon | 边数（3=三角形, 6=六边形等） |
| `innerRadius` | Float | `0.0` | Ring | 内半径 |
| `outerRadius` | Float | `0.0` | Ring | 外半径 |
| `u1` | Float | `0.0` | TexturedRegion | 左上角 U 坐标（0-1） |
| `v1` | Float | `0.0` | TexturedRegion | 左上角 V 坐标（0-1） |
| `u2` | Float | `1.0` | TexturedRegion | 右下角 U 坐标（0-1） |
| `v2` | Float | `1.0` | TexturedRegion | 右下角 V 坐标（0-1） |

---

### 形状类型配置示例

#### 1. Circle - 圆形
渲染实心圆形区域。

```json
"data": {
    "type": "Circle",
    "r": 40.0,
    "seg": 48
}
```

**必填字段**: `type`, `r`  
**可选字段**: `seg`（默认 48）

---

#### 2. Quad - 四边形
渲染世界空间矩形区域。

```json
"data": {
    "type": "Quad",
    "width": 64.0,
    "height": 64.0
}
```

**必填字段**: `type`, `width`, `height`

---

#### 3. Polygon - 正多边形
渲染正多边形。

```json
"data": {
    "type": "Polygon",
    "r": 50.0,
    "sides": 6
}
```

**必填字段**: `type`, `r`, `sides`

---

#### 4. Ring - 圆环
渲染空心圆环。

```json
"data": {
    "type": "Ring",
    "innerRadius": 20.0,
    "outerRadius": 40.0,
    "seg": 48
}
```

**必填字段**: `type`, `innerRadius`, `outerRadius`  
**可选字段**: `seg`（默认 48）

---

#### 5. ScreenQuad - 屏幕空间四边形
渲染屏幕对齐的矩形（不受世界旋转影响）。

```json
"data": {
    "type": "ScreenQuad",
    "width": 100.0,
    "height": 100.0
}
```

**必填字段**: `type`, `width`, `height`

---

#### 6. TexturedRegion - 纹理映射区域
渲染带 UV 坐标的纹理区域。

```json
"data": {
    "type": "TexturedRegion",
    "width": 64.0,
    "height": 64.0,
    "u1": 0.0,
    "v1": 0.0,
    "u2": 1.0,
    "v2": 1.0
}
```

**必填字段**: `type`, `width`, `height`  
**可选字段**: `u1`, `v1`, `u2`, `v2`（默认 0,0,1,1）

---

## 完整配置示例

### 示例 1：基础圆形着色器（热能扩散效果）
```json
{
    "type": "voidshield.other.drawers.DrawShader",
    "shaderName": "HeatShader",
    "data": {
        "type": "Circle",
        "r": 48.0,
        "seg": 64
    }
}
```

### 示例 2：六边形能量场
```json
{
    "type": "voidshield.other.drawers.DrawShader",
    "shaderName": "EnergyFieldShader",
    "data": {
        "type": "Polygon",
        "r": 60.0,
        "sides": 6
    }
}
```

### 示例 3：带纹理的矩形区域
```json
{
    "type": "voidshield.other.drawers.DrawShader",
    "shaderName": "CustomTextureShader",
    "data": {
        "type": "TexturedRegion",
        "width": 128.0,
        "height": 64.0,
        "u1": 0.0,
        "v1": 0.0,
        "u2": 1.0,
        "v2": 0.5
    }
}
```

### 示例 4：复合绘制器中使用（圆环指示器）
```json
{
    "type": "drawMulti",
    "drawers": [
        {
            "type": "drawRegion",
            "suffix": "-base"
        },
        {
            "type": "voidshield.other.drawers.DrawShader",
            "shaderName": "GlowShader",
            "data": {
                "type": "Ring",
                "innerRadius": 16.0,
                "outerRadius": 24.0,
                "seg": 32
            }
        }
    ]
}
```

### 示例 5：屏幕空间 UI 元素
```json
{
    "type": "voidshield.other.drawers.DrawShader",
    "shaderName": "UIShader",
    "data": {
        "type": "ScreenQuad",
        "width": 200.0,
        "height": 100.0
    }
}
```

---

## 注意事项

1. **着色器名称**：`shaderName` 必须已在 `VsVars.shaders` 中注册，否则自动回退到 `defaultShader`
2. **类型匹配**：`data.type` 必须与代码中 `when` 表达式的大小写完全匹配（首字母大写）
3. **ID 生成**：运行时自动生成 `[${build.id}]` 作为网格唯一标识符，每个建筑实例独立
4. **透明度控制**：通过 `build.warmup()` 自动调整网格 alpha 值（预热阶段为 1，运行期为 warmup 值）
5. **网格复用**：`hasMesh()` 检查防止重复创建，确保每个建筑只初始化一次几何体
6. **日志输出**：未知 `type` 值将在日志中输出警告：`[DrawShader]Unknown shader data: $data`
