# Spring MVC と Thymeleaf

`MemoController.java` と `templates/` 配下の HTML が、
リクエストを受けてから画面を返すまでに何をしているのか、その背景をまとめます。

## この記事で分かること

- ブラウザのリクエストが画面になるまでの Spring MVC の流れ
- URL とメソッドを結びつけるアノテーション(`@Controller` / `@GetMapping` / `@PathVariable` など)
- `Model` に載せたデータが Thymeleaf に届く仕組み
- なぜ登録・更新後に「リダイレクト」するのか(PRG パターン)
- Thymeleaf の主要な `th:` 属性・フラグメントによる共通化・自動エスケープ

## Spring MVC のリクエスト処理の流れ

従来型は「サーバーが HTML を組み立てて返す」方式(サーバーサイドレンダリング)です。
1 回のリクエストは、おおよそ次の順で処理されます。

```
ブラウザ  ──GET /memos──▶  DispatcherServlet(受付窓口)
                              │ HandlerMapping が担当メソッドを探す
                              ▼
                          MemoController.list()  ← Model にデータを載せる
                              │ "memos/list" というビュー名を返す
                              ▼
                          ViewResolver が templates/memos/list.html を解決
                              │ Thymeleaf が ${...} を評価して HTML を生成
                              ▼
ブラウザ  ◀──完成した HTML──  レスポンス
```

`DispatcherServlet` はすべてのリクエストの入口です。開発者はこの仕組みを直接書かず、
「どの URL でどのメソッドを動かすか」をアノテーションで宣言するだけで済みます。

## URL とハンドラの対応

`@Controller` はこのクラスが画面(ビュー名)を返す Controller であることを示します。
クラスに付けた `@RequestMapping("/memos")` が全メソッド共通の URL 接頭辞になり、
各メソッドの `@GetMapping` / `@PostMapping` と組み合わさって最終的な URL が決まります。

| アノテーション | 役割 | 例 |
|----------------|------|-----|
| `@GetMapping("/{id}")` | GET リクエストを受ける | `GET /memos/1` |
| `@PostMapping("")` | POST リクエストを受ける | `POST /memos` |
| `@PathVariable Long id` | URL パスの一部を引数で受け取る | `/memos/1` → `id=1` |
| `@RequestParam String keyword` | クエリパラメータを受け取る | `/memos?keyword=買い物` |
| `@ModelAttribute MemoDto dto` | フォーム送信値を DTO にまとめる | `title=...&content=...` → `dto` |

`@PathVariable` は文字列を自動で `Long` などに型変換します。`/memos/abc` のように
変換できない値が来た場合はエラーになり、例外ハンドラ([例外処理とエラーページ.md](例外処理とエラーページ.md))が処理します。

## Model と画面へのデータ受け渡し

Controller は `Model` にデータを載せ、Thymeleaf 側は同じ名前で参照します。

```java
model.addAttribute("memos", memos);   // Java 側
```
```html
<div th:each="memo : ${memos}">       <!-- Thymeleaf 側で ${memos} として参照 -->
```

`Model` は「画面に渡す入れ物」で、Spring が引数に自動で用意します。

## ビュー名の解決とリダイレクト

Controller が返す文字列がビュー名です。`"memos/list"` は
`application.yml` の設定(prefix=`classpath:/templates/`、suffix=`.html`)により
`templates/memos/list.html` に解決されます。

一方 `"redirect:/memos"` を返すと、画面を描かずに「`/memos` にアクセスし直せ」という
指示(HTTP 302)をブラウザに返します。これが次の PRG パターンの土台です。

### なぜリダイレクトするのか(PRG パターン)

登録・更新・削除は POST で受けますが、処理後に画面を直接返すと、
ユーザーがブラウザで再読み込みしたときに **同じ POST が再送信され、二重登録** が起きます。

そこで **Post → Redirect → Get** の順にします。POST の処理が終わったら一覧へ
リダイレクトし、続く GET で一覧を描きます。再読み込みされても繰り返されるのは
安全な GET だけなので、二重送信を防げます。登録・更新後に一覧や詳細へ
`redirect:` している箇所は、すべてこの理由です。

## Thymeleaf の基本

Thymeleaf は HTML の属性(`th:*`)にロジックを埋め込むテンプレートエンジンです。
`xmlns:th="http://www.thymeleaf.org"` を宣言すると `th:` 属性が使えます。

| 属性 | 働き |
|------|------|
| `th:text="${memo.title}"` | 要素のテキストを差し替える(自動エスケープあり) |
| `th:href="@{/memos/{id}(id=${memo.id})}"` | URL を組み立てる(`@{}` はコンテキストパスを補完) |
| `th:each="memo : ${memos}"` | リストを繰り返す(ループ) |
| `th:if` / `th:unless` | 条件で要素を出す/出さない |
| `th:object` / `th:field` | フォームを DTO に結びつける(詳細は [Bean-Validation.md](Bean-Validation.md)) |

### フラグメントによる共通化

ヘッダーやフッターは `fragments/parts.html` に一度だけ定義し、各画面は
`th:replace="~{fragments/parts :: siteHeader('list')}"` で部品として取り込みます。
コピペをやめることで「ナビを 1 つ足すだけで全画面を修正」という手間と修正漏れを防げます。

### 自動エスケープと Natural Templating

`th:text` はユーザー入力を自動で HTML エスケープするため、`<script>` を入力されても
文字列として表示され、XSS を防ぎます(エスケープを外す `th:utext` は通常使いません)。
また `th:*` 属性は素の HTML としても壊れないため、テンプレートをブラウザで直接開けば
`placeholder` やデフォルト文字が見える「Natural Templating」も成り立ちます。

## モダン版との対比

モダン版(`learn-memo-app-modern`)は同じリクエストでも **JSON を返す** REST API です。
画面の組み立ては、返ってきた JSON をもとに React がブラウザ側で行います。

| 観点 | 従来型(この記事) | モダン版 |
|------|-------------------|----------|
| Controller | `@Controller`(ビュー名を返す) | `@RestController`(JSON を返す) |
| 画面の生成 | サーバーが Thymeleaf で HTML を生成 | ブラウザ側で React が生成 |
| 共通部品 | Thymeleaf フラグメント | React コンポーネント |
| 二重送信対策 | PRG パターン(リダイレクト) | クライアント側の状態管理で回避 |

「共通部分を部品化して再利用する」考え方は、フラグメントもコンポーネントも同じです。

## 参照しているコード

- `src/main/java/com/example/memoapp/controller/MemoController.java` — マッピング・Model・リダイレクト
- `src/main/resources/templates/memos/list.html` — `th:each` / `th:if` / `@{}` リンク
- `src/main/resources/templates/fragments/parts.html` — `th:fragment` / `th:replace`
- `src/main/resources/templates/memos/new.html` — フォームと `th:object` / `th:field`
