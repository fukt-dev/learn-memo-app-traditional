package com.example.memoapp.controller;

import com.example.memoapp.dto.MemoDto;
import com.example.memoapp.exception.MemoNotFoundException;
import com.example.memoapp.service.MemoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * MemoController のスライステスト(@WebMvcTest)
 *
 * 【このテストの位置付け】
 * プレゼンテーション層(Controller + 例外ハンドラ)だけを起動して、
 * 「HTTPリクエスト → ステータスコード・ビュー・リダイレクト」の対応を検証する。
 * Service はモックに差し替えるため、DBは不要。
 *
 * 【学習ポイント】
 * - @WebMvcTest による「スライステスト」(アプリの一部だけ起動する)
 * - MockMvc での擬似HTTPリクエストの送り方
 * - 例外 → HTTPステータスの変換(GlobalExceptionHandler)の回帰テスト
 */

/**
 * @WebMvcTest(MemoController.class)
 *
 * Spring MVC 関連のBean(指定したController・@ControllerAdvice・
 * Thymeleafの設定など)だけを起動する。
 * @Service や @Mapper はコンテナに登録されないため、
 * 依存する MemoService は @MockitoBean で偽物を差し込む。
 *
 * 【@SpringBootTest との違い】
 * - @SpringBootTest: アプリ全体を起動(遅い・DB接続も必要になりがち)
 * - @WebMvcTest: Web層のみ起動(速い・HTTP層の検証に最適)
 */
@WebMvcTest(MemoController.class)
class MemoControllerTest {

    /**
     * MockMvc
     * サーバーを実際に起動せずに、DispatcherServlet へ
     * 擬似的なHTTPリクエストを送り込むテスト用の道具。
     * ステータスコード・ビュー名・モデルの中身などを検証できる
     */
    @Autowired
    private MockMvc mockMvc;

    /**
     * @MockitoBean
     * DIコンテナ内の MemoService をMockitoのモックに置き換える。
     *
     * 【落とし穴: @MockBean は廃止予定】
     * Spring Boot 3.4 以降、従来の @MockBean は非推奨となり、
     * Spring Framework 本体が提供する @MockitoBean に置き換えられた
     * (パッケージも org.springframework.test.context.bean.override.mockito に変更)。
     * 古い記事のサンプルコードをコピーすると非推奨警告が出るので注意
     */
    @MockitoBean
    private MemoService memoService;

    /** テストデータ生成のヘルパーメソッド */
    private MemoDto createDto(Long id, String title, String content) {
        MemoDto dto = new MemoDto();
        dto.setId(id);
        dto.setTitle(title);
        dto.setContent(content);
        dto.setCreatedAtFormatted("2026年07月21日 10:30");
        dto.setUpdatedAtFormatted("2026年07月21日 15:45");
        return dto;
    }

    @Nested
    @DisplayName("GET /memos: 一覧・検索")
    class ListMemos {

        @Test
        @DisplayName("200 で一覧ビューが返り、モデルにメモ一覧が入る")
        void returnsListView() throws Exception {
            when(memoService.search(null)).thenReturn(List.of(createDto(1L, "メモ1", "本文1")));

            mockMvc.perform(get("/memos"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("memos/list"))
                    .andExpect(model().attributeExists("memos"));
        }

        @Test
        @DisplayName("keyword パラメータがそのまま Service に委譲される")
        void delegatesKeywordToService() throws Exception {
            when(memoService.search("買い物")).thenReturn(List.of());

            mockMvc.perform(get("/memos").param("keyword", "買い物"))
                    .andExpect(status().isOk());

            // Controller が if/else せず、そのまま search() に渡していることの検証
            verify(memoService).search("買い物");
        }
    }

    @Nested
    @DisplayName("GET /memos/{id}: 詳細表示と404")
    class ShowMemo {

        @Test
        @DisplayName("存在するIDなら 200 で詳細ビューが返る")
        void returnsShowView() throws Exception {
            when(memoService.findById(1L)).thenReturn(createDto(1L, "メモ1", "本文1"));

            mockMvc.perform(get("/memos/1"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("memos/show"))
                    .andExpect(model().attributeExists("memo"));
        }

        @Test
        @DisplayName("存在しないIDなら 404 + エラー画面が返る(回帰テスト)")
        void returns404_whenMemoNotFound() throws Exception {
            /*
             * 【このテストが守っている修正】
             * かつて GlobalExceptionHandler は「エラー画面を出すがステータスは200」
             * というバグを持っていた(例外クラス側の @ResponseStatus は
             * @ExceptionHandler に捕捉されると効かないため)。
             * ハンドラ側に @ResponseStatus(NOT_FOUND) を付けて修正済み。
             * このテストはその修正が退行しないことを保証する
             */
            when(memoService.findById(999L))
                    .thenThrow(new MemoNotFoundException("メモが見つかりません: id=999"));

            mockMvc.perform(get("/memos/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(view().name("error/error"))
                    .andExpect(model().attributeExists("errorMessage"));
        }
    }

    @Nested
    @DisplayName("存在しないURL: NoResourceFoundException → 404")
    class UnknownUrl {

        @Test
        @DisplayName("マッピングのないURLは 404 + エラー画面が返る")
        void returns404_forUnknownUrl() throws Exception {
            /*
             * Spring Boot 3.2 以降、未マッピングURLへのアクセスは
             * NoResourceFoundException としてスローされる。
             * 専用ハンドラがないと Exception用のフォールバックに吸われて
             * 「システムエラー画面 + ERRORログ」になっていた(修正済み)
             */
            mockMvc.perform(get("/nonexistent"))
                    .andExpect(status().isNotFound())
                    .andExpect(view().name("error/error"));
        }
    }

    @Nested
    @DisplayName("POST /memos: 新規登録")
    class CreateMemo {

        @Test
        @DisplayName("正常な入力なら登録して一覧へリダイレクト(PRGパターン)")
        void createsAndRedirects() throws Exception {
            mockMvc.perform(post("/memos")
                            .param("title", "新しいメモ")
                            .param("content", "内容"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/memos"));

            verify(memoService).create(any(MemoDto.class));
        }

        @Test
        @DisplayName("タイトルが空ならバリデーションエラーで入力画面に戻り、登録されない")
        void returnsFormView_whenValidationFails() throws Exception {
            mockMvc.perform(post("/memos")
                            .param("title", "")
                            .param("content", "内容"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("memos/new"))
                    // memoDto の title フィールドにエラーが付いていること
                    .andExpect(model().attributeHasFieldErrors("memoDto", "title"));

            // バリデーションで弾かれた場合、Service まで処理が届かないこと
            verify(memoService, never()).create(any(MemoDto.class));
        }
    }

    @Nested
    @DisplayName("POST /memos/{id}/update: 更新")
    class UpdateMemo {

        @Test
        @DisplayName("正常な入力なら更新して詳細へリダイレクト")
        void updatesAndRedirects() throws Exception {
            mockMvc.perform(post("/memos/1/update")
                            .param("title", "更新タイトル")
                            .param("content", "更新本文"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/memos/1"));

            verify(memoService).update(any(MemoDto.class));
        }

        @Test
        @DisplayName("バリデーションエラー時は編集画面に戻り、日時が再設定される(回帰テスト)")
        void refillsTimestamps_whenValidationFails() throws Exception {
            /*
             * 【このテストが守っている修正】
             * フォームには title と content しか含まれないため、
             * エラーで編集画面に戻ると日時欄が空になるバグがあった。
             * Controller が DB から日時を取り直して補完する修正の回帰テスト
             */
            when(memoService.findById(1L)).thenReturn(createDto(1L, "元タイトル", "元本文"));

            mockMvc.perform(post("/memos/1/update")
                            .param("title", "")
                            .param("content", "本文"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("memos/edit"))
                    // 再設定された日時がモデルの memoDto に入っていること
                    .andExpect(model().attribute("memoDto",
                            org.hamcrest.Matchers.hasProperty("createdAtFormatted",
                                    org.hamcrest.Matchers.equalTo("2026年07月21日 10:30"))));

            verify(memoService, never()).update(any(MemoDto.class));
        }
    }

    @Nested
    @DisplayName("POST /memos/{id}/delete: 削除")
    class DeleteMemo {

        @Test
        @DisplayName("削除して一覧へリダイレクト")
        void deletesAndRedirects() throws Exception {
            mockMvc.perform(post("/memos/1/delete"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/memos"));

            verify(memoService).delete(1L);
        }
    }
}
