# Bean Validation(入力値検証)

`MemoDto.java` の `@NotBlank` `@Size`、`MemoController.java` の `@Valid` と
`BindingResult`、そして `new.html` / `edit.html` のエラー表示が何をしているのか、
その背景をまとめます。

## この記事で分かること

- なぜ入力検証を「サーバー側で必ず」やるのか
- `@NotBlank` `@Size` `@NotEmpty` の意味と使い分け
- `@Valid` と `BindingResult` で検証を実行し、失敗時に入力画面へ戻す流れ
- 検証エラーを Thymeleaf で表示する書き方(`th:errors` / `#fields`)
- DTO と Entity の役割分担、両者を変換する意味

## なぜサーバー側で検証するのか

入力チェックは「ブラウザ側(HTML5 の `required` など)でやっているから十分」ではありません。
ブラウザの検証は簡単に突破できるからです。

- 開発者ツールで属性を書き換える
- `curl` や Postman でフォームを通さず直接リクエストする
- JavaScript のバグでチェックが素通りする

検証をすり抜けたデータは、空タイトルのメモや DB のカラム長超過エラーなど、
後続の層に被害を広げます。**入口(Controller)で弾く**のが最も安全で安価です。

## 主要アノテーション

Bean Validation は検証ルールをアノテーションで宣言的に書く Java 標準の仕組みです。
`MemoDto` のフィールドに付けると、そのフィールドの制約になります。

| アノテーション | 意味 | `null` | `""` | `"   "`(空白のみ) |
|----------------|------|:------:|:----:|:------:|
| `@NotNull` | null でない | NG | OK | OK |
| `@NotEmpty` | null でも空でもない | NG | NG | OK |
| `@NotBlank` | null でも空でも空白のみでもない | NG | NG | NG |
| `@Size(min, max)` | 文字列長・要素数の範囲 | スキップ | 長さで判定 | 長さで判定 |

文字列の必須入力には `@NotBlank` が最適です。このアプリの `title` は
`@NotBlank` と `@Size(min = 1, max = 200)` を組み合わせています。

```java
@NotBlank(message = "タイトルを入力してください")
@Size(min = 1, max = 200, message = "タイトルは1文字以上200文字以内で入力してください")
private String title;
```

### 落とし穴: `@Size` は空文字を拒否しない

`@Size(max = 200)` は「長さの範囲」だけを見ます。`""` は長さ 0 なので上限内に
収まり **通過** します。必須にしたいなら `@NotBlank` を併用します。`title` に
`@NotBlank` と `@Size` の両方を付けているのはこのためで、`min = 1` は
`@NotBlank` があるので厳密には冗長ですが、意図を明示するために書いています。

## `@Valid` と `BindingResult` の連携

Controller の引数に `@Valid` を付けると、メソッド本体が動く **前** に検証が走ります。
検証結果は直後に置いた `BindingResult` に格納されます(この 2 つは必ず隣り合わせにします)。

```java
public String create(@Valid @ModelAttribute MemoDto memoDto,
                      BindingResult bindingResult, ...) {
    if (bindingResult.hasErrors()) {
        return "memos/new";       // エラーなら入力画面に戻す
    }
    memoService.create(memoDto);  // 合格したデータだけ Service へ
    ...
}
```

検証に失敗しても例外は投げず、`hasErrors()` で分岐して同じ入力画面を返すのが
従来型(画面遷移型)のやり方です。このとき入力値は `Model` に残っているため、
ユーザーは入力し直さずに済みます。

## 検証エラーの画面表示

入力画面に戻したあと、エラーは Thymeleaf のフォームユーティリティで表示します。

```html
<input type="text" th:field="*{title}"
       th:classappend="${#fields.hasErrors('title')} ? 'is-invalid' : ''">
<span th:if="${#fields.hasErrors('title')}" th:errors="*{title}"></span>
```

- `th:object="${memoDto}"`(フォーム側)で対象 DTO を指定すると、`*{title}` で
  各フィールドを簡潔に参照できます
- `#fields.hasErrors('title')` でそのフィールドにエラーがあるか判定します
- `th:errors="*{title}"` がアノテーションの `message` 属性の文言を表示します

エラーメッセージは `message` 属性に直接書く方式のほか、`messages.properties` に
外出しして一元管理・多言語対応する方式もあります。このアプリは初学者向けに
シンプルな前者を採用しています。

## Entity と DTO の役割分担

`MemoDto` は検証ルールを持つだけでなく、Entity(`Memo`)との橋渡しも担います。

| | Entity(`Memo`) | DTO(`MemoDto`) |
|--|----------------|-----------------|
| 役割 | DB のテーブルの 1 行を表す | 画面とのやり取り・入力検証 |
| 検証 | なし | `@NotBlank` などを持つ |
| 変更の影響 | DB 構造の変更に追従 | 画面の変更に追従 |

分けておくと、画面の都合(表示用にフォーマット済みの日時文字列を持つ等)を
DB の構造から切り離せます。変換は `MemoDto.fromEntity()` / `toEntity()` が担当し、
`created_at` / `updated_at` は DB 側が管理するため DTO→Entity ではコピーしません
(画面から日時を改ざんされないという安全上の意味もあります)。

## モダン版との対比

モダン版(`learn-memo-app-modern`)も **同じ Bean Validation** を使います。
違いは「検証エラーの見せ方」です。

| 観点 | 従来型(この記事) | モダン版(REST API) |
|------|-------------------|---------------------|
| 受け取り方 | `@Valid @ModelAttribute` + `BindingResult` | `@Valid @RequestBody` |
| 失敗時の扱い | `hasErrors()` で入力画面に戻す | 例外 → ハンドラが 400 JSON |
| エラー表示 | サーバーが HTML を再描画 | JSON を返し React が表示 |
| `title` 上限 | 200 文字 | 100 文字 |
| `content` | 必須(`@NotBlank`) | 任意(`null` 可) |

検証ルールの書き方(アノテーション)は共通で、`BindingResult` で画面に戻すか
例外にして JSON を返すかが、両者の差そのものです。

## 参照しているコード

- `src/main/java/com/example/memoapp/dto/MemoDto.java` — `@NotBlank` / `@Size` と変換メソッド
- `src/main/java/com/example/memoapp/controller/MemoController.java` — `@Valid` / `BindingResult`
- `src/main/resources/templates/memos/new.html` — `th:field` / `th:errors` / `#fields`
- `src/main/resources/templates/memos/edit.html` — 更新フォームでの同じ表示
