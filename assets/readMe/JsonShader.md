## 自定义着色器

在 `shaders` 文件夹下放置你的 `.frag` 文件和 `.vert` 文件，然后在 `content/config` 文件夹中新建 `shadersConfig.json` 文件，内容格式如下：

```json
{
  "shaders": [
    {
      "name": "Default",
      "frag": "default",
      "vert": "default"
    },
    {
      "name": "Glow",
      "frag": "glow",
      "vert": "default"
    }
  ]
}
```

**字段说明：**
- `name`: 着色器标识名
- `frag`: 片段着色器文件名（不含 `.frag` 后缀）
- `vert`: 顶点着色器文件名（不含 `.vert` 后缀）
---

### 着色器编写

查看两个默认顶点/片段着色器文件，建议只修改变量声明以下的部分。