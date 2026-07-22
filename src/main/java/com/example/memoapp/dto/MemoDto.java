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
 * 画面と Controller/Service の間でデータを運ぶ DTO（Data Transfer Object）。
 *
 * Entity（{@link Memo}）が DB 構造を表すのに対し、DTO は「画面が必要とする形」を表す。
 * 入力検証ルール（@NotBlank / @Size）と、表示用にフォーマット済みの日時文字列を持つ。
 *
 * 学習ポイント:
 * - DTO と Entity を分ける理由・両者の変換の意味 → docs/解説/Bean-Validation.md
 * - @NotBlank / @Size / messages.properties など検証アノテーション → docs/解説/Bean-Validation.md
 *
 * モダン版との対比: モダン版は用途ごとに DTO を分割（CreateMemoRequest / UpdateMemoRequest /
 * MemoResponse）し JSON で授受する。従来型は 1 つの DTO を入力・表示で使い回し、画面へ直接渡す。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemoDto {

    /**
     * メモ ID。新規作成時は null（未採番）、編集・詳細表示時は対象メモの ID が入る。
     * ID は URL やサーバー側で扱う値なので検証は付けない。
     */
    private Long id;

    /**
     * タイトル。@NotBlank（null・空文字・空白のみを拒否）と @Size（長さ範囲）を併用する。
     *
     * 落とし穴: @Size は「長さ」しか見ないため、min = 1 でも空白 1 文字（" "）は長さ 1 として
     * 通過してしまい、null は検証対象外としてそのまま通る。null・空白のみまで拒否するには
     * @NotBlank が必要で、この 2 つを組み合わせている。min = 1 は @NotBlank があるので厳密には
     * 冗長だが、下限の意図を明示するために残している。
     * max = 200 は DB の VARCHAR(200) に合わせ、DB エラーになる前に画面で弾くためのもの。
     */
    @NotBlank(message = "タイトルを入力してください")
    @Size(min = 1, max = 200, message = "タイトルは1文字以上200文字以内で入力してください")
    private String title;

    /**
     * 本文。@NotBlank で必須のみ検証する。DB が TEXT 型で長さ制限が実質無いため @Size は付けない。
     */
    @NotBlank(message = "本文を入力してください")
    private String content;

    /**
     * 作成日時（表示用）。LocalDateTime ではなくフォーマット済みの String を持つ。
     * DTO 側で「yyyy年MM月dd日 HH:mm」に整形しておくことで、Thymeleaf 側の整形処理を無くせる。
     * フォームからの入力は受け取らず、Service（fromEntity）が設定する。
     */
    private String createdAtFormatted;

    /**
     * 更新日時（表示用）。createdAtFormatted と同じくフォーマット済み文字列。
     */
    private String updatedAtFormatted;

    /**
     * Entity から表示用 DTO を生成するファクトリメソッド。日時をこの場でフォーマットする。
     *
     * @param memo 変換元の Entity
     * @return 表示用に整形した DTO
     */
    public static MemoDto fromEntity(Memo memo) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm");

        MemoDto dto = new MemoDto();
        dto.setId(memo.getId());
        dto.setTitle(memo.getTitle());
        dto.setContent(memo.getContent());

        // 日時は DB 側で設定されるため、新規直後など null のことがある。null 安全に整形する
        if (memo.getCreatedAt() != null) {
            dto.setCreatedAtFormatted(memo.getCreatedAt().format(formatter));
        }
        if (memo.getUpdatedAt() != null) {
            dto.setUpdatedAtFormatted(memo.getUpdatedAt().format(formatter));
        }
        return dto;
    }

    /**
     * DTO から DB 保存用の Entity を生成する。
     *
     * createdAt / updatedAt は DB が管理する（DEFAULT とトリガー）ためコピーしない。
     * 画面から日時を送らせない = 改ざんできる項目を増やさない、という安全上の意味もある。
     *
     * @return title・content（更新時は id も）を持つ Entity
     */
    public Memo toEntity() {
        Memo memo = new Memo();
        // 更新時のみ id を設定（新規作成時は null なので採番を DB に任せる）
        if (this.id != null) {
            memo.setId(this.id);
        }
        memo.setTitle(this.title);
        memo.setContent(this.content);
        return memo;
    }
}
