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
 * メモ機能の Controller（プレゼンテーション層）。
 *
 * 3 層アーキテクチャの入口。HTTP リクエストを受け取り、入力検証と Service 呼び出しを行い、
 * ビュー名（Thymeleaf テンプレート）を返す。ビジネスロジックや SQL はここには書かず、
 * Service / Mapper に委ねる。
 *
 * 学習ポイント:
 * - リクエスト処理の流れ・マッピング・Model・ビュー解決・PRG パターン → docs/解説/Spring-MVCとThymeleaf.md
 * - @Valid と BindingResult による入力検証の流れ → docs/解説/Bean-Validation.md
 *
 * モダン版との対比: モダン版は @RestController で JSON を返し、画面生成は React が担う。
 * 二重送信対策も従来型の PRG（リダイレクト）ではなくクライアント側の状態管理で行う。
 */
@Controller
@Slf4j                    // ログ出力用の log フィールドを生成（docs/解説/DIとLombok.md）
@RequiredArgsConstructor  // final フィールドのコンストラクタを生成し memoService を注入
@RequestMapping("/memos") // このクラスの全メソッド共通の URL 接頭辞
public class MemoController {

    private final MemoService memoService;

    /**
     * 一覧画面を表示（keyword があれば検索、なければ全件）。
     *
     * 一覧と検索を同じ GET /memos で受ける。keyword が空かどうかの判断は Service.search() に
     * 委譲し、Controller 側に if/else を書かない。「キーワードが空なら全件」はビジネスルール
     * なので判断は Service に置き、同じ判断を Controller にも書いて二重管理・将来ズレるのを避ける。
     *
     * @param keyword 検索キーワード（任意。/memos は null、/memos?keyword=... で値が入る）
     * @param model 画面に渡すデータの入れ物
     * @return ビュー名（templates/memos/list.html）
     */
    @GetMapping("")
    public String list(
            @RequestParam(name = "keyword", required = false) String keyword,
            Model model
    ) {
        log.debug("メモ一覧画面を表示します。検索キーワード: {}", keyword);

        List<MemoDto> memos = memoService.search(keyword);

        model.addAttribute("memos", memos);
        model.addAttribute("keyword", keyword); // 検索後もフォームにキーワードを残すため
        return "memos/list";
    }

    /**
     * 新規作成画面を表示。
     *
     * 空の MemoDto を渡すのは、フォームの th:object="${memoDto}" が束ねる対象を用意し、
     * かつ検証エラー時に入力値を保持できるようにするため。
     *
     * @param model 画面に渡すデータの入れ物
     * @return ビュー名（templates/memos/new.html）
     */
    @GetMapping("/new")
    public String newMemo(Model model) {
        log.debug("メモ新規作成画面を表示します");
        model.addAttribute("memoDto", new MemoDto());
        return "memos/new";
    }

    /**
     * メモを新規登録。
     *
     * @Valid で検証し、失敗時は入力画面へ戻す。成功時は一覧へリダイレクトする（PRG パターン。
     * リダイレクトせず画面を直接返すと、F5 で POST が再送信され二重登録になる。詳細は
     * docs/解説/Spring-MVCとThymeleaf.md）。BindingResult は @Valid の直後に置く必要がある。
     *
     * @param memoDto フォーム送信値（@ModelAttribute でバインド）
     * @param bindingResult 検証結果
     * @param redirectAttributes リダイレクト先へ渡すフラッシュメッセージ用
     * @return 検証失敗時は入力画面、成功時は一覧へのリダイレクト
     */
    @PostMapping("")
    public String create(
            @Valid @ModelAttribute MemoDto memoDto,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes
    ) {
        log.debug("メモを登録します: {}", memoDto);

        if (bindingResult.hasErrors()) {
            log.debug("バリデーションエラー: {}", bindingResult.getAllErrors());
            return "memos/new"; // memoDto は Model に残るので入力値とエラーを再表示できる
        }

        memoService.create(memoDto);
        redirectAttributes.addFlashAttribute("successMessage", "メモを登録しました");
        return "redirect:/memos";
    }

    /**
     * メモ詳細画面を表示。
     *
     * @param id メモ ID（URL パスから取得）
     * @param model 画面に渡すデータの入れ物
     * @return ビュー名（templates/memos/show.html）
     */
    @GetMapping("/{id}")
    public String show(@PathVariable Long id, Model model) {
        log.debug("メモ詳細画面を表示します: id={}", id);
        MemoDto memo = memoService.findById(id);
        model.addAttribute("memo", memo);
        return "memos/show";
    }

    /**
     * 編集画面を表示。取得済みのメモをフォームの初期値にする。
     *
     * @param id メモ ID
     * @param model 画面に渡すデータの入れ物
     * @return ビュー名（templates/memos/edit.html）
     */
    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model model) {
        log.debug("メモ編集画面を表示します: id={}", id);
        MemoDto memo = memoService.findById(id);
        model.addAttribute("memoDto", memo);
        return "memos/edit";
    }

    /**
     * メモを更新。検証成功時は詳細画面へリダイレクトする（PRG パターン）。
     *
     * @param id メモ ID
     * @param memoDto フォーム送信値
     * @param bindingResult 検証結果
     * @param redirectAttributes リダイレクト先へ渡すフラッシュメッセージ用
     * @return 検証失敗時は編集画面、成功時は詳細へのリダイレクト
     */
    @PostMapping("/{id}/update")
    public String update(
            @PathVariable Long id,
            @Valid @ModelAttribute MemoDto memoDto,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes
    ) {
        log.debug("メモを更新します: id={}, {}", id, memoDto);

        if (bindingResult.hasErrors()) {
            log.debug("バリデーションエラー: {}", bindingResult.getAllErrors());
            // フォームの action URL 生成に id が要るので、URL から得た id を詰め直す
            memoDto.setId(id);

            // 【なぜ日時を取り直すのか】
            // フォームからは title と content しか送られてこないため、この時点の memoDto の
            // 日時フィールドは null。そのまま画面へ戻すと日時欄が空になる。DB から現在値を
            // 取り直して表示用フィールドだけ補完する。日時を hidden で往復させる手もあるが、
            // 改ざんできる項目を増やさないため、サーバー側で取り直す方が安全。
            MemoDto current = memoService.findById(id);
            memoDto.setCreatedAtFormatted(current.getCreatedAtFormatted());
            memoDto.setUpdatedAtFormatted(current.getUpdatedAtFormatted());
            return "memos/edit";
        }

        memoDto.setId(id); // フォームからは title/content のみ。id は URL から詰める
        memoService.update(memoDto);
        redirectAttributes.addFlashAttribute("successMessage", "メモを更新しました");
        return "redirect:/memos/" + id;
    }

    /**
     * メモを削除。
     *
     * 削除は GET ではなく POST で受ける。GET は「安全な操作（データを変えない）」に限るべきで、
     * GET にするとリンククリックやブラウザのプリフェッチで誤削除が起きうるため。
     *
     * @param id メモ ID
     * @param redirectAttributes リダイレクト先へ渡すフラッシュメッセージ用
     * @return 一覧へのリダイレクト
     */
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        log.debug("メモを削除します: id={}", id);
        memoService.delete(id);
        redirectAttributes.addFlashAttribute("successMessage", "メモを削除しました");
        return "redirect:/memos";
    }
}
