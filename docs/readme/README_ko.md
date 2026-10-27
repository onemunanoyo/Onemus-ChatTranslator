# ChatTranslator — Minecraft 26.2 (Fabric)

DeepL을 사용해 Minecraft 채팅과 아이템 툴팁을 번역하는 클라이언트 모드입니다.

이 브랜치는 Minecraft 26.2 및 Fabric용입니다.

[English](../../README.md) · [日本語](README_ja.md) · [简体中文](README_zh-hans.md) · [한국어](README_ko.md) · [Deutsch](README_de.md) · [Español](README_es.md) · [Français](README_fr.md) · [Português (Brasil)](README_pt-br.md) · [Tiếng Việt](README_vi.md) · [ไทย](README_th.md)

## 주요 기능

대상 채팅을 자동 또는 클릭으로 번역하고, 아이템 툴팁과 Hypixel 채널별 동작을 설정할 수 있습니다. 무시 목록은 대소문자를 구분하지 않고 완전히 일치할 때 적용됩니다.

## 시작하기

맞는 Minecraft 버전과 로더에 모드를 설치하세요. DeepL API 키와 인터넷 연결이 필요합니다.

J 키(기본값)로 설정을 열고 API 키와 번역 대상 언어를 설정하세요.

## 명령어

- `/tr <text>`: 텍스트를 번역해 자신에게만 표시합니다.
- `/trt <text>`: 텍스트를 번역해 채팅 입력창에 넣습니다.
- `/translate on` / `/translate off`: 자동 번역을 켜거나 끕니다.
- `/translate status`: 현재 번역 설정을 표시합니다.
- `/translate key <API_KEY>`: DeepL API 키를 설정합니다.
- `/translate rule add <source> <target>`: 원본 언어와 대상 언어의 규칙을 추가합니다.
- `/translate rule list`: 설정된 규칙을 나열합니다.

## 추가 정보

다른 버전은 main 안내를 확인하고 Modrinth에서 맞는 파일을 선택하세요.

[Main](https://github.com/onemunanoyo/Onemus-ChatTranslator/tree/main) · [Modrinth](https://modrinth.com/mod/onemus-chattranslator/versions)

비공식 프로젝트이며 DeepL SE, Mojang, Microsoft와 관련이 없습니다.

## 라이선스

[Apache License 2.0](../../LICENSE)
