# ChatTranslator — Minecraft 26.1 (NeoForge)

DeepL を使って Minecraft のチャットとアイテムのツールチップを翻訳するクライアント MOD です。

このブランチは Minecraft 26.1・NeoForge 向けです。

NeoForge の旧版です。Fabric 版とは動作やメンテナンス状況が異なります。

[English](../../README.md) · [日本語](README_ja.md) · [简体中文](README_zh-hans.md) · [한국어](README_ko.md) · [Deutsch](README_de.md) · [Español](README_es.md) · [Français](README_fr.md) · [Português (Brasil)](README_pt-br.md) · [Tiếng Việt](README_vi.md) · [ไทย](README_th.md)

## 主な機能

対象チャットを自動またはクリックで翻訳でき、アイテムのツールチップや Hypixel のチャンネルごとの動作も設定できます。無視リストは大文字・小文字を区別しない完全一致です。

## 使い始める

対応する Minecraft とローダーに MOD を導入してください。DeepL API キーとインターネット接続が必要です。

J キー（初期設定）で設定画面を開き、API キーと翻訳先言語を設定します。

## コマンド

- `/trt <text>`: 翻訳結果を自分だけに表示します。
- `/tr <text>`: 翻訳結果をチャット入力欄に入れます。
- `/translate on` / `/translate off`: 自動翻訳のオン・オフを切り替えます。
- `/translate status`: 現在の翻訳設定を表示します。
- `/translate key <API_KEY>`: DeepL API キーを設定します。
- `/translate rule add <source> <target>`: 翻訳元と翻訳先のルールを追加します。
- `/translate rule list`: 設定済みのルールを一覧表示します。

## その他の情報

ほかのバージョンは main の案内を参照し、配布ファイルは Modrinth で選んでください。

[Main](https://github.com/onemunanoyo/Onemus-ChatTranslator/tree/main) · [Modrinth](https://modrinth.com/mod/onemus-chattranslator/versions)

この MOD は非公式で、DeepL SE、Mojang、Microsoft とは関係ありません。

## ライセンス

[Apache License 2.0](../../LICENSE)
