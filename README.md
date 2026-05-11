# MMod

MMod 这是一个基于 Fabric 的 Minecraft 客户端模组。通过集成 Cobalt 引擎，在游戏内引入了 Lua 脚本运行环境，允许玩家或开发者通过编写 Lua 脚本来与游戏客户端进行交互。

## 功能介绍

*   **资源包驱动脚本加载**：模组会通过资源包管理器加载 Lua 脚本。你可以像加载材质包一样加载和管理你的脚本，或可从服务端像客户端发布 Lua 脚本供客户端执行。
*   **Lua 客户端 API**：模组向 Lua 环境中注入了多个自定义 API，用于实现与 Minecraft 客户端的交互：
    *   `ConsoleAPI`: 控制台交互及日志输出
    *   `WorldAPI`: 操控当前游戏世界的数据和状态
    *   `RenderAPI`: 客户端自定义渲染功能
    *   `EventsAPI`: 监听和处理游戏内事件
*   **自定义网络通信**：基于 Minecraft 网络协议的自定义数据包 `mmod:lua_packet`，支持带有频道分类 (`luaChannel`) 的字符串数据 (`data`) 传输

## 使用说明

1. 模组启动或资源包重载时，会自动寻找并执行以下路径的入口脚本：
   ```text
   assets/mmod/script/main.lua
   ```
2. 你可以在资源包的上述路径创建 `main.lua` 文件，并开始编写你的代码。
