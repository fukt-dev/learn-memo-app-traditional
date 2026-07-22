# learn-memo-app-traditional

学習用メモ帳 Web アプリ(**従来型**)。開発未経験者が読んで学ぶことを目的とし、詳細な日本語コメントを仕様とする。
モダン版 [learn-memo-app-modern](https://github.com/fukt-dev/learn-memo-app-modern) と対で比較学習するために作られている。

## 技術スタック

| カテゴリ | 技術 |
|----------|------|
| 言語 / FW | Java 17 / Spring Boot 3.5.16(Issue #8 で 3.2 系から更新) |
| DB アクセス | MyBatis 3.0.5(2waySQL: `src/main/resources/mybatis/mapper/MemoMapper.xml`) |
| 画面 | Thymeleaf(サーバーサイドレンダリング) |
| DB | PostgreSQL 16(Docker)+ Flyway マイグレーション |
| その他 | Lombok / Maven |

## アーキテクチャ

3 層レイヤー(モノリシック)。URL: `http://localhost:8080/memos`

```
Controller(+Thymeleaf) → Service → Mapper(MyBatis) → PostgreSQL
```

## コマンド

```bash
docker compose up --build        # 全部 Docker で起動(推奨)
docker compose up -d postgres    # DB のみ起動
mvn spring-boot:run              # アプリをローカル起動
mvn clean package                # JAR ビルド
mvn test                         # テスト実行
```

テストは JUnit 5 + Mockito + MockMvc(Issue #8 で導入。Controller / Service / DTO の 3 本)。

## ディレクトリ構成

```
src/main/java/com/example/memoapp/
  controller/  # プレゼンテーション層
  service/     # ビジネスロジック層
  mapper/      # データアクセス層(インターフェース)
  entity/ dto/ exception/
src/test/java/com/example/memoapp/  # テスト(JUnit 5 + Mockito + MockMvc)
src/main/resources/
  mybatis/mapper/MemoMapper.xml  # SQL 本体(2waySQL)
  db/migration/                  # Flyway マイグレーション
  templates/                     # Thymeleaf テンプレート
  static/                        # CSS / JS
```

## DB 接続情報

`localhost:5432` / DB 名 `memoapp` / ユーザー `memoapp_user`
※ モダン版(learn-memo-app-modern)は別コンテナ(ホスト側 5433)を使う。
同じ DB を共有すると Flyway のマイグレーション履歴が衝突するため、共有してはいけない。

## コメント方針(このリポの仕様)

- 教育目的の詳細な日本語コメントを書く。ただし**コードの言い換え(What の繰り返し)は書かない**
- 書く価値があるのは: Why(選定理由)/ 仕組み(フレームワークが裏でやること)/ 落とし穴 / モダン版との対比
- コードを変えたらコメントも直す(実装と食い違うコメントはバグと同格)

## ドキュメント

| ファイル | 内容 |
|----------|------|
| `docs/設計書.md` | 画面・API・DB 設計 |
| `docs/環境構築手順.md` | 初学者向け詳細セットアップ |
| `docs/開発ガイド.md` | 開発のベストプラクティス |
| `docs/MyBatis2waySQL解説.md` | MyBatis の解説 |
| `docs/Webアプリケーション開発ロードマップ.md` | 学習全体構想(全リポ共通) |

## Git

- リポジトリ: `fukt-dev/learn-memo-app-traditional`
- `main` へ直接コミットせず、`feature/` `fix/` `docs/` ブランチ + PR で作業する(1 PR = 1 関心事)
