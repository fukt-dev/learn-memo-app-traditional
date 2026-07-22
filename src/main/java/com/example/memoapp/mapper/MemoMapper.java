package com.example.memoapp.mapper;

import com.example.memoapp.entity.Memo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * memos テーブルへの CRUD を定義する MyBatis の Mapper（データアクセス層）。
 *
 * インターフェースだけを書き、実装は MyBatis が自動生成する。実際の SQL は同名の XML
 * （src/main/resources/mybatis/mapper/MemoMapper.xml）にメソッド名と対応づけて記述する。
 * @Mapper が付いていると Spring Boot が実装を Bean 登録するので、Service から注入できる。
 *
 * 学習ポイント（@Mapper・XML 対応・#{} と ${}・useGeneratedKeys・動的 SQL など）:
 * docs/MyBatis2waySQL解説.md（※ docs/解説/ ではなく docs/ 直下）。
 *
 * モダン版との対比: モダン版は Spring Data JPA。この Mapper + XML に相当する SQL は、
 * リポジトリのメソッド名（findAllByOrderByCreatedAtDesc 等）から自動生成される。
 */
@Mapper
public interface MemoMapper {

    /**
     * 全件取得（作成日時の新しい順）。
     *
     * @return メモ一覧（0 件なら空リスト。null は返らない）
     */
    List<Memo> findAll();

    /**
     * ID 指定で 1 件取得。存在しないこともあるため戻り値を Optional にし、null を返さない。
     *
     * @param id メモ ID
     * @return 見つかれば中身あり、無ければ空の Optional
     */
    Optional<Memo> findById(Long id);

    /**
     * 新規登録。採番された id は XML の useGeneratedKeys="true" により引数の memo にセットされる。
     *
     * @param memo 登録するメモ（title / content を設定しておく。id・日時は DB 側で決まる）
     */
    void insert(Memo memo);

    /**
     * 更新。戻り値の int は影響行数（1=成功、0=対象 ID なし）。updated_at は DB トリガーで自動更新。
     *
     * @param memo 更新するメモ（id / title / content を設定しておく）
     * @return 更新された行数
     */
    int update(Memo memo);

    /**
     * ID 指定で削除。戻り値の int は影響行数（1=成功、0=対象 ID なし）。
     *
     * @param id 削除するメモの ID
     * @return 削除された行数
     */
    int deleteById(Long id);

    /**
     * キーワード検索（タイトルまたは本文の部分一致）。
     *
     * パラメータが 1 つでも @Param を付けているのは、XML 側で #{keyword} と名前で参照するため
     * （複数パラメータでは @Param が必須。LIKE の詳細や落とし穴は XML 側のコメントを参照）。
     *
     * @param keyword 検索キーワード
     * @return 一致したメモ一覧
     */
    List<Memo> searchByKeyword(@Param("keyword") String keyword);

    /**
     * ページング付き全件取得（実装演習用・未使用。演習手順は MemoService のコメント参照）。
     *
     * @param limit 取得件数（1 ページの表示件数）
     * @param offset 開始位置（スキップする件数）
     * @return 指定範囲のメモ一覧
     */
    List<Memo> findAllWithPaging(@Param("limit") int limit, @Param("offset") int offset);

    /**
     * 総件数を取得（総ページ数の計算に使う。実装演習用・未使用）。
     *
     * @return メモの総件数
     */
    long count();
}
