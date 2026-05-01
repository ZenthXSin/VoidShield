# TeleportAbility JSON 配置文档

## 概述
`TeleportAbility` 为单位提供短距离跃迁（传送）能力。玩家通过短时间内双击攻击键触发，跃迁期间可自定义特效、音效和子弹生成。
默认特效可在content/effectConfig/teleportEffectConfig.json修改
---

## 主配置结构

### TeleportAbility
- **类型**: `voidshield.entities.abilities.TeleportAbility`

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `type` | String | 是 | 固定值：`voidshield.entities.abilities.TeleportAbility` |
| `data` | Object | 是 | 跃迁配置数据对象，见下方 TeleportData |

---

## TeleportData 配置

- **类型**: `voidshield.entities.abilities.TeleportData`

| 字段 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `type` | String | - | `voidshield.entities.abilities.TeleportData` |
| `range` | Float | 200 | 跃迁最大距离（像素/格数，视代码实现而定） |
| `cooldown` | Float | 60 | 冷却时间（帧，60帧=1秒） |
| `readyTime` | Float | 290 | 跃迁准备/蓄力时间（帧），之后执行传送 |
| `endTime` | Float | 130 | 跃迁后摇/结束动画时间（帧） |
| `useDefaultEffect` | Boolean | true | 是否使用默认特效（true 时忽略 effects/sounds，使用代码内置特效） |
| `statusEffect` | String | "none" | 跃迁期间给予单位的状态效果 ID（如 "unmoving", "slow" 等） |
| `statusTime` | Float | 0 | 状态效果持续时间（帧） |
| `effects` | Array | [] | 自定义特效序列（useDefaultEffect=false 时生效） |
| `sounds` | Array | [] | 自定义音效序列（useDefaultEffect=false 时生效） |
| **bullets** | **Array** | **[]** | **跃迁期间发射的子弹序列（关键字段）** |

---

## 子配置类型

### TeleportEffectData - 特效节点
- **类型**: `voidshield.entities.abilities.TeleportEffectData`

```json
{
    "type": "voidshield.entities.abilities.TeleportEffectData",
    "effect": "effectId",
    "startTime": 0
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `effect` | String | 特效 ID（如 "upgradeCoreBloom", "teleportActivate"） |
| `startTime` | Float | 触发时间（帧），从跃迁开始计时 |

---

### TeleportSoundData - 音效节点
- **类型**: `voidshield.entities.abilities.TeleportSoundData`

```json
{
    "type": "voidshield.entities.abilities.TeleportSoundData",
    "sound": "soundId",
    "volume": 1.0,
    "startTime": 0
}
```

| 字段 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `sound` | String | "none" | 音效 ID（如 "laser", "blast", "none"） |
| `volume` | Float | 1.0 | 音量（0.0 - 1.0+） |
| `startTime` | Float | 0 | 触发时间（帧），循环播放 5 帧 |

---

### **TeleportBulletData - 子弹节点（重点）**
- **类型**: `voidshield.entities.abilities.TeleportBulletData`

```json
{
    "type": "voidshield.entities.abilities.TeleportBulletData",
    "bullet": "bulletTypeId",
    "startTime": 0
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `bullet` | String | **子弹类型 ID**（如 "standardCopper", "missileExplosive", "lancerLaser"） |
| `startTime` | Float | 发射时间（帧），从跃迁开始计时。在 readyTime 前发射则从起点射出，在 readyTime 后发射则从终点射出 |

**子弹生成逻辑**：
- 每个子弹只发射一次（去重机制）
- 子弹以单位当前位置和旋转角度创建
- 继承单位所属队伍
- 时间点判断：`startTime <= 当前跃迁时间`

---

## 完整配置示例

### 示例 1：基础跃迁（仅默认特效）
```json
{
    "abilities": [
        {
            "type": "voidshield.entities.abilities.TeleportAbility",
            "data": {
                "type": "voidshield.entities.abilities.TeleportData",
                "range": 160,
                "cooldown": 1800,
                "readyTime": 290,
                "endTime": 130,
                "useDefaultEffect": true,
                "statusEffect": "unmoving",
                "statusTime": 420
            }
        }
    ]
}
```

### 示例 2：带子弹发射的跃迁
```json
{
    "abilities": [
        {
            "type": "voidshield.entities.abilities.TeleportAbility",
            "data": {
                "type": "voidshield.entities.abilities.TeleportData",
                "range": 200,
                "cooldown": 1200,
                "readyTime": 300,
                "endTime": 100,
                "useDefaultEffect": false,
                "statusEffect": "slow",
                "statusTime": 300,
                "effects": [
                    {
                        "type": "voidshield.entities.abilities.TeleportEffectData",
                        "effect": "teleportCharge",
                        "startTime": 0
                    },
                    {
                        "type": "voidshield.entities.abilities.TeleportEffectData",
                        "effect": "teleportFlash",
                        "startTime": 300
                    }
                ],
                "sounds": [
                    {
                        "type": "voidshield.entities.abilities.TeleportSoundData",
                        "sound": "laserCharge",
                        "volume": 0.8,
                        "startTime": 0
                    },
                    {
                        "type": "voidshield.entities.abilities.TeleportSoundData",
                        "sound": "warp",
                        "volume": 1.0,
                        "startTime": 300
                    }
                ],
                "bullets": [
                    {
                        "type": "voidshield.entities.abilities.TeleportBulletData",
                        "bullet": "standardThorium",
                        "startTime": 50
                    },
                    {
                        "type": "voidshield.entities.abilities.TeleportBulletData",
                        "bullet": "missileSurge",
                        "startTime": 300
                    },
                    {
                        "type": "voidshield.entities.abilities.TeleportBulletData",
                        "bullet": "blastExplosive",
                        "startTime": 350
                    }
                ]
            }
        }
    ]
}
```

### 示例 3：全自动跃迁陷阱（AI 用）
```json
{
    "abilities": [
        {
            "type": "voidshield.entities.abilities.TeleportAbility",
            "data": {
                "type": "voidshield.entities.abilities.TeleportData",
                "range": 120,
                "cooldown": 600,
                "readyTime": 60,
                "endTime": 30,
                "useDefaultEffect": true,
                "bullets": [
                    {
                        "type": "voidshield.entities.abilities.TeleportBulletData",
                        "bullet": "fireball",
                        "startTime": 60
                    }
                ]
            }
        }
    ]
}
```

---

## 时间线说明

跃迁过程按时间（帧）分为三个阶段：

```
0 ─────── readyTime ─────── (readyTime+endTime)
│          │                    │
准备阶段    传送瞬间              结束阶段
│          │                    │
特效开始    单位坐标变更           冷却开始
子弹发射   若配置了此时刻的子弹     状态清除
           则从目标位置发射
```

**注意**：
- `startTime < readyTime`：子弹从**跃迁起点**发射
- `startTime >= readyTime`：子弹从**跃迁终点**（目标位置）发射
- 所有时间单位均为**游戏帧**（通常 60fps）