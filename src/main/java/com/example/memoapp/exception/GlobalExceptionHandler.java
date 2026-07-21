package com.example.memoapp.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * ============================================
 * GlobalExceptionHandlerクラス
 * ============================================
 *
 * 【このクラスの役割】
 * アプリケーション全体で発生する例外を一箇所で処理します
 *
 * 【例外ハンドリングとは】
 * プログラム実行中にエラー（例外）が発生したときの対処方法を定義すること
 *
 * 【なぜ必要か】
 * - 各Controllerメソッドで個別にtry-catchを書くと冗長になる
 * - 統一されたエラー画面を表示できる
 * - エラーログを一箇所で記録できる
 * - ユーザーにわかりやすいエラーメッセージを表示できる
 *
 * 【例外ハンドラがない場合】
 * エラーが発生すると、ブラウザに以下のような画面が表示される:
 *
 * Whitelabel Error Page
 * This application has no explicit mapping for /error, so you are seeing this as a fallback.
 * There was an unexpected error (type=Internal Server Error, status=500).
 *
 * → ユーザーフレンドリーではない
 * → 何が問題かわからない
 *
 * 【例外ハンドラがある場合】
 * エラーが発生すると、カスタムエラー画面を表示できる:
 *
 * エラーが発生しました
 * 指定されたメモが見つかりませんでした
 * [トップページに戻る]
 *
 * → わかりやすい
 * → 次のアクションを提示できる
 */

/**
 * @ControllerAdvice
 *
 * このクラスがグローバルな例外ハンドラであることを示す
 *
 * 【動作】
 * - 全てのControllerで発生した例外を捕捉できる
 * - @ExceptionHandlerを付けたメソッドが自動的に呼ばれる
 * - Controllerと同様にビュー名を返せる
 *
 * 【@ControllerAdvice の用途】
 * 1. 例外ハンドリング（このクラスの用途）
 * 2. @ModelAttribute でグローバルなModel属性を設定
 * 3. @InitBinder でグローバルなバリデーション設定
 *
 * 【適用範囲】
 * デフォルトでは全Controllerに適用される
 * 特定のパッケージだけに適用することも可能:
 * @ControllerAdvice(basePackages = "com.example.memoapp.controller")
 */
@ControllerAdvice

/**
 * @Slf4j
 * Lombokのアノテーション
 * ログ出力用の log フィールドを自動生成
 *
 * エラーが発生したら、ログに記録して後で調査できるようにする
 */
@Slf4j
public class GlobalExceptionHandler {

    /**
     * MemoNotFoundException のハンドラ
     *
     * 【このハンドラの役割】
     * メモが見つからない場合にカスタムエラー画面を表示し、
     * HTTPステータス 404（Not Found）を返す
     *
     * 【落とし穴: 例外クラス側の @ResponseStatus は「ここでは」効かない】
     * MemoNotFoundException クラスには @ResponseStatus(HttpStatus.NOT_FOUND) が
     * 付いているが、あれが効くのは「どの @ExceptionHandler にも捕捉されずに」
     * Spring のデフォルト処理（ResponseStatusExceptionResolver）まで届いた場合だけ。
     *
     * このハンドラのように @ExceptionHandler が例外を捕捉してビューを返すと、
     * Spring から見れば「正常にレスポンスを生成できた」ことになり、
     * 何も指定しなければ HTTPステータスは 200（OK）になってしまう。
     *
     * → エラー画面を見せているのにステータスは 200、という
     *   「人間には分かるが機械（クローラや監視ツール）には分からない」状態になる
     *
     * そのため、ハンドラメソッド側にも @ResponseStatus を明示的に付けている
     *
     * 【IllegalArgumentExceptionハンドラとの違い】
     * - IllegalArgumentException: ERROR レベル、400ステータス
     * - MemoNotFoundException: WARN レベル、404ステータス
     *
     * メモが見つからないのは、システムエラーではなく、
     * 「リソースが存在しない」という正常な応答なので、
     * WARNレベルが適切
     *
     * @param ex 発生した例外オブジェクト
     * @param model Modelオブジェクト（エラー画面に渡すデータ）
     * @return ビュー名（templates/error/error.html）
     */
    @ExceptionHandler(MemoNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleMemoNotFoundException(
            MemoNotFoundException ex,
            /*
             * 発生した例外オブジェクト
             * ex.getMessage() でエラーメッセージを取得できる
             */
            Model model
            /*
             * Modelオブジェクト
             * エラー画面に渡すデータを設定する
             */
    ) {
        /*
         * WARNログを出力
         *
         * log.warn():
         * - WARN レベルのログ
         * - ERROR より低い（重大度が低い）
         * - 本番環境でも出力される
         * - リソースが見つからないのはシステムエラーではない
         *
         * ERRORとWARNの使い分け:
         * - ERROR: システムの異常、予期しないエラー
         * - WARN: 正常だが注意すべき状況
         */
        log.warn("メモが見つかりませんでした: {}", ex.getMessage());

        /*
         * Modelにエラーメッセージを設定
         * エラー画面で表示するため
         */
        model.addAttribute("errorMessage", ex.getMessage());

        /*
         * エラー画面のビュー名を返す
         * templates/error/error.html を表示
         *
         * HTTPステータスは、このメソッドに付けた
         * @ResponseStatus(HttpStatus.NOT_FOUND) により404になる
         * （例外クラス側のアノテーションだけでは 200 が返る。上記の落とし穴を参照）
         */
        return "error/error";
    }

    /**
     * IllegalArgumentException のハンドラ
     *
     * 【IllegalArgumentExceptionとは】
     * 不正な引数が渡されたときに発生する例外
     *
     * 【このアプリでの使用例】
     * MemoService.findAllWithPaging() で、不正なページ番号が指定された場合
     * throw new IllegalArgumentException("ページ番号は1以上である必要があります: " + pageNumber);
     *
     * 【なぜ @ResponseStatus(HttpStatus.BAD_REQUEST) を付けているのか】
     * IllegalArgumentException 自体には HTTPステータスの情報はない。
     * ハンドラを書かなければ未処理の例外として 500（Internal Server Error）になり、
     * ハンドラを書いてもステータスを指定しなければ 200（OK）になってしまう。
     * 「クライアントの入力が不正」という意味の 400 を返すには、
     * このようにハンドラ側で明示する必要がある
     *
     * @param ex 発生した例外オブジェクト
     * @param model Modelオブジェクト（エラー画面に渡すデータ）
     * @return ビュー名（templates/error/error.html）
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    /*
     * @ExceptionHandler
     *
     * どの例外を処理するかを指定する
     * @ExceptionHandler(IllegalArgumentException.class)
     * → IllegalArgumentException が発生したらこのメソッドを呼ぶ
     *
     * 複数の例外を指定することも可能:
     * @ExceptionHandler({IllegalArgumentException.class, NullPointerException.class})
     */
    public String handleIllegalArgumentException(
            IllegalArgumentException ex,
            /*
             * 発生した例外オブジェクト
             * ex.getMessage() でエラーメッセージを取得できる
             */
            Model model
            /*
             * Modelオブジェクト
             * エラー画面に渡すデータを設定する
             */
    ) {
        /*
         * エラーログを出力
         *
         * log.error():
         * - ERROR レベルのログ
         * - 本番環境でも出力される
         * - システムの異常を記録
         *
         * 引数:
         * - 第1引数: ログメッセージ
         * - 第2引数: 例外オブジェクト（スタックトレースも記録される）
         */
        log.error("不正な引数が指定されました: {}", ex.getMessage(), ex);

        /*
         * Modelにエラーメッセージを設定
         * エラー画面で表示するため
         */
        model.addAttribute("errorMessage", ex.getMessage());

        /*
         * エラー画面のビュー名を返す
         * templates/error/error.html を表示
         */
        return "error/error";
    }

    /**
     * 存在しないURLへのアクセスのハンドラ（404）
     *
     * 【NoResourceFoundExceptionとは】
     * Spring Boot 3.2（Spring Framework 6.1）以降で、
     * どのControllerにもマッピングされず、静的リソースとしても見つからない
     * URLにアクセスされたときにスローされる例外
     * 例: GET /memoss（タイプミス）、GET /favicon.ico（未配置）
     *
     * 【なぜ専用ハンドラが必要か】
     * このクラスには Exception を捕捉するフォールバックハンドラがあるため、
     * 専用ハンドラがないと NoResourceFoundException もそこに吸い込まれ、
     * 「ただのURLタイプミス」が「システムエラー画面 + ERRORログ」になってしまう。
     *
     * - ユーザーには「ページが見つからない」と伝えるのが正しい（404）
     * - ログもERROR（システム異常）ではなくWARN（注意）が適切
     *   （favicon.ico のような機械的なアクセスで ERROR が積もると、
     *    本当の障害ログが埋もれてしまう）
     *
     * @param ex 発生した例外オブジェクト
     * @param model Modelオブジェクト
     * @return ビュー名（templates/error/error.html）
     */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNoResourceFoundException(NoResourceFoundException ex, Model model) {
        /*
         * getResourcePath() で「どのURLが見つからなかったか」を記録できる
         */
        log.warn("存在しないURLへのアクセス: /{}", ex.getResourcePath());

        model.addAttribute("errorMessage", "お探しのページが見つかりませんでした");

        return "error/error";
    }

    /**
     * すべての例外のハンドラ（フォールバック）
     *
     * 【役割】
     * 他のExceptionHandlerで捕捉されなかった例外を処理する
     *
     * 【例】
     * - NullPointerException
     * - データベース接続エラー
     * - 予期しないエラー
     * など
     *
     * 【処理の優先順位】
     * Springは「発生した例外に最も近い型のハンドラ」を選ぶ
     * 1. 具体的な例外ハンドラ（MemoNotFoundException など）
     * 2. 汎用的な例外ハンドラ（Exception）← このメソッド
     *
     * 【なぜ 500 を明示するのか】
     * ここに来るのは「予期しないエラー」= サーバー側の異常なので、
     * 500（Internal Server Error）が適切。
     * @ResponseStatus を付けないと、エラー画面を表示しているのに
     * ステータスは 200（OK）が返ってしまう
     *
     * @param ex 発生した例外オブジェクト
     * @param model Modelオブジェクト
     * @return ビュー名（templates/error/error.html）
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleException(Exception ex, Model model) {
        /*
         * エラーログを出力
         * すべての例外情報（スタックトレース含む）を記録
         */
        log.error("予期しないエラーが発生しました", ex);

        /*
         * Modelにエラーメッセージを設定
         *
         * ユーザーには詳細なエラー内容を見せない
         * → セキュリティリスク（内部情報の漏洩）
         * → わかりにくい（技術的な内容）
         *
         * 汎用的なメッセージを表示する
         */
        model.addAttribute("errorMessage", "システムエラーが発生しました。時間をおいて再度お試しください。");

        /*
         * エラー画面を表示
         */
        return "error/error";
    }

    /*
     * ============================================
     * 他の例外ハンドラの例（必要に応じて追加）
     * ============================================
     */

    /**
     * データベース関連のエラーハンドラ（例）
     *
     * @ExceptionHandler(DataAccessException.class)
     * public String handleDataAccessException(DataAccessException ex, Model model) {
     *     log.error("データベースエラーが発生しました", ex);
     *     model.addAttribute("errorMessage", "データベースへのアクセスに失敗しました");
     *     return "error/error";
     * }
     */

    /**
     * バリデーションエラーのハンドラ（例）
     *
     * @ExceptionHandler(MethodArgumentNotValidException.class)
     * public String handleMethodArgumentNotValidException(
     *         MethodArgumentNotValidException ex,
     *         Model model
     * ) {
     *     log.error("バリデーションエラー: {}", ex.getMessage());
     *     model.addAttribute("errorMessage", "入力内容に誤りがあります");
     *     return "error/error";
     * }
     */

    /**
     * 404エラーページをカスタマイズする別の方法（参考）
     *
     * このクラスでは NoResourceFoundException のハンドラで404画面を出しているが、
     * Spring Boot にはテンプレートの配置だけで実現する仕組みもある:
     * 1. src/main/resources/templates/error/404.html を作成
     * 2. ハンドルされなかった404エラー発生時、Spring Bootが自動的にこのテンプレートを使用する
     *
     * 同様に、500.html, 403.html なども作成可能。
     * 「ステータスコードごとに画面を変えるだけ」ならこちらの方がシンプル。
     * ログ出力やModelへの値設定など「処理」も挟みたい場合は @ExceptionHandler を使う
     */
}

/*
 * ============================================
 * エラー画面の作成（Thymeleaf）
 * ============================================
 *
 * templates/error/error.html:
 *
 * <!DOCTYPE html>
 * <html xmlns:th="http://www.thymeleaf.org">
 * <head>
 *     <title>エラー</title>
 * </head>
 * <body>
 *     <h1>エラーが発生しました</h1>
 *     <p th:text="${errorMessage}">エラーメッセージ</p>
 *     <a href="/memos">トップページに戻る</a>
 * </body>
 * </html>
 */

/*
 * ============================================
 * ログの活用
 * ============================================
 *
 * 【ログファイルの場所】
 * application.ymlで設定可能:
 *
 * logging:
 *   file:
 *     name: logs/memo-app.log
 *
 * → logs/memo-app.log にログが出力される
 *
 * 【ログレベル】
 * TRACE < DEBUG < INFO < WARN < ERROR
 *
 * - ERROR: システムの異常（このクラスで使用）
 * - WARN: 警告（エラーではないが注意が必要）
 * - INFO: 通常のビジネスイベント
 * - DEBUG: デバッグ情報（開発時のみ）
 * - TRACE: 最も詳細なログ
 *
 * 【本番環境での設定】
 * application-prod.yml:
 *
 * logging:
 *   level:
 *     root: INFO  # 通常はINFO以上のみ出力
 *     com.example.memoapp: INFO
 *   file:
 *     name: /var/log/memo-app/app.log
 *     max-size: 10MB
 *     max-history: 30  # 30日分保持
 */

/*
 * ============================================
 * エラーハンドリングのベストプラクティス
 * ============================================
 *
 * 【1. ユーザーフレンドリーなメッセージ】
 * ✗ 悪い例: "NullPointerException at line 123"
 * ✓ 良い例: "メモが見つかりませんでした"
 *
 * 【2. 適切なログ出力】
 * - ユーザーには簡潔なメッセージ
 * - ログには詳細な情報（スタックトレース含む）
 *
 * 【3. セキュリティ】
 * - 内部的なエラー詳細をユーザーに見せない
 * - データベース構造などの情報漏洩を防ぐ
 *
 * 【4. 復旧方法の提示】
 * - "トップページに戻る" などのリンクを提供
 * - "再度お試しください" などのガイダンス
 *
 * 【5. エラーの分類】
 * - ユーザーの入力ミス → バリデーションエラー
 * - データが見つからない → 404エラー
 * - システムエラー → 500エラー
 *
 * それぞれに適したエラー画面を用意する
 */
