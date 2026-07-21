# メモ帳アプリ（従来型アーキテクチャ）

伝統的な3層レイヤーアーキテクチャで構築されたメモ帳Webアプリケーションです。
開発未経験者が参照・改修することを目的としており、コード全体に詳細なコメントを記載しています。

## 目次

- [クイックスタート](#クイックスタート)
- [概要](#概要)
- [技術スタック](#技術スタック)
- [アーキテクチャ](#アーキテクチャ)
- [前提条件](#前提条件)
- [環境構築](#環境構築)
- [アプリケーションの起動](#アプリケーションの起動)
- [使い方](#使い方)
- [プロジェクト構成](#プロジェクト構成)
- [開発ガイド](#開発ガイド)
- [トラブルシューティング](#トラブルシューティング)

## クイックスタート

**とにかく最短で動かしてみたい方へ：**

### 🐳 Docker方式（推奨）

**必要なもの：** Docker Desktop のみ

```bash
# 1. 全てのサービスを起動（PostgreSQL + アプリケーション）
docker compose up --build

# 2. ブラウザでアクセス
# http://localhost:8080/memos
```

**バックグラウンド起動する場合：**
```bash
docker compose up -d --build
```

**停止する場合：**
```bash
docker compose down
```

**ログ確認：**
```bash
# 全体のログ
docker compose logs -f

# アプリケーションのログのみ
docker compose logs -f app
```

---

### 💻 ローカル開発方式（従来型）

**必要なもの：** Java 17、Maven、Docker Desktop

```bash
# 1. PostgreSQLのみ起動
docker compose up -d postgres

# 2. アプリケーションをローカルで起動
mvn spring-boot:run

# 3. ブラウザでアクセス
# http://localhost:8080/memos
```

---

**起動成功の確認：**
- Docker方式: `docker compose logs app` で `Started MemoAppApplication` と表示される
- ローカル方式: コンソールに `Started MemoAppApplication in X.XXX seconds` と表示される
- ブラウザで http://localhost:8080/memos にアクセスするとメモ一覧画面が表示される

**トラブル時は：**
- [前提条件](#前提条件) を確認
- [トラブルシューティング](#トラブルシューティング) を参照

---

## 概要

このアプリケーションは、メモの作成・表示・編集・削除ができるシンプルなWebアプリケーションです。

### 主な機能

- ✅ メモの一覧表示
- ✅ メモの新規作成
- ✅ メモの詳細表示
- ✅ メモの編集
- ✅ メモの削除
- ✅ キーワード検索
- ✅ レスポンシブデザイン（スマートフォン対応）

## 技術スタック

### バックエンド

- **Java 17** - プログラミング言語
- **Spring Boot 3.5.16** - Webアプリケーションフレームワーク
- **MyBatis Spring Boot Starter 3.0.5** (MyBatis 3.5系) - SQLマッパーフレームワーク（2waySQL）
- **PostgreSQL 16** - リレーショナルデータベース
- **Flyway 11**（Spring Boot管理） - データベースマイグレーションツール（アプリ起動時に自動実行）
- **Lombok** - ボイラープレートコード削減

### フロントエンド

- **Thymeleaf** - サーバーサイドテンプレートエンジン（共通部品は th:fragment で共通化）
- **HTML5/CSS3** - マークアップ・スタイリング
- **JavaScript (Vanilla)** - クライアントサイドスクリプト（jQuery等のライブラリは不使用）

### ビルドツール

- **Maven** - プロジェクト管理・ビルドツール

### 開発ツール

- **Docker / Docker Compose** - PostgreSQLのコンテナ実行
- **Spring Boot DevTools** - 開発時の自動リロード

## アーキテクチャ

### 3層レイヤーアーキテクチャ

```
┌─────────────────────────────────┐
│  プレゼンテーション層          │
│  (Controller, Thymeleaf)        │
└─────────────────────────────────┘
            ↓↑
┌─────────────────────────────────┐
│  ビジネスロジック層            │
│  (Service)                      │
└─────────────────────────────────┘
            ↓↑
┌─────────────────────────────────┐
│  データアクセス層              │
│  (Mapper, MyBatis, PostgreSQL)  │
└─────────────────────────────────┘
```

### データフロー

1. **ユーザー** → Controller（HTTPリクエスト）
2. **Controller** → Service（ビジネスロジック呼び出し）
3. **Service** → Mapper（データベースアクセス）
4. **Mapper** → PostgreSQL（SQLクエリ実行）
5. **PostgreSQL** → Mapper（結果取得）
6. **Mapper** → Service（Entity返却）
7. **Service** → Controller（DTO返却）
8. **Controller** → Thymeleaf（Model設定）
9. **Thymeleaf** → ユーザー（HTMLレンダリング）

## 前提条件

### 🐳 Docker方式（推奨）の場合

- **Docker Desktop**
  - インストール確認: `docker --version` および `docker compose version`
  - [Docker Desktop](https://www.docker.com/products/docker-desktop/)
  - **これだけで動きます！** Java、Mavenのインストールは不要です

### 💻 ローカル開発方式の場合

以下全てが必要です：

- **JDK 17以上**
  - インストール確認: `java -version`
  - [Oracle JDK](https://www.oracle.com/java/technologies/downloads/) または [OpenJDK](https://adoptium.net/)

- **Maven 3.6以上**
  - インストール確認: `mvn -version`
  - [Apache Maven](https://maven.apache.org/download.cgi)

- **Docker Desktop**
  - インストール確認: `docker --version`
  - [Docker Desktop](https://www.docker.com/products/docker-desktop/)
  - （PostgreSQL用）

### 推奨

- **IDE (統合開発環境)**
  - [IntelliJ IDEA](https://www.jetbrains.com/idea/) (Community版で十分)
  - [Eclipse](https://www.eclipse.org/downloads/)
  - [Visual Studio Code](https://code.visualstudio.com/) + Java拡張機能

- **Git**
  - バージョン管理ツール
  - インストール確認: `git --version`

## 環境構築

### 1. リポジトリのクローン

```bash
git clone https://github.com/fukt-dev/learn-memo-app-traditional.git
cd learn-memo-app-traditional
```

### 2. 起動方法の選択

#### 🐳 方法A: Docker方式（推奨）

**全てをDockerで実行**

```bash
# 全サービスを起動（ビルド含む）
docker compose up --build

# バックグラウンド起動の場合
docker compose up -d --build
```

**初回起動時の流れ：**
1. Dockerfileに基づいてJavaアプリケーションのイメージをビルド（5-10分）
2. PostgreSQLコンテナを起動
3. アプリケーションコンテナを起動
4. データベースマイグレーションを自動実行

**起動確認:**
```bash
# コンテナの状態確認
docker compose ps

# ログ確認
docker compose logs -f app
```

ブラウザで http://localhost:8080/memos にアクセス

---

#### 💻 方法B: ローカル開発方式

**PostgreSQLのみDockerで実行、アプリケーションはローカルで実行**

##### B-1. PostgreSQLの起動

```bash
# PostgreSQLコンテナのみ起動
docker compose up -d postgres

# 起動確認
docker compose ps postgres

# ログ確認
docker compose logs -f postgres
```

**起動成功の確認:**
```
NAME                IMAGE                COMMAND                  SERVICE    CREATED         STATUS         PORTS
memoapp-postgres    postgres:16-alpine   "docker-entrypoint.s…"   postgres   5 seconds ago   Up 4 seconds   0.0.0.0:5432->5432/tcp
```

##### B-2. 依存関係のダウンロード

```bash
mvn clean install
```

初回実行時は、必要なライブラリがダウンロードされるため時間がかかります。

##### B-3. アプリケーションの起動

**オプション1: Mavenコマンドで起動**
```bash
mvn spring-boot:run
```

**オプション2: IDEから起動**
1. `src/main/java/com/example/memoapp/MemoAppApplication.java` を開く
2. `main()` メソッドの左側の緑色の▶ボタンをクリック
3. "Run 'MemoAppApplication'" を選択

**オプション3: JARファイルを作成して起動**
```bash
# JARファイルをビルド
mvn clean package

# JARファイルを実行
java -jar target/memo-app-1.0.0-SNAPSHOT.jar
```

**起動確認:**
```
Started MemoAppApplication in 3.456 seconds (JVM running for 4.123)
Tomcat started on port(s): 8080 (http) with context path ''
```

ブラウザで http://localhost:8080/memos にアクセス

---

### 3. データベースの確認（オプション）

PostgreSQLに接続してデータベースを確認できます。

```bash
# PostgreSQLコンテナに接続
docker compose exec postgres psql -U memoapp_user -d memoapp

# テーブル一覧を表示
\dt

# 接続を終了
\q
```

## 使い方

### メモの新規作成

1. トップページの「新規作成」ボタンをクリック
2. タイトルと本文を入力
3. 「登録」ボタンをクリック

### メモの表示

- 一覧画面で任意のメモのタイトルをクリック

### メモの編集

1. 詳細画面で「編集」ボタンをクリック
2. タイトルや本文を修正
3. 「更新」ボタンをクリック

### メモの削除

1. 詳細画面で「削除」ボタンをクリック
2. 確認ダイアログで「OK」をクリック

### キーワード検索

1. 一覧画面の検索ボックスにキーワードを入力
2. 「検索」ボタンをクリック

## プロジェクト構成

```
learn-memo-app-traditional/
├── docs/                               # ドキュメント
│   ├── 設計書.md                       # アプリケーション設計書
│   ├── 環境構築手順.md                 # 詳細な環境構築手順
│   ├── 開発ガイド.md                   # 開発ガイド
│   ├── MyBatis2waySQL解説.md           # MyBatis 2waySQL解説
│   ├── Webアプリケーション開発ロードマップ.md  # 学習全体構想
│   └── レビュー.md                     # ドキュメントレビュー記録
│
├── src/
│   ├── main/
│   │   ├── java/com/example/memoapp/
│   │   │   ├── MemoAppApplication.java          # Spring Bootアプリケーションのエントリーポイント
│   │   │   ├── controller/
│   │   │   │   └── MemoController.java          # プレゼンテーション層
│   │   │   ├── service/
│   │   │   │   └── MemoService.java             # ビジネスロジック層
│   │   │   ├── mapper/
│   │   │   │   └── MemoMapper.java              # データアクセス層（インターフェース）
│   │   │   ├── entity/
│   │   │   │   └── Memo.java                    # エンティティクラス
│   │   │   ├── dto/
│   │   │   │   └── MemoDto.java                 # データ転送オブジェクト
│   │   │   └── exception/
│   │   │       ├── MemoNotFoundException.java   # メモ未発見例外（404）
│   │   │       └── GlobalExceptionHandler.java  # グローバル例外ハンドラ
│   │   │
│   │   └── resources/
│   │       ├── application.yml                   # アプリケーション設定
│   │       ├── db/migration/
│   │       │   └── V1__Create_memos_table.sql    # Flywayマイグレーション
│   │       ├── mybatis/mapper/
│   │       │   └── MemoMapper.xml                # MyBatis 2waySQL定義
│   │       ├── templates/                        # Thymeleafテンプレート
│   │       │   ├── fragments/
│   │       │   │   └── parts.html                # 共通ヘッダー・フッター（th:fragment）
│   │       │   ├── memos/
│   │       │   │   ├── list.html                 # メモ一覧画面
│   │       │   │   ├── new.html                  # メモ新規作成画面
│   │       │   │   ├── edit.html                 # メモ編集画面
│   │       │   │   └── show.html                 # メモ詳細画面
│   │       │   └── error/
│   │       │       └── error.html                # エラー画面
│   │       └── static/
│   │           ├── css/
│   │           │   └── style.css                 # スタイルシート
│   │           └── js/
│   │               └── script.js                 # JavaScript（Vanilla）
│   │
│   └── test/java/com/example/memoapp/             # テストコード（全30テスト、DB不要）
│       ├── controller/MemoControllerTest.java     # @WebMvcTest スライステスト
│       ├── service/MemoServiceTest.java           # Mockito 単体テスト
│       └── dto/MemoDtoTest.java                   # DTO変換テスト
│
├── Dockerfile                                     # アプリのマルチステージビルド
├── docker-compose.yml                             # Docker Compose設定
├── pom.xml                                        # Maven設定
└── README.md                                      # このファイル
```

## 開発ガイド

### コーディング規約

- **Java**: Googleのコーディング規約に準拠
- **SQL**: 大文字で記述（SELECT, FROM, WHERE など）
- **HTML/CSS**: BEM記法を参考にクラス名を設定
- **コメント**: 日本語で詳細に記述（学習用のため）

### Git運用

#### ブランチ戦略

- `main`: 安定版。**直接コミットせず、ブランチ + Pull Request で作業する**（1 PR = 1 関心事）
- `feature/*`: 機能追加用
- `fix/*`: バグ修正用
- `docs/*`: ドキュメント修正用

#### コミットメッセージ

```
<type>: <subject>

<body>
```

**type の種類:**
- `feat`: 新機能
- `fix`: バグ修正
- `docs`: ドキュメント変更
- `style`: コードフォーマット
- `refactor`: リファクタリング
- `test`: テスト追加・修正
- `chore`: ビルドプロセスやツールの変更

**例:**
```
feat: メモ検索機能を追加

タイトルと本文に対するキーワード検索を実装
LIKE句を使用した部分一致検索
```

### データベース操作

#### マイグレーションの追加

新しいマイグレーションを追加する場合:

1. `src/main/resources/db/migration/` に新しいSQLファイルを作成
2. ファイル名: `V2__Description.sql` （バージョン番号を連番で）
3. アプリケーション再起動時に自動的に実行される

#### データベースのリセット

開発中にデータベースをリセットしたい場合:

```bash
# PostgreSQLコンテナとボリュームを削除
docker compose down -v
# または旧形式: docker-compose down -v

# PostgreSQLを再起動
docker compose up -d
# または旧形式: docker-compose up -d

# アプリケーションを再起動
mvn spring-boot:run
```

### ログ確認

#### アプリケーションログ

アプリケーション起動時のコンソールに出力されます。

ログレベルを変更する場合は、`application.yml` を編集:

```yaml
logging:
  level:
    com.example.memoapp: DEBUG  # DEBUG, INFO, WARN, ERROR
```

#### SQLログ

MyBatisが実行するSQLは自動的にコンソールに出力されます:

```
==>  Preparing: SELECT * FROM memos ORDER BY created_at DESC
==> Parameters:
<==      Total: 5
```

## トラブルシューティング

### 🐳 Docker方式のトラブルシューティング

#### 1. コンテナが起動しない

**状態確認:**
```bash
docker compose ps
docker compose logs
```

**よくある原因と対処法:**

**a) ポート競合**
```bash
# 既に8080番ポートが使用されている
# エラー例: "Bind for 0.0.0.0:8080 failed: port is already allocated"

# 解決策: docker-compose.ymlでポート変更
# ports:
#   - "8081:8080"  # ホスト側を8081に変更
```

**b) PostgreSQLの起動を待たずにアプリが起動**
```bash
# depends_on の healthcheck が機能していない場合
# 解決策: 一度停止して再起動
docker compose down
docker compose up --build
```

**c) ビルドエラー**
```bash
# Mavenのビルドが失敗
# ログ確認
docker compose logs app

# キャッシュをクリアして再ビルド
docker compose build --no-cache app
docker compose up app
```

#### 2. データベース接続エラー

**エラー例:**
```
Connection to postgres:5432 refused
```

**解決策:**
```bash
# PostgreSQLの状態確認
docker compose ps postgres

# healthyになるまで待つ
docker compose logs -f postgres

# データベースをリセット
docker compose down -v
docker compose up --build
```

#### 3. イメージのリビルドが反映されない

**ソースコード変更後:**
```bash
# 強制的に再ビルド
docker compose up --build --force-recreate
```

#### 4. 全てをリセットしたい

```bash
# コンテナ、ネットワーク、ボリューム全て削除
docker compose down -v

# イメージも削除
docker compose down -v --rmi all

# 再起動
docker compose up --build
```

---

### 💻 ローカル開発方式のトラブルシューティング

### アプリケーションが起動しない

#### 1. ポートが既に使用されている

**エラーメッセージ:**
```
Error: Port 8080 was already in use
```

**解決策:**

方法1: `application.yml` でポート番号を変更
```yaml
server:
  port: 8081  # 8080 から 8081 に変更
```

方法2: 8080番ポートを使用しているプロセスを終了
```bash
# Windowsの場合
netstat -ano | findstr :8080
taskkill /PID <PID> /F

# Mac/Linuxの場合
lsof -i :8080
kill -9 <PID>
```

#### 2. PostgreSQLに接続できない

**エラーメッセージ:**
```
Connection refused: localhost:5432
```

**解決策:**

```bash
# PostgreSQLの状態を確認
docker compose ps
# または旧形式: docker-compose ps

# 起動していない場合
docker compose up -d
# または旧形式: docker-compose up -d

# ログを確認
docker compose logs postgres
# または旧形式: docker-compose logs postgres
```

#### 3. Flywayマイグレーションエラー

**エラーメッセージ:**
```
FlywayException: Migration checksum mismatch
```

**解決策:**

データベースをリセット:
```bash
docker compose down -v
docker compose up -d
mvn spring-boot:run
```

### ビルドエラー

#### 1. 依存関係が見つからない

**エラーメッセージ:**
```
Could not resolve dependencies
```

**解決策:**

```bash
# Mavenのローカルリポジトリをクリーン
mvn clean

# 依存関係を再ダウンロード
mvn dependency:resolve

# ビルド
mvn clean install
```

#### 2. Lombokが動作しない

**エラーメッセージ:**
```
Cannot find symbol: method getId()
```

**解決策:**

IDEにLombokプラグインをインストール:

- **IntelliJ IDEA**: Settings → Plugins → "Lombok" を検索してインストール
- **Eclipse**: Lombokのjarファイルをダウンロードして実行

### ブラウザで画面が表示されない

#### 1. 404エラー

**原因**: URLが間違っている

**解決策:**
- 正しいURL: `http://localhost:8080/memos`
- ポート番号を変更した場合は、それに合わせる

#### 2. Whitelabel Error Page

**原因**: テンプレートファイルが見つからない

**解決策:**
- `src/main/resources/templates/` 配下にHTMLファイルが存在するか確認
- ファイル名とController のreturn文が一致しているか確認

## さらに学ぶために

### ドキュメント

このリポジトリの `docs/` フォルダには、以下の詳細なドキュメントがあります:

- **設計書.md**: アプリケーションの設計詳細（実装と同期済み）
- **環境構築手順.md**: より詳細な環境構築手順
- **開発ガイド.md**: 開発のベストプラクティス
- **MyBatis2waySQL解説.md**: MyBatisの使い方
- **Webアプリケーション開発ロードマップ.md**: 学習全体の構想（全リポジトリ共通）
- **レビュー.md**: ドキュメントレビューの記録と対応状況

### 公式ドキュメント

- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [MyBatis Documentation](https://mybatis.org/mybatis-3/)
- [Thymeleaf Documentation](https://www.thymeleaf.org/documentation.html)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)

## ライセンス

このプロジェクトは学習目的で作成されています。

## 問い合わせ

質問や問題がある場合は、Issueを作成してください。