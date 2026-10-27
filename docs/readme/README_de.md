# ChatTranslator — Minecraft 1.21.11 (Fabric)

Eine clientseitige Minecraft-Mod, die Chat und Gegenstands-Tooltips mit DeepL übersetzt.

Dieser Branch ist für Minecraft 1.21.11 mit Fabric.

[English](../../README.md) · [日本語](README_ja.md) · [简体中文](README_zh-hans.md) · [한국어](README_ko.md) · [Deutsch](README_de.md) · [Español](README_es.md) · [Français](README_fr.md) · [Português (Brasil)](README_pt-br.md) · [Tiếng Việt](README_vi.md) · [ไทย](README_th.md)

## Funktionen

Geeignete Chatnachrichten lassen sich automatisch oder per Klick übersetzen; außerdem gibt es Tooltip-Übersetzung und Hypixel-Kanaleinstellungen. Die Ignorierliste verwendet exakte Vergleiche ohne Beachtung der Groß- und Kleinschreibung.

## Erste Schritte

Installiere die Mod mit der passenden Minecraft-Version und dem passenden Loader. Ein DeepL-API-Schlüssel und Internet sind erforderlich.

Öffne die Einstellungen mit J (Standard), gib den API-Schlüssel ein und wähle die Zielsprachen.

## Befehle

- `/tr <text>`: Übersetzt Text und zeigt das Ergebnis nur dir an.
- `/trt <text>`: Übersetzt Text und setzt das Ergebnis in die Chat-Eingabe.
- `/translate on` / `/translate off`: Schaltet die automatische Übersetzung ein oder aus.
- `/translate status`: Zeigt die aktuellen Übersetzungseinstellungen an.
- `/translate key <API_KEY>`: Legt den DeepL-API-Schlüssel fest.
- `/translate rule add <source> <target>`: Fügt eine Regel für Quell- und Zielsprache hinzu.
- `/translate rule list`: Listet die eingerichteten Regeln auf.

## Weitere Informationen

Andere Versionen stehen in der Übersicht auf main; passende Downloads findest du auf Modrinth.

[Main](https://github.com/onemunanoyo/Onemus-ChatTranslator/tree/main) · [Modrinth](https://modrinth.com/mod/onemus-chattranslator/versions)

Dies ist ein inoffizielles Projekt ohne Verbindung zu DeepL SE, Mojang oder Microsoft.

## Lizenz

[Apache License 2.0](../../LICENSE)
