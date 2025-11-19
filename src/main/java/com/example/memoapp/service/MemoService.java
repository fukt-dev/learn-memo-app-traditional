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
 * ============================================
 * MemoServiceクラス（ビジネスロジック層）
 * ============================================
 *
 * 【このクラスの役割】
 * メモ帳アプリケーションのビジネスロジック（業務ロジック）を実装します
 *
 * 【ビジネスロジック層とは】
 * アプリケーションの「頭脳」に相当する層
 * - データの加工・変換
 * - 業務ルールの実装
 * - トランザクション管理
 * - エラーハンドリング
 *
 * 【3層アーキテクチャにおける位置付け】
 *
 * ┌─────────────────────────┐
 * │ プレゼンテーション層    │ ← Controller（画面からの入力を受け取る）
 * └─────────────────────────┘
 *           ↓↑
 * ┌─────────────────────────┐
 * │ ビジネスロジック層      │ ← Service（このクラス）
 * └─────────────────────────┘
 *           ↓↑
 * ┌─────────────────────────┐
 * │ データアクセス層        │ ← Mapper（データベースへのアクセス）
 * └─────────────────────────┘
 *
 * 【Serviceの責務】
 *
 * ○ やるべきこと:
 * - DTOとEntityの変換
 * - 複数のMapperメソッドを組み合わせた処理
 * - トランザクション境界の定義
 * - ビジネスルールのチェック
 * - ログ出力
 *
 * × やってはいけないこと:
 * - HTTPリクエスト/レスポンスの処理（Controllerの責務）
 * - SQLを直接書く（Mapperの責務）
 * - 画面表示の制御（Controllerの責務）
 */

/**
 * @Service
 *
 * このクラスがサービス層のコンポーネントであることを示す
 * Spring Bootが自動的にBeanとして登録し、DIコンテナで管理する
 *
 * これにより、以下のようにControllerから注入できる:
 * @Autowired
 * private MemoService memoService;
 */
@Service

/**
 * @Slf4j
 *
 * Lombokのアノテーション
 * ログ出力用の log フィールドを自動生成する
 *
 * 自動生成されるコード:
 * private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MemoService.class);
 *
 * 使用例:
 * log.debug("メモを検索します: {}", keyword);
 * log.info("メモを登録しました: id={}", memo.getId());
 * log.error("エラーが発生しました", exception);
 */
@Slf4j

/**
 * @RequiredArgsConstructor
 *
 * Lombokのアノテーション
 * final フィールドを引数に取るコンストラクタを自動生成する
 *
 * 自動生成されるコード:
 * public MemoService(MemoMapper memoMapper) {
 *     this.memoMapper = memoMapper;
 * }
 *
 * これにより、以下のような冗長なコードを書かなくて済む:
 * @Autowired
 * private MemoMapper memoMapper;
 *
 * 【コンストラクタインジェクションのメリット】
 * 1. フィールドがfinalにできる（不変性）
 * 2. テストしやすい（モックを渡しやすい）
 * 3. 循環参照を防げる
 */
@RequiredArgsConstructor
public class MemoService {

    /*
     * ============================================
     * 依存関係（フィールド）
     * ============================================
     */

    /**
     * MemoMapper（データアクセス層）
     *
     * final キーワード:
     * - 一度セットされたら変更できない（イミュータブル）
     * - @RequiredArgsConstructor によりコンストラクタで注入される
     */
    private final MemoMapper memoMapper;

    /*
     * ============================================
     * ビジネスロジックメソッド
     * ============================================
     */

    /**
     * 全件取得
     *
     * 【処理の流れ】
     * 1. Mapperから全件取得（Entity のリスト）
     * 2. Entity を DTO に変換
     * 3. DTOのリストを返す
     *
     * 【なぜEntityをDTOに変換するのか】
     * - Entityはデータベースの構造に依存する
     * - DTOは画面表示に適した形式に整形する
     * - 日付のフォーマットなど、表示用の加工を行う
     *
     * @return メモのDTOリスト
     */
    public List<MemoDto> findAll() {
        /*
         * ログ出力（DEBUG level）
         * 開発時のデバッグに使用
         * 本番環境では出力されない（application.ymlで制御）
         */
        log.debug("メモの全件取得を開始します");

        /*
         * Mapperから全件取得
         * List<Memo> = データベースのレコードをそのまま表現
         */
        List<Memo> memos = memoMapper.findAll();

        /*
         * ログ出力
         * {}はプレースホルダー（memos.size()の値が入る）
         */
        log.debug("{}件のメモを取得しました", memos.size());

        /*
         * EntityのリストをDTOのリストに変換
         *
         * 【Stream APIの説明】
         * memos.stream()          : ListをStreamに変換
         * .map(MemoDto::fromEntity) : 各要素をEntityからDTOに変換
         * .collect(Collectors.toList()) : StreamをListに戻す
         *
         * 【従来の書き方（for文）】
         * List<MemoDto> dtos = new ArrayList<>();
         * for (Memo memo : memos) {
         *     MemoDto dto = MemoDto.fromEntity(memo);
         *     dtos.add(dto);
         * }
         * return dtos;
         *
         * Stream APIを使うと、1行で書ける！
         */
        return memos.stream()
                .map(MemoDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * ID指定で1件取得
     *
     * 【処理の流れ】
     * 1. Mapperから指定IDのメモを取得
     * 2. 存在しない場合は例外をスロー
     * 3. EntityをDTOに変換して返す
     *
     * @param id メモID
     * @return メモのDTO
     * @throws MemoNotFoundException メモが見つからない場合
     */
    public MemoDto findById(Long id) {
        log.debug("メモを取得します: id={}", id);

        /*
         * Mapperから取得
         * Optional<Memo> = 値が存在するかもしれないし、しないかもしれない
         */
        return memoMapper.findById(id)
                /*
                 * .map(MemoDto::fromEntity)
                 * Optional<Memo> → Optional<MemoDto> に変換
                 * Optionalの中身（Memo）をDTOに変換する
                 */
                .map(MemoDto::fromEntity)
                /*
                 * .orElseThrow()
                 * Optionalの中身がある場合: その値を返す
                 * Optionalの中身がない場合: 例外をスロー
                 *
                 * ラムダ式の説明:
                 * () -> new MemoNotFoundException("メモが見つかりません: id=" + id)
                 *
                 * これは以下の省略形:
                 * () -> {
                 *     return new MemoNotFoundException("メモが見つかりません: id=" + id);
                 * }
                 *
                 * 【MemoNotFoundExceptionについて】
                 * - @ResponseStatus(HttpStatus.NOT_FOUND) により404を返す
                 * - GlobalExceptionHandlerで専用ハンドリングが可能
                 * - IllegalArgumentException（400）より適切なHTTPステータス
                 */
                .orElseThrow(() -> new MemoNotFoundException("メモが見つかりません: id=" + id));

        /*
         * 【Optionalを使わない場合の書き方】
         * Memo memo = memoMapper.findById(id);
         * if (memo == null) {
         *     throw new IllegalArgumentException("メモが見つかりません: id=" + id);
         * }
         * return MemoDto.fromEntity(memo);
         *
         * Optionalを使うと、null チェックが不要で、安全にコードが書ける
         */
    }

    /**
     * 新規登録
     *
     * 【処理の流れ】
     * 1. DTOをEntityに変換
     * 2. Mapperでデータベースに登録
     * 3. ログ出力
     *
     * @param dto 登録するメモのDTO
     */
    @Transactional
    /*
     * @Transactional
     *
     * このメソッドをトランザクション境界とする
     *
     * 【トランザクションとは】
     * 一連の処理をまとめて、「全て成功」か「全て失敗」にする仕組み
     *
     * 例:
     * 1. メモを登録
     * 2. 関連データを登録
     * 3. 履歴を記録
     *
     * この3つがすべて成功した場合のみコミット（確定）
     * 1つでも失敗したら、全てロールバック（取り消し）
     *
     * 【動作】
     * - メソッド開始時: トランザクション開始
     * - メソッド正常終了時: コミット（データベースに反映）
     * - 例外発生時: ロールバック（処理を取り消す）
     *
     * 【なぜ必要か】
     * データの整合性を保つため
     * 例: お金の振込処理で、「引き落とし成功、振込失敗」となると困る
     *    トランザクションにより、両方成功か両方失敗になる
     *
     * このアプリでは、INSERT/UPDATE/DELETEメソッドに付ける
     */
    public void create(MemoDto dto) {
        log.debug("メモを登録します: title={}", dto.getTitle());

        /*
         * DTOをEntityに変換
         * toEntity() メソッドは MemoDto で定義している
         */
        Memo memo = dto.toEntity();

        /*
         * データベースに登録
         * insert() 実行後、memo.getId() で採番されたIDが取得できる
         * （MemoMapper.xml の useGeneratedKeys="true" により）
         */
        memoMapper.insert(memo);

        /*
         * ログ出力（INFO level）
         * 本番環境でも出力される重要な情報
         * ビジネス上の重要なイベント（登録、更新、削除など）を記録
         */
        log.info("メモを登録しました: id={}, title={}", memo.getId(), memo.getTitle());
    }

    /**
     * 更新
     *
     * 【処理の流れ】
     * 1. 指定IDのメモが存在するか確認
     * 2. DTOをEntityに変換
     * 3. Mapperで更新
     * 4. 更新件数をチェック
     * 5. ログ出力
     *
     * @param dto 更新するメモのDTO
     * @throws MemoNotFoundException メモが見つからない場合
     */
    @Transactional
    public void update(MemoDto dto) {
        log.debug("メモを更新します: id={}, title={}", dto.getId(), dto.getTitle());

        /*
         * 【事前チェック】
         * 更新前に、対象のメモが存在するか確認する
         *
         * なぜ必要か:
         * - 存在しないIDに対する更新を防ぐ
         * - わかりやすいエラーメッセージを返す
         *
         * findById() で見つからない場合は MemoNotFoundException がスローされる
         */
        findById(dto.getId());  // 存在チェック（見つからなければ例外）

        /*
         * DTOをEntityに変換
         */
        Memo memo = dto.toEntity();

        /*
         * データベースを更新
         * 戻り値: 更新された行数（0 or 1）
         */
        int updatedCount = memoMapper.update(memo);

        /*
         * 更新件数のチェック
         * 通常は findById() で存在チェック済みなので、
         * ここでのチェックは念のため（ダブルチェック）
         *
         * 可能性のあるケース:
         * - 別のユーザーが同時に削除した
         * - 楽観的ロック（updated_at チェック）で更新できなかった
         */
        if (updatedCount == 0) {
            log.warn("更新対象のメモが見つかりませんでした: id={}", dto.getId());
            throw new MemoNotFoundException("メモが見つかりません: id=" + dto.getId());
        }

        log.info("メモを更新しました: id={}, title={}", memo.getId(), memo.getTitle());
    }

    /**
     * 削除
     *
     * 【処理の流れ】
     * 1. 指定IDのメモが存在するか確認
     * 2. Mapperで削除
     * 3. 削除件数をチェック
     * 4. ログ出力
     *
     * @param id 削除するメモのID
     * @throws MemoNotFoundException メモが見つからない場合
     */
    @Transactional
    public void delete(Long id) {
        log.debug("メモを削除します: id={}", id);

        /*
         * 存在チェック
         */
        MemoDto dto = findById(id);

        /*
         * データベースから削除
         */
        int deletedCount = memoMapper.deleteById(id);

        /*
         * 削除件数のチェック
         */
        if (deletedCount == 0) {
            log.warn("削除対象のメモが見つかりませんでした: id={}", id);
            throw new MemoNotFoundException("メモが見つかりません: id=" + id);
        }

        log.info("メモを削除しました: id={}, title={}", id, dto.getTitle());
    }

    /**
     * キーワード検索
     *
     * 【処理の流れ】
     * 1. Mapperでキーワード検索
     * 2. EntityのリストをDTOのリストに変換
     * 3. 返す
     *
     * @param keyword 検索キーワード
     * @return 検索結果のDTOリスト
     */
    public List<MemoDto> search(String keyword) {
        log.debug("キーワードでメモを検索します: keyword={}", keyword);

        /*
         * nullチェック
         * キーワードがnullまたは空文字列の場合は、全件取得と同じ
         */
        if (keyword == null || keyword.trim().isEmpty()) {
            log.debug("キーワードが空のため、全件取得します");
            return findAll();
        }

        /*
         * キーワード検索
         */
        List<Memo> memos = memoMapper.searchByKeyword(keyword);

        log.debug("検索結果: {}件", memos.size());

        /*
         * EntityのリストをDTOのリストに変換
         */
        return memos.stream()
                .map(MemoDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * ページング付き全件取得
     *
     * 【処理の流れ】
     * 1. ページ番号とページサイズからoffsetを計算
     * 2. Mapperでページング取得
     * 3. EntityのリストをDTOのリストに変換
     *
     * @param pageNumber ページ番号（1始まり）
     * @param pageSize 1ページあたりの件数
     * @return 指定ページのDTOリスト
     */
    public List<MemoDto> findAllWithPaging(int pageNumber, int pageSize) {
        log.debug("ページング付きでメモを取得します: page={}, size={}", pageNumber, pageSize);

        /*
         * 入力値の検証
         */
        if (pageNumber < 1) {
            throw new IllegalArgumentException("ページ番号は1以上である必要があります: " + pageNumber);
        }
        if (pageSize < 1 || pageSize > 100) {
            throw new IllegalArgumentException("ページサイズは1～100の範囲である必要があります: " + pageSize);
        }

        /*
         * offsetの計算
         * offset = (ページ番号 - 1) × ページサイズ
         *
         * 例: 1ページ10件の場合
         * - 1ページ目: offset = (1 - 1) × 10 = 0
         * - 2ページ目: offset = (2 - 1) × 10 = 10
         */
        int offset = (pageNumber - 1) * pageSize;

        /*
         * ページング取得
         */
        List<Memo> memos = memoMapper.findAllWithPaging(pageSize, offset);

        log.debug("{}件のメモを取得しました", memos.size());

        /*
         * EntityのリストをDTOのリストに変換
         */
        return memos.stream()
                .map(MemoDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 総件数を取得
     *
     * ページングの総ページ数を計算するために使用
     *
     * @return メモの総件数
     */
    public long count() {
        long count = memoMapper.count();
        log.debug("メモの総件数: {}", count);
        return count;
    }

    /**
     * 総ページ数を計算
     *
     * 【計算式】
     * 総ページ数 = ceil(総件数 / ページサイズ)
     *
     * 例:
     * - 総件数25、ページサイズ10 → 3ページ
     * - 総件数30、ページサイズ10 → 3ページ
     * - 総件数31、ページサイズ10 → 4ページ
     *
     * @param pageSize 1ページあたりの件数
     * @return 総ページ数
     */
    public int getTotalPages(int pageSize) {
        long totalCount = count();
        /*
         * Math.ceil() : 切り上げ
         * (double) totalCount / pageSize : 小数の割り算
         * (int) : 整数にキャスト
         */
        return (int) Math.ceil((double) totalCount / pageSize);
    }
}

/*
 * ============================================
 * このクラスの使われ方（Controllerから）
 * ============================================
 *
 * @Controller
 * @RequiredArgsConstructor
 * public class MemoController {
 *
 *     private final MemoService memoService;
 *
 *     // 一覧表示
 *     @GetMapping("/memos")
 *     public String list(Model model) {
 *         List<MemoDto> memos = memoService.findAll();
 *         model.addAttribute("memos", memos);
 *         return "memos/list";
 *     }
 *
 *     // 新規登録
 *     @PostMapping("/memos")
 *     public String create(@Valid MemoDto dto, BindingResult result) {
 *         if (result.hasErrors()) {
 *             return "memos/new";
 *         }
 *         memoService.create(dto);
 *         return "redirect:/memos";
 *     }
 *
 *     // 更新
 *     @PostMapping("/memos/{id}/update")
 *     public String update(@PathVariable Long id, @Valid MemoDto dto, BindingResult result) {
 *         if (result.hasErrors()) {
 *             return "memos/edit";
 *         }
 *         dto.setId(id);
 *         memoService.update(dto);
 *         return "redirect:/memos/" + id;
 *     }
 *
 *     // 削除
 *     @PostMapping("/memos/{id}/delete")
 *     public String delete(@PathVariable Long id) {
 *         memoService.delete(id);
 *         return "redirect:/memos";
 *     }
 * }
 */

/*
 * ============================================
 * ログレベルの使い分け
 * ============================================
 *
 * TRACE: 最も詳細なログ（通常は使わない）
 * DEBUG: 開発時のデバッグ情報（本番では出力しない）
 * INFO: ビジネス上の重要なイベント（本番でも出力）
 * WARN: 警告（エラーではないが注意が必要）
 * ERROR: エラー（システムの異常）
 *
 * 【このクラスでの使い分け】
 * - log.debug(): メソッドの開始、取得件数など
 * - log.info(): 登録、更新、削除などの重要イベント
 * - log.warn(): 更新・削除対象が見つからない場合
 * - log.error(): 例外が発生した場合（今回は使用していない）
 *
 * application.yml で出力レベルを制御:
 * logging:
 *   level:
 *     com.example.memoapp: DEBUG  # 開発時
 *     com.example.memoapp: INFO   # 本番環境
 */
