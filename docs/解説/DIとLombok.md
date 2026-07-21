# 依存性注入(DI)と Lombok

`MemoAppApplication.java` の起動処理、各層の `@Service` / `@Controller` / `@Mapper`、
`Memo.java` や `MemoService.java` の Lombok アノテーションに共通する
「アプリの組み立て方」の基礎をまとめます。

## この記事で分かること

- Spring Boot が起動時に自動でやっていること(コンポーネントスキャン・自動設定)
- Bean と DI コンテナとは何か
- `@Service` / `@Controller` / `@Mapper` が 3 層をどう構成するか
- 依存性注入の 3 方式と、コンストラクタ注入を選ぶ理由
- Lombok が肩代わりするボイラープレート、ロギングの基礎

## Spring Boot の起動と自動設定

`main()` から呼ばれる `SpringApplication.run()` が、アプリ全体を立ち上げます。
起動クラスに付いた `@SpringBootApplication` は、実は 3 つのアノテーションの合成です。

| 内包アノテーション | 役割 |
|--------------------|------|
| `@Configuration` | このクラスを設定クラスとして扱う |
| `@EnableAutoConfiguration` | 依存関係を見て必要な設定を自動で行う |
| `@ComponentScan` | 同じパッケージ以下の Bean を自動検出する |

`@EnableAutoConfiguration` は `pom.xml` の依存を見て設定を決めます。たとえば
`spring-boot-starter-web` があれば Spring MVC と組み込み Tomcat を、
`mybatis-spring-boot-starter` があれば MyBatis を、`flyway-core` があれば
マイグレーション実行を、それぞれ自動で構成します。「設定より規約」の思想により、
XML の山を書かずに動く土台が整います。

## Bean とコンポーネントスキャン

**Bean** とは、Spring の DI コンテナが生成・管理するオブジェクトです。
`@ComponentScan` が起動クラスのパッケージ(`com.example.memoapp`)以下を走査し、
目印のアノテーションが付いたクラスを見つけて Bean として登録します。

```
com.example.memoapp
  ├─ controller/  @Controller  → プレゼンテーション層
  ├─ service/     @Service     → ビジネスロジック層
  └─ mapper/      @Mapper      → データアクセス層
```

`@Controller` / `@Service` はどれも技術的には Bean 登録の目印(`@Component` 系)ですが、
名前で「この層の部品」という意図を伝えます。`@Mapper` は MyBatis 用で、
インターフェースから実装を自動生成して Bean にします(詳細は
[../MyBatis2waySQL解説.md](../MyBatis2waySQL解説.md))。こうして 3 層が Bean として
そろい、互いに注入し合ってアプリが組み上がります。

## 依存性注入の 3 方式

**依存性注入(DI)** は「必要なオブジェクトを自分で `new` せず、外から渡してもらう」
仕組みです。`new` で自作すると密結合になりテストが難しくなりますが、DI なら
テスト時にモック(偽物)を渡せます。注入の書き方には 3 方式あります。

### コンストラクタ注入(推奨・このアプリで使用)

```java
@Service
@RequiredArgsConstructor
public class MemoService {
    private final MemoMapper memoMapper;   // final にできる
}
```

- 依存がコンストラクタの引数として明確になる
- `final` にでき、注入後に差し替わらない(不変)
- テストでモックをコンストラクタに渡すだけで済む

`@RequiredArgsConstructor`(Lombok)が `final` フィールドを引数に取る
コンストラクタを自動生成し、コンストラクタが 1 つなら Spring が
`@Autowired` なしで注入します。

### フィールド注入・セッター注入

`@Autowired` をフィールドに直接付ける方式(フィールド注入)は簡潔ですが、
依存が見えにくく `final` にできず、テストでの差し替えも難しくなります。
セッター注入はオプショナルな依存の差し替え用で、現代のアプリではほぼ使いません。

## Lombok によるボイラープレート削減

Lombok は定型コードをコンパイル時に自動生成するライブラリです。
このアプリで使っているアノテーションは次の通りです。

| アノテーション | 自動生成するもの |
|----------------|------------------|
| `@Data` | 全フィールドの getter/setter・`toString`・`equals`・`hashCode` |
| `@NoArgsConstructor` | 引数なしコンストラクタ(MyBatis がリフレクションで使う) |
| `@AllArgsConstructor` | 全フィールドを引数に取るコンストラクタ |
| `@RequiredArgsConstructor` | `final` フィールドを引数に取るコンストラクタ(DI 用) |
| `@Slf4j` | ロガー用の `log` フィールド |

`Memo` に `@NoArgsConstructor` が要るのは、MyBatis が「引数なしで生成 → setter で
値を設定」という手順でオブジェクトを組み立てるためです。Lombok がなければ
getter/setter だけで数十行になるところを、アノテーション数個に圧縮できます。

## ロギング(`@Slf4j` とログレベル)

`@Slf4j` が生成する `log` を使ってログを出します。レベルは 5 段階です。

```
TRACE < DEBUG < INFO < WARN < ERROR   (右ほど重要)
```

| レベル | 用途 | このアプリでの例 |
|--------|------|------------------|
| DEBUG | 開発時の詳細 | メソッド開始・取得件数 |
| INFO | 重要な業務イベント | 「メモを登録しました: id=1」 |
| WARN | 異常だが処理は続行 | 404(存在しない ID) |
| ERROR | 障害・調査が必要 | 500(予期しない例外) |

落とし穴として、例外を記録するときは `log.error("...", ex)` のように
**例外オブジェクトを第 2 引数で渡す** とスタックトレースが残ります。
`log.error(ex.getMessage())` だけでは手がかりがほとんど残りません。

## モダン版との対比

モダン版(`learn-memo-app-modern`)は Kotlin ですが、DI とロギングの考え方は同じです。

| 観点 | 従来型(この記事) | モダン版 |
|------|-------------------|----------|
| ボイラープレート削減 | Lombok(`@Data` など) | Kotlin の言語機能(data class・プロパティ) |
| データアクセス層 | `@Mapper`(MyBatis) | `@Repository`(Spring Data JPA) |
| コンストラクタ注入 | `@RequiredArgsConstructor` で生成 | 言語標準のコンストラクタ(`@Autowired` 不要) |
| ロガー定義 | `@Slf4j` が自動生成 | `companion object` に手書き |

Kotlin は data class やプロパティが言語に組み込まれているため、Lombok に相当する
ものを外部ライブラリなしで実現できる、というのが大きな違いです。

## 参照しているコード

- `src/main/java/com/example/memoapp/MemoAppApplication.java` — `@SpringBootApplication` と起動
- `src/main/java/com/example/memoapp/entity/Memo.java` — `@Data` / `@NoArgsConstructor` など
- `src/main/java/com/example/memoapp/service/MemoService.java` — `@Service` / `@RequiredArgsConstructor` / `@Slf4j`
