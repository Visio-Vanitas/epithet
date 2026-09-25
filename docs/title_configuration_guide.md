# Epithet 称号配置规范与操作指南

本文档说明 Epithet 模组中称号元数据（Metadata）的定义结构、数据驱动配置规范（Datapack Schema）、服务端指令接口以及生命周期同步机制。

---

## 1. 称号数据模型 (Data Model)

称号系统采用基于 `TitleDefinition` Record 的不可变元数据结构，定义包含标识、显示样式、获取条件及排序权重。

### 1.1 字段定义与约束

| 字段名 | 字段类型 | 约束条件 | 默认值 | 说明 |
| :--- | :--- | :---: | :---: | :--- |
| `id` | `ResourceLocation` | 必填，符合 `[a-z0-9_.-]+:[a-z0-9_.-]+` | - | 唯一命名空间标识符。 |
| `displayName` | `Component` / `String` | 必填 | - | 称号展示名称。支持原版组件对象、翻译键或字面量文本。 |
| `description` | `Component` / `String` | 可选 | `""` (空组件) | 称号描述说明。用于 GUI 列表展示与聊天栏悬浮提示卡片。 |
| `color` | `Integer` / `String` | 可选 | `0xFFFFFF` | 默认渲染颜色（32 位 RGB 色值）。支持 Hex、§ 格式及标准色名。 |
| `priority` | `Integer` | 可选 | `0` | 排序权重。GUI 渲染时按权重降序排列；权重相同时按 ID 字典序排序。 |
| `rarity` | `String` | 可选 | `"common"` | 稀有度枚举标识（对应语言文件 `rarity.epithet.<rarity>`）。 |
| `icon` | `ResourceLocation` | 可选 | `epithet:default` | 图标资源标识符（预留字段）。 |
| `defaultUnlocked` | `Boolean` | 可选 | `false` | 全局解锁标记。为 `true` 时，所有玩家在登录阶段自动写入解锁列表。 |
| `advancement` | `ResourceLocation` | 可选 | 无 (`null`) | 关联的原版进度标识符。达成该进度时自动触发解锁。 |

---

## 2. 数据驱动配置规范 (Datapack Schema)

数据驱动称号通过 Minecraft 标准 Datapack 加载，路径规范与原版配方/进度系统保持一致。

### 2.1 目录结构与命名空间
称号定义文件必须存放于：
```text
<datapack>/data/<namespace>/titles/<path>.json
```
*示例目录树：*
```text
datapack_example/
├── pack.mcmeta
└── data/
    └── custom_server/
        └── titles/
            ├── rank_vip.json          # 对应 ID: custom_server:rank_vip
            ├── ender_slayer.json      # 对应 ID: custom_server:ender_slayer
            └── quest_complete.json    # 对应 ID: custom_server:quest_complete
```

---

### 2.2 JSON 模式定义 (JSON Schema)

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "TitleDefinition",
  "type": "object",
  "required": ["id", "displayName"],
  "properties": {
    "id": {
      "type": "string",
      "pattern": "^[a-z0-9_.-]+:[a-z0-9_.-]+$"
    },
    "displayName": {
      "oneOf": [
        { "type": "string" },
        { "type": "object" }
      ]
    },
    "description": {
      "oneOf": [
        { "type": "string" },
        { "type": "object" }
      ]
    },
    "color": {
      "oneOf": [
        { "type": "integer" },
        { "type": "string" }
      ]
    },
    "priority": {
      "type": "integer"
    },
    "rarity": {
      "type": "string"
    },
    "icon": {
      "type": "string"
    },
    "defaultUnlocked": {
      "type": "boolean"
    },
    "advancement": {
      "type": "string",
      "pattern": "^[a-z0-9_.-]+:[a-z0-9_.-]+$"
    }
  }
}
```

---

### 2.3 属性解析语义

#### 2.3.1 多态显示名 (`displayName`)
系统内置多态文本解析器（`FLEXIBLE_COMPONENT_CODEC`），根据传入类型执行不同解析策略：
1. **字符串字面量 / 翻译键**：
   - 传入普通字符串时，底层通过 `Component.translatableWithFallback(str, str)` 加载。
   - 若资源包包含该键的本地化翻译（如 `title.server.vip`），则展示为对应语言；若无翻译，则原样回退展示为该字符串字面量。
2. **复合组件对象**：
   - 传入原生 JSON Object 时，通过原版 `ComponentSerialization.CODEC` 解析。支持标准样式属性：`bold`, `italic`, `underlined`, `strikethrough`, `obfuscated`, `color`, `font`。

```json
// 复合组件示例
"displayName": {
  "text": "ARCHITECT",
  "bold": true,
  "color": "#FF5500"
}
```

#### 2.3.2 颜色解析规则 (`color`)
由 `ColorHelper` 进行动态转译：
- **Hex 格式**：`#RRGGBB`（例如 `"#FFAA00"`）或 `0xRRGGBB`（例如 `0x55FF55`）。
- **Formatting Code**：包含 `§` 或 `&` 前缀的原版颜色码（例如 `"§6"`, `"&a"`）。
- **标准颜色名**：原版十六种标准色名称（`"black"`, `"dark_blue"`, `"dark_green"`, `"dark_aqua"`, `"dark_red"`, `"dark_purple"`, `"gold"`, `"gray"`, `"dark_gray"`, `"blue"`, `"green"`, `"aqua"`, `"red"`, `"light_purple"`, `"yellow"`, `"white"`）。

#### 2.3.3 进度绑定机制 (`advancement`)
- 当配置 `advancement` 属性时，服务端自动注册该关系的映射。
- **触发链路**：
  1. 玩家触发进度，NeoForge 触发 `AdvancementEarnEvent`。
  2. 服务端对比 `earnedAdvId == titleDef.advancement()`。
  3. 若条件成立且玩家未拥有该称号，执行 `data.unlockTitle(id)`，持久化保存并通过 `TitleNetworkHandler.syncPlayerTitle` 下发 S2C 数据包。
  4. 广播解锁系统消息（对应语言键 `message.epithet.unlock_by_advancement`）。
- **状态补齐**：玩家加入游戏时（`PlayerLoggedInEvent`），系统会自动比对玩家已完成的所有进度，自动解锁缺失的历史成就称号。

---

### 2.4 数据包配置范例

#### 范例 A：全服默认解锁基础称号
```json
{
  "id": "server:settler",
  "displayName": "定居者",
  "description": "已在服务器完成常住登记。",
  "color": "gray",
  "priority": 10,
  "rarity": "common",
  "defaultUnlocked": true
}
```

#### 范例 B：绑定进度的阶段性奖励称号
```json
{
  "id": "server:nether_walker",
  "displayName": {
    "text": "深渊行者",
    "bold": true,
    "color": "#AA0000"
  },
  "description": "成功建立跨维度通道进入下界荒原。",
  "color": "#AA0000",
  "priority": 50,
  "rarity": "rare",
  "defaultUnlocked": false,
  "advancement": "minecraft:nether/root"
}
```

---

### 2.5 运行时重载生命周期 (Hot Reloading)

通过执行 `/reload` 指令，触发以下更新管道：
1. **服务端解析**：`TitleReloadListener` 读取所有激活数据包的 `titles/*.json`，完成 Schema 校验与实例化。
2. **双源合并**：将数据包静态定义与 `TitleSavedData` 中的动态称号进行合并（静态定义优先级高于同 ID 的动态定义）。
3. **数据一致性清理**：检查当前在线玩家持有的称号 ID 集合，自动剔除已在数据包中删除的孤立引用（Orphaned ID Pruning）。
4. **网络广播**：通过 `TitleRegistrySyncPayload` 全量同步最新注册表至所有在线客户端，客户端清空并重构选择器列表缓存。

---

## 3. 服务端指令管理接口 (Command Interface)

适用于运行时动态增发、管理调试及第三方脚本系统集成。

### 3.1 权限控制
- 根节点：`/epithet`
- 管理节点：`/epithet admin`
- 权限判定条件：`source.hasPermission(2)`（Dedicated Server OP Level 2+ 或局域网开启作弊）。

---

### 3.2 命令语法与参数表

```text
/epithet admin create <id> <name> [color] [desc]
/epithet admin give <targets> <id>
/epithet admin take <targets> <id>
/epithet admin set <targets> <id>
/epithet admin lock <targets>
/epithet admin unlock <targets>
```

#### 参数规范说明：
- `<id>`：`ResourceLocation` 格式（如 `custom:champion`）。
- `<name>`：字符串参数。支持输入普通文本（带空格需使用引号包裹）或标准的组件 JSON 字符串（如 `{"text":"ACE","color":"gold"}`）。
- `[color]`：颜色字面量（可选，默认为 `white`）。
- `[desc]`：贪婪字符串参数（可选，包含描述文本）。
- `<targets>`：标准实体选择器（支持 `@p`, `@a`, `@e`, `玩家名`）。

---

### 3.3 指令执行范例

| 操作行为 | 示例命令 |
| :--- | :--- |
| 创建纯文本称号 | `/epithet admin create rank:mvp MVP gold "全场最佳选手"` |
| 创建富文本 JSON 称号 | `/epithet admin create rank:mod "{\"text\":\"MOD\",\"bold\":true}" #00AAFF "服务管理人员"` |
| 批量赋予指定玩家称号 | `/epithet admin give @a[distance=..20] rank:mvp` |
| 强制玩家佩戴目标称号 | `/epithet admin set Player_Steve rank:mvp` |
| 剥夺玩家已有称号 | `/epithet admin take Player_Steve rank:mvp` |
| 锁定玩家称号佩戴状态 | `/epithet admin lock Player_Steve` |
| 解除玩家称号锁定状态 | `/epithet admin unlock Player_Steve` |

---

### 3.4 动态称号存储模型 (Storage & Persistence)

- 通过 `/epithet admin create` 创建的动态称号脱离数据包独立存在，注册到全局 `TitleRegistry`。
- **持久化路径**：位于主世界存档目录下的 `data/epithet_titles.dat`。
- **序列化载体**：由 `TitleSavedData`（继承自 `SavedData`）在世界保存（World Save）阶段自动序列化为 NBT 结构；服务器启动或维度初始化时载入内存。

---

## 4. 全局配置文件规范 (Configuration Specification)

服务端基础开局行为定义于主配置文件中。

### 4.1 配置文件路径
```text
config/epithet-common.toml
```

### 4.2 配置属性对照表

| 配置键名 | 数据类型 | 默认值 | 作用说明 |
| :--- | :---: | :---: | :--- |
| `default_title.enableDefaultTitle` | `Boolean` | `true` | 是否为首次登入服务器的玩家分发初始开局称号。 |
| `default_title.autoEquipDefaultTitle` | `Boolean` | `true` | 当玩家未激活任何称号时，登入后是否自动装备该初始称号。 |
| `default_title.defaultTitleId` | `String` | `"epithet:beginner"` | 内置初始称号的唯一标识符。 |
| `default_title.defaultTitleDisplayName`| `String` | `"初出茅庐"` | 内置初始称号的展示文本。 |
| `default_title.defaultTitleDescription`| `String` | `"初入方块世界的懵懂冒险家。"`| 内置初始称号的详细说明。 |
| `default_title.defaultTitleColor` | `String` | `"green"` | 内置初始称号的色彩值（支持 Hex、代码及色名）。 |

---

## 5. 故障排查与边界条件 (Troubleshooting)

### 5.1 称号无法自动随进度解锁
1. 校验进度的命名空间与 ID 是否完全匹配（如 `minecraft:story/mine_stone` 而非 `story.mine_stone`）。
2. 执行 `/advancement grant <player> only <advancement_id>` 模拟完成，验证服务端事件总线是否正确捕获 `AdvancementEarnEvent`。

### 5.2 数据包删除称号后的客户端表现
1. 服务端重载后，玩家持有的该称号 ID 标记为 Orphaned 并被移除。
2. 客户端持有的 `TitleRegistry` 缓存会即时删除对应元数据。
3. 若玩家当前佩戴的称号被删除，系统自动将其状态重置为空，并刷新玩家 `getDisplayName()` 缓存。

### 5.3 界面显示未翻译的键名
- 若在 `displayName` 中传入了自定义命名空间翻译键（如 `title.server.custom`），客户端必须加载包含相应键名的 Resourcepack，否则原版文本管线将输出键名本体作为安全回退。
