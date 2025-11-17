# Learn Memo App (Traditional)

メモアプリケーションの学習用プロジェクト（従来型実装版）

## 概要

このプロジェクトは、メモアプリケーションを従来のWeb技術（HTML/CSS/JavaScript）を使用して実装する学習用プロジェクトです。

## 技術スタック

- HTML5
- CSS3
- Vanilla JavaScript
- Local Storage（データ保存用）

## 機能

- メモの作成・編集・削除
- メモの一覧表示
- ローカルストレージへのデータ保存
- 検索機能
- カテゴリー分類

## セットアップ

1. リポジトリのクローン
```bash
git clone https://github.com/fukt-dev/learn-memo-app-traditional.git
cd learn-memo-app-traditional
```

2. ブラウザで`index.html`を開く

## プロジェクト構造

```
learn-memo-app-traditional/
├── index.html        # メインHTMLファイル
├── css/              # スタイルシート
│   └── style.css
├── js/               # JavaScriptファイル
│   └── app.js
├── .gitignore
└── README.md
```

## 開発

### コーディング規約

- ES6+の機能を使用
- セマンティックHTMLを意識
- BEMによるCSS設計
- 関数型プログラミングのアプローチを採用

### ブランチ戦略

- `main`: 本番環境
- `develop`: 開発環境
- `feature/*`: 機能開発

## ライセンス

MIT