# Epithet - 功能机制与指令参考手册

本文档提供 Epithet 模组的完整功能机制说明、原生界面使用指南、游戏内控制台指令表以及配置项参考。

---

## 1. 核心渲染与状态机制

### 1.1 头顶名牌渲染 (NameTag Rendering)
本模组监听 `RenderNameTagEvent` 直接介入渲染管线，而非传统计分板队伍（Scoreboard Team）前缀注入：
- **潜行弱化**：玩家蹲伏潜行时，称号跟随名称进入半透明弱化模式 (`isDiscrete`)。
- **隐身隐藏**：玩家隐身时，若对观察者不可见 (`isInvisibleTo`)，称号彻底隐藏。
- **深度与裁剪**：遵循原生深度测试 (`seeThrough`) 与 64 格最大视距裁剪。
- **队伍规则**：严格继承原版计分板队伍的可见性规则 (`NameTagVisibility`)。

### 1.2 聊天栏与 Tab 列表联动
- **发言前缀**：玩家在公屏聊天时自动附加 `<「称号」 玩家名> 消息`。
- **成就式悬停卡片**：鼠标悬浮在聊天栏称号上时弹出原版成就风格的浮窗，展示称号名称、稀有度与详细描述（可在客户端配置中开启/关闭）。
- **Tab 列表同步**：在线玩家列表（Tab 栏）、死亡消息及系统命令反馈中同步显示称号。

### 1.3 权限与服务端锁定
- 玩家数据通过 NeoForge Data Attachments 序列化存储，附带 `copyOnDeath()`，重生不丢失。
- 管理员可强制锁定玩家称号，锁定状态下客户端无法自行更换或卸下。

---

## 2. 界面与交互指南 (GUI)

- **暂停菜单快捷按钮**：
  - 位于暂停界面 (`PauseScreen`) 右上方，使用原版命名牌图标 (`Items.NAME_TAG`)。
  - 坐标固定在 `y = height / 4 + 24`，与常见模组（如 *Redeem Gift Codes*）保持安全垂直间距，避免重叠。
- **称号选择菜单 (`/epithet gui`)**：
  - 展示玩家已解锁拥有的所有称号列表。
  - 普通玩家底部仅展示居中的 `[完成]` 按钮。
  - 拥有 OP 等级 2+ 的管理员底部额外显示 `[玩家管理]` 按钮。
- **玩家称号管理器**：
  - 管理员可浏览所有在线玩家的称号状态。
  - 支持在线给指定玩家赋予、收回、强制佩戴及锁定称号。

---

## 3. 指令参考总表

根指令为 `/epithet`（未占用 `/title` 别名以避免与原版屏幕字幕指令冲突）。

### 3.1 玩家指令
| 指令 | 权限等级 | 说明 |
| :--- | :---: | :--- |
| `/epithet gui` | 所有人 (0) | 打开原版风格称号选择界面 |
| `/epithet list` | 所有人 (0) | 在聊天栏列出已拥有的称号与佩戴状态 |

### 3.2 管理员指令
| 指令 | 权限等级 | 说明 |
| :--- | :---: | :--- |
| `/epithet admin create <id> <name> [color] [desc]` | OP 2+ | 创建新的动态世界称号 |
| `/epithet admin give <targets> <id>` | OP 2+ | 为目标玩家授予指定称号 |
| `/epithet admin take <targets> <id>` | OP 2+ | 剥夺目标玩家已有的指定称号 |
| `/epithet admin set <targets> <id>` | OP 2+ | 强制为目标玩家佩戴指定称号 |
| `/epithet admin lock <targets>` | OP 2+ | 锁定指定玩家的称号栏（禁止自行更换） |
| `/epithet admin unlock <targets>` | OP 2+ | 解除指定玩家的称号锁定状态 |

### 3.3 视效测试假人指令 (用于本地联调)
| 指令 | 权限等级 | 说明 |
| :--- | :---: | :--- |
| `/epithet test dummy spawn [name] [title]` | OP 2+ | 在玩家面前生成测试假人并佩戴称号 |
| `/epithet test dummy sneak <true\|false>` | OP 2+ | 切换假人潜行状态（检验半透明弱化） |
| `/epithet test dummy invisible <true\|false>` | OP 2+ | 切换假人隐身状态（检验是否完全隐形） |
| `/epithet test dummy title <title_id>` | OP 2+ | 动态更换假人佩戴的称号 |
| `/epithet test dummy look` | OP 2+ | 令假人面朝当前玩家视角 |
| `/epithet test dummy remove` | OP 2+ | 清除附近的测试假人 |

---

## 4. 配置文件规范

### 4.1 客户端配置 (`config/epithet-client.toml`)
```toml
[rendering]
    # 悬停在聊天栏称号上时是否显示类似成就的详细描述卡片
    enableChatHoverTooltip = true
    # 是否自动添加包围符（若文本已包含括号则不会二次包裹）
    wrapWithBrackets = true
    # 左侧/起始包围符号（默认为 「）
    prefixBracket = "「"
    # 右侧/结束包围符号（默认为 」）
    suffixBracket = "」"
```

### 4.2 服务端配置 (`config/epithet-common.toml`)
```toml
[default_title]
    # 是否对新进服玩家开启内置初始称号
    enableDefaultTitle = true
    # 首次进入且无佩戴称号时是否自动佩戴
    autoEquipDefaultTitle = true
    # 初始默认称号 ID
    defaultTitleId = "epithet:beginner"
    # 初始默认称号展示文本
    defaultTitleDisplayName = "初出茅庐"
    # 初始默认称号描述文本
    defaultTitleDescription = "初入方块世界的懵懂冒险家。"
    # 初始默认称号颜色（支持十六进制、§代码或颜色名）
    defaultTitleColor = "green"
```

---

## 5. 数据包定义快速参考

详细规范与高级范例请参见 [docs/title_configuration_guide.md](title_configuration_guide.md)。

数据包路径：`data/<namespace>/titles/<title_id>.json`
```json
{
  "id": "example:slayer",
  "displayName": "屠龙勇者",
  "description": "击败终末之龙的勇者",
  "color": "light_purple",
  "priority": 100,
  "rarity": "epic",
  "advancement": "minecraft:end/kill_dragon",
  "defaultUnlocked": false
}
```
