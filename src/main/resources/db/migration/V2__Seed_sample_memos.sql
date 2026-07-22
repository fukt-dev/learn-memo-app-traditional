-- V2: 動作確認・画面キャプチャ用のサンプルメモを投入するマイグレーション。
--
-- 【なぜスキーマ(V1)とシード(V2)を分けるか】
-- Flyway のバージョンは「変更の単位」を表す。テーブル定義とサンプルデータは変更理由が別物
-- （前者は仕様変更、後者は運用上のデータ投入）なので別バージョンに分け、どちらか一方だけを
-- 見返したい・別環境に個別適用したい場合に対応しやすくしている。V1 のスキーマを変更せず、
-- 追加のみで完結する変更なので新しいバージョン番号（V2）を採番する
-- （「適用済みマイグレーションは書き換えない」という Flyway の鉄則。詳細は
-- docs/解説/Flywayとマイグレーション.md）。
--
-- 【なぜシードを入れるか】
-- このアプリは初回起動時点で memos テーブルが空で、一覧・詳細・編集の画面を確認するには
-- 手動でメモを作る必要があった。README のスクリーンショットや動作確認のたびに手動投入するのは
-- 再現性がなく、環境によって表示内容がぶれる。Flyway 経由で固定データを入れておけば、
-- どの環境で `docker compose up --build` してもメモ一覧が同じ内容になる。
--
-- 【内容の設計意図】
-- - キーワード検索（MemoMapper.xml の searchByKeyword、LIKE 部分一致）を試せるように、
--   一部のメモ（1件目・3件目・4件目）にだけ共通の語「Spring」を含めている。
--   検索欄に「Spring」と入力すると3件だけ絞り込まれる、という体験ができる。
-- - created_at をメモごとに数日ずつずらしている。一覧は
--   `ORDER BY created_at DESC`（V1 コメント・MemoMapper.xml 参照）で新しい順に並ぶため、
--   INSERT の記述順と画面表示順があえて逆になる（一番下に書いた「今週のTODO」が一覧の先頭に出る）。
--   これも「一覧が created_at で並び替えられている」ことを画面から確認できる材料になる。
-- - updated_at は created_at と同値にしている。V1 のトリガーは UPDATE 時にしか働かないため、
--   INSERT で入れた値がそのまま「未編集」の状態を表す（編集すれば updated_at だけ現在時刻に動く）。
-- - CURRENT_TIMESTAMP からの相対指定（INTERVAL 減算）にしているのは、絶対日付を書くと
--   マイグレーションを適用するたびに「過去の日付で作成されたメモ」という不自然な表示になるため。
--   相対指定なら、いつ `docker compose up --build` してもキャプチャ日を起点にした自然な日付になる。

INSERT INTO memos (title, content, created_at, updated_at) VALUES
    (
        'メモ帳アプリの使い方',
        'このメモ帳アプリはJava 17とSpring Boot 3.5、MyBatis、Thymeleafで作られた学習用のWebアプリです。' ||
        E'\n\n' ||
        '画面右上の「新規作成」からメモを追加できます。一覧のタイトルをクリックすると詳細画面に移動し、' ||
        '詳細画面から編集・削除ができます。一覧上部の検索欄はタイトルと本文の両方を対象に部分一致で絞り込みます。',
        CURRENT_TIMESTAMP - INTERVAL '5 days',
        CURRENT_TIMESTAMP - INTERVAL '5 days'
    ),
    (
        '買い物リスト',
        '牛乳' || E'\n' || '卵' || E'\n' || 'コーヒー豆' || E'\n' || '食器用洗剤' || E'\n' || 'キッチンペーパー',
        CURRENT_TIMESTAMP - INTERVAL '4 days',
        CURRENT_TIMESTAMP - INTERVAL '4 days'
    ),
    (
        '定例会議メモ(7/18)',
        '次期リリースの検討会。議題は次のとおり。' ||
        E'\n\n' ||
        '1. Spring Bootのバージョンアップ対応の進捗確認（3.2系から3.5系への移行は完了済み）' ||
        E'\n' ||
        '2. DBマイグレーション方針の相談（Flywayのバージョン採番ルールを再確認）' ||
        E'\n' ||
        '3. 次回スプリントのタスク割り振り',
        CURRENT_TIMESTAMP - INTERVAL '3 days',
        CURRENT_TIMESTAMP - INTERVAL '3 days'
    ),
    (
        '読書メモ:速習 Spring Boot',
        '第3章まで読了。DIコンテナが依存関係の解決を肩代わりしてくれる仕組みが腰を据えて理解できた。' ||
        E'\n\n' ||
        '次はAOPとトランザクション管理（@Transactional）の章。このアプリのMemoServiceにも' ||
        '@Transactionalが使われているので、実装と読み合わせながら読み進める。',
        CURRENT_TIMESTAMP - INTERVAL '2 days',
        CURRENT_TIMESTAMP - INTERVAL '2 days'
    ),
    (
        '今週のTODO',
        '・レビュー依頼への対応' || E'\n' ||
        '・買い物リストの消化' || E'\n' ||
        '・技術ブログの下書き執筆' || E'\n' ||
        '・ジムに行く',
        CURRENT_TIMESTAMP - INTERVAL '1 day',
        CURRENT_TIMESTAMP - INTERVAL '1 day'
    );
