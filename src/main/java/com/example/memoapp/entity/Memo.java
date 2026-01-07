package com.example.memoapp.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * ============================================
 * Memoエンティティクラス
 * ============================================
 *
 * 【このクラスの役割】
 * データベースの「memos」テーブルの1行（1レコード）を表現するクラスです
 * データアクセス層（Mapper）からデータベースのデータを受け取るために使います
 *
 * 【エンティティ（Entity）とは】
 * データベースのテーブル構造をJavaのクラスで表現したもの
 * 1つのインスタンス = データベースの1行に対応します
 *
 * 例:
 * データベース:
 * | id | title | content | created_at | updated_at |
 * |----|-------|---------|------------|------------|
 * | 1  | 買い物 | 牛乳を買う | 2025-11-17 | 2025-11-17 |
 *
 * ↓ これをJavaで表現
 *
 * Memo memo = new Memo();
 * memo.setId(1L);
 * memo.setTitle("買い物");
 * memo.setContent("牛乳を買う");
 * ...
 *
 * 【なぜEntityとDTOを分けるのか】
 * - Entity: データベースの構造に対応（すべてのカラムを持つ）
 * - DTO: 画面やAPIとのやり取りに使う（必要な項目だけ持つ）
 *
 * Entityはデータベースの変更に影響されますが、
 * DTOを分けることで、画面側のコードは影響を受けにくくなります
 */

/*
 * ============================================
 * Lombokアノテーションの説明
 * ============================================
 *
 * Lombokとは、定型的なコード（ボイラープレートコード）を
 * 自動生成してくれるライブラリです
 *
 * これらのアノテーションを付けるだけで、
 * getter/setter/toString/equals/hashCode/コンストラクタ
 * などが自動的に生成されます
 */

/**
 * @Data
 *
 * 【自動生成されるもの】
 * - すべてのフィールドのgetterメソッド
 *   例: getId(), getTitle(), getContent() など
 *
 * - すべての非final フィールドのsetterメソッド
 *   例: setId(Long id), setTitle(String title) など
 *
 * - toString()メソッド
 *   例: "Memo(id=1, title=買い物, content=牛乳を買う, ...)"
 *
 * - equals()とhashCode()メソッド
 *   オブジェクトの同一性を判定するメソッド
 *
 * - requiredArgsConstructor
 *   final/非nullフィールドのコンストラクタ
 *
 * 【もしLombokがなかったら】
 * 以下のようなコードを全て手書きする必要があります：
 *
 * public Long getId() { return id; }
 * public void setId(Long id) { this.id = id; }
 * public String getTitle() { return title; }
 * public void setTitle(String title) { this.title = title; }
 * ... (以下、全フィールド分)
 *
 * public String toString() {
 *     return "Memo(id=" + id + ", title=" + title + ", ...";
 * }
 *
 * public boolean equals(Object o) { ... }
 * public int hashCode() { ... }
 *
 * これが @Data 一つで全て自動生成されます！
 */
@Data

/**
 * @NoArgsConstructor
 *
 * 【自動生成されるもの】
 * 引数なしのコンストラクタ
 *
 * 例:
 * public Memo() {
 * }
 *
 * 【なぜ必要か】
 * - MyBatisがデータベースから取得したデータをオブジェクトに変換する際、
 *   まず引数なしコンストラクタでインスタンスを作成してから、
 *   setterメソッドで値を設定します
 *
 * - フレームワークの多くは、リフレクション機能を使ってオブジェクトを生成するため、
 *   デフォルトコンストラクタ（引数なし）が必要になります
 */
@NoArgsConstructor

/**
 * @AllArgsConstructor
 *
 * 【自動生成されるもの】
 * すべてのフィールドを引数に取るコンストラクタ
 *
 * 例:
 * public Memo(Long id, String title, String content, LocalDateTime createdAt, LocalDateTime updatedAt) {
 *     this.id = id;
 *     this.title = title;
 *     this.content = content;
 *     this.createdAt = createdAt;
 *     this.updatedAt = updatedAt;
 * }
 *
 * 【いつ使うか】
 * テストコードなどで、簡単にインスタンスを作りたい場合に便利
 *
 * 例:
 * Memo memo = new Memo(1L, "買い物", "牛乳を買う", LocalDateTime.now(), LocalDateTime.now());
 *
 * setter を何度も呼ぶより、1行で書けるので楽！
 */
@AllArgsConstructor
public class Memo {

    /*
     * ============================================
     * フィールド定義
     * ============================================
     *
     * 【命名規則】
     * - キャメルケース（camelCase）で書く
     * - データベースのカラム名（スネークケース）とは異なるが、
     *   application.yml の設定により自動変換される
     *
     * 例:
     * created_at (DB) ←→ createdAt (Java)
     *
     * 【型の選び方】
     * - id: Long型（BIGSERIALに対応）
     * - title, content: String型（VARCHAR, TEXTに対応）
     * - createdAt, updatedAt: LocalDateTime型（TIMESTAMPに対応）
     */

    /**
     * メモID（主キー）
     *
     * 【型】Long
     * - Javaのlong（プリミティブ型）ではなく、Long（ラッパークラス）を使う
     *
     * 【なぜLongか】
     * - データベースのBIGSERIAL型は64ビット整数
     * - Javaのintは32ビットなので足りない
     * - longは64ビットなので対応できる
     *
     * 【なぜプリミティブ型ではなくラッパークラスか】
     * - long: nullを表現できない（デフォルト値は0）
     * - Long: nullを表現できる
     *
     * データベースのNULL値を正しく扱うためにはLongを使う必要がある
     * また、新規作成時（まだIDが割り当てられていない）にnullとして扱える
     */
    private Long id;

    /**
     * メモのタイトル
     *
     * 【型】String
     * - データベースのVARCHAR(200)に対応
     *
     * 【制約】
     * - データベース側: NOT NULL, 最大200文字
     * - バリデーション: DTO側で @NotBlank と @Size で検証
     */
    private String title;

    /**
     * メモの本文
     *
     * 【型】String
     * - データベースのTEXT型に対応
     *
     * 【制約】
     * - データベース側: NOT NULL
     * - バリデーション: DTO側で @NotBlank で検証
     *
     * 【メモ】
     * TEXT型は制限がほぼないが、Javaの String型 は
     * 最大で約21億文字（2^31 - 1）まで扱える
     */
    private String content;

    /**
     * 作成日時
     *
     * 【型】LocalDateTime
     * - Java 8 以降で導入された日付・時刻を扱うクラス
     * - データベースのTIMESTAMP型に対応
     *
     * 【LocalDateTime vs Date】
     * 古い java.util.Date ではなく java.time.LocalDateTime を使う理由:
     * - LocalDateTime: イミュータブル（不変）で安全
     * - LocalDateTime: 直感的なAPIで扱いやすい
     * - LocalDateTime: タイムゾーンの問題が起きにくい
     *
     * 【値の設定】
     * データベース側で自動設定されるため、通常はJava側で設定しない
     */
    private LocalDateTime createdAt;

    /**
     * 更新日時
     *
     * 【型】LocalDateTime
     * - データベースのTIMESTAMP型に対応
     *
     * 【値の設定】
     * データベースのトリガーで自動更新されるため、Java側で設定しない
     * （V1__Create_memos_table.sql のトリガー関数により自動更新）
     */
    private LocalDateTime updatedAt;

    /*
     * ============================================
     * ビジネスロジックメソッド（オプション）
     * ============================================
     *
     * Entityクラスには、そのデータに関連するビジネスロジックを
     * 書くこともできます（Domain Driven Designの考え方）
     *
     * 例:
     *
     * メモが最近作成されたか判定する
     * @return 作成から24時間以内の場合 true
     *
     * public boolean isNew() {
     *     return createdAt.isAfter(LocalDateTime.now().minusDays(1));
     * }
     *
     * メモが編集されているか判定する
     * @return 作成日時と更新日時が異なる場合 true
     *
     * public boolean isEdited() {
     *     return !createdAt.equals(updatedAt);
     * }
     *
     * ただし、このアプリではシンプルに保つため、
     * ビジネスロジックは主にServiceレイヤーに書きます
     */
}

/*
 * ============================================
 * このクラスの使われ方
 * ============================================
 *
 * 【1. Mapperから取得】
 * MemoMapper.java:
 * List<Memo> findAll();
 *
 * MemoService.java:
 * List<Memo> memos = memoMapper.findAll();
 * // データベースから取得したメモの一覧が List<Memo> で返ってくる
 *
 * 【2. 新規作成】
 * MemoService.java:
 * Memo memo = new Memo();
 * memo.setTitle("新しいメモ");
 * memo.setContent("内容");
 * memoMapper.insert(memo);
 * // id, createdAt, updatedAt はデータベース側で自動設定される
 *
 * 【3. 更新】
 * MemoService.java:
 * Memo memo = memoMapper.findById(1L);
 * memo.setTitle("更新されたタイトル");
 * memoMapper.update(memo);
 * // updatedAt はデータベースのトリガーで自動更新される
 *
 * 【4. DTOへの変換】
 * MemoService.java:
 * Memo memo = memoMapper.findById(1L);
 * MemoDto dto = new MemoDto();
 * dto.setId(memo.getId());
 * dto.setTitle(memo.getTitle());
 * dto.setContent(memo.getContent());
 * // EntityからDTOに変換して、Controllerに渡す
 *
 * このように、Entityはデータベースとのやり取りに特化したクラスです
 */
