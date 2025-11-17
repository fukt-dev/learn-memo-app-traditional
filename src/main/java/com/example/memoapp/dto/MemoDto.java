package com.example.memoapp.dto;

import com.example.memoapp.entity.Memo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * ============================================
 * MemoDtoクラス（Data Transfer Object）
 * ============================================
 *
 * 【このクラスの役割】
 * 画面（View）とController/Serviceの間でデータをやり取りするためのクラスです
 *
 * 【DTO（Data Transfer Object）とは】
 * データを転送するためのオブジェクト
 * - 画面からの入力を受け取る
 * - 画面に表示するデータを渡す
 * - バリデーション（入力検証）のルールを持つ
 *
 * 【EntityとDTOの違い】
 *
 * ┌──────────────┬─────────────────┬─────────────────┐
 * │            │ Entity          │ DTO             │
 * ├──────────────┼─────────────────┼─────────────────┤
 * │ 役割       │ DBとのやり取り   │ 画面とのやり取り │
 * │ 対応先     │ テーブル構造     │ 画面の入力フォーム│
 * │ バリデーション │ なし         │ あり            │
 * │ 変更の影響 │ DB構造変更に影響 │ 画面変更に影響   │
 * └──────────────┴─────────────────┴─────────────────┘
 *
 * 【なぜ分けるのか】
 * 1. 責任の分離
 *    - Entity: データベースの構造を表現する責任
 *    - DTO: 画面とのやり取りの責任
 *
 * 2. バリデーションの分離
 *    - 画面の入力チェックはDTOで行う
 *    - Entityはデータベースの値をそのまま表現する
 *
 * 3. セキュリティ
 *    - 画面から受け取るのは必要な項目だけ（例: id, title, content）
 *    - createdAt, updatedAt は画面から受け取らない（改ざん防止）
 *
 * 4. 柔軟性
 *    - 画面の要件が変わってもEntityは変更不要
 *    - データベース構造が変わっても画面側は影響を受けにくい
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemoDto {

    /*
     * ============================================
     * フィールド定義
     * ============================================
     *
     * 【DTOに含めるフィールドの選び方】
     * - 画面で表示・入力する項目のみ含める
     * - システムが自動で設定する項目（createdAt, updatedAt）は含めない
     *   → 表示用には別途、フォーマット済みの文字列フィールドを用意
     */

    /**
     * メモID（主キー）
     *
     * 【用途】
     * - 新規作成時: null（まだIDが割り当てられていない）
     * - 編集時: 既存のIDが入る（どのメモを編集するか識別するため）
     * - 詳細表示時: 表示するメモのID
     *
     * 【バリデーション】
     * バリデーションは不要
     * - 新規作成時は null で良い
     * - 編集時はURLパラメータから取得した値をそのまま使う
     */
    private Long id;

    /**
     * メモのタイトル
     *
     * 【バリデーション】
     * @NotBlank: 空白文字のみの入力を許可しない
     * @Size: 最小1文字、最大200文字
     */

    /**
     * @NotBlank
     *
     * 【どういう検証か】
     * - null を許可しない
     * - 空文字列（""）を許可しない
     * - 空白文字のみ（"   "）を許可しない
     *
     * 【@NotEmpty との違い】
     * - @NotEmpty: null と空文字列（""）はNG、空白文字のみ（"   "）はOK
     * - @NotBlank: null、空文字列、空白文字のみ、すべてNG
     *
     * メモのタイトルは「意味のある文字」が必須なので、@NotBlank を使う
     *
     * 【エラーメッセージ】
     * message属性で、エラー時に表示するメッセージを指定
     * このメッセージは画面に表示される
     */
    @NotBlank(message = "タイトルを入力してください")

    /**
     * @Size
     *
     * 【どういう検証か】
     * 文字列の長さを検証する
     *
     * min = 1: 最小1文字（実際には@NotBlankで保証されるので冗長だが、明示的に書いている）
     * max = 200: 最大200文字（データベースのVARCHAR(200)に合わせる）
     *
     * 【なぜ必要か】
     * データベースの制約（VARCHAR(200)）を超える文字列を受け取ると、
     * データベースエラーが発生する
     * 画面側で事前にチェックすることで、わかりやすいエラーメッセージを表示できる
     */
    @Size(min = 1, max = 200, message = "タイトルは1文字以上200文字以内で入力してください")
    private String title;

    /**
     * メモの本文
     *
     * 【バリデーション】
     * @NotBlank: 必須入力
     *
     * 【なぜ@Sizeがないか】
     * データベースのTEXT型は制限がほぼないため、
     * 文字数制限は設けない
     * （必要であれば後から追加可能）
     */
    @NotBlank(message = "本文を入力してください")
    private String content;

    /*
     * ============================================
     * 表示用フィールド（オプション）
     * ============================================
     *
     * 以下のフィールドは、画面に表示するための読み取り専用フィールド
     * フォームからの入力は受け付けず、Serviceから設定される
     */

    /**
     * 作成日時（表示用）
     *
     * 【型】String
     * LocalDateTime ではなく、フォーマット済みの文字列を持つ
     *
     * 【なぜStringか】
     * - 画面では「2025年11月17日 10:30」のような形式で表示したい
     * - LocalDateTime のままだと、テンプレート側でフォーマット処理が必要
     * - DTO側でフォーマット済みの文字列を持つことで、テンプレートがシンプルになる
     *
     * 【設定方法】
     * Serviceレイヤーで、EntityのcreatedAtをフォーマットして設定する
     *
     * 例:
     * MemoDto dto = new MemoDto();
     * dto.setCreatedAtFormatted(
     *     memo.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm"))
     * );
     */
    private String createdAtFormatted;

    /**
     * 更新日時（表示用）
     *
     * 作成日時と同様、フォーマット済みの文字列
     */
    private String updatedAtFormatted;

    /*
     * ============================================
     * ヘルパーメソッド（便利メソッド）
     * ============================================
     */

    /**
     * Entity（Memo）からDTOへ変換する
     *
     * 【使い方】
     * Memo memo = memoMapper.findById(1L);
     * MemoDto dto = MemoDto.fromEntity(memo);
     *
     * 【なぜstaticメソッドか】
     * インスタンスを作らずに呼び出せるので便利
     * コンストラクタの代わりに使える（ファクトリメソッド）
     *
     * @param memo Entityオブジェクト
     * @return DTOオブジェクト
     */
    public static MemoDto fromEntity(Memo memo) {
        // 日付フォーマッター
        // "yyyy年MM月dd日 HH:mm" 形式でフォーマットする
        // 例: "2025年11月17日 10:30"
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm");

        // DTOのインスタンスを作成
        MemoDto dto = new MemoDto();

        // Entityのフィールドをコピー
        dto.setId(memo.getId());
        dto.setTitle(memo.getTitle());
        dto.setContent(memo.getContent());

        // 日付フィールドをフォーマットして設定
        if (memo.getCreatedAt() != null) {
            dto.setCreatedAtFormatted(memo.getCreatedAt().format(formatter));
        }

        if (memo.getUpdatedAt() != null) {
            dto.setUpdatedAtFormatted(memo.getUpdatedAt().format(formatter));
        }

        return dto;
    }

    /**
     * DTOからEntity（Memo）へ変換する
     *
     * 【使い方】
     * MemoDto dto = new MemoDto();
     * dto.setTitle("新しいメモ");
     * dto.setContent("内容");
     * Memo memo = dto.toEntity();
     * memoMapper.insert(memo);
     *
     * 【注意】
     * - createdAt, updatedAt はコピーしない（データベース側で自動設定）
     * - createdAtFormatted, updatedAtFormatted は表示用なのでコピーしない
     *
     * @return Entityオブジェクト
     */
    public Memo toEntity() {
        Memo memo = new Memo();
        memo.setId(this.id);
        memo.setTitle(this.title);
        memo.setContent(this.content);
        // createdAt, updatedAt は設定しない（DBが自動設定）
        return memo;
    }

}

/*
 * ============================================
 * このクラスの使われ方
 * ============================================
 *
 * 【1. フォームの入力を受け取る】
 * Controller:
 * @PostMapping("/memos")
 * public String create(@Valid MemoDto dto, BindingResult result) {
 *     if (result.hasErrors()) {
 *         return "memos/new";  // バリデーションエラー時は入力画面に戻る
 *     }
 *     memoService.create(dto);
 *     return "redirect:/memos";
 * }
 *
 * @Valid アノテーションにより、MemoDto の @NotBlank, @Size が検証される
 * エラーがあれば BindingResult に格納される
 *
 * 【2. 画面にデータを渡す】
 * Service:
 * public MemoDto findById(Long id) {
 *     Memo memo = memoMapper.findById(id);
 *     return MemoDto.fromEntity(memo);  // EntityからDTOに変換
 * }
 *
 * Controller:
 * @GetMapping("/memos/{id}")
 * public String show(@PathVariable Long id, Model model) {
 *     MemoDto dto = memoService.findById(id);
 *     model.addAttribute("memo", dto);
 *     return "memos/show";
 * }
 *
 * Thymeleaf:
 * <h1 th:text="${memo.title}">タイトル</h1>
 * <p th:text="${memo.content}">本文</p>
 * <p>作成日時: <span th:text="${memo.createdAtFormatted}">2025年11月17日 10:30</span></p>
 *
 * 【3. DTOからEntityへ変換してデータベースに保存】
 * Service:
 * public void create(MemoDto dto) {
 *     Memo memo = dto.toEntity();  // DTOからEntityに変換
 *     memoMapper.insert(memo);
 * }
 *
 * このように、DTOは画面とビジネスロジック層の橋渡し役です
 */

/*
 * ============================================
 * バリデーションエラーメッセージのカスタマイズ
 * ============================================
 *
 * 【方法1: アノテーションのmessage属性（現在の方法）】
 * @NotBlank(message = "タイトルを入力してください")
 *
 * 【方法2: messages.properties ファイルを使う】
 * src/main/resources/messages.properties:
 * NotBlank.memoDto.title=タイトルを入力してください
 * Size.memoDto.title=タイトルは1文字以上200文字以内で入力してください
 *
 * アノテーション側:
 * @NotBlank  // message属性を省略
 * @Size(min = 1, max = 200)
 *
 * 【どちらを使うべきか】
 * - 小規模: アノテーションのmessage属性で十分
 * - 大規模: messages.properties で一元管理（多言語対応もしやすい）
 *
 * このアプリでは初学者向けにシンプルに、アノテーションのmessage属性を使用
 */
