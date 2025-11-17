package com.example.memoapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ============================================
 * MemoAppApplicationクラス
 * ============================================
 *
 * 【このクラスの役割】
 * Spring Bootアプリケーションのエントリーポイント（起動クラス）です
 * このクラスのmain()メソッドを実行することで、アプリケーションが起動します
 *
 * 【エントリーポイントとは】
 * プログラムの実行が開始される場所
 * Javaの場合、public static void main(String[] args) がエントリーポイント
 *
 * 【Spring Bootの起動の流れ】
 * 1. main()メソッドが実行される
 * 2. SpringApplication.run() が呼ばれる
 * 3. Spring Bootが以下を自動で実行:
 *    - コンポーネントスキャン（@Service, @Controller, @Mapper などを探す）
 *    - AutoConfiguration（自動設定）
 *    - 組み込みTomcatの起動（Webサーバー）
 *    - データベース接続の確立
 *    - Flywayによるマイグレーション実行
 * 4. アプリケーションが起動完了
 *
 * 【起動後のログ例】
 * Started MemoAppApplication in 3.456 seconds (JVM running for 4.123)
 * Tomcat started on port(s): 8080 (http)
 *
 * 起動後、http://localhost:8080 でアクセス可能
 */

/**
 * @SpringBootApplication
 *
 * Spring Bootアプリケーションであることを示す
 * このアノテーション1つで、以下の3つのアノテーションを含む
 *
 * 【1. @Configuration】
 * このクラスがSpringの設定クラスであることを示す
 * Beanの定義などができる
 *
 * 【2. @EnableAutoConfiguration】
 * Spring Bootの自動設定を有効にする
 * pom.xmlの依存関係を見て、必要な設定を自動で行う
 *
 * 例:
 * - spring-boot-starter-web → Spring MVCの設定、組み込みTomcatの起動
 * - mybatis-spring-boot-starter → MyBatisの設定
 * - postgresql → PostgreSQL接続の設定
 * - flyway-core → Flywayの設定とマイグレーション実行
 *
 * 【3. @ComponentScan】
 * このクラスと同じパッケージ（com.example.memoapp）およびサブパッケージを
 * スキャンして、@Component, @Service, @Controller, @Mapper などの
 * アノテーションが付いたクラスを自動的に検出し、Beanとして登録する
 *
 * スキャン対象:
 * com.example.memoapp.controller.MemoController
 * com.example.memoapp.service.MemoService
 * com.example.memoapp.mapper.MemoMapper
 * など
 *
 * 【なぜこのアノテーション1つで良いのか】
 * Spring Bootの「設定より規約（Convention over Configuration）」の思想
 * デフォルトの動作で十分な場合は、細かい設定を書く必要がない
 */
@SpringBootApplication
public class MemoAppApplication {

    /**
     * アプリケーションのエントリーポイント
     *
     * 【main()メソッドとは】
     * Javaプログラムの開始点
     * JVMは、このメソッドを最初に実行する
     *
     * 【引数】
     * args: コマンドライン引数
     * 例: java -jar memo-app.jar --server.port=8081
     * この場合、args = ["--server.port=8081"]
     *
     * Spring Bootは、この引数を読み取って、設定を上書きできる
     *
     * @param args コマンドライン引数
     */
    public static void main(String[] args) {
        /*
         * Spring Bootアプリケーションを起動
         *
         * 【引数】
         * 第1引数: MemoAppApplication.class
         *          → このクラスを起動クラスとして使用
         *
         * 第2引数: args
         *          → コマンドライン引数を渡す
         *
         * 【戻り値】
         * ApplicationContext（Spring DIコンテナ）
         * Beanの取得などができるが、通常は使わない
         *
         * 【起動の流れ】
         * 1. Springコンテナの初期化
         *    - application.ymlの読み込み
         *    - 環境変数の読み込み
         *
         * 2. コンポーネントスキャン
         *    - @Service, @Controller, @Mapper などを検出
         *    - Beanとして登録
         *
         * 3. AutoConfiguration
         *    - DataSourceの設定（PostgreSQL接続）
         *    - MyBatisの設定（Mapperの登録）
         *    - Thymeleafの設定
         *    - など
         *
         * 4. Flywayマイグレーション実行
         *    - V1__Create_memos_table.sql などを実行
         *
         * 5. 組み込みTomcatの起動
         *    - ポート8080でリッスン開始
         *
         * 6. 起動完了
         */
        SpringApplication.run(MemoAppApplication.class, args);

        /*
         * 【アプリケーション起動後のログ例】
         *
         * 起動中のログ:
         * Starting MemoAppApplication using Java 17
         * No active profile set, falling back to 1 default profile: "default"
         * HikariPool-1 - Starting...
         * HikariPool-1 - Start completed.
         * Flyway Community Edition 9.16.0
         * Database: jdbc:postgresql://localhost:5432/memoapp
         * Successfully validated 1 migration (execution time 00:00.012s)
         * Creating Schema History table "public"."flyway_schema_history" ...
         * Current version of schema "public": << Empty Schema >>
         * Migrating schema "public" to version "1 - Create memos table"
         * Successfully applied 1 migration to schema "public" (execution time 00:00.045s)
         * Tomcat started on port(s): 8080 (http) with context path ''
         * Started MemoAppApplication in 3.456 seconds (JVM running for 4.123)
         *
         * 起動成功のサイン:
         * "Started MemoAppApplication in X.XXX seconds"
         *
         * ブラウザでアクセス:
         * http://localhost:8080
         */
    }

    /*
     * ============================================
     * アプリケーション起動方法
     * ============================================
     *
     * 【方法1: IDE（IntelliJ IDEA）から起動】
     * 1. このファイル（MemoAppApplication.java）を開く
     * 2. main()メソッドの左側にある緑色の▶ボタンをクリック
     * 3. "Run 'MemoAppApplication'" を選択
     *
     * 【方法2: Mavenコマンドで起動】
     * mvn spring-boot:run
     *
     * 【方法3: JARファイルを作成して起動】
     * 1. ビルド:
     *    mvn clean package
     *    → target/memo-app-1.0.0-SNAPSHOT.jar が作成される
     *
     * 2. 実行:
     *    java -jar target/memo-app-1.0.0-SNAPSHOT.jar
     *
     * 【方法4: Docker Composeと一緒に起動】
     * 1. PostgreSQLを起動:
     *    docker-compose up -d
     *
     * 2. アプリケーションを起動:
     *    mvn spring-boot:run
     *
     * ============================================
     * トラブルシューティング
     * ============================================
     *
     * 【ポートが既に使われている】
     * Error: Port 8080 was already in use
     *
     * 解決策:
     * 1. application.ymlでポート番号を変更:
     *    server:
     *      port: 8081
     *
     * 2. または、8080番ポートを使っているプロセスを終了
     *
     * 【データベースに接続できない】
     * Error: Connection refused
     *
     * 解決策:
     * 1. PostgreSQLが起動しているか確認:
     *    docker-compose ps
     *
     * 2. 起動していない場合:
     *    docker-compose up -d
     *
     * 3. application.ymlの接続設定を確認
     *
     * 【Flywayマイグレーションエラー】
     * Error: Migration checksum mismatch
     *
     * 解決策:
     * 1. データベースをリセット:
     *    docker-compose down -v
     *    docker-compose up -d
     *
     * 2. アプリケーションを再起動
     */
}

/*
 * ============================================
 * Spring Bootの仕組み（補足）
 * ============================================
 *
 * 【DI（Dependency Injection）コンテナ】
 * Spring Bootの中心的な機能
 *
 * 従来の書き方（DIなし）:
 * public class MemoController {
 *     private MemoService memoService = new MemoService();  // 自分で生成
 * }
 *
 * Spring Bootの書き方（DIあり）:
 * @Controller
 * public class MemoController {
 *     @Autowired
 *     private MemoService memoService;  // Springが自動的に注入
 * }
 *
 * 【メリット】
 * 1. 疎結合: クラス間の依存が減る
 * 2. テストしやすい: モックオブジェクトを注入できる
 * 3. 管理が楽: ライフサイクルをSpringが管理
 *
 * 【Bean】
 * SpringのDIコンテナが管理するオブジェクト
 *
 * Beanになるクラス:
 * - @Service が付いたクラス → MemoService
 * - @Controller が付いたクラス → MemoController
 * - @Mapper が付いたインターフェース → MemoMapper
 * - @Configuration の @Bean メソッドが返すオブジェクト
 *
 * 【コンポーネントスキャン】
 * @SpringBootApplication が付いたクラスのパッケージ以下を
 * 自動的にスキャンして、Beanを登録する
 *
 * スキャン範囲:
 * com.example.memoapp      ← @SpringBootApplication がある
 *   ├─ controller          ← スキャン対象
 *   ├─ service             ← スキャン対象
 *   ├─ mapper              ← スキャン対象
 *   ├─ entity              ← @Entityがないので対象外（ただし使用可能）
 *   └─ dto                 ← Beanではないので対象外
 *
 * 【AutoConfiguration】
 * pom.xmlの依存関係を見て、自動的に設定を行う
 *
 * 例:
 * - spring-boot-starter-web を追加
 *   → Spring MVCを自動設定、Tomcatを起動
 *
 * - mybatis-spring-boot-starter を追加
 *   → MyBatisを自動設定、Mapperを登録
 *
 * - flyway-core を追加
 *   → Flywayを自動設定、マイグレーションを実行
 *
 * この「設定より規約」により、XMLや複雑な設定ファイルが不要
 */
