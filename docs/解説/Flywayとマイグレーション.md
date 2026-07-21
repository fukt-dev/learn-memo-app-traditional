# Flyway とマイグレーション

`src/main/resources/db/migration/V1__Create_memos_table.sql` が起動時に何をしているのか、
その背景をまとめます。

## この記事で分かること

- なぜ DB のスキーマ(表の設計)をコードとして管理するのか
- `V` 番号・適用順・`flyway_schema_history` と checksum の関係
- 「適用済みのマイグレーションは書き換えない」という鉄則
- DDL と DML、制約、インデックスなど SQL の最小知識
- トリガーによる `updated_at` の自動更新

## なぜスキーマをコード管理するのか

テーブルを作る `CREATE TABLE` を各自が手動実行していると、「自分の環境では動くが
他人の環境では列が足りない」「本番でどこまで変更を適用したか誰も分からない」と
いった食い違いが起きます。

Flyway は、スキーマの変更を **番号付きの SQL ファイル** として Git 管理し、
アプリ起動時に「まだ適用していないものだけ」を自動実行します。

| 観点 | 手動実行 | Flyway |
|------|----------|--------|
| 変更の記録 | 口伝・手順書 | Git 上の SQL ファイル |
| 適用状態 | 環境ごとにバラバラ | 全環境で同一 |
| 実行 | 人が手で流す | 起動時に自動 |

## 命名規則と適用順・checksum

ファイル名は `V<番号>__<説明>.sql` の形です(アンダースコア 2 つに注意)。
このリポジトリのマイグレーションは、現時点で `V1__Create_memos_table.sql` の 1 本です。

Flyway は番号の小さい順に実行し、成功すると `flyway_schema_history` テーブルに
履歴を残します。次回起動時はこの履歴を見て、記録済みのものはスキップし、
新しい番号のファイルだけを実行します。履歴には各ファイルの **checksum**
(内容のハッシュ値)も記録されます。

## 鉄則: 適用済みマイグレーションは書き換えない

**一度適用したファイルは二度と編集しない**。これが Flyway 運用の大原則です。
Flyway は起動のたびに checksum を再計算して履歴と照合するため、コメント 1 文字を
直しただけでも checksum が変わり、既存環境では `Migration checksum mismatch` で
**起動に失敗** します。変更は必ず新しい番号のファイル(`V2` 以降)で足します。

うっかり書き換えてしまったときは、本番では避けますが、開発中の使い捨て DB なら
`docker compose down -v && docker compose up -d` で DB を作り直すのが最も安全です
(Flyway が最初から全マイグレーションを流し直します)。

## SQL の最小知識(V1 の設計を例に)

SQL の命令は大きく 2 種類に分かれます。

- **DDL**(Data Definition Language): 構造を定義する。`CREATE TABLE` など
- **DML**(Data Manipulation Language): データを操作する。`INSERT` など

`V1` はすべて DDL(テーブル・インデックス・トリガーの作成)です。中心の
`CREATE TABLE memos` から SQL の要点を拾います。

```sql
CREATE TABLE memos (
    id         BIGSERIAL PRIMARY KEY,                          -- 主キー・自動採番
    title      VARCHAR(200) NOT NULL,                          -- 必須・最大200文字
    content    TEXT NOT NULL,                                  -- 必須・長さ実質無制限
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,   -- 既定値=現在時刻
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

- **制約** は「守るべきルール」を DB 自身に持たせる仕組みです。`PRIMARY KEY`
  (一意かつ NOT NULL)、`NOT NULL`(必須)、`DEFAULT`(既定値)など。アプリの
  Bean Validation([Bean-Validation.md](Bean-Validation.md))が入口で弾くのに対し、
  制約は DB を最後の砦として二重に守ります
- `BIGSERIAL` は自動採番の 64 ビット整数です。`INSERT` で `id` を指定しなくても
  DB が連番を振り、MyBatis の `useGeneratedKeys` がその値を受け取ります

### インデックス

**インデックス** は検索を速くする索引です。`V1` は 2 つ作っています。

- `idx_memos_created_at`(B-tree・降順): 一覧の `ORDER BY created_at DESC` を速くする
- `idx_memos_fulltext`(GIN + `to_tsvector`): 全文検索演算子(`@@`)専用

ただし注意点があり、GIN インデックスは **全文検索(`@@`)のときだけ** 効き、
このアプリの検索機能が使う `LIKE '%...%'` では使われません。`LIKE` を速くするには
`pg_trgm` 拡張が必要です(実装演習として V1 のコメントに詳しく書かれています)。
インデックスは INSERT/UPDATE を遅くするため、使う検索に絞って足すのが原則です。

## トリガーによる `updated_at` の自動更新

`updated_at` を手で更新するのは忘れやすいため、`V1` は **トリガー** を使っています。
トリガーは特定のイベント(ここでは UPDATE)で自動実行される処理です。

```sql
CREATE TRIGGER trigger_update_memos_updated_at
    BEFORE UPDATE ON memos
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();  -- 行が更新される直前に現在時刻をセット
```

これにより、`MemoMapper.xml` の UPDATE 文が `updated_at` を書かなくても、
更新のたびに DB 側で現在時刻が入ります。関数は PL/pgSQL(PostgreSQL の手続き型言語)で
書かれ、`NEW`(更新後の行)の `updated_at` を書き換えて返します。

## モダン版との対比: 同じ Flyway でも DB は共有できない

モダン版(`learn-memo-app-modern`)**も Flyway を使います**。しかし両アプリで
**同じ DB を共有してはいけません**。同じ `V1` という番号なのに中身が違うからです。

| 観点 | 従来型の V1 | モダン版の V1 |
|------|-------------|---------------|
| `title` | `VARCHAR(200)` | `VARCHAR(100)` |
| `content` | `TEXT NOT NULL`(必須) | `TEXT`(任意) |
| `updated_at` の更新 | DB トリガーで自動更新 | JPA Auditing で自動更新 |
| 全文検索索引 | GIN インデックスあり | なし |

Flyway は「`V1` を適用済み」を checksum で記録するため、片方が適用した DB に
もう片方をつなぐと即座に `checksum mismatch` で起動に失敗します。だから両アプリの
PostgreSQL は別コンテナ・別ポート(従来型 5432 / モダン型 5433)に分けています。

## 参照しているコード

- `src/main/resources/db/migration/V1__Create_memos_table.sql` — テーブル・インデックス・トリガーの作成(DDL)
- `src/main/resources/mybatis/mapper/MemoMapper.xml` — `useGeneratedKeys` と、トリガー前提の UPDATE 文
