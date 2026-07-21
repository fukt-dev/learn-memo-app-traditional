package com.example.memoapp.dto;

import com.example.memoapp.entity.Memo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MemoDto の変換ロジックの単体テスト
 *
 * 【このテストの位置付け】
 * Entity ⇔ DTO の変換はフレームワークに依存しない純粋な Java ロジックなので、
 * モックも Spring も使わない「素の JUnit」でテストする。
 * 依存が少ないテストほど速く・壊れにくい(テストピラミッドの土台)
 */
class MemoDtoTest {

    @Test
    @DisplayName("fromEntity: 日時が「yyyy年MM月dd日 HH:mm」形式の文字列に変換される")
    void fromEntity_formatsTimestamps() {
        Memo memo = new Memo();
        memo.setId(1L);
        memo.setTitle("タイトル");
        memo.setContent("本文");
        memo.setCreatedAt(LocalDateTime.of(2026, 7, 21, 9, 5));
        memo.setUpdatedAt(LocalDateTime.of(2026, 12, 3, 23, 59));

        MemoDto dto = MemoDto.fromEntity(memo);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getTitle()).isEqualTo("タイトル");
        assertThat(dto.getContent()).isEqualTo("本文");
        // 月・日・時・分が0埋めされることも仕様(MM, dd, HH, mm)
        assertThat(dto.getCreatedAtFormatted()).isEqualTo("2026年07月21日 09:05");
        assertThat(dto.getUpdatedAtFormatted()).isEqualTo("2026年12月03日 23:59");
    }

    @Test
    @DisplayName("fromEntity: 日時がnullでも例外にならない(フォーマットをスキップ)")
    void fromEntity_toleratesNullTimestamps() {
        /*
         * 【なぜnullケースをテストするのか】
         * INSERT直後のEntityなど、日時が未設定の場面がありうる。
         * null に format() を呼ぶと NullPointerException になるため、
         * fromEntity 内の null ガードが仕様として機能していることを固定する
         */
        Memo memo = new Memo();
        memo.setId(1L);
        memo.setTitle("タイトル");
        memo.setContent("本文");

        MemoDto dto = MemoDto.fromEntity(memo);

        assertThat(dto.getCreatedAtFormatted()).isNull();
        assertThat(dto.getUpdatedAtFormatted()).isNull();
    }

    @Test
    @DisplayName("toEntity: id・title・contentのみコピーされ、日時はコピーされない")
    void toEntity_copiesOnlyEditableFields() {
        MemoDto dto = new MemoDto();
        dto.setId(1L);
        dto.setTitle("タイトル");
        dto.setContent("本文");
        dto.setCreatedAtFormatted("2026年07月21日 09:05");

        Memo memo = dto.toEntity();

        assertThat(memo.getId()).isEqualTo(1L);
        assertThat(memo.getTitle()).isEqualTo("タイトル");
        assertThat(memo.getContent()).isEqualTo("本文");
        /*
         * 日時はDB側(DEFAULT値とトリガー)が管理するため、
         * DTO→Entityでは常にnullのまま。
         * 「画面から日時を書き換えられない」ことの裏付けでもある
         */
        assertThat(memo.getCreatedAt()).isNull();
        assertThat(memo.getUpdatedAt()).isNull();
    }

    @Test
    @DisplayName("toEntity: 新規作成時(id=null)はEntityのidもnullのまま")
    void toEntity_keepsNullId_forNewMemo() {
        MemoDto dto = new MemoDto();
        dto.setTitle("新規メモ");
        dto.setContent("本文");

        Memo memo = dto.toEntity();

        // idがnull = 「DBに採番を任せる」という契約
        assertThat(memo.getId()).isNull();
    }
}
