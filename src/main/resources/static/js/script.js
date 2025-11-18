/**
 * ============================================
 * メモ帳アプリ JavaScript
 * ============================================
 *
 * このファイルはクライアントサイド（ブラウザ）で実行されるJavaScriptです
 *
 * 【JavaScriptとは】
 * - ブラウザで動作するプログラミング言語
 * - HTMLやCSSを動的に操作できる
 * - ユーザーのアクションに応じて処理を実行できる
 *
 * 【このファイルの役割】
 * - フラッシュメッセージの自動非表示
 * - フォームのバリデーション補助
 * - ユーザーエクスペリエンスの向上
 */

/*
 * ============================================
 * DOMContentLoaded イベント
 * ============================================
 *
 * HTMLの読み込みが完了したら実行される
 * これにより、HTML要素が存在することが保証される
 */
document.addEventListener('DOMContentLoaded', function() {
    /*
     * addEventListener
     * イベントリスナーを登録する
     *
     * 引数:
     * 1. イベント名: 'DOMContentLoaded'
     * 2. 実行する関数: function() { ... }
     */

    console.log('メモ帳アプリが読み込まれました');
    /*
     * console.log()
     * ブラウザの開発者ツールのコンソールに出力
     * デバッグに使用
     *
     * 確認方法:
     * ブラウザでF12 → Console タブ
     */

    // フラッシュメッセージの自動非表示を初期化
    initAutoHideAlerts();

    // フォームのバリデーションを初期化
    initFormValidation();

    // 削除確認ダイアログを初期化
    initDeleteConfirmation();
});

/*
 * ============================================
 * フラッシュメッセージの自動非表示
 * ============================================
 *
 * 成功メッセージなどを一定時間後に自動的に消す
 */
function initAutoHideAlerts() {
    /*
     * function 関数名() { ... }
     * 関数の定義
     *
     * 関数とは、処理をまとめたもの
     * 必要な時に何度でも呼び出せる
     */

    // アラート要素を取得
    const alerts = document.querySelectorAll('.alert');
    /*
     * document.querySelectorAll()
     * 指定したCSSセレクタに一致する全ての要素を取得
     *
     * 戻り値: NodeList（配列のようなオブジェクト）
     */

    /*
     * forEach()
     * 配列やNodeListの各要素に対して処理を実行
     *
     * 引数の関数が各要素に対して呼ばれる
     * alert = 現在の要素
     */
    alerts.forEach(function(alert) {
        /*
         * setTimeout()
         * 指定した時間後に関数を実行
         *
         * 引数:
         * 1. 実行する関数
         * 2. 待機時間（ミリ秒）
         *
         * 5000ミリ秒 = 5秒
         */
        setTimeout(function() {
            /*
             * アニメーションで消える
             * opacity を 0 にして、その後要素を削除
             */

            // フェードアウトアニメーション
            alert.style.transition = 'opacity 0.5s';
            /*
             * style プロパティ
             * 要素のインラインスタイルを設定
             *
             * alert.style.transition = 'opacity 0.5s'
             * → <div style="transition: opacity 0.5s">
             */

            alert.style.opacity = '0';
            // 透明度を0にする（透明になる）

            // アニメーション完了後に要素を削除
            setTimeout(function() {
                alert.remove();
                /*
                 * remove()
                 * DOM（Document Object Model）から要素を削除
                 * 画面から消える
                 */
            }, 500);  // 0.5秒後
        }, 5000);  // 5秒後
    });
}

/*
 * ============================================
 * フォームのバリデーション
 * ============================================
 *
 * 送信前にクライアント側でもチェックする
 * サーバー側のバリデーションと併用する
 */
function initFormValidation() {
    // フォームを取得
    const forms = document.querySelectorAll('.memo-form');

    forms.forEach(function(form) {
        /*
         * submit イベントリスナーを追加
         * フォームが送信される直前に実行される
         */
        form.addEventListener('submit', function(event) {
            /*
             * event
             * イベントオブジェクト
             * イベントに関する情報を持つ
             */

            // タイトル入力欄を取得
            const titleInput = form.querySelector('#title');
            const contentInput = form.querySelector('#content');

            let isValid = true;  // バリデーション結果
            /*
             * let
             * 変数の宣言
             * 値を変更できる
             *
             * const との違い:
             * - const: 再代入不可（定数）
             * - let: 再代入可能（変数）
             */

            // タイトルのチェック
            if (titleInput && titleInput.value.trim() === '') {
                /*
                 * &&
                 * AND演算子
                 * 両方がtrueの場合のみtrue
                 *
                 * ===
                 * 厳密等価演算子
                 * 値と型が同じ場合true
                 *
                 * trim()
                 * 文字列の前後の空白を削除
                 * '  abc  '.trim() → 'abc'
                 */

                // エラーメッセージを表示
                showError(titleInput, 'タイトルを入力してください');
                isValid = false;
            } else if (titleInput) {
                // エラーメッセージをクリア
                clearError(titleInput);
            }

            // 本文のチェック
            if (contentInput && contentInput.value.trim() === '') {
                showError(contentInput, '本文を入力してください');
                isValid = false;
            } else if (contentInput) {
                clearError(contentInput);
            }

            // バリデーション失敗の場合、送信を中止
            if (!isValid) {
                /*
                 * !
                 * NOT演算子
                 * trueとfalseを反転
                 * !true → false
                 * !false → true
                 */

                event.preventDefault();
                /*
                 * preventDefault()
                 * イベントのデフォルト動作をキャンセル
                 *
                 * submitイベントの場合:
                 * フォームの送信を中止する
                 */
            }
        });
    });
}

/*
 * エラーメッセージを表示する関数
 */
function showError(input, message) {
    /*
     * 引数:
     * input - 入力要素（<input>や<textarea>）
     * message - エラーメッセージ
     */

    // is-invalid クラスを追加
    input.classList.add('is-invalid');
    /*
     * classList
     * 要素のクラスリストを操作するオブジェクト
     *
     * .add('クラス名') - クラスを追加
     * .remove('クラス名') - クラスを削除
     * .toggle('クラス名') - クラスの追加/削除を切り替え
     * .contains('クラス名') - クラスが存在するかチェック
     */

    // 既存のエラーメッセージを削除
    const existingError = input.parentElement.querySelector('.error-message');
    /*
     * parentElement
     * 親要素を取得
     *
     * querySelector()
     * 指定したセレクタに一致する最初の要素を取得
     */

    if (existingError) {
        existingError.remove();
    }

    // エラーメッセージ要素を作成
    const errorSpan = document.createElement('span');
    /*
     * createElement()
     * 新しいHTML要素を作成
     */

    errorSpan.className = 'error-message';
    // クラスを設定

    errorSpan.textContent = message;
    /*
     * textContent
     * 要素のテキスト内容を設定
     * HTMLエスケープが自動的に行われる（XSS対策）
     */

    // エラーメッセージを入力欄の後に挿入
    input.parentElement.appendChild(errorSpan);
    /*
     * appendChild()
     * 子要素として追加
     */
}

/*
 * エラーメッセージをクリアする関数
 */
function clearError(input) {
    // is-invalid クラスを削除
    input.classList.remove('is-invalid');

    // エラーメッセージを削除
    const errorSpan = input.parentElement.querySelector('.error-message');
    if (errorSpan) {
        errorSpan.remove();
    }
}

/*
 * ============================================
 * 削除確認ダイアログ
 * ============================================
 *
 * 削除ボタンをクリックした時の確認ダイアログ
 * HTMLのonclick属性でも実装できるが、JavaScriptで一元管理する
 */
function initDeleteConfirmation() {
    /*
     * この関数は、HTMLの onclick 属性と重複するため、
     * 現在は使用していない
     *
     * HTMLでの実装:
     * <button onclick="return confirm('本当に削除しますか？')">削除</button>
     *
     * JavaScriptでの実装例:
     */

    const deleteForms = document.querySelectorAll('form[action*="/delete"]');
    /*
     * [action*="/delete"]
     * 属性セレクタ
     * action属性に "/delete" を含む要素を選択
     *
     * *= は「含む」という意味
     * 他の演算子:
     * - [attr="value"]: 完全一致
     * - [attr^="value"]: 前方一致
     * - [attr$="value"]: 後方一致
     */

    deleteForms.forEach(function(form) {
        form.addEventListener('submit', function(event) {
            /*
             * confirm()
             * 確認ダイアログを表示
             *
             * 戻り値:
             * - OK: true
             * - キャンセル: false
             */
            const isConfirmed = confirm('本当に削除しますか？');

            if (!isConfirmed) {
                // キャンセルされた場合、送信を中止
                event.preventDefault();
            }
        });
    });
}

/*
 * ============================================
 * ユーティリティ関数
 * ============================================
 */

/*
 * 文字数カウンター（オプション）
 *
 * 使用例:
 * <textarea id="content" maxlength="1000"></textarea>
 * <span id="charCount">0 / 1000</span>
 *
 * addCharCounter('content', 'charCount', 1000);
 */
function addCharCounter(inputId, counterId, maxLength) {
    const input = document.getElementById(inputId);
    const counter = document.getElementById(counterId);

    if (!input || !counter) {
        return;  // 要素が見つからない場合は何もしない
    }

    // 初期表示
    updateCount();

    // 入力時に更新
    input.addEventListener('input', updateCount);
    /*
     * input イベント
     * テキストが入力されるたびに発火
     */

    function updateCount() {
        const length = input.value.length;
        counter.textContent = `${length} / ${maxLength}`;
        /*
         * テンプレートリテラル
         * バッククォート ` で囲む
         * ${変数} で変数を埋め込める
         *
         * 例:
         * const name = '太郎';
         * `こんにちは、${name}さん` → "こんにちは、太郎さん"
         */

        // 最大文字数を超えた場合、赤くする
        if (length > maxLength) {
            counter.style.color = 'red';
        } else {
            counter.style.color = '';  // デフォルトの色に戻す
        }
    }
}

/*
 * ============================================
 * JavaScriptのデバッグ方法
 * ============================================
 *
 * 【1. console.log()】
 * console.log('変数の値:', 変数);
 * → ブラウザのコンソールに出力
 *
 * 【2. ブレークポイント】
 * ブラウザの開発者ツール → Sources タブ
 * → 行番号をクリックしてブレークポイントを設定
 * → 実行がそこで止まり、変数の値を確認できる
 *
 * 【3. debugger文】
 * debugger;
 * → この行で実行が一時停止する
 *
 * 【4. エラーメッセージの確認】
 * ブラウザのコンソールにエラーが表示される
 * - エラーの内容
 * - 発生した行番号
 * - スタックトレース
 */

/*
 * ============================================
 * JavaScriptのベストプラクティス
 * ============================================
 *
 * 【1. use strict】
 * 'use strict';
 * → 厳格モード
 * 危険な構文をエラーにする
 *
 * 【2. const / let の使い分け】
 * - 再代入しない場合: const
 * - 再代入する場合: let
 * - var は使わない（スコープの問題がある）
 *
 * 【3. 関数の命名】
 * - 動詞で始める: initForm(), showError(), validateInput()
 * - キャメルケース: myFunction, getUserName
 *
 * 【4. セキュリティ】
 * - textContent を使う（HTMLエスケープされる）
 * - innerHTML は避ける（XSSの危険）
 * - ユーザー入力は必ずバリデーション
 *
 * 【5. パフォーマンス】
 * - DOMアクセスは最小限に
 * - イベントリスナーは適切に削除
 * - 大量のデータは仮想化を検討
 */
