# ChatTranslator — Minecraft 1.21.11 (Fabric)

Client-side Minecraft mod for chat and item tooltip translation with DeepL.

This branch targets Minecraft 1.21.11 with Fabric.

[English](README.md) · [日本語](docs/readme/README_ja.md) · [简体中文](docs/readme/README_zh-hans.md) · [한국어](docs/readme/README_ko.md) · [Deutsch](docs/readme/README_de.md) · [Español](docs/readme/README_es.md) · [Français](docs/readme/README_fr.md) · [Português (Brasil)](docs/readme/README_pt-br.md) · [Tiếng Việt](docs/readme/README_vi.md) · [ไทย](docs/readme/README_th.md)

## Features

Translate eligible chat messages automatically or on demand, translate item tooltips, and configure Hypixel channel modes. The ignore list uses exact, case-insensitive matching.

## Getting started

Install the mod with the matching loader and Minecraft version. A DeepL API key and an internet connection are required.

Press J (default) to open settings, enter your API key, and configure the target languages.

## Commands

- `/tr <text>`: Translate text and display it only to you.
- `/trt <text>`: Translate text and place the result in the chat input.
- `/translate on` / `/translate off`: Enable or disable automatic translation.
- `/translate status`: Show the current translation settings.
- `/translate key <API_KEY>`: Set the DeepL API key.
- `/translate rule add <source> <target>`: Add a source-to-target language rule.
- `/translate rule list`: List the configured rules.

## More information

Browse other versions in the main branch or select the matching download on Modrinth.

[Main](https://github.com/onemunanoyo/Onemus-ChatTranslator/tree/main) · [Modrinth](https://modrinth.com/mod/onemus-chattranslator/versions)

This is an unofficial project, not affiliated with DeepL SE, Mojang, or Microsoft.

## License

[Apache License 2.0](LICENSE)
