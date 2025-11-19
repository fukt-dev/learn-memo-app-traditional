package com.example.memoapp.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

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
     * 【MemoNotFoundExceptionとは】
     * メモが見つからない場合にスローされるカスタム例外
     * @ResponseStatus(HttpStatus.NOT_FOUND) により404を返す
     *
     * 【このハンドラの役割】
     * MemoNotFoundExceptionをキャッチして、
     * わかりやすいエラー画面を表示する
     *
     * 【処理の流れ】
     * 1. MemoService.findById(999) を呼ぶ（存在しないID）
     * 2. MemoNotFoundException がスローされる
     * 3. このメソッドが自動的に呼ばれる
     * 4. WARNレベルでログ出力（ERRORより低い）
     * 5. エラー画面が表示される
     * 6. HTTPステータス 404 が返される
     *
     * 【IllegalArgumentExceptionとの違い】
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
         * HTTPステータスは @ResponseStatus(HttpStatus.NOT_FOUND) により
         * 自動的に404になる
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
     * MemoService.findById() で、存在しないIDが指定された場合
     * throw new IllegalArgumentException("メモが見つかりません: id=" + id);
     *
     * 【処理の流れ】
     * 1. MemoService.findById(999) を呼ぶ
     * 2. IDが見つからない
     * 3. IllegalArgumentException がスローされる
     * 4. このメソッドが自動的に呼ばれる
     * 5. エラー画面が表示される
     *
     * @param ex 発生した例外オブジェクト
     * @param model Modelオブジェクト（エラー画面に渡すデータ）
     * @return ビュー名（templates/error/error.html）
     */
    @ExceptionHandler(IllegalArgumentException.class)
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
     * 1. 具体的な例外ハンドラ（IllegalArgumentException など）
     * 2. 汎用的な例外ハンドラ（Exception）← このメソッド
     *
     * @param ex 発生した例外オブジェクト
     * @param model Modelオブジェクト
     * @return ビュー名（templates/error/error.html）
     */
    @ExceptionHandler(Exception.class)
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
     * 404エラー（ページが見つからない）のハンドラ（例）
     *
     * 【注意】
     * 404エラーは例外ではなく、HTTPステータスとして返される
     * そのため、@ExceptionHandler では捕捉できない
     *
     * 404エラーページをカスタマイズする方法:
     * 1. src/main/resources/templates/error/404.html を作成
     * 2. Spring Bootが自動的にこのテンプレートを使用する
     *
     * 同様に、500.html, 403.html なども作成可能
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
