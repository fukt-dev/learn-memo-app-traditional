package com.example.memoapp.controller;

import com.example.memoapp.dto.MemoDto;
import com.example.memoapp.service.MemoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * ============================================
 * MemoControllerクラス（プレゼンテーション層）
 * ============================================
 *
 * 【このクラスの役割】
 * HTTPリクエストを受け取り、適切な処理を行い、レスポンスを返します
 *
 * 【プレゼンテーション層とは】
 * アプリケーションの「窓口」に相当する層
 * - HTTPリクエストの受け取り
 * - リクエストパラメータの取得
 * - バリデーション
 * - Serviceの呼び出し
 * - レスポンスの返却（画面表示、リダイレクト）
 *
 * 【3層アーキテクチャにおける位置付け】
 *
 * ┌─────────────────────────┐
 * │ ブラウザ（ユーザー）    │
 * └─────────────────────────┘
 *           ↓↑ HTTPリクエスト/レスポンス
 * ┌─────────────────────────┐
 * │ プレゼンテーション層    │ ← Controller（このクラス）
 * └─────────────────────────┘
 *           ↓↑
 * ┌─────────────────────────┐
 * │ ビジネスロジック層      │ ← Service
 * └─────────────────────────┘
 *           ↓↑
 * ┌─────────────────────────┐
 * │ データアクセス層        │ ← Mapper
 * └─────────────────────────┘
 *
 * 【Controllerの責務】
 *
 * ○ やるべきこと:
 * - HTTPリクエストのマッピング（@GetMapping, @PostMapping）
 * - リクエストパラメータの取得（@PathVariable, @RequestParam）
 * - バリデーション（@Valid, BindingResult）
 * - Serviceの呼び出し
 * - Modelへのデータ設定（画面に渡すデータ）
 * - ビュー名の返却（どのHTMLを表示するか）
 *
 * × やってはいけないこと:
 * - ビジネスロジックを書く（Serviceの責務）
 * - 直接Mapperを呼ぶ（Serviceを経由する）
 * - データベース操作（Mapperの責務）
 */

/**
 * @Controller
 *
 * このクラスがSpring MVCのControllerであることを示す
 * @Component の特殊化版で、以下の機能を持つ:
 * - Beanとして登録される
 * - リクエストハンドラメソッドを持てる
 * - ビュー名を返せる
 *
 * 【@Controller と @RestController の違い】
 * @Controller: ビュー名を返す（Thymeleafなどのテンプレートを使用）
 * @RestController: JSONやXMLを返す（RESTful APIに使用）
 *
 * このアプリはThymeleafを使うので @Controller を使用
 */
@Controller

/**
 * @Slf4j
 * Lombokのアノテーション
 * ログ出力用の log フィールドを自動生成
 */
@Slf4j

/**
 * @RequiredArgsConstructor
 * Lombokのアノテーション
 * final フィールドを引数に取るコンストラクタを自動生成
 * MemoService を注入するために使用
 */
@RequiredArgsConstructor

/**
 * @RequestMapping
 *
 * このControllerの基底URLパスを指定
 * このControllerの全メソッドに共通のパスプレフィックス
 *
 * 例:
 * @RequestMapping("/memos") + @GetMapping("") → GET /memos
 * @RequestMapping("/memos") + @GetMapping("/{id}") → GET /memos/{id}
 * @RequestMapping("/memos") + @PostMapping("") → POST /memos
 */
@RequestMapping("/memos")
public class MemoController {

    /*
     * ============================================
     * 依存関係（フィールド）
     * ============================================
     */

    /**
     * MemoService（ビジネスロジック層）
     * @RequiredArgsConstructor によりコンストラクタで注入される
     */
    private final MemoService memoService;

    /*
     * ============================================
     * リクエストハンドラメソッド
     * ============================================
     */

    /**
     * メモ一覧画面を表示（検索機能統合）
     *
     * 【URL】
     * GET /memos
     * GET /memos?keyword=xxx
     *
     * 【処理の流れ】
     * 1. keywordパラメータがある場合は検索、ない場合は全件取得
     * 2. Modelに設定
     * 3. ビュー名を返す
     * 4. Thymeleafがテンプレートをレンダリング
     * 5. HTMLがブラウザに返される
     *
     * 【RESTful設計】
     * 一覧取得と検索を同じエンドポイントで処理する
     * これにより、URLがシンプルになり、一貫性が向上する
     *
     * @param keyword 検索キーワード（任意パラメータ）
     * @param model Modelオブジェクト（画面に渡すデータを格納）
     * @return ビュー名（templates/memos/list.html）
     */
    @GetMapping("")
    /*
     * @GetMapping
     *
     * GETリクエストを処理するメソッドであることを示す
     * @GetMapping("") → /memos へのGETリクエスト
     * @GetMapping("/abc") → /memos/abc へのGETリクエスト
     *
     * 【HTTPメソッド】
     * - GET: データの取得（一覧表示、詳細表示など）
     * - POST: データの送信（登録、更新、削除など）
     * - PUT: データの更新（RESTful APIで使用）
     * - DELETE: データの削除（RESTful APIで使用）
     *
     * Webアプリ（HTML）では、主にGETとPOSTを使用
     */
    public String list(
            @RequestParam(name = "keyword", required = false) String keyword,
            /*
             * @RequestParam
             * クエリパラメータを受け取る
             *
             * URL: /memos?keyword=買い物
             * → keyword = "買い物"
             *
             * URL: /memos
             * → keyword = null（required=false のため）
             *
             * required = false: 任意パラメータ（なくてもOK）
             * これにより、一覧表示と検索を同じエンドポイントで処理できる
             */
            Model model
            /*
             * Model
             *
             * 画面（View）に渡すデータを格納するオブジェクト
             * Springが自動的に引数に渡してくれる
             *
             * model.addAttribute("キー", 値)
             * → Thymeleafで ${キー} として参照できる
             */
    ) {
        log.debug("メモ一覧画面を表示します。検索キーワード: {}", keyword);

        /*
         * keywordがある場合は検索、ない場合は全件取得
         * MemoService.search() 内でnullチェックを行っているため、
         * ここでは単純に呼び出すだけでOK
         */
        List<MemoDto> memos;
        if (keyword != null && !keyword.trim().isEmpty()) {
            memos = memoService.search(keyword);
        } else {
            memos = memoService.findAll();
        }

        /*
         * Modelにデータを設定
         * "memos" → Thymeleaf側で ${memos} として参照可能
         * "keyword" → 検索後もキーワードをフォームに残すため
         */
        model.addAttribute("memos", memos);
        model.addAttribute("keyword", keyword);

        /*
         * ビュー名を返す
         *
         * "memos/list" → src/main/resources/templates/memos/list.html
         *
         * 【ビュー名の解決ルール】
         * application.ymlの設定により:
         * spring:
         *   thymeleaf:
         *     prefix: classpath:/templates/
         *     suffix: .html
         *
         * "memos/list"
         * → prefix + "memos/list" + suffix
         * → "classpath:/templates/memos/list.html"
         */
        return "memos/list";
    }

    /**
     * 新規作成画面を表示
     *
     * 【URL】
     * GET /memos/new
     *
     * @param model Modelオブジェクト
     * @return ビュー名（templates/memos/new.html）
     */
    @GetMapping("/new")
    public String newMemo(Model model) {
        log.debug("メモ新規作成画面を表示します");

        /*
         * 空のDTOをModelに設定
         *
         * なぜ必要か:
         * - Thymeleafのフォームで th:object="${memoDto}" を使うため
         * - 初期表示では空のフォームを表示する
         * - バリデーションエラー時に入力値を保持するため
         */
        model.addAttribute("memoDto", new MemoDto());

        return "memos/new";
    }

    /**
     * メモを新規登録
     *
     * 【URL】
     * POST /memos
     *
     * 【処理の流れ】
     * 1. フォームから送信されたデータをMemoDtoで受け取る
     * 2. バリデーション（@Valid）
     * 3. エラーがあれば入力画面に戻る
     * 4. エラーがなければServiceで登録
     * 5. 一覧画面にリダイレクト
     *
     * @param memoDto フォームから送信されたデータ
     * @param bindingResult バリデーション結果
     * @param redirectAttributes リダイレクト先に渡すデータ
     * @return ビュー名またはリダイレクトURL
     */
    @PostMapping("")
    public String create(
            @Valid @ModelAttribute MemoDto memoDto,
            /*
             * @Valid
             * バリデーションを実行する
             * MemoDto の @NotBlank, @Size などをチェック
             *
             * @ModelAttribute
             * フォームのデータをMemoDtoにバインドする
             * 省略可能だが、明示的に書くことを推奨
             *
             * HTMLフォーム:
             * <input name="title" />    → memoDto.setTitle()
             * <textarea name="content"> → memoDto.setContent()
             */
            BindingResult bindingResult,
            /*
             * BindingResult
             * バリデーション結果を格納するオブジェクト
             * @Valid の直後に配置する必要がある
             *
             * bindingResult.hasErrors(): エラーがあるか
             * bindingResult.getAllErrors(): 全エラーのリスト
             * bindingResult.getFieldErrors(): フィールドごとのエラー
             */
            RedirectAttributes redirectAttributes
            /*
             * RedirectAttributes
             * リダイレクト先に渡すデータを格納
             *
             * redirectAttributes.addFlashAttribute("message", "登録しました")
             * → リダイレクト先で ${message} として参照可能
             * → リロードしてもメッセージは消える（フラッシュスコープ）
             */
    ) {
        log.debug("メモを登録します: {}", memoDto);

        /*
         * バリデーションエラーチェック
         */
        if (bindingResult.hasErrors()) {
            /*
             * エラーがある場合
             * - 入力画面（new.html）に戻る
             * - ModelにmemoDtoが自動的に設定される
             * - Thymeleafでエラーメッセージを表示できる
             */
            log.debug("バリデーションエラー: {}", bindingResult.getAllErrors());
            return "memos/new";
        }

        /*
         * Serviceで登録
         */
        memoService.create(memoDto);

        /*
         * フラッシュメッセージを設定
         * リダイレクト先（一覧画面）で表示される
         */
        redirectAttributes.addFlashAttribute("successMessage", "メモを登録しました");

        /*
         * リダイレクト
         *
         * "redirect:/memos"
         * → 別のURLにリダイレクトする
         * → ブラウザに「GET /memos にアクセスしてね」という指示を返す
         * → ブラウザが GET /memos を実行
         * → list() メソッドが呼ばれる
         *
         * 【なぜリダイレクトするのか（PRG パターン）】
         * Post-Redirect-Get パターン
         *
         * リダイレクトしない場合:
         * POST /memos → 登録成功 → そのまま画面表示
         * → ブラウザでF5（リロード）すると POST が再送信される
         * → 同じデータが2重登録される！
         *
         * リダイレクトする場合:
         * POST /memos → 登録成功 → リダイレクト → GET /memos → 一覧表示
         * → ブラウザでF5すると GET /memos が再送信される
         * → 一覧表示が再表示されるだけ（2重登録されない）
         */
        return "redirect:/memos";
    }

    /**
     * メモ詳細画面を表示
     *
     * 【URL】
     * GET /memos/{id}
     * 例: GET /memos/1
     *
     * @param id メモID（URLパスから取得）
     * @param model Modelオブジェクト
     * @return ビュー名（templates/memos/show.html）
     */
    @GetMapping("/{id}")
    public String show(
            @PathVariable Long id,
            /*
             * @PathVariable
             * URLパスの一部を変数として受け取る
             *
             * @GetMapping("/{id}") + @PathVariable Long id
             * → URL /memos/1 の場合、id = 1
             * → URL /memos/123 の場合、id = 123
             *
             * 【型変換】
             * URLの文字列が自動的にLong型に変換される
             * 変換できない場合（例: /memos/abc）はエラー
             */
            Model model
    ) {
        log.debug("メモ詳細画面を表示します: id={}", id);

        /*
         * Serviceから指定IDのメモを取得
         */
        MemoDto memo = memoService.findById(id);

        /*
         * Modelに設定
         */
        model.addAttribute("memo", memo);

        return "memos/show";
    }

    /**
     * 編集画面を表示
     *
     * 【URL】
     * GET /memos/{id}/edit
     * 例: GET /memos/1/edit
     *
     * @param id メモID
     * @param model Modelオブジェクト
     * @return ビュー名（templates/memos/edit.html）
     */
    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model model) {
        log.debug("メモ編集画面を表示します: id={}", id);

        /*
         * Serviceから指定IDのメモを取得
         */
        MemoDto memo = memoService.findById(id);

        /*
         * Modelに設定
         * フォームの初期値として使用される
         */
        model.addAttribute("memoDto", memo);

        return "memos/edit";
    }

    /**
     * メモを更新
     *
     * 【URL】
     * POST /memos/{id}/update
     *
     * @param id メモID
     * @param memoDto フォームから送信されたデータ
     * @param bindingResult バリデーション結果
     * @param redirectAttributes リダイレクト先に渡すデータ
     * @return ビュー名またはリダイレクトURL
     */
    @PostMapping("/{id}/update")
    public String update(
            @PathVariable Long id,
            @Valid @ModelAttribute MemoDto memoDto,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes
    ) {
        log.debug("メモを更新します: id={}, {}", id, memoDto);

        /*
         * バリデーションエラーチェック
         */
        if (bindingResult.hasErrors()) {
            log.debug("バリデーションエラー: {}", bindingResult.getAllErrors());
            /*
             * エラー時は編集画面に戻る
             * memoDto.setId() でIDを設定しておかないと、
             * フォームのaction URLが正しく生成されない
             */
            memoDto.setId(id);
            return "memos/edit";
        }

        /*
         * DTOにIDを設定
         * フォームからはtitle, contentのみ送信される
         * idはURLから取得してセット
         */
        memoDto.setId(id);

        /*
         * Serviceで更新
         */
        memoService.update(memoDto);

        /*
         * フラッシュメッセージを設定
         */
        redirectAttributes.addFlashAttribute("successMessage", "メモを更新しました");

        /*
         * 詳細画面にリダイレクト
         * "redirect:/memos/" + id
         * → 例: id=1 の場合、"redirect:/memos/1"
         */
        return "redirect:/memos/" + id;
    }

    /**
     * メモを削除
     *
     * 【URL】
     * POST /memos/{id}/delete
     *
     * 【なぜPOSTか】
     * - GETは安全な操作（データを変更しない）のみに使うべき
     * - 削除はデータを変更するのでPOSTを使う
     * - GETだと、リンククリックやブラウザのプリフェッチで誤削除の危険
     *
     * @param id メモID
     * @param redirectAttributes リダイレクト先に渡すデータ
     * @return リダイレクトURL
     */
    @PostMapping("/{id}/delete")
    public String delete(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes
    ) {
        log.debug("メモを削除します: id={}", id);

        /*
         * Serviceで削除
         */
        memoService.delete(id);

        /*
         * フラッシュメッセージを設定
         */
        redirectAttributes.addFlashAttribute("successMessage", "メモを削除しました");

        /*
         * 一覧画面にリダイレクト
         */
        return "redirect:/memos";
    }

}

/*
 * ============================================
 * Spring MVCの仕組み（補足）
 * ============================================
 *
 * 【リクエスト処理の流れ】
 *
 * 1. ブラウザから HTTPリクエスト
 *    例: GET /memos
 *
 * 2. DispatcherServlet（Spring MVCの中心）がリクエストを受け取る
 *
 * 3. HandlerMapping が適切なControllerメソッドを探す
 *    @GetMapping("") のメソッドを見つける
 *
 * 4. Controllerメソッドを実行
 *    list() メソッドが呼ばれる
 *
 * 5. Controllerがビュー名を返す
 *    return "memos/list"
 *
 * 6. ViewResolver がビュー名からテンプレートファイルを解決
 *    "memos/list" → templates/memos/list.html
 *
 * 7. Thymeleaf がテンプレートをレンダリング
 *    ${memos} などの式を評価してHTMLを生成
 *
 * 8. 生成されたHTMLをブラウザに返す
 *
 * 【Modelの仕組み】
 *
 * Controller:
 * model.addAttribute("memos", memos);
 *
 * ↓ Spring MVCが内部で管理
 *
 * Thymeleaf:
 * <div th:each="memo : ${memos}">
 *
 * 【リダイレクトの仕組み】
 *
 * Controller:
 * return "redirect:/memos";
 *
 * ↓
 *
 * HTTPレスポンス:
 * HTTP/1.1 302 Found
 * Location: /memos
 *
 * ↓
 *
 * ブラウザ:
 * GET /memos を実行
 *
 * ↓
 *
 * Controller:
 * list() メソッドが呼ばれる
 */

/*
 * ============================================
 * バリデーションエラーの表示（Thymeleaf側）
 * ============================================
 *
 * Controller:
 * if (bindingResult.hasErrors()) {
 *     return "memos/new";
 * }
 *
 * Thymeleaf (new.html):
 * <form th:object="${memoDto}">
 *     <input th:field="*{title}" />
 *     <span th:if="${#fields.hasErrors('title')}" th:errors="*{title}"></span>
 * </form>
 *
 * エラーメッセージ:
 * <span class="error">タイトルを入力してください</span>
 */

/*
 * ============================================
 * フラッシュメッセージの表示（Thymeleaf側）
 * ============================================
 *
 * Controller:
 * redirectAttributes.addFlashAttribute("successMessage", "登録しました");
 * return "redirect:/memos";
 *
 * Thymeleaf (list.html):
 * <div th:if="${successMessage}" class="alert alert-success">
 *     <span th:text="${successMessage}"></span>
 * </div>
 *
 * 表示結果:
 * <div class="alert alert-success">
 *     <span>登録しました</span>
 * </div>
 */
