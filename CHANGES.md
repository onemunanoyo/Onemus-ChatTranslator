# Changes

## [Tr?] ボタンが表示されない問題の修正

### `ChatHudMixin.java`
**ボタン表示条件の変更**
```diff
- if (!config.enabled) {
+ if (config.apiKey.isBlank()) {
      return message;
  }
```
APIキーが設定されていれば、自動翻訳のON/OFFに関係なく `[Tr?]` ボタンを表示するよう変更。
`/trt` コマンド自体は `enabled` を見ていないため、ボタン表示だけ `enabled` に依存するのは設計上の矛盾だった。

---

### `TranslatorScreen.java`
**GUI トグルのサイレント失敗を修正**
```diff
  if (!config.enabled && config.apiKey.isBlank()) {
+     if (client != null && client.player != null)
+         client.player.sendMessage(I18n.translate("translatormod.command.on.no_api"), true);
      return;
  }
```
APIキー未設定でトグルを押した際、何も起きない（無言失敗）だった挙動を修正。
アクションバーにエラーメッセージを表示するようにした。

---

### `TranslatorConfig.java`
**`hypixelAllChat` の既存ユーザー向けマイグレーション**
```diff
- if (!VALID_CHANNEL_MODES.contains(hypixelAllChat)) hypixelAllChat = "CLICK";
+ if (!VALID_CHANNEL_MODES.contains(hypixelAllChat) || "OFF".equals(hypixelAllChat)) hypixelAllChat = "CLICK";
```
デフォルト値が誤って `"OFF"` になっていたため、既存ユーザーの設定ファイルに `"OFF"` が残っていた。
`validate()` で `"OFF"` を `"CLICK"` に強制移行するよう変更。
（新規ユーザー向けのデフォルト変更は前回対応済み）

---

### `TranslatorModClient.java`
**Hypixelモード時のバニラ形式メッセージ対応**
```diff
  if (text.contains(": "))                                           return config.hypixelAllChat;
+ // バニラ形式 <PlayerName> message もAll Chatとして扱う
+ if (text.startsWith("<") && text.contains("> "))                  return config.hypixelAllChat;

  return "OFF";
```
Hypixelモード ON の状態でバニラ形式 `<Player> message` のメッセージがどのチャンネルパターンにもマッチせず、ハードコードされた `"OFF"` にフォールスルーしていた。
`<Player>` 形式を All Chat として扱うよう追加。

