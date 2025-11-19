-- ============================================
-- Flywayマイグレーションファイル: V1
-- ============================================
--
-- 【このファイルの目的】
-- メモ帳アプリケーションのメインテーブル「memos」を作成します
--
-- 【Flywayのファイル命名規則】
-- V{バージョン番号}__{説明}.sql
-- 例: V1__Create_memos_table.sql
--
-- - V1 = バージョン1（数字は連番で増やす）
-- - __ = アンダースコア2つで区切る（重要！）
-- - Create_memos_table = 説明（何をするか）
--
-- 【実行タイミング】
-- アプリケーション起動時、Flywayが自動的に実行します
-- 一度実行されたマイグレーションは、再度実行されません
-- （flyway_schema_history テーブルに実行履歴が記録される）
--
-- ============================================

-- --------------------------------------------
-- memosテーブルの作成
-- --------------------------------------------
--
-- 【テーブル設計のポイント】
-- 1. 主キー（id）は自動採番のBIGSERIAL型
-- 2. タイトルと本文は必須（NOT NULL）
-- 3. 作成日時・更新日時は自動で設定される
-- 4. 全文検索用のインデックスを作成
--
CREATE TABLE memos (
    -- ----------------------------------------
    -- id: 主キー（メモを一意に識別するID）
    -- ----------------------------------------
    -- BIGSERIAL型 = 自動で連番を振ってくれる64ビット整数
    -- （1, 2, 3, ... と自動的に増えていく）
    --
    -- 【なぜBIGSERIALか】
    -- - SERIAL: 最大21億件（2^31 - 1）
    -- - BIGSERIAL: 最大9京件（2^63 - 1）
    -- 将来的にデータが増えても安心なBIGSERIALを使用
    --
    -- PRIMARY KEY = このカラムが主キー（一意で、NULL不可）
    id BIGSERIAL PRIMARY KEY,

    -- ----------------------------------------
    -- title: メモのタイトル
    -- ----------------------------------------
    -- VARCHAR(200) = 可変長文字列、最大200文字
    --
    -- 【文字数の根拠】
    -- - 日本語なら100文字程度（全角1文字=1文字としてカウント）
    -- - 英語なら200文字程度
    -- - Twitterの旧制限（140文字）より少し長め
    --
    -- NOT NULL = 必須入力（空欄は許可しない）
    title VARCHAR(200) NOT NULL,

    -- ----------------------------------------
    -- content: メモの本文
    -- ----------------------------------------
    -- TEXT型 = 文字数制限がほぼない文字列
    -- （最大1GBまで保存可能）
    --
    -- 【なぜTEXT型か】
    -- - メモの本文は長くなる可能性がある
    -- - VARCHARで制限するより、自由に書けるようにする
    --
    -- NOT NULL = 必須入力
    content TEXT NOT NULL,

    -- ----------------------------------------
    -- created_at: 作成日時
    -- ----------------------------------------
    -- TIMESTAMP型 = 日付と時刻を保存する型
    -- 例: 2025-11-17 10:30:45.123456
    --
    -- DEFAULT CURRENT_TIMESTAMP = デフォルト値として現在時刻を設定
    -- つまり、INSERT時に created_at を指定しなくても、
    -- 自動的に現在時刻が入る
    --
    -- NOT NULL = 必ず値が入る（DEFAULTがあるので自動的に入る）
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- ----------------------------------------
    -- updated_at: 更新日時
    -- ----------------------------------------
    -- 最後に更新された日時を記録
    -- トリガー（後述）により、UPDATE時に自動更新される
    --
    -- DEFAULT CURRENT_TIMESTAMP = 作成時は created_at と同じ時刻
    -- NOT NULL = 必ず値が入る
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- インデックスの作成
-- ============================================
--
-- 【インデックスとは】
-- データベースの「索引」のようなもの
-- 本の索引があると目的のページをすぐに見つけられるように、
-- インデックスがあるとデータベースの検索が速くなる
--
-- 【どういう時に作るか】
-- - WHERE句で頻繁に検索するカラム
-- - ORDER BY句でソートするカラム
-- - JOIN条件に使うカラム
--

-- --------------------------------------------
-- インデックス1: 作成日時の降順インデックス
-- --------------------------------------------
--
-- idx_memos_created_at = インデックスの名前
-- idx_ = index の略（命名規則）
--
-- ON memos (created_at DESC)
-- = memosテーブルのcreated_atカラムに、降順でインデックスを作成
--
-- 【なぜ降順か】
-- メモ一覧では「新しい順」に表示することが多いため
-- 例: SELECT * FROM memos ORDER BY created_at DESC
-- このクエリが高速になる
CREATE INDEX idx_memos_created_at ON memos (created_at DESC);

-- --------------------------------------------
-- インデックス2: 全文検索用インデックス（GIN）
-- --------------------------------------------
--
-- 【GINインデックスとは】
-- Generalized Inverted Index の略
-- 全文検索に特化したインデックス
--
-- 【to_tsvectorとは】
-- テキストを「検索用の形式」に変換する関数
-- - 'japanese' = 日本語の形態素解析を行う
-- - title || ' ' || content = タイトルと本文を結合
--
-- 【このインデックスでできること】
-- - タイトルや本文に含まれるキーワードを高速検索
-- - 部分一致検索が速くなる
--
-- 【使用例】
-- WHERE to_tsvector('simple', title || ' ' || content) @@ plainto_tsquery('simple', 'キーワード')
-- このクエリが高速に実行できる
--
-- 【text search configurationについて】
-- 'simple': 基本的なトークン化（全PostgreSQLに標準搭載）
--   - 単語の分割、大文字小文字の正規化などの基本処理
--   - 日本語も基本的な検索が可能
--
-- 'japanese': 日本語専用設定（別途拡張が必要）
--   - postgres:16-alpineには含まれていない
--   - 本格的な日本語形態素解析が必要な場合は pg_bigm や pgroonga を検討
--
-- このアプリでは学習用としてLIKE検索を使用するため、simple設定で十分です
--
-- 【注意】
-- PostgreSQLの全文検索は英語に比べて日本語は精度が低い場合があります
-- より高度な日本語検索が必要な場合は、pg_bigmやElasticsearchの利用を検討してください
CREATE INDEX idx_memos_fulltext ON memos USING GIN (to_tsvector('simple', title || ' ' || content));

-- ============================================
-- トリガー関数の作成
-- ============================================
--
-- 【トリガーとは】
-- 特定のイベント（INSERT、UPDATE、DELETEなど）が発生したときに
-- 自動的に実行される処理
--
-- 【なぜトリガーを使うか】
-- updated_at を手動で更新するのは忘れやすい
-- トリガーを使えば、UPDATE時に自動的に現在時刻が設定される
--

-- --------------------------------------------
-- トリガー関数: update_updated_at_column
-- --------------------------------------------
--
-- この関数は、UPDATEが実行されたときに呼び出される
--
-- CREATE OR REPLACE FUNCTION = 関数を作成（既にあれば上書き）
-- RETURNS TRIGGER = この関数はトリガーで使われることを示す
--
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    -- NEW.updated_at に現在時刻を設定
    --
    -- 【NEWとは】
    -- トリガーで使える特殊な変数
    -- NEW = UPDATE後の新しい行のデータ
    -- OLD = UPDATE前の古い行のデータ
    --
    -- CURRENT_TIMESTAMP = 現在時刻
    --
    NEW.updated_at = CURRENT_TIMESTAMP;

    -- 更新後の行（NEW）を返す
    -- これにより、updated_at が更新された状態でデータベースに保存される
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 【$$とは】
-- PostgreSQLの「ドルクォート」記法
-- 関数本体を囲むための記号
-- シングルクォート（'）のエスケープが不要になるので便利
--
-- 【plpgsqlとは】
-- PL/pgSQL = PostgreSQLで使える手続き型言語
-- IF文やループ、変数などが使える

-- --------------------------------------------
-- トリガーの作成: trigger_update_memos_updated_at
-- --------------------------------------------
--
-- CREATE TRIGGER = トリガーを作成
-- trigger_update_memos_updated_at = トリガーの名前
--
-- BEFORE UPDATE = UPDATEの直前に実行
-- ON memos = memosテーブルに対して
-- FOR EACH ROW = 更新される各行ごとに実行
--
-- EXECUTE FUNCTION update_updated_at_column()
-- = 上で作成した関数を実行
--
CREATE TRIGGER trigger_update_memos_updated_at
    BEFORE UPDATE ON memos
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ============================================
-- テーブルとカラムのコメント
-- ============================================
--
-- 【コメントの重要性】
-- SQLだけではテーブルやカラムの目的がわかりにくい
-- COMMENTを追加することで、将来的にメンテナンスしやすくなる
--
-- pgAdminなどのGUIツールでもこのコメントが表示される
--

-- テーブル全体のコメント
COMMENT ON TABLE memos IS 'メモ情報を管理するテーブル';

-- 各カラムのコメント
COMMENT ON COLUMN memos.id IS 'メモID（主キー、自動採番）';
COMMENT ON COLUMN memos.title IS 'メモのタイトル（最大200文字）';
COMMENT ON COLUMN memos.content IS 'メモの本文（制限なし）';
COMMENT ON COLUMN memos.created_at IS '作成日時（自動設定）';
COMMENT ON COLUMN memos.updated_at IS '更新日時（更新時に自動更新）';

-- ============================================
-- マイグレーション完了
-- ============================================
--
-- このファイルが実行されると、以下が作成されます：
-- 1. memosテーブル（5つのカラム）
-- 2. 2つのインデックス（created_at、全文検索用）
-- 3. トリガー関数（updated_at自動更新用）
-- 4. トリガー（UPDATE時に関数を呼び出す）
--
-- 【確認方法】
-- アプリケーション起動後、以下のSQLで確認できます：
--
-- -- テーブル一覧を表示
-- \dt
--
-- -- memosテーブルの構造を表示
-- \d memos
--
-- -- インデックス一覧を表示
-- \di
--
-- -- コメントも含めて詳細表示
-- SELECT
--     cols.column_name,
--     cols.data_type,
--     cols.is_nullable,
--     pgd.description
-- FROM pg_catalog.pg_statio_all_tables AS st
-- INNER JOIN pg_catalog.pg_description pgd ON (pgd.objoid = st.relid)
-- INNER JOIN information_schema.columns cols ON (
--     pgd.objsubid = cols.ordinal_position AND
--     cols.table_schema = st.schemaname AND
--     cols.table_name = st.relname
-- )
-- WHERE st.relname = 'memos';
--
