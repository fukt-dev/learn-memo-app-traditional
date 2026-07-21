package com.example.memoapp.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全 Controller で発生した例外を横断的に捕捉するグローバル例外ハンドラ。
 *
 * 各 Controller に try-catch を書き散らす代わりに、例外ごとの「エラー画面・HTTP ステータス・
 * ログレベル」をここに集約する。Spring は「発生した例外に最も型が近いハンドラ」を選ぶため、
 * 具体的な例外から順に、最後に Exception のフォールバックを置いている。
 *
 * 学習ポイント:
 * - @ControllerAdvice / @ExceptionHandler / カスタム例外 / @ResponseStatus の落とし穴 →
 *   docs/解説/例外処理とエラーページ.md
 *
 * モダン版との対比: モダン版は @RestControllerAdvice で、エラー画面ではなくエラー JSON を返す。
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * メモが見つからない場合（404）。
     *
     * 【落とし穴: 例外クラス側の @ResponseStatus は「ここでは」効かない】
     * MemoNotFoundException にも @ResponseStatus(NOT_FOUND) が付いているが、あれが効くのは
     * どの @ExceptionHandler にも捕捉されず Spring の既定処理まで届いた場合だけ。この
     * ハンドラが捕捉してビューを返すと Spring 的には「正常応答」となり、何も指定しなければ
     * ステータスは 200 になってしまう。そのためハンドラメソッド側にも 404 を明示している
     * （詳細は docs/解説/例外処理とエラーページ.md）。
     *
     * @param ex 発生した例外
     * @param model エラー画面へ渡すデータ
     * @return エラー画面のビュー名
     */
    @ExceptionHandler(MemoNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleMemoNotFoundException(MemoNotFoundException ex, Model model) {
        // リソース不在はシステム異常ではないので ERROR ではなく WARN
        log.warn("メモが見つかりませんでした: {}", ex.getMessage());
        model.addAttribute("errorMessage", ex.getMessage());
        return "error/error";
    }

    /**
     * 不正な引数（400）。MemoService.findAllWithPaging() の範囲外ページ指定などで発生する。
     *
     * IllegalArgumentException 自体に HTTP ステータスの情報は無く、ハンドラを書かなければ 500、
     * 書いてもステータス未指定なら 200 になる。「クライアントの入力が不正」の意味である 400 を
     * 返すにはハンドラ側で明示する必要がある。
     *
     * @param ex 発生した例外
     * @param model エラー画面へ渡すデータ
     * @return エラー画面のビュー名
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleIllegalArgumentException(IllegalArgumentException ex, Model model) {
        log.error("不正な引数が指定されました: {}", ex.getMessage(), ex);
        model.addAttribute("errorMessage", ex.getMessage());
        return "error/error";
    }

    /**
     * 存在しない URL へのアクセス（404）。Spring Boot 3.2 以降、未マッピングかつ静的リソースにも
     * 無い URL でスローされる（例: GET /memoss のタイプミス、未配置の /favicon.ico）。
     *
     * 専用ハンドラがないと、下の Exception フォールバックに吸い込まれ、ただの URL タイプミスが
     * 「システムエラー画面 + ERROR ログ」になってしまう。ユーザーには 404、ログは WARN が適切
     * （favicon.ico のような機械的アクセスで ERROR が積もると本当の障害が埋もれる）。
     *
     * @param ex 発生した例外
     * @param model エラー画面へ渡すデータ
     * @return エラー画面のビュー名
     */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNoResourceFoundException(NoResourceFoundException ex, Model model) {
        log.warn("存在しないURLへのアクセス: /{}", ex.getResourcePath());
        model.addAttribute("errorMessage", "お探しのページが見つかりませんでした");
        return "error/error";
    }

    /**
     * 上記のどれにも当てはまらない例外を受ける最後の砦（500）。
     *
     * ここに来るのは予期しないサーバー側の異常なので 500 を明示する（未指定だとエラー画面を
     * 出しつつステータス 200 になる）。ユーザーには汎用メッセージだけを見せ、内部情報を漏らさない。
     *
     * @param ex 発生した例外
     * @param model エラー画面へ渡すデータ
     * @return エラー画面のビュー名
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleException(Exception ex, Model model) {
        log.error("予期しないエラーが発生しました", ex); // 第 2 引数に例外を渡すとスタックトレースが残る
        model.addAttribute("errorMessage", "システムエラーが発生しました。時間をおいて再度お試しください。");
        return "error/error";
    }
}
