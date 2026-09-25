<div align="center">

# Epithet (铭衔)

*专为 Minecraft 1.21.1 (NeoForge) 设计的现代原版风格玩家称号系统模组。*

[简体中文](README_CN.md) · [English](README.md)

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-brightgreen.svg?style=flat-square&logo=minecraft)](https://minecraft.net/)
[![NeoForge](https://img.shields.io/badge/NeoForge-21.1.248-orange.svg?style=flat-square)](https://neoforged.net/)
[![Java](https://img.shields.io/badge/Java-21-blue.svg?style=flat-square&logo=openjdk)](https://adoptium.net/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg?style=flat-square)](LICENSE)

</div>

---

## 🌟 特性亮点

不同于传统通过计分板队伍（Scoreboard Team）修改前缀的实现方式，**Epithet** 直接介入实体渲染管线与文本格式化系统：

- **🎯 头顶渲染原生对齐**：监听 `RenderNameTagEvent`，完全继承原版潜行半透明弱化 (`isDiscrete`)、隐身完全隐藏 (`isInvisibleTo`)、视距深度遮挡 (`seeThrough`) 及队伍可见性规则。
- **💬 聊天与 Tab 列表联动**：公屏聊天呈现 `<「称号」 玩家名> 消息` 前缀，鼠标悬停弹出类似原版成就的富文本卡片；Tab 在线玩家列表与死亡消息实时同步。
- **🖥️ 原版风格交互界面**：暂停菜单注入防冲突的命名牌图标快捷按钮 (`PauseScreen`)；包含玩家称号选择器及管理员 CRUD 玩家管理控制台。
- **📦 数据驱动与成就解锁**：全面支持通过数据包（Datapack `data/<namespace>/titles/*.json`）配置，支持 `/reload` 热重载；支持绑定原版进度（Advancement）自动触发解锁。
- **🔒 服务端权威锁定保护**：支持服主与管理员对指定玩家锁定佩戴状态，杜绝客户端绕过篡改。
- **🎨 丰富色彩与流光渐变**：全面支持十六进制颜色 (`#FFAA00`)、原版样式代码 (`§6`)、静态多色线性平滑渐变 (`<gradient:...>`) 及 60/144 FPS 高刷动态流光波浪 (`<animated-gradient:...>`, `<animated-rainbow>`)，支持自定义前后包围符。

---

## 📚 文档导航

详细的技术规格说明、配置参数与操作指令已归档至 [docs/](docs/) 目录：

| 文档手册 | 说明内容 |
| :--- | :--- |
| 📖 **[功能机制与指令参考手册](docs/features.md)** | 包含全量游戏内指令、GUI 交互细节、视效测试假人及配置文件详解。 |
| 📖 **[称号添加与配置规范指南](docs/title_configuration_guide.md)** | 包含数据驱动 (Datapack) JSON 规范、Schema 约束、动态创建与故障排查。 |

---

## 🚀 快速上手

### 玩家操作
- 按 <kbd>ESC</kbd> 打开暂停界面，点击右上角**命名牌图标**；或直接在聊天栏输入 `/epithet gui`。
- 在聊天栏输入 `/epithet list` 可检视当前已解锁的称号与佩戴状态。

### 管理员操作 (OP 2+)
```bash
# 授予 / 收回称号
/epithet admin give <玩家> <称号ID>
/epithet admin take <玩家> <称号ID>

# 强制佩戴 / 锁定佩戴栏
/epithet admin set <玩家> <称号ID>
/epithet admin lock <玩家>
/epithet admin unlock <玩家>
```
*更多完整语法与视效测试假人操作指令，请参阅 [docs/features.md](docs/features.md)。*

---

## 🛠️ 构建与开发

```bash
# 编译 Java 源码
./gradlew compileJava

# 应用标准代码格式化 (Spotless)
./gradlew spotlessApply

# 生成四国语言本地化资源 (zh_cn, zh_tw, en_us, en_gb)
./gradlew generateLang

# 在开发环境中启动客户端测试
./gradlew runClient
```

---

## 📄 开源协议

本项目基于 [MIT License](LICENSE) 许可协议开源。
