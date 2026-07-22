# docs/解説/ — 汎用概念の解説記事

ソースコード内のコメントから「特定のファイルに閉じない汎用的な解説」を集約した記事集です。
コード側のコメントには「そのコードでなぜその書き方を選んだか(Why)・固有の落とし穴・モダン版との対比」だけを残し、
仕組みの背景説明はここへのリンクで参照します。

対になるモダン版(`learn-memo-app-modern`)の `docs/解説/` と同じ方式で、
従来型 ⇔ モダンの比較学習ができるよう、各記事に「モダン版との対比」を設けています。

## 記事一覧

| 記事 | 内容 | 主に参照しているコード |
|------|------|------------------------|
| [Spring-MVCとThymeleaf.md](Spring-MVCとThymeleaf.md) | MVC のリクエスト処理の流れ / マッピング / Model / ビュー解決 / PRG パターン / Thymeleaf の属性・フラグメント・自動エスケープ | `MemoController.java` / `templates/` |
| [Bean-Validation.md](Bean-Validation.md) | サーバー側検証の理由 / `@NotBlank`・`@Size` / `@Valid` と `BindingResult` / エラーの画面表示 / Entity と DTO の役割分担 | `MemoDto.java` / `MemoController.java` / `new.html`・`edit.html` |
| [例外処理とエラーページ.md](例外処理とエラーページ.md) | `@ControllerAdvice` / `@ExceptionHandler` / カスタム例外 / `@ResponseStatus` の落とし穴 / checked と unchecked / エラーページ | `GlobalExceptionHandler.java` / `MemoNotFoundException.java` / `error.html` |
| [DIとLombok.md](DIとLombok.md) | Spring Boot の起動と自動設定 / コンポーネントスキャンと Bean / 依存性注入の 3 方式 / Lombok / ロギング | `MemoAppApplication.java` / `MemoService.java` / `Memo.java` |
| [トランザクション.md](トランザクション.md) | `@Transactional` の動作 / `readOnly` / ロールバックの条件 / 自己呼び出しの落とし穴 | `MemoService.java` |
| [Flywayとマイグレーション.md](Flywayとマイグレーション.md) | スキーマのコード管理 / 命名規則と checksum / DDL・制約・インデックス / トリガー | `db/migration/V1__Create_memos_table.sql` |
| [テスト.md](テスト.md) | 単体テストと Web レイヤーテストの役割分担 / Mockito / MockMvc / Given-When-Then | `MemoServiceTest.java` / `MemoControllerTest.java` / `MemoDtoTest.java` |

## MyBatis / 2waySQL について

MyBatis と 2waySQL の汎用解説は、この記事集ではなく既存の
[../MyBatis2waySQL解説.md](../MyBatis2waySQL解説.md) が担っています。
`MemoMapper.java` / `MemoMapper.xml` の `#{}` と `${}` の違い・動的 SQL・`resultMap` などは
そちらを参照してください。

## この構成にしている理由

- 同じ解説(「バリデーションとは」等)が複数ファイルに重複コピーされ、修正漏れで食い違う事故を防ぐ
- コードファイルがコメントで肥大化して「コード本体が探せない」状態を解消する
- 記事として通読できる形にすることで、コードを読む前の予習・読んだ後の復習に使える

コメントの書き方の基準はワークスペースの `.claude/rules/learning-comments.md` を参照してください。
