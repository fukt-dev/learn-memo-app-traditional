-- V1: memos テーブルを作成する最初のマイグレーション。
--
-- Flyway が起動時に自動実行し、適用履歴を flyway_schema_history に記録する。
-- 命名規則（V{番号}__{説明}.sql）・適用順と checksum・「適用済みは書き換えない」鉄則・
-- DDL/制約/インデックスの一般知識は docs/解説/Flywayとマイグレーション.md にまとめてある。
-- このファイルには V1 固有の設計意図（GIN インデックスの注意・トリガーによる自動更新）を残す。

CREATE TABLE memos (
    -- 主キー。BIGSERIAL = 自動採番の 64 ビット整数。SERIAL（32 ビット/最大約 21 億）より広く、
    -- 将来件数が増えても枯渇しにくい方を選んでいる。
    id BIGSERIAL PRIMARY KEY,

    -- タイトル。最大 200 文字（DTO 側の @Size(max=200) と一致させている）。NOT NULL で必須。
    title VARCHAR(200) NOT NULL,

    -- 本文。TEXT は長さ実質無制限。メモ本文は長くなりうるので VARCHAR で縛らない。NOT NULL で必須。
    content TEXT NOT NULL,

    -- 作成日時。DEFAULT CURRENT_TIMESTAMP により INSERT 時に自動で現在時刻が入る（アプリは値を送らない）。
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- 更新日時。作成時は created_at と同時刻。以降は下のトリガーで UPDATE のたびに自動更新される。
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- インデックス1: 一覧の "ORDER BY created_at DESC"（新しい順）を速くするための降順 B-tree インデックス。
CREATE INDEX idx_memos_created_at ON memos (created_at DESC);

-- インデックス2: 全文検索用の GIN インデックス（to_tsvector で単語の転置索引を作る）。
--
-- 【重要: このインデックスが効くのは全文検索演算子（@@）だけ】
-- 使えるのは次のような全文検索クエリのみ:
--   WHERE to_tsvector('simple', title || ' ' || content) @@ plainto_tsquery('simple', 'キーワード')
-- 一方、このアプリの検索（MemoMapper.xml の searchByKeyword）は LIKE '%キーワード%' の部分一致で、
-- LIKE は tsvector を経由しないため、このインデックスは全く使われない
-- （＝「作ってあるが、どのクエリからも使われない」状態）。
--
-- 【LIKE '%...%' を速くするには】pg_trgm 拡張（文字列を 3 文字ずつの断片に分けて索引化）を使う:
--   CREATE EXTENSION IF NOT EXISTS pg_trgm;
--   CREATE INDEX idx_memos_title_trgm   ON memos USING GIN (title   gin_trgm_ops);
--   CREATE INDEX idx_memos_content_trgm ON memos USING GIN (content gin_trgm_ops);
--
-- 【実装演習】新しい V2__Add_trgm_index.sql に上記を書き、EXPLAIN ANALYZE で
--   LIKE 検索の実行計画が Seq Scan から Bitmap Index Scan に変わるか観察してみよう
--  （数十件程度だと PostgreSQL があえて全件走査を選ぶこともある。その観察も含めて良い演習）。
--
-- 【残している理由】全文検索（@@）へのステップアップ教材として残している。なお日本語の
--   本格的な形態素解析には pg_bigm / pgroonga などの拡張が必要（postgres:16-alpine には含まれない）。
CREATE INDEX idx_memos_fulltext ON memos USING GIN (to_tsvector('simple', title || ' ' || content));

-- updated_at を手で更新するのは忘れやすいため、UPDATE 直前に現在時刻をセットするトリガーを使う。
-- NEW は「更新後の行」を指す特殊変数。その updated_at を書き換えて返すことで自動更新を実現する。
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_memos_updated_at
    BEFORE UPDATE ON memos
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- テーブル・カラムのコメント。pgAdmin などの GUI にも表示され、設計意図の記録になる。
COMMENT ON TABLE memos IS 'メモ情報を管理するテーブル';
COMMENT ON COLUMN memos.id IS 'メモID（主キー、自動採番）';
COMMENT ON COLUMN memos.title IS 'メモのタイトル（最大200文字）';
COMMENT ON COLUMN memos.content IS 'メモの本文（制限なし）';
COMMENT ON COLUMN memos.created_at IS '作成日時（自動設定）';
COMMENT ON COLUMN memos.updated_at IS '更新日時（更新時に自動更新）';
