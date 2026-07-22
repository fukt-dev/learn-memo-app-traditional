package com.example.memoapp.service;

import com.example.memoapp.dto.MemoDto;
import com.example.memoapp.entity.Memo;
import com.example.memoapp.exception.MemoNotFoundException;
import com.example.memoapp.mapper.MemoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * メモ機能のビジネスロジック層。
 *
 * 3 層アーキテクチャの中段。Controller と Mapper の間で、Entity ⇄ DTO の変換・
 * トランザクション境界の定義・業務ルール（空キーワードなら全件など）を担う。
 * HTTP 処理は Controller、SQL は Mapper の責務なのでここには書かない。
 *
 * 学習ポイント:
 * - @Service / コンストラクタ注入 / @Slf4j とログレベル → docs/解説/DIとLombok.md
 * - @Transactional・readOnly・ロールバック条件・自己呼び出しの落とし穴 → docs/解説/トランザクション.md
 *
 * モダン版との対比: @Transactional はフレームワーク共通で、モダン版（JPA）でも同じ。
 * 違いはトランザクション内で動く DB アクセスが MyBatis の SQL か JPA の操作かだけ。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MemoService {

    private final MemoMapper memoMapper;

    /**
     * 全件取得（新しい順）。
     *
     * Entity をそのまま返さず DTO に変換するのは、表示用に整形（日時フォーマット等）した形を
     * 画面へ渡すため。readOnly = true の効果は docs/解説/トランザクション.md を参照。
     *
     * @return メモの DTO リスト（0 件なら空リスト）
     */
    @Transactional(readOnly = true)
    public List<MemoDto> findAll() {
        log.debug("メモの全件取得を開始します");
        List<Memo> memos = memoMapper.findAll();
        log.debug("{}件のメモを取得しました", memos.size());
        return memos.stream()
                .map(MemoDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * ID 指定で 1 件取得。
     *
     * findById が返す Optional に対し、orElseThrow で「無ければ MemoNotFoundException」を表現する。
     * この例外は GlobalExceptionHandler が 404 に変換する（docs/解説/例外処理とエラーページ.md）。
     *
     * @param id メモ ID
     * @return メモの DTO
     * @throws MemoNotFoundException メモが見つからない場合
     */
    @Transactional(readOnly = true)
    public MemoDto findById(Long id) {
        log.debug("メモを取得します: id={}", id);
        return memoMapper.findById(id)
                .map(MemoDto::fromEntity)
                .orElseThrow(() -> new MemoNotFoundException("メモが見つかりません: id=" + id));
    }

    /**
     * 新規登録。更新系なので通常の @Transactional（例外時ロールバック）を付ける。
     *
     * @param dto 登録するメモ（title / content を持つ。id は DB が採番）
     */
    @Transactional
    public void create(MemoDto dto) {
        log.debug("メモを登録します: title={}", dto.getTitle());
        Memo memo = dto.toEntity();
        // insert 後、採番された id が memo にセットされる（MemoMapper.xml の useGeneratedKeys="true"）
        memoMapper.insert(memo);
        log.info("メモを登録しました: id={}, title={}", memo.getId(), memo.getTitle());
    }

    /**
     * 更新。存在しない ID には分かりやすい 404 を返すため、更新前に findById で存在チェックする。
     *
     * @param dto 更新するメモ（id / title / content を持つ）
     * @throws MemoNotFoundException メモが見つからない場合
     */
    @Transactional
    public void update(MemoDto dto) {
        log.debug("メモを更新します: id={}, title={}", dto.getId(), dto.getTitle());

        findById(dto.getId()); // 存在チェック（見つからなければ例外）

        Memo memo = dto.toEntity();
        int updatedCount = memoMapper.update(memo);

        // 存在チェック済みでも 0 件になりうる（同時に別ユーザーが削除した等）ため、念のため確認する
        if (updatedCount == 0) {
            log.warn("更新対象のメモが見つかりませんでした: id={}", dto.getId());
            throw new MemoNotFoundException("メモが見つかりません: id=" + dto.getId());
        }
        log.info("メモを更新しました: id={}, title={}", memo.getId(), memo.getTitle());
    }

    /**
     * 削除（物理削除）。更新と同じく先に存在チェックしてから削除する。
     *
     * @param id 削除するメモの ID
     * @throws MemoNotFoundException メモが見つからない場合
     */
    @Transactional
    public void delete(Long id) {
        log.debug("メモを削除します: id={}", id);

        MemoDto dto = findById(id); // 存在チェック

        int deletedCount = memoMapper.deleteById(id);
        if (deletedCount == 0) {
            log.warn("削除対象のメモが見つかりませんでした: id={}", id);
            throw new MemoNotFoundException("メモが見つかりません: id=" + id);
        }
        log.info("メモを削除しました: id={}, title={}", id, dto.getTitle());
    }

    /**
     * キーワード検索。
     *
     * keyword が null や空白のみなら「検索意図なし」とみなして全件取得に委ねる。この業務ルールを
     * Service に置くことで、Controller は一覧・検索を分岐せず search() に一本化できる。
     *
     * @param keyword 検索キーワード
     * @return 検索結果の DTO リスト
     */
    @Transactional(readOnly = true)
    public List<MemoDto> search(String keyword) {
        log.debug("キーワードでメモを検索します: keyword={}", keyword);

        if (keyword == null || keyword.trim().isEmpty()) {
            log.debug("キーワードが空のため、全件取得します");
            return findAll();
        }

        List<Memo> memos = memoMapper.searchByKeyword(keyword);
        log.debug("検索結果: {}件", memos.size());
        return memos.stream()
                .map(MemoDto::fromEntity)
                .collect(Collectors.toList());
    }

    // ------------------------------------------------------------------
    // ページング機能（現在は未使用・実装演習用）
    //
    // findAllWithPaging / count / getTotalPages はどの Controller からも呼ばれていない。
    // 一覧画面にページングを追加するための「部品」を先に用意してある。
    //
    // 実装演習: 一覧にページングを足してみよう
    // 1. MemoController.list() に @RequestParam(required = false) で page を追加
    // 2. search()/findAll() の代わりに findAllWithPaging(page, 10) を呼ぶ
    // 3. getTotalPages(10) の結果を Model に渡す
    // 4. list.html に「前へ / 1 2 3 / 次へ」のリンクを追加（th:each と @{/memos(page=${i})}）
    // 5. 検索（keyword）とページングの併用をどう設計するか考える
    // ------------------------------------------------------------------

    /**
     * ページング付き全件取得（実装演習用・未使用）。
     *
     * @param pageNumber ページ番号（1 始まり）
     * @param pageSize 1 ページあたりの件数
     * @return 指定ページの DTO リスト
     */
    @Transactional(readOnly = true)
    public List<MemoDto> findAllWithPaging(int pageNumber, int pageSize) {
        log.debug("ページング付きでメモを取得します: page={}, size={}", pageNumber, pageSize);

        if (pageNumber < 1) {
            throw new IllegalArgumentException("ページ番号は1以上である必要があります: " + pageNumber);
        }
        if (pageSize < 1 || pageSize > 100) {
            throw new IllegalArgumentException("ページサイズは1～100の範囲である必要があります: " + pageSize);
        }

        // offset = (ページ番号 - 1) × ページサイズ（2 ページ目・10 件なら offset=10）
        int offset = (pageNumber - 1) * pageSize;

        List<Memo> memos = memoMapper.findAllWithPaging(pageSize, offset);
        log.debug("{}件のメモを取得しました", memos.size());
        return memos.stream()
                .map(MemoDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 総件数を取得（総ページ数の計算に使う。実装演習用・未使用）。
     *
     * @return メモの総件数
     */
    @Transactional(readOnly = true)
    public long count() {
        long count = memoMapper.count();
        log.debug("メモの総件数: {}", count);
        return count;
    }

    /**
     * 総ページ数を計算（実装演習用・未使用）。
     *
     * 【落とし穴: 自己呼び出しでは @Transactional が効かない】
     * このメソッドは内部で this.count() を呼ぶが、同一クラス内の直接呼び出しは Spring の
     * プロキシ（トランザクションを差し込む仕組み）を経由しないため、count() 側のアノテーションは
     * 効かない。だから「外から呼ばれる入口」であるこのメソッド自身にも付けている
     * （仕組みの詳細は docs/解説/トランザクション.md）。
     *
     * @param pageSize 1 ページあたりの件数
     * @return 総ページ数（総件数をページサイズで割って切り上げ）
     */
    @Transactional(readOnly = true)
    public int getTotalPages(int pageSize) {
        long totalCount = count();
        return (int) Math.ceil((double) totalCount / pageSize);
    }
}
