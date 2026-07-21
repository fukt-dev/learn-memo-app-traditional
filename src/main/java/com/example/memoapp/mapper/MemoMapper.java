package com.example.memoapp.mapper;

import com.example.memoapp.entity.Memo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * ============================================
 * MemoMapperインターフェース
 * ============================================
 *
 * 【このインターフェースの役割】
 * データベースのmemosテーブルに対するCRUD操作を定義します
 * MyBatisがこのインターフェースの実装クラスを自動生成してくれます
 *
 * 【Mapperとは】
 * MyBatisにおける「データアクセス層（DAO層）」のインターフェース
 * - データベースへのアクセス方法を定義する
 * - 実際のSQL文は MemoMapper.xml に記述する
 * - MyBatisが自動的にインターフェースとXMLを結びつける
 *
 * 【@Mapper アノテーション】
 * このインターフェースがMyBatisのMapperであることを示す
 * Spring Bootが自動的に実装クラスを生成し、Beanとして登録する
 *
 * つまり、実装クラスを書かなくても、@Autowired で注入できる！
 *
 * 例:
 * @Service
 * public class MemoService {
 *     @Autowired
 *     private MemoMapper memoMapper;  // 自動的にインスタンスが注入される
 * }
 *
 * 【なぜインターフェースだけでいいのか】
 * MyBatisが以下のような実装クラスを自動生成するから：
 *
 * public class MemoMapperImpl implements MemoMapper {
 *     @Override
 *     public List<Memo> findAll() {
 *         // MemoMapper.xml の findAll を実行
 *         // 結果を List<Memo> に変換して返す
 *     }
 *     // ... 他のメソッドも同様
 * }
 *
 * 開発者はインターフェースとXMLを書くだけで、面倒な実装コードは不要！
 */
@Mapper
public interface MemoMapper {

    /*
     * ============================================
     * CRUD操作のメソッド定義
     * ============================================
     *
     * 【命名規則】
     * - findAll: 全件取得
     * - findById: ID指定で1件取得
     * - insert: 新規登録
     * - update: 更新
     * - deleteById: ID指定で削除
     * - search: 検索
     *
     * 【戻り値の型】
     * - List<Memo>: 複数件の結果
     * - Optional<Memo>: 1件の結果（存在しない場合もある）
     * - int: 影響を受けた行数
     * - void: 戻り値なし
     */

    /**
     * 全件取得
     *
     * 【SQL】
     * SELECT * FROM memos ORDER BY created_at DESC;
     *
     * 【戻り値】
     * List<Memo> - メモの一覧（作成日時の新しい順）
     * データが0件の場合は空のListが返る（nullは返らない）
     *
     * 【使用例】
     * List<Memo> memos = memoMapper.findAll();
     * if (memos.isEmpty()) {
     *     System.out.println("メモがありません");
     * } else {
     *     for (Memo memo : memos) {
     *         System.out.println(memo.getTitle());
     *     }
     * }
     *
     * 【実装場所】
     * src/main/resources/mybatis/mapper/MemoMapper.xml の <select id="findAll">
     */
    List<Memo> findAll();

    /**
     * ID指定で1件取得
     *
     * 【SQL】
     * SELECT * FROM memos WHERE id = #{id};
     *
     * 【パラメータ】
     * @param id メモID
     *
     * 【戻り値】
     * Optional<Memo> - メモのデータ
     * - データが存在する場合: Optional.of(memo)
     * - データが存在しない場合: Optional.empty()
     *
     * 【なぜOptionalか】
     * - IDが存在しない可能性がある（削除済み、不正なIDなど）
     * - nullを返すとNullPointerExceptionの危険がある
     * - Optionalを使うことで、「値が存在しないかもしれない」ことを明示できる
     *
     * 【使用例】
     * Optional<Memo> memoOpt = memoMapper.findById(1L);
     * if (memoOpt.isPresent()) {
     *     Memo memo = memoOpt.get();
     *     System.out.println(memo.getTitle());
     * } else {
     *     System.out.println("メモが見つかりません");
     * }
     *
     * // またはラムダ式で
     * memoMapper.findById(1L)
     *     .ifPresent(memo -> System.out.println(memo.getTitle()));
     *
     * 【実装場所】
     * MemoMapper.xml の <select id="findById">
     */
    Optional<Memo> findById(Long id);

    /**
     * 新規登録
     *
     * 【SQL】
     * INSERT INTO memos (title, content) VALUES (#{title}, #{content});
     *
     * 【パラメータ】
     * @param memo 登録するメモ（title, content を設定しておく）
     *
     * 【戻り値】
     * void（なし）
     *
     * 【動作】
     * 1. データベースに新しい行を追加
     * 2. データベースが自動的に id を採番（BIGSERIAL）
     * 3. 採番された id が、引数の memo オブジェクトに自動的にセットされる
     *    （MyBatisの useGeneratedKeys 機能により）
     *
     * 【使用例】
     * Memo memo = new Memo();
     * memo.setTitle("新しいメモ");
     * memo.setContent("内容");
     * memoMapper.insert(memo);
     *
     * // insert後、memo.getId() で採番されたIDを取得できる
     * System.out.println("登録されたID: " + memo.getId());
     *
     * 【なぜIDが自動セットされるのか】
     * MemoMapper.xml で以下のように設定しているから：
     * <insert id="insert" useGeneratedKeys="true" keyProperty="id">
     *
     * useGeneratedKeys="true": 自動採番されたキーを取得
     * keyProperty="id": 取得したキーを memo.id にセット
     *
     * 【実装場所】
     * MemoMapper.xml の <insert id="insert">
     */
    void insert(Memo memo);

    /**
     * 更新
     *
     * 【SQL】
     * UPDATE memos SET title = #{title}, content = #{content} WHERE id = #{id};
     *
     * 【パラメータ】
     * @param memo 更新するメモ（id, title, content を設定しておく）
     *
     * 【戻り値】
     * int - 更新された行数
     * - 1: 更新成功
     * - 0: 対象のIDが存在しない（更新失敗）
     *
     * 【動作】
     * 1. 指定されたIDのメモを更新
     * 2. updated_at はデータベースのトリガーで自動更新される
     *
     * 【使用例】
     * Memo memo = memoMapper.findById(1L).orElseThrow();
     * memo.setTitle("更新されたタイトル");
     * memo.setContent("更新された内容");
     * int count = memoMapper.update(memo);
     * if (count == 0) {
     *     System.out.println("更新対象が見つかりませんでした");
     * }
     *
     * 【なぜ戻り値がintか】
     * - 更新対象が存在したかどうかを確認できる
     * - 楽観的ロックの実装にも使える（後述）
     *
     * 【楽観的ロックとは】
     * 複数のユーザーが同時に更新した場合の競合を防ぐ仕組み
     * 例: WHERE id = #{id} AND updated_at = #{updatedAt}
     * updated_atが変わっていたら更新されず、count = 0 になる
     *
     * 【実装場所】
     * MemoMapper.xml の <update id="update">
     */
    int update(Memo memo);

    /**
     * ID指定で削除
     *
     * 【SQL】
     * DELETE FROM memos WHERE id = #{id};
     *
     * 【パラメータ】
     * @param id 削除するメモのID
     *
     * 【戻り値】
     * int - 削除された行数
     * - 1: 削除成功
     * - 0: 対象のIDが存在しない（削除失敗）
     *
     * 【使用例】
     * int count = memoMapper.deleteById(1L);
     * if (count == 0) {
     *     System.out.println("削除対象が見つかりませんでした");
     * }
     *
     * 【実装場所】
     * MemoMapper.xml の <delete id="deleteById">
     */
    int deleteById(Long id);

    /**
     * キーワード検索
     *
     * 【SQL】
     * SELECT * FROM memos
     * WHERE title LIKE CONCAT('%', #{keyword}, '%')
     *    OR content LIKE CONCAT('%', #{keyword}, '%')
     * ORDER BY created_at DESC;
     *
     * 【パラメータ】
     * @param keyword 検索キーワード
     *
     * 【戻り値】
     * List<Memo> - 検索結果のメモ一覧
     *
     * 【動作】
     * タイトルまたは本文にキーワードを含むメモを検索
     * 例: keyword = "買い物" → "買い物リスト" や "明日買い物に行く" がヒット
     *
     * 【@Param アノテーション】
     * このアノテーションを付けることで、XML側で #{keyword} として参照できる
     *
     * @Param("keyword") → XML側で #{keyword}
     * @Param("id") → XML側で #{id}
     *
     * パラメータが1つの場合は省略可能だが、明示的に書くことを推奨
     * （複数パラメータの場合は必須）
     *
     * 【使用例】
     * List<Memo> results = memoMapper.searchByKeyword("買い物");
     * System.out.println("検索結果: " + results.size() + "件");
     *
     * 【実装場所】
     * MemoMapper.xml の <select id="searchByKeyword">
     */
    List<Memo> searchByKeyword(@Param("keyword") String keyword);

    /**
     * ページング付き全件取得
     *
     * 【現在は未使用（実装演習用）】
     * このメソッドと count() は、現時点ではどの画面からも使われていない。
     * 一覧画面へのページング追加演習用の部品（演習手順は MemoService のコメント参照）
     *
     * 【SQL】
     * SELECT * FROM memos
     * ORDER BY created_at DESC
     * LIMIT #{limit} OFFSET #{offset};
     *
     * 【パラメータ】
     * @param limit 取得件数（1ページあたりの表示件数）
     * @param offset 開始位置（スキップする件数）
     *
     * 【戻り値】
     * List<Memo> - 指定範囲のメモ一覧
     *
     * 【ページングの計算】
     * - 1ページ目: offset = 0, limit = 10 → 1～10件目を取得
     * - 2ページ目: offset = 10, limit = 10 → 11～20件目を取得
     * - 3ページ目: offset = 20, limit = 10 → 21～30件目を取得
     *
     * offset = (ページ番号 - 1) * 1ページあたりの件数
     *
     * 【なぜページングが必要か】
     * - メモが数千件、数万件になると、全件取得は遅い
     * - 画面に一度に表示できる件数は限られている
     * - 必要な分だけ取得することでパフォーマンスを向上
     *
     * 【使用例】
     * // 1ページ目（1～10件）を取得
     * List<Memo> page1 = memoMapper.findAllWithPaging(10, 0);
     *
     * // 2ページ目（11～20件）を取得
     * List<Memo> page2 = memoMapper.findAllWithPaging(10, 10);
     *
     * // ページ番号から計算
     * int pageNumber = 2;  // 2ページ目
     * int pageSize = 10;   // 1ページ10件
     * int offset = (pageNumber - 1) * pageSize;
     * List<Memo> memos = memoMapper.findAllWithPaging(pageSize, offset);
     *
     * 【実装場所】
     * MemoMapper.xml の <select id="findAllWithPaging">
     */
    List<Memo> findAllWithPaging(@Param("limit") int limit, @Param("offset") int offset);

    /**
     * 総件数を取得
     *
     * 【SQL】
     * SELECT COUNT(*) FROM memos;
     *
     * 【戻り値】
     * long - メモの総件数
     *
     * 【なぜ必要か】
     * ページングを実装する際、総ページ数を計算するために必要
     *
     * 総ページ数 = CEIL(総件数 / 1ページあたりの件数)
     *
     * 例:
     * - 総件数: 25件、1ページ10件 → 総ページ数: 3ページ
     * - 総件数: 30件、1ページ10件 → 総ページ数: 3ページ
     * - 総件数: 31件、1ページ10件 → 総ページ数: 4ページ
     *
     * 【使用例】
     * long totalCount = memoMapper.count();
     * int pageSize = 10;
     * int totalPages = (int) Math.ceil((double) totalCount / pageSize);
     * System.out.println("総ページ数: " + totalPages);
     *
     * 【実装場所】
     * MemoMapper.xml の <select id="count">
     */
    long count();
}

/*
 * ============================================
 * MyBatisの仕組み（補足）
 * ============================================
 *
 * 【1. インターフェースとXMLの対応付け】
 *
 * MemoMapper.java:
 * package com.example.memoapp.mapper;
 * public interface MemoMapper {
 *     List<Memo> findAll();
 * }
 *
 * ↓ 対応
 *
 * MemoMapper.xml:
 * <mapper namespace="com.example.memoapp.mapper.MemoMapper">
 *     <select id="findAll" resultType="Memo">
 *         SELECT * FROM memos ORDER BY created_at DESC
 *     </select>
 * </mapper>
 *
 * namespace と package が一致
 * メソッド名 と id が一致
 * → MyBatisが自動的に結びつける
 *
 * 【2. パラメータの渡し方】
 *
 * 単一パラメータ:
 * Optional<Memo> findById(Long id);
 * → XML側で #{id} として参照
 *
 * @Param付き:
 * List<Memo> searchByKeyword(@Param("keyword") String keyword);
 * → XML側で #{keyword} として参照
 *
 * 複数パラメータ:
 * List<Memo> findAllWithPaging(@Param("limit") int limit, @Param("offset") int offset);
 * → XML側で #{limit}, #{offset} として参照
 *
 * オブジェクト:
 * void insert(Memo memo);
 * → XML側で #{title}, #{content} のようにプロパティ名で参照
 *
 * 【3. 戻り値の型とresultType】
 *
 * List<Memo>:
 * <select id="findAll" resultType="Memo">
 * → 複数行の結果を List<Memo> に変換
 *
 * Optional<Memo>:
 * <select id="findById" resultType="Memo">
 * → 0件または1件の結果を Optional<Memo> に変換
 *
 * int, long:
 * <select id="count" resultType="long">
 * → 単一の数値を返す
 *
 * void:
 * <insert id="insert">
 * → 戻り値なし
 *
 * 【4. application.ymlの設定との関連】
 *
 * mybatis:
 *   mapper-locations: classpath:mybatis/mapper/**.xml
 *   → MemoMapper.xmlの場所を指定
 *
 *   type-aliases-package: com.example.memoapp.entity
 *   → resultType="Memo" と書くだけでMemoクラスを参照できる
 *      （完全修飾名 com.example.memoapp.entity.Memo を書かなくてもOK）
 *
 *   configuration:
 *     map-underscore-to-camel-case: true
 *     → created_at ↔ createdAt の自動変換
 */
