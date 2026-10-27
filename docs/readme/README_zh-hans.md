# ChatTranslator — Minecraft 26.2 (Fabric)

使用 DeepL 翻译 Minecraft 聊天与物品提示的客户端模组。

此分支适用于 Minecraft 26.2 和 Fabric。

[English](../../README.md) · [日本語](README_ja.md) · [简体中文](README_zh-hans.md) · [한국어](README_ko.md) · [Deutsch](README_de.md) · [Español](README_es.md) · [Français](README_fr.md) · [Português (Brasil)](README_pt-br.md) · [Tiếng Việt](README_vi.md) · [ไทย](README_th.md)

## 主要功能

可自动或手动翻译符合条件的聊天消息、翻译物品提示，并设置 Hypixel 各频道的模式。忽略列表采用不区分大小写的完全匹配。

## 开始使用

请在对应版本的 Minecraft 和加载器中安装模组。需要 DeepL API 密钥及网络连接。

按 J 键（默认）打开设置，输入 API 密钥并设置目标语言。

## 命令

- `/tr <text>`: 翻译文本并仅向自己显示结果。
- `/trt <text>`: 翻译文本并将结果放入聊天输入框。
- `/translate on` / `/translate off`: 启用或关闭自动翻译。
- `/translate status`: 显示当前翻译设置。
- `/translate key <API_KEY>`: 设置 DeepL API 密钥。
- `/translate rule add <source> <target>`: 添加源语言到目标语言的规则。
- `/translate rule list`: 列出已配置的规则。

## 更多信息

其他版本请查看 main 页面，并在 Modrinth 选择对应下载。

[Main](https://github.com/onemunanoyo/Onemus-ChatTranslator/tree/main) · [Modrinth](https://modrinth.com/mod/onemus-chattranslator/versions)

本项目非官方，与 DeepL SE、Mojang 或 Microsoft 无关。

## 许可证

[Apache License 2.0](../../LICENSE)
