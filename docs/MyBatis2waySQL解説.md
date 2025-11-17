# MyBatis 2waySQL 完全解説

## 目次

1. [2waySQLとは](#1-2waysqlとは)
2. [なぜ2waySQLが重要なのか](#2-なぜ2waysqlが重要なのか)
3. [MyBatisの基本構成](#3-mybatisの基本構成)
4. [基本的なSQL記述](#4-基本的なsql記述)
5. [動的SQL](#5-動的sql)
6. [実践例集](#6-実践例集)
7. [テスト方法](#7-テスト方法)
8. [パフォーマンスチューニング](#8-パフォーマンスチューニング)
9. [トラブルシューティング](#9-トラブルシューティング)
10. [ベストプラクティス](#10-ベストプラクティス)

---

## 1. 2waySQLとは

### 1.1 定義

**2waySQL**（ツーウェイSQL）とは、以下の2つの使い方ができるSQLのことです：

1. **アプリケーション内で動的SQLとして実行**
   - 条件によってSQLが変化する
   - MyBatisが実行時に組み立てる

2. **SQLツールで直接実行してテスト可能**
   - pgAdminやDBeaverで実行できる
   - SQL単体でのテストが可能

### 1.2 具体例

#### 従来のSQL（文字列連結）

```java
// ❌ 悪い例: Javaコードで文字列連結
String sql = "SELECT * FROM memos WHERE 1=1";
if (keyword != null) {
    sql += " AND (title LIKE '%" + keyword + "%' OR content LIKE '%" + keyword + "%')";
}
sql += " ORDER BY created_at DESC";

// 問題点:
// - SQLインジェクションの危険性
// - SQL単体でテストできない
// - 読みにくい
```

#### 2waySQL（MyBatis）

```xml
<!-- ✅ 良い例: 2waySQL -->
<select id="searchByKeyword" resultMap="memoResultMap">
    SELECT
        id,
        title,
        content,
        created_at,
        updated_at
    FROM
        memos
    <where>
        <if test="keyword != null and keyword != ''">
            AND (
                title LIKE CONCAT('%', #{keyword}, '%')
                OR content LIKE CONCAT('%', #{keyword}, '%')
            )
        </if>
    </where>
    ORDER BY
        created_at DESC
</select>
```

**メリット**:
- ✅ SQLインジェクション対策済み（`#{keyword}` がプリペアドステートメント）
- ✅ SQLツールで実行可能（`<if>` タグを削除すれば普通のSQL）
- ✅ 読みやすい
- ✅ 保守しやすい

---

## 2. なぜ2waySQLが重要なのか

### 2.1 実務での必要性

日本の業務システム開発では、以下の理由で2waySQLが広く使われています：

#### 2.1.1 複雑なクエリへの対応

**業務システムの特徴**:
- 複数テーブルのJOIN
- 条件による動的なWHERE句
- 複雑な集計処理

**ORMの限界**:
```java
// ORMだとこうなる（読みにくい！）
List<Memo> memos = memoRepository.findAll(
    Specification.where(MemoSpecifications.titleContains(keyword))
        .or(MemoSpecifications.contentContains(keyword))
);
```

**2waySQLだとこうなる（読みやすい！）**:
```xml
<select id="searchByKeyword">
    SELECT * FROM memos
    WHERE title LIKE CONCAT('%', #{keyword}, '%')
       OR content LIKE CONCAT('%', #{keyword}, '%')
</select>
```

#### 2.1.2 パフォーマンスチューニング

**SQLが明示的**:
- どんなSQLが実行されるかすぐわかる
- EXPLAIN（実行計画）を簡単に確認できる
- インデックスの効果を検証しやすい

**ORMの場合**:
- 生成されるSQLを確認するのが大変
- 意図しないN+1問題が発生しやすい

#### 2.1.3 チーム開発

**SQLの可視性**:
- DBAやSQLに詳しいメンバーがレビューしやすい
- SQLファイルだけ見ればロジックがわかる
- 新しいメンバーでも理解しやすい

### 2.2 Spring Data JPAとの比較

| 項目 | MyBatis 2waySQL | Spring Data JPA |
|------|----------------|-----------------|
| **学習曲線** | SQLがわかれば使える | JPQLとエンティティ管理を学ぶ必要 |
| **複雑なクエリ** | 得意 | 苦手（ネイティブクエリに頼る） |
| **パフォーマンス** | チューニングしやすい | N+1問題が発生しやすい |
| **コード量** | やや多い | 少ない（単純なCRUDは楽） |
| **実務採用** | 日本で多い | 海外で多い |

**推奨**:
- 業務システム（複雑なSQL） → MyBatis
- シンプルなCRUD → Spring Data JPA
- 両方を組み合わせることも可能

---

## 3. MyBatisの基本構成

### 3.1 3つのコンポーネント

```
【Mapperインターフェース】
    ↓ ↑
【Mapper XML】
    ↓ ↑
【データベース】
```

#### 3.1.1 Mapperインターフェース（Java）

**役割**: データベース操作のメソッドを定義

```java
package com.example.memoapp.mapper;

import com.example.memoapp.entity.Memo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * メモMapperインターフェース
 *
 * このインターフェースのメソッドと、Mapper XMLのSQLが紐付く
 */
@Mapper  // このアノテーションで、MyBatisがMapperとして認識
public interface MemoMapper {

    /**
     * すべてのメモを取得
     *
     * @return メモのリスト
     */
    List<Memo> findAll();

    /**
     * IDでメモを取得
     *
     * @param id メモID
     * @return メモ（存在しない場合はnull）
     */
    Memo findById(@Param("id") Long id);  // @Param で XML側のパラメータ名を指定

    /**
     * メモを新規作成
     *
     * @param memo メモエンティティ
     * @return 挿入件数（通常は1）
     */
    int insert(Memo memo);

    /**
     * メモを更新
     *
     * @param memo メモエンティティ
     * @return 更新件数（通常は1）
     */
    int update(Memo memo);

    /**
     * メモを削除
     *
     * @param id メモID
     * @return 削除件数（通常は1）
     */
    int deleteById(@Param("id") Long id);
}
```

#### 3.1.2 Mapper XML（2waySQL）

**役割**: 実際に実行されるSQLを定義

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">

<!-- namespace: Mapperインターフェースの完全修飾名 -->
<mapper namespace="com.example.memoapp.mapper.MemoMapper">

    <!-- resultMap: SELECT結果とJavaオブジェクトのマッピング定義 -->
    <resultMap id="memoResultMap" type="com.example.memoapp.entity.Memo">
        <id property="id" column="id"/>
        <result property="title" column="title"/>
        <result property="content" column="content"/>
        <result property="createdAt" column="created_at"/>
        <result property="updatedAt" column="updated_at"/>
    </resultMap>

    <!-- id: Mapperインターフェースのメソッド名と一致させる -->
    <select id="findAll" resultMap="memoResultMap">
        SELECT
            id,
            title,
            content,
            created_at,
            updated_at
        FROM
            memos
        ORDER BY
            created_at DESC
    </select>

    <select id="findById" resultMap="memoResultMap">
        SELECT
            id,
            title,
            content,
            created_at,
            updated_at
        FROM
            memos
        WHERE
            id = #{id}  <!-- #{id} がプリペアドステートメントのパラメータ -->
    </select>

    <!-- useGeneratedKeys: 自動採番されたIDを取得 -->
    <!-- keyProperty: IDを格納するプロパティ名 -->
    <insert id="insert" useGeneratedKeys="true" keyProperty="id">
        INSERT INTO memos (
            title,
            content,
            created_at,
            updated_at
        ) VALUES (
            #{title},
            #{content},
            CURRENT_TIMESTAMP,
            CURRENT_TIMESTAMP
        )
    </insert>

    <update id="update">
        UPDATE memos
        SET
            title = #{title},
            content = #{content}
            <!-- updated_atはトリガーで自動更新されるため不要 -->
        WHERE
            id = #{id}
    </update>

    <delete id="deleteById">
        DELETE FROM memos
        WHERE id = #{id}
    </delete>

</mapper>
```

### 3.2 ファイル配置

```
src/main/
├── java/com/example/memoapp/
│   └── mapper/
│       └── MemoMapper.java          ← Mapperインターフェース
└── resources/
    └── mybatis/mapper/
        └── MemoMapper.xml            ← Mapper XML（2waySQL）
```

**application.ymlで場所を指定**:
```yaml
mybatis:
  mapper-locations: classpath:mybatis/mapper/**/*.xml
  type-aliases-package: com.example.memoapp.entity
```

---

## 4. 基本的なSQL記述

### 4.1 パラメータの渡し方

#### 4.1.1 `#{}` と `${}` の違い

| 記法 | 用途 | SQLインジェクション対策 | 使いどころ |
|------|------|----------------------|-----------|
| `#{param}` | **プリペアドステートメント** | ✅ 安全 | **通常はこちらを使う** |
| `${param}` | 文字列置換 | ❌ 危険 | テーブル名、カラム名の動的変更（非推奨） |

**例**:
```xml
<!-- ✅ 正しい: #{} を使う -->
<select id="findById">
    SELECT * FROM memos WHERE id = #{id}
</select>
<!-- 実行されるSQL: SELECT * FROM memos WHERE id = ? -->
<!-- パラメータ: [1] -->

<!-- ❌ 危険: ${} を使うとSQLインジェクションの恐れ -->
<select id="findById">
    SELECT * FROM memos WHERE id = ${id}
</select>
<!-- 実行されるSQL: SELECT * FROM memos WHERE id = 1 -->
<!-- もし id = "1 OR 1=1" と渡されたら... -->
```

#### 4.1.2 単一パラメータ

```java
// Mapperインターフェース
Memo findById(@Param("id") Long id);
```

```xml
<!-- Mapper XML -->
<select id="findById">
    SELECT * FROM memos WHERE id = #{id}
</select>
```

#### 4.1.3 複数パラメータ

```java
// Mapperインターフェース
List<Memo> findByTitleAndContent(
    @Param("title") String title,
    @Param("content") String content
);
```

```xml
<!-- Mapper XML -->
<select id="findByTitleAndContent">
    SELECT * FROM memos
    WHERE title = #{title}
      AND content = #{content}
</select>
```

#### 4.1.4 オブジェクトパラメータ

```java
// Mapperインターフェース
int insert(Memo memo);
```

```xml
<!-- Mapper XML -->
<insert id="insert">
    INSERT INTO memos (title, content)
    VALUES (#{title}, #{content})
    <!-- Memoオブジェクトのgetterを使ってtitle、contentを取得 -->
</insert>
```

### 4.2 resultMap と resultType

#### 4.2.1 resultType（シンプル）

**用途**: カラム名とプロパティ名が一致している場合

```xml
<!-- Entityクラス: com.example.memoapp.entity.Memo -->
<select id="findAll" resultType="com.example.memoapp.entity.Memo">
    SELECT
        id,
        title,
        content,
        created_at AS createdAt,  <!-- スネークケース → キャメルケース -->
        updated_at AS updatedAt
    FROM
        memos
</select>
```

**または、application.ymlで自動変換**:
```yaml
mybatis:
  configuration:
    map-underscore-to-camel-case: true  # created_at → createdAt
```

これで `AS` が不要になります：
```xml
<select id="findAll" resultType="com.example.memoapp.entity.Memo">
    SELECT id, title, content, created_at, updated_at
    FROM memos
</select>
```

#### 4.2.2 resultMap（詳細）

**用途**: 複雑なマッピングが必要な場合

```xml
<resultMap id="memoResultMap" type="com.example.memoapp.entity.Memo">
    <id property="id" column="id"/>          <!-- 主キー -->
    <result property="title" column="title"/>
    <result property="content" column="content"/>
    <result property="createdAt" column="created_at"/>
    <result property="updatedAt" column="updated_at"/>
</resultMap>

<select id="findAll" resultMap="memoResultMap">
    SELECT id, title, content, created_at, updated_at
    FROM memos
</select>
```

---

## 5. 動的SQL

### 5.1 `<if>` - 条件分岐

**用途**: 条件によってSQLの一部を追加/削除

```xml
<select id="searchByKeyword">
    SELECT * FROM memos
    WHERE 1=1
    <if test="keyword != null and keyword != ''">
        AND (
            title LIKE CONCAT('%', #{keyword}, '%')
            OR content LIKE CONCAT('%', #{keyword}, '%')
        )
    </if>
    ORDER BY created_at DESC
</select>
```

**動作**:
- `keyword` が `null` または空文字 → `WHERE 1=1` のみ
- `keyword` が指定されている → `WHERE 1=1 AND (...)` が追加

**SQLツールでのテスト**:
```sql
-- keywordが指定されている場合
SELECT * FROM memos
WHERE 1=1
  AND (title LIKE '%学習%' OR content LIKE '%学習%')
ORDER BY created_at DESC;

-- keywordが指定されていない場合
SELECT * FROM memos
WHERE 1=1
ORDER BY created_at DESC;
```

### 5.2 `<where>` - WHERE句の自動生成

**用途**: `WHERE 1=1` を書かずに動的にWHERE句を生成

```xml
<select id="searchMemos">
    SELECT * FROM memos
    <where>
        <if test="keyword != null and keyword != ''">
            AND (title LIKE CONCAT('%', #{keyword}, '%')
                 OR content LIKE CONCAT('%', #{keyword}, '%'))
        </if>
        <if test="startDate != null">
            AND created_at >= #{startDate}
        </if>
        <if test="endDate != null">
            AND created_at &lt;= #{endDate}  <!-- < は &lt; でエスケープ -->
        </if>
    </where>
    ORDER BY created_at DESC
</select>
```

**メリット**:
- 先頭の `AND` を自動で削除してくれる
- すべての条件が `false` なら `WHERE` 自体を削除

**動作例**:
```sql
-- keywordのみ指定
SELECT * FROM memos
WHERE title LIKE '%学習%' OR content LIKE '%学習%'
ORDER BY created_at DESC;

-- すべて指定
SELECT * FROM memos
WHERE (title LIKE '%学習%' OR content LIKE '%学習%')
  AND created_at >= '2025-01-01'
  AND created_at <= '2025-12-31'
ORDER BY created_at DESC;

-- 何も指定なし
SELECT * FROM memos
ORDER BY created_at DESC;
```

### 5.3 `<choose>`, `<when>`, `<otherwise>` - switch文

**用途**: 複数の条件から1つを選ぶ

```xml
<select id="findMemos">
    SELECT * FROM memos
    ORDER BY
    <choose>
        <when test="sortBy == 'title'">
            title ASC
        </when>
        <when test="sortBy == 'updated'">
            updated_at DESC
        </when>
        <otherwise>
            created_at DESC
        </otherwise>
    </choose>
</select>
```

**動作**:
- `sortBy` が `"title"` → `ORDER BY title ASC`
- `sortBy` が `"updated"` → `ORDER BY updated_at DESC`
- それ以外 → `ORDER BY created_at DESC`

### 5.4 `<foreach>` - IN句の動的生成

**用途**: 配列やリストを使ったIN句

```java
// Mapperインターフェース
List<Memo> findByIds(@Param("ids") List<Long> ids);
```

```xml
<!-- Mapper XML -->
<select id="findByIds">
    SELECT * FROM memos
    WHERE id IN
    <foreach item="id" collection="ids" open="(" separator="," close=")">
        #{id}
    </foreach>
</select>
```

**動作**:
```java
// Java
List<Long> ids = Arrays.asList(1L, 2L, 3L);
memoMapper.findByIds(ids);

// 生成されるSQL
SELECT * FROM memos
WHERE id IN (?, ?, ?)
-- パラメータ: [1, 2, 3]
```

### 5.5 `<set>` - UPDATE文の動的生成

**用途**: 指定されたフィールドのみ更新

```xml
<update id="updateSelective">
    UPDATE memos
    <set>
        <if test="title != null">
            title = #{title},
        </if>
        <if test="content != null">
            content = #{content},
        </if>
        updated_at = CURRENT_TIMESTAMP
    </set>
    WHERE id = #{id}
</update>
```

**動作**:
```java
// titleのみ更新
Memo memo = new Memo();
memo.setId(1L);
memo.setTitle("新しいタイトル");
memoMapper.updateSelective(memo);

// 生成されるSQL
UPDATE memos
SET title = ?, updated_at = CURRENT_TIMESTAMP
WHERE id = ?
```

---

## 6. 実践例集

### 6.1 ページング

```java
// Mapperインターフェース
List<Memo> findWithPaging(
    @Param("offset") int offset,
    @Param("limit") int limit
);
```

```xml
<!-- Mapper XML -->
<select id="findWithPaging">
    SELECT * FROM memos
    ORDER BY created_at DESC
    LIMIT #{limit} OFFSET #{offset}
</select>
```

**使用例**:
```java
// 1ページ目（0〜9件目）
List<Memo> page1 = memoMapper.findWithPaging(0, 10);

// 2ページ目（10〜19件目）
List<Memo> page2 = memoMapper.findWithPaging(10, 10);
```

### 6.2 複数テーブルのJOIN

```sql
-- テーブル構成
CREATE TABLE categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL
);

ALTER TABLE memos ADD COLUMN category_id BIGINT;
ALTER TABLE memos ADD FOREIGN KEY (category_id) REFERENCES categories(id);
```

```java
// Entityクラス
@Data
public class MemoWithCategory {
    private Long id;
    private String title;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long categoryId;
    private String categoryName;  // JOINで取得
}
```

```xml
<!-- Mapper XML -->
<resultMap id="memoWithCategoryMap" type="com.example.memoapp.entity.MemoWithCategory">
    <id property="id" column="memo_id"/>
    <result property="title" column="title"/>
    <result property="content" column="content"/>
    <result property="createdAt" column="created_at"/>
    <result property="updatedAt" column="updated_at"/>
    <result property="categoryId" column="category_id"/>
    <result property="categoryName" column="category_name"/>
</resultMap>

<select id="findAllWithCategory" resultMap="memoWithCategoryMap">
    SELECT
        m.id AS memo_id,
        m.title,
        m.content,
        m.created_at,
        m.updated_at,
        m.category_id,
        c.name AS category_name
    FROM
        memos m
    LEFT JOIN
        categories c ON m.category_id = c.id
    ORDER BY
        m.created_at DESC
</select>
```

### 6.3 集計クエリ

```java
// DTOクラス
@Data
public class MemoCategoryCount {
    private String categoryName;
    private Long count;
}
```

```xml
<!-- Mapper XML -->
<select id="countByCategory" resultType="com.example.memoapp.dto.MemoCategoryCount">
    SELECT
        c.name AS categoryName,
        COUNT(m.id) AS count
    FROM
        categories c
    LEFT JOIN
        memos m ON c.id = m.category_id
    GROUP BY
        c.id, c.name
    ORDER BY
        count DESC
</select>
```

### 6.4 複雑な検索条件

```java
// 検索条件DTO
@Data
public class MemoSearchCriteria {
    private String keyword;
    private Long categoryId;
    private LocalDate startDate;
    private LocalDate endDate;
    private String sortBy;  // "title", "created", "updated"
}
```

```xml
<!-- Mapper XML -->
<select id="search" resultMap="memoResultMap">
    SELECT * FROM memos
    <where>
        <if test="keyword != null and keyword != ''">
            AND (title LIKE CONCAT('%', #{keyword}, '%')
                 OR content LIKE CONCAT('%', #{keyword}, '%'))
        </if>
        <if test="categoryId != null">
            AND category_id = #{categoryId}
        </if>
        <if test="startDate != null">
            AND created_at >= #{startDate}
        </if>
        <if test="endDate != null">
            AND created_at &lt;= #{endDate}
        </if>
    </where>
    ORDER BY
    <choose>
        <when test="sortBy == 'title'">
            title ASC
        </when>
        <when test="sortBy == 'updated'">
            updated_at DESC
        </when>
        <otherwise>
            created_at DESC
        </otherwise>
    </choose>
</select>
```

---

## 7. テスト方法

### 7.1 SQLツールでの直接テスト

**手順**:
1. Mapper XMLからSQLをコピー
2. 動的SQL部分（`<if>`など）を削除または手動で編集
3. パラメータ（`#{xxx}`）を実際の値に置き換え
4. SQLツール（DBeaver、pgAdminなど）で実行

**例**:
```xml
<!-- Mapper XML -->
<select id="searchByKeyword">
    SELECT * FROM memos
    <where>
        <if test="keyword != null and keyword != ''">
            AND (title LIKE CONCAT('%', #{keyword}, '%')
                 OR content LIKE CONCAT('%', #{keyword}, '%'))
        </if>
    </where>
    ORDER BY created_at DESC
</select>
```

↓ SQLツールで実行

```sql
-- keywordありの場合
SELECT * FROM memos
WHERE (title LIKE '%学習%' OR content LIKE '%学習%')
ORDER BY created_at DESC;

-- keywordなしの場合
SELECT * FROM memos
ORDER BY created_at DESC;
```

### 7.2 単体テスト

```java
@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class MemoMapperTest {

    @Autowired
    private MemoMapper memoMapper;

    @Test
    void findById_存在するIDを指定すると_メモが取得できる() {
        // Given: テストデータ準備
        Memo memo = new Memo();
        memo.setTitle("テストメモ");
        memo.setContent("テスト内容");
        memoMapper.insert(memo);

        // When: テスト対象メソッド実行
        Memo found = memoMapper.findById(memo.getId());

        // Then: 検証
        assertNotNull(found);
        assertEquals("テストメモ", found.getTitle());
        assertEquals("テスト内容", found.getContent());
    }

    @Test
    void searchByKeyword_キーワードを含むメモが取得できる() {
        // Given
        Memo memo1 = new Memo();
        memo1.setTitle("Java学習");
        memo1.setContent("Spring Bootを勉強中");
        memoMapper.insert(memo1);

        Memo memo2 = new Memo();
        memo2.setTitle("買い物リスト");
        memo2.setContent("牛乳、パン");
        memoMapper.insert(memo2);

        // When
        List<Memo> results = memoMapper.searchByKeyword("学習");

        // Then
        assertEquals(1, results.size());
        assertEquals("Java学習", results.get(0).getTitle());
    }
}
```

---

## 8. パフォーマンスチューニング

### 8.1 実行計画の確認

```sql
-- PostgreSQL
EXPLAIN ANALYZE
SELECT * FROM memos
WHERE title LIKE '%学習%';

-- 結果例:
-- Seq Scan on memos  (cost=0.00..35.50 rows=1 width=100)
--   Filter: (title ~~ '%学習%'::text)
--   Rows Removed by Filter: 4
-- Planning Time: 0.123 ms
-- Execution Time: 0.456 ms
```

**確認ポイント**:
- `Seq Scan`（全件スキャン） → インデックスが使われていない
- `Index Scan` → インデックスが使われている ✅

### 8.2 N+1問題の回避

**悪い例**:
```xml
<!-- メモ一覧を取得 -->
<select id="findAll">
    SELECT * FROM memos
</select>

<!-- カテゴリを1件ずつ取得（N回実行される！） -->
<select id="findCategoryById">
    SELECT * FROM categories WHERE id = #{id}
</select>
```

```java
// Service層で
List<Memo> memos = memoMapper.findAll();  // 1回
for (Memo memo : memos) {
    Category category = categoryMapper.findById(memo.getCategoryId());  // N回
    memo.setCategory(category);
}
```

**良い例**:
```xml
<!-- 1回のSQLでJOINして取得 -->
<select id="findAllWithCategory">
    SELECT
        m.*,
        c.name AS category_name
    FROM
        memos m
    LEFT JOIN
        categories c ON m.category_id = c.id
</select>
```

### 8.3 インデックスの活用

```sql
-- LIKE検索のインデックス（PostgreSQL）
CREATE INDEX idx_memos_title_trgm ON memos USING gin (title gin_trgm_ops);

-- 複合インデックス
CREATE INDEX idx_memos_category_created ON memos(category_id, created_at DESC);
```

---

## 9. トラブルシューティング

### 9.1 よくあるエラー

#### 9.1.1 `Mapped Statements collection does not contain value for ...`

**原因**: Mapper XMLの `id` とMapperインターフェースのメソッド名が一致していない

**解決方法**:
```java
// Mapperインターフェース
List<Memo> findAllMemos();  // メソッド名
```

```xml
<!-- Mapper XML -->
<select id="findAllMemos">  <!-- id をメソッド名と一致させる -->
    SELECT * FROM memos
</select>
```

#### 9.1.2 `Invalid bound statement (not found)`

**原因**: Mapper XMLが読み込まれていない

**解決方法**:
```yaml
# application.yml
mybatis:
  mapper-locations: classpath:mybatis/mapper/**/*.xml  # パスを確認
```

#### 9.1.3 `There is no getter for property named 'xxx'`

**原因**: パラメータ名が間違っている

**解決方法**:
```java
// @Param でパラメータ名を明示
Memo findById(@Param("id") Long id);
```

```xml
<!-- Mapper XML -->
<select id="findById">
    SELECT * FROM memos WHERE id = #{id}  <!-- @Paramで指定した名前を使う -->
</select>
```

### 9.2 デバッグ方法

#### 9.2.1 実行されるSQLを確認

```yaml
# application.yml
mybatis:
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
```

これでコンソールに実行SQLが出力されます：
```
==>  Preparing: SELECT * FROM memos WHERE id = ?
==> Parameters: 1(Long)
<==      Total: 1
```

#### 9.2.2 ログレベルを上げる

```yaml
logging:
  level:
    com.example.memoapp.mapper: DEBUG  # Mapper のログを詳細表示
```

---

## 10. ベストプラクティス

### 10.1 コーディング規約

#### 10.1.1 SQLの整形

```xml
<!-- ✅ 良い例: 整形されている -->
<select id="findAll">
    SELECT
        id,
        title,
        content,
        created_at,
        updated_at
    FROM
        memos
    ORDER BY
        created_at DESC
</select>

<!-- ❌ 悪い例: 読みにくい -->
<select id="findAll">
    SELECT id,title,content,created_at,updated_at FROM memos ORDER BY created_at DESC
</select>
```

#### 10.1.2 カラム名の明示

```xml
<!-- ✅ 良い例: カラム名を明示 -->
<select id="findAll">
    SELECT
        id,
        title,
        content
    FROM
        memos
</select>

<!-- ❌ 悪い例: * を使う -->
<select id="findAll">
    SELECT * FROM memos
</select>
```

**理由**:
- 必要なカラムだけ取得することでパフォーマンス向上
- テーブル構造が変わっても影響を受けにくい

#### 10.1.3 コメントの活用

```xml
<!--
    メモ検索
    動的条件:
    - keyword: タイトルまたは内容に部分一致
    - categoryId: カテゴリーで絞り込み
    - startDate, endDate: 作成日の範囲指定
-->
<select id="search">
    SELECT * FROM memos
    <where>
        <!-- キーワード検索 -->
        <if test="keyword != null and keyword != ''">
            AND (title LIKE CONCAT('%', #{keyword}, '%')
                 OR content LIKE CONCAT('%', #{keyword}, '%'))
        </if>
        <!-- カテゴリー絞り込み -->
        <if test="categoryId != null">
            AND category_id = #{categoryId}
        </if>
    </where>
</select>
```

### 10.2 セキュリティ

#### 10.2.1 常に `#{}` を使う

```xml
<!-- ✅ 安全 -->
<select id="findById">
    SELECT * FROM memos WHERE id = #{id}
</select>

<!-- ❌ 危険: SQLインジェクションの恐れ -->
<select id="findById">
    SELECT * FROM memos WHERE id = ${id}
</select>
```

#### 10.2.2 `${}` を使う場合の注意

どうしても `${}` を使う必要がある場合（テーブル名の動的変更など）:
```java
// アプリケーション側でホワイトリストチェック
String tableName = request.getParameter("table");
if (!Arrays.asList("memos", "categories").contains(tableName)) {
    throw new IllegalArgumentException("Invalid table name");
}
```

### 10.3 保守性

#### 10.3.1 SQL の再利用

```xml
<!-- 共通部分を <sql> タグで定義 -->
<sql id="memoColumns">
    id,
    title,
    content,
    created_at,
    updated_at
</sql>

<!-- <include> で再利用 -->
<select id="findAll">
    SELECT
        <include refid="memoColumns"/>
    FROM
        memos
</select>

<select id="findById">
    SELECT
        <include refid="memoColumns"/>
    FROM
        memos
    WHERE
        id = #{id}
</select>
```

---

## 付録

### A. MyBatis設定チートシート

```yaml
mybatis:
  # Mapper XMLの場所
  mapper-locations: classpath:mybatis/mapper/**/*.xml

  # 型エイリアスのパッケージ
  type-aliases-package: com.example.memoapp.entity

  configuration:
    # スネークケース → キャメルケース自動変換
    map-underscore-to-camel-case: true

    # SQL実行ログ
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl

    # デフォルトのFetchサイズ
    default-fetch-size: 100

    # デフォルトのタイムアウト（秒）
    default-statement-timeout: 30
```

### B. 参考リンク

- [MyBatis 公式ドキュメント](https://mybatis.org/mybatis-3/)
- [MyBatis-Spring-Boot-Starter](https://mybatis.org/spring-boot-starter/mybatis-spring-boot-autoconfigure/)
- [PostgreSQL EXPLAIN解説](https://www.postgresql.org/docs/current/using-explain.html)

---

**MyBatisと2waySQLをマスターして、実務で通用する開発者になりましょう！** 🚀
