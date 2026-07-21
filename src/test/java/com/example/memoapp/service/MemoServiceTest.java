package com.example.memoapp.service;

import com.example.memoapp.dto.MemoDto;
import com.example.memoapp.entity.Memo;
import com.example.memoapp.exception.MemoNotFoundException;
import com.example.memoapp.mapper.MemoMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * MemoService の単体テスト(JUnit 5 + Mockito)。
 *
 * 3層アーキテクチャの「ビジネスロジック層」だけを切り出し、依存する MemoMapper はモックに
 * 差し替えて DB なしで高速に検証する。正常系だけでなく、存在しない ID の例外や
 * 並行削除の隙間(更新件数0)も、仕様の一部として固定する。
 *
 * Mockito(@Mock / @InjectMocks / when / verify / ArgumentCaptor)と
 * Given-When-Then の汎用解説 → docs/解説/テスト.md
 */
@ExtendWith(MockitoExtension.class)
class MemoServiceTest {

    /** MemoMapper のモック(偽物)。when(...).thenReturn(...) で戻り値を仕込み、verify で呼ばれ方を検証する */
    @Mock
    private MemoMapper memoMapper;

    /**
     * テスト対象の MemoService を生成し、上の @Mock をコンストラクタ経由で注入する。
     * MemoService が @RequiredArgsConstructor のコンストラクタ注入を採るため、Mockito が
     * 「MemoMapper を受け取るコンストラクタ」を見つけて差し込める。
     * (フィールドインジェクションだと差し替えにくい ＝「コンストラクタ注入はテストしやすい」の実例)
     */
    @InjectMocks
    private MemoService memoService;

    /**
     * テストデータ生成のヘルパーメソッド
     * 各テストで同じ組み立てコードを繰り返さないためにまとめている
     */
    private Memo createMemo(Long id, String title, String content) {
        Memo memo = new Memo();
        memo.setId(id);
        memo.setTitle(title);
        memo.setContent(content);
        memo.setCreatedAt(LocalDateTime.of(2026, 7, 21, 10, 30));
        memo.setUpdatedAt(LocalDateTime.of(2026, 7, 21, 15, 45));
        return memo;
    }

    // @Nested + @DisplayName でメソッド単位にグループ化し、レポートを「仕様書」のように読ませる
    @Nested
    @DisplayName("findById: ID指定で1件取得")
    class FindById {

        @Test
        @DisplayName("存在するIDなら、EntityがDTOに変換されて返る")
        void returnsDto_whenMemoExists() {
            // Arrange(準備): モックの動作を定義
            when(memoMapper.findById(1L))
                    .thenReturn(Optional.of(createMemo(1L, "買い物リスト", "牛乳を買う")));

            // Act(実行)
            MemoDto result = memoService.findById(1L);

            // Assert(検証)
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getTitle()).isEqualTo("買い物リスト");
            assertThat(result.getContent()).isEqualTo("牛乳を買う");
            // DTOでは日時がフォーマット済み文字列になる(MemoDto.fromEntity の責務)
            assertThat(result.getCreatedAtFormatted()).isEqualTo("2026年07月21日 10:30");
        }

        @Test
        @DisplayName("存在しないIDなら、MemoNotFoundException がスローされる")
        void throwsException_whenMemoDoesNotExist() {
            // Optional.empty() = 「DBに該当行がなかった」状況を再現
            when(memoMapper.findById(999L)).thenReturn(Optional.empty());

            /*
             * assertThatThrownBy: 「例外が起きること」を検証する書き方
             *
             * 【なぜ例外もテストするのか】
             * 「見つからないときに正しく404系の例外を投げる」ことは
             * このアプリの重要な仕様(GlobalExceptionHandler が 404 を返す前提)。
             * 正常系だけでなく異常系も仕様の一部としてテストする
             */
            assertThatThrownBy(() -> memoService.findById(999L))
                    .isInstanceOf(MemoNotFoundException.class)
                    .hasMessageContaining("id=999");
        }
    }

    @Nested
    @DisplayName("findAll: 全件取得")
    class FindAll {

        @Test
        @DisplayName("Entityのリストが同じ件数のDTOリストに変換される")
        void convertsAllEntitiesToDtos() {
            when(memoMapper.findAll()).thenReturn(List.of(
                    createMemo(1L, "メモ1", "本文1"),
                    createMemo(2L, "メモ2", "本文2")
            ));

            List<MemoDto> result = memoService.findAll();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getTitle()).isEqualTo("メモ1");
            assertThat(result.get(1).getTitle()).isEqualTo("メモ2");
        }
    }

    @Nested
    @DisplayName("create: 新規登録")
    class Create {

        @Test
        @DisplayName("DTOの内容がEntityに変換されてMapperに渡される")
        void passesConvertedEntityToMapper() {
            MemoDto dto = new MemoDto();
            dto.setTitle("新しいメモ");
            dto.setContent("内容");

            memoService.create(dto);

            /*
             * ArgumentCaptor: モックに渡された引数を「捕まえて」中身を検証する仕組み
             *
             * 【なぜ必要か】
             * insert() の戻り値は void なので、戻り値では検証できない。
             * 「Service が DTO→Entity 変換を正しく行って Mapper に渡したか」は、
             * 渡された引数そのものを見るしかない
             */
            ArgumentCaptor<Memo> captor = ArgumentCaptor.forClass(Memo.class);
            verify(memoMapper).insert(captor.capture());
            assertThat(captor.getValue().getTitle()).isEqualTo("新しいメモ");
            assertThat(captor.getValue().getContent()).isEqualTo("内容");
            // 新規作成なのでIDはnull(DBが採番する)
            assertThat(captor.getValue().getId()).isNull();
        }
    }

    @Nested
    @DisplayName("update: 更新")
    class Update {

        @Test
        @DisplayName("存在するメモなら更新される")
        void updatesMemo_whenExists() {
            when(memoMapper.findById(1L))
                    .thenReturn(Optional.of(createMemo(1L, "旧タイトル", "旧本文")));
            when(memoMapper.update(any(Memo.class))).thenReturn(1);

            MemoDto dto = new MemoDto();
            dto.setId(1L);
            dto.setTitle("新タイトル");
            dto.setContent("新本文");

            memoService.update(dto);

            verify(memoMapper).update(any(Memo.class));
        }

        @Test
        @DisplayName("存在しないメモなら例外がスローされ、更新は実行されない")
        void throwsException_whenMemoDoesNotExist() {
            when(memoMapper.findById(999L)).thenReturn(Optional.empty());

            MemoDto dto = new MemoDto();
            dto.setId(999L);
            dto.setTitle("タイトル");
            dto.setContent("本文");

            assertThatThrownBy(() -> memoService.update(dto))
                    .isInstanceOf(MemoNotFoundException.class);

            /*
             * verify(..., never()): 「呼ばれていないこと」の検証
             * 事前チェックで弾かれた場合、update という副作用が
             * 実行されていないことまで確認する
             */
            verify(memoMapper, never()).update(any(Memo.class));
        }

        @Test
        @DisplayName("事前チェック後に他者が削除した場合(更新件数0)も例外になる")
        void throwsException_whenUpdateCountIsZero() {
            /*
             * 【何を再現しているか】
             * findById では存在したのに、update の瞬間には消えていた
             * = 「事前チェックと更新の間に、別のユーザーが削除した」という
             * 並行アクセスの隙間。Service のダブルチェックが効くことを確認する
             */
            when(memoMapper.findById(1L))
                    .thenReturn(Optional.of(createMemo(1L, "タイトル", "本文")));
            when(memoMapper.update(any(Memo.class))).thenReturn(0);

            MemoDto dto = new MemoDto();
            dto.setId(1L);
            dto.setTitle("新タイトル");
            dto.setContent("新本文");

            assertThatThrownBy(() -> memoService.update(dto))
                    .isInstanceOf(MemoNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("delete: 削除")
    class Delete {

        @Test
        @DisplayName("存在するメモなら削除される")
        void deletesMemo_whenExists() {
            when(memoMapper.findById(1L))
                    .thenReturn(Optional.of(createMemo(1L, "タイトル", "本文")));
            when(memoMapper.deleteById(1L)).thenReturn(1);

            memoService.delete(1L);

            verify(memoMapper).deleteById(1L);
        }

        @Test
        @DisplayName("存在しないメモなら例外がスローされ、削除は実行されない")
        void throwsException_whenMemoDoesNotExist() {
            when(memoMapper.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> memoService.delete(999L))
                    .isInstanceOf(MemoNotFoundException.class);

            verify(memoMapper, never()).deleteById(any());
        }
    }

    @Nested
    @DisplayName("search: キーワード検索")
    class Search {

        @Test
        @DisplayName("キーワードがあれば部分一致検索が実行される")
        void searchesByKeyword() {
            when(memoMapper.searchByKeyword("買い物"))
                    .thenReturn(List.of(createMemo(1L, "買い物リスト", "牛乳")));

            List<MemoDto> result = memoService.search("買い物");

            assertThat(result).hasSize(1);
            verify(memoMapper).searchByKeyword("買い物");
            verify(memoMapper, never()).findAll();
        }

        @Test
        @DisplayName("キーワードがnullなら全件取得にフォールバックする")
        void fallsBackToFindAll_whenKeywordIsNull() {
            /*
             * 【この仕様が Controller の設計を支えている】
             * MemoController.list() は if/else なしで search(keyword) を
             * 呼ぶだけで済んでいる。それは「空なら全件」という判断を
             * Service が引き受けているから。このテストはその契約の裏付け
             */
            when(memoMapper.findAll()).thenReturn(List.of());

            memoService.search(null);

            verify(memoMapper).findAll();
            verify(memoMapper, never()).searchByKeyword(any());
        }

        @Test
        @DisplayName("キーワードが空白のみでも全件取得にフォールバックする")
        void fallsBackToFindAll_whenKeywordIsBlank() {
            when(memoMapper.findAll()).thenReturn(List.of());

            memoService.search("   ");

            verify(memoMapper).findAll();
            verify(memoMapper, never()).searchByKeyword(any());
        }
    }

    @Nested
    @DisplayName("findAllWithPaging / getTotalPages: ページング(未使用APIの仕様固定)")
    class Paging {

        @Test
        @DisplayName("ページ番号とサイズからoffsetが正しく計算される(2ページ目=offset 10)")
        void calculatesOffsetFromPageNumber() {
            when(memoMapper.findAllWithPaging(10, 10)).thenReturn(List.of());

            memoService.findAllWithPaging(2, 10);

            // offset = (2 - 1) × 10 = 10 で Mapper が呼ばれること
            verify(memoMapper).findAllWithPaging(10, 10);
        }

        @Test
        @DisplayName("ページ番号が0以下なら IllegalArgumentException")
        void rejectsInvalidPageNumber() {
            assertThatThrownBy(() -> memoService.findAllWithPaging(0, 10))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("ページサイズが範囲外(101以上)なら IllegalArgumentException")
        void rejectsInvalidPageSize() {
            assertThatThrownBy(() -> memoService.findAllWithPaging(1, 101))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("総ページ数は切り上げで計算される(25件/10件 → 3ページ)")
        void calculatesTotalPagesWithCeiling() {
            when(memoMapper.count()).thenReturn(25L);

            assertThat(memoService.getTotalPages(10)).isEqualTo(3);
        }
    }
}
