package com.example.memoapp.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * memos テーブルの 1 行に対応する Entity（データアクセス層のデータ入れ物）。
 *
 * Mapper が DB から取り出した行をこのクラスに詰め、Service が {@link com.example.memoapp.dto.MemoDto}
 * へ変換して画面に渡す。DB 構造をそのまま表すので、検証ルールや表示用の加工は持たない。
 *
 * 学習ポイント:
 * - Lombok（@Data など）が肩代わりするボイラープレート → docs/解説/DIとLombok.md
 * - Entity と DTO を分ける理由 → docs/解説/Bean-Validation.md
 * - 各フィールドの型選定（Long vs long など）の Why はこのクラス固有として下に残す
 *
 * モダン版との対比: モダン版は Kotlin の data class + JPA Auditing で id/日時を自動設定する。
 * 従来型は Lombok で getter/setter を生成し、日時は DB 側（DEFAULT とトリガー）に任せる。
 */
@Data                 // getter/setter・toString・equals・hashCode を生成
@NoArgsConstructor    // 引数なしコンストラクタ。MyBatis が「空生成 → setter」で組み立てるのに必須
@AllArgsConstructor   // 全フィールドのコンストラクタ。テストで 1 行生成したいとき便利
public class Memo {

    /**
     * メモ ID（主キー、DB が採番）。
     *
     * プリミティブの long ではなく Long を使うのは null を表せるようにするため。
     * 新規作成時（採番前）と DB の NULL を「値なし」として同じ null で扱える。
     * BIGSERIAL は 64 ビットなので、32 ビットの int では受けきれず Long にする。
     */
    private Long id;

    /**
     * タイトル。DB は VARCHAR(200) NOT NULL。長さ・必須の検証は Entity ではなく
     * MemoDto 側（@NotBlank / @Size）で行う（Entity は DB の値をそのまま持つだけ）。
     */
    private String title;

    /**
     * 本文。DB は TEXT NOT NULL（長さ実質無制限）。必須検証は MemoDto 側で行う。
     */
    private String content;

    /**
     * 作成日時。INSERT 時に DB の DEFAULT CURRENT_TIMESTAMP が入るため、Java 側では設定しない。
     *
     * 旧 java.util.Date ではなく java.time.LocalDateTime を使うのは、不変（イミュータブル）で
     * スレッド安全、かつ API が扱いやすいため。
     */
    private LocalDateTime createdAt;

    /**
     * 更新日時。UPDATE のたびに DB のトリガーで自動更新される
     * （V1__Create_memos_table.sql の update_updated_at_column）。Java 側では設定しない。
     */
    private LocalDateTime updatedAt;
}
