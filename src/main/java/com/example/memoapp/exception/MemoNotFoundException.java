package com.example.memoapp.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * ============================================
 * MemoNotFoundExceptionクラス
 * ============================================
 *
 * 【このクラスの役割】
 * メモが見つからない場合にスローされるカスタム例外
 *
 * 【なぜカスタム例外を作るのか】
 * IllegalArgumentException などの汎用的な例外ではなく、
 * 用途に特化した例外を定義することで、以下のメリットがある:
 *
 * 1. コードの意図が明確になる
 *    - 「不正な引数」ではなく「メモが見つからない」という意味が明確
 *    - コードレビューやデバッグがしやすい
 *
 * 2. HTTPステータスを適切に設定できる
 *    - @ResponseStatus(HttpStatus.NOT_FOUND) により404を返せる
 *    - IllegalArgumentExceptionは400（Bad Request）になってしまう
 *
 * 3. 例外ハンドリングを分けられる
 *    - GlobalExceptionHandlerで専用のハンドラを書ける
 *    - メッセージや画面を用途に応じてカスタマイズできる
 *
 * 【HTTPステータスコードについて】
 * - 200 OK: 成功
 * - 201 Created: 新規作成成功
 * - 400 Bad Request: クライアントのリクエストが不正
 * - 404 Not Found: リソースが見つからない ← このクラスで使用
 * - 500 Internal Server Error: サーバー内部エラー
 */

/**
 * @ResponseStatus
 *
 * この例外がスローされたときに返すHTTPステータスコードを指定
 *
 * HttpStatus.NOT_FOUND = 404
 *
 * 【動作】
 * 1. MemoServiceでこの例外がスローされる
 * 2. Springが自動的に404レスポンスを返す
 * 3. GlobalExceptionHandlerでカスタムエラー画面を表示できる
 *
 * 【例】
 * GET /memos/99999 (存在しないID)
 * → MemoService.findById() がこの例外をスロー
 * → HTTPステータス 404 Not Found を返す
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class MemoNotFoundException extends RuntimeException {
    /*
     * RuntimeException を継承
     *
     * 【Javaの例外の種類】
     * 1. checked例外（Exception継承）
     *    - メソッド宣言にthrows指定が必要
     *    - 呼び出し側でtry-catchが必須
     *    - 例: IOException, SQLException
     *
     * 2. unchecked例外（RuntimeException継承）← このクラス
     *    - throws指定不要
     *    - try-catch任意
     *    - 例: NullPointerException, IllegalArgumentException
     *
     * 【なぜRuntimeExceptionか】
     * - ビジネスロジック層の例外は通常uncheckedにする
     * - 呼び出し側で毎回try-catchを書かなくて済む
     * - GlobalExceptionHandlerで一括処理できる
     */

    /**
     * コンストラクタ（メッセージあり）
     *
     * @param message エラーメッセージ
     *                例: "メモが見つかりません: id=123"
     */
    public MemoNotFoundException(String message) {
        /*
         * super(message)
         * 親クラス（RuntimeException）のコンストラクタを呼ぶ
         * これによりエラーメッセージが設定される
         */
        super(message);
    }

    /**
     * コンストラクタ（メッセージと原因あり）
     *
     * @param message エラーメッセージ
     * @param cause 原因となった例外
     *              例: データベース接続エラーなど
     */
    public MemoNotFoundException(String message, Throwable cause) {
        /*
         * super(message, cause)
         * メッセージと原因の両方を設定
         *
         * 【使用例】
         * try {
         *     // データベースアクセス
         * } catch (SQLException e) {
         *     throw new MemoNotFoundException("メモ取得失敗", e);
         * }
         *
         * こうすることで、エラーログに原因の例外も記録される
         */
        super(message, cause);
    }
}

/*
 * ============================================
 * 使用例（MemoServiceから）
 * ============================================
 *
 * 【修正前】
 * return memoMapper.findById(id)
 *         .map(MemoDto::fromEntity)
 *         .orElseThrow(() -> new IllegalArgumentException("メモが見つかりません: id=" + id));
 *
 * 【修正後】
 * return memoMapper.findById(id)
 *         .map(MemoDto::fromEntity)
 *         .orElseThrow(() -> new MemoNotFoundException("メモが見つかりません: id=" + id));
 *
 * 【違い】
 * 修正前: HTTPステータス 400（Bad Request）
 * 修正後: HTTPステータス 404（Not Found） ← より適切
 */

/*
 * ============================================
 * GlobalExceptionHandlerとの連携
 * ============================================
 *
 * GlobalExceptionHandlerに専用ハンドラを追加:
 *
 * @ExceptionHandler(MemoNotFoundException.class)
 * public String handleMemoNotFoundException(
 *         MemoNotFoundException ex,
 *         Model model
 * ) {
 *     log.warn("メモが見つかりませんでした: {}", ex.getMessage());
 *     model.addAttribute("errorMessage", ex.getMessage());
 *     return "error/error";
 * }
 *
 * これにより、メモが見つからない場合に:
 * 1. WARN レベルでログ出力（ERROR より低い）
 * 2. カスタムエラー画面を表示
 * 3. HTTPステータス 404 を返す
 */
