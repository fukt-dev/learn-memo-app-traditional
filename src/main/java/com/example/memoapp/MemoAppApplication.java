package com.example.memoapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * アプリケーションの起動クラス（エントリーポイント）。
 *
 * 3 層アーキテクチャ（Controller → Service → Mapper）の外側にある「土台」で、
 * ここから Spring の DI コンテナ・組み込み Tomcat・Flyway が立ち上がる。
 *
 * 学習ポイント:
 * - @SpringBootApplication の内訳（@Configuration / @EnableAutoConfiguration /
 *   @ComponentScan）とコンポーネントスキャン・DI コンテナの概念 → docs/解説/DIとLombok.md
 * - 起動方法・ポート競合や DB 接続などのトラブルシュート → docs/環境構築手順.md
 */
@SpringBootApplication
public class MemoAppApplication {

    /**
     * JVM が最初に実行するメソッド。SpringApplication.run() に起動クラスを渡すと、
     * コンポーネントスキャン・自動設定・Tomcat 起動・Flyway マイグレーションまでが
     * 一括で走る（各処理の中身は上記 DIとLombok.md を参照）。
     *
     * @param args コマンドライン引数。{@code --server.port=8081} のように渡すと
     *             application.yml の設定を上書きできる
     */
    public static void main(String[] args) {
        SpringApplication.run(MemoAppApplication.class, args);
    }
}
