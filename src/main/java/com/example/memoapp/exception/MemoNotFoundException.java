package com.example.memoapp.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * メモが見つからないときにスローするカスタム例外。
 *
 * 汎用の IllegalArgumentException ではなく専用の例外にすることで、「不正な引数」ではなく
 * 「メモが存在しない = 404」という意図を型で表現でき、GlobalExceptionHandler で専用ハンドリング
 * できる。RuntimeException を継承した unchecked 例外なので、呼び出し側は try-catch を強制されず、
 * @ControllerAdvice で一括処理できる。
 *
 * カスタム例外を作る理由・checked/unchecked の違い・HTTP ステータスの使い分けは
 * docs/解説/例外処理とエラーページ.md を参照。
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
// 【注意】この @ResponseStatus が実際に 404 を決めるのは、例外がどの @ExceptionHandler にも
// 捕捉されず Spring の既定処理まで届いた場合だけ。本アプリでは GlobalExceptionHandler が捕捉して
// ビューを返すため、404 を決めているのはハンドラメソッド側の @ResponseStatus。ここに残すのは
// 「この例外は 404 を意図する」という仕様の宣言と、将来ハンドラを外したときの保険のため。
public class MemoNotFoundException extends RuntimeException {

    /**
     * メッセージ付きコンストラクタ。
     *
     * @param message エラーメッセージ（例: "メモが見つかりません: id=123"）
     */
    public MemoNotFoundException(String message) {
        super(message);
    }

    /**
     * メッセージと原因例外を持つコンストラクタ。原因を渡すとログにスタックトレースが残る。
     *
     * @param message エラーメッセージ
     * @param cause 原因となった例外
     */
    public MemoNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
