# ============================================
# マルチステージビルド
# ============================================
# マルチステージビルドとは、複数のステージに分けてDockerイメージを構築する手法です
# メリット：
# 1. 最終イメージにビルドツールを含めないため、イメージサイズが小さくなる
# 2. セキュリティ向上（不要なツールが含まれない）
# 3. ビルド環境と実行環境を分離できる

# ============================================
# ステージ1: ビルドステージ
# ============================================
# このステージでは、Mavenを使ってJavaアプリケーションをビルドします
# ビルドに必要なツール（Maven、JDK）はこのステージにのみ存在します

# ベースイメージ: Maven 3.9.6 + OpenJDK 17
# maven:3.9.6-eclipse-temurin-17 は以下を含みます：
# - OpenJDK 17（Java開発キット）
# - Maven 3.9.6（ビルドツール）
# - 必要な依存関係
FROM maven:3.9.6-eclipse-temurin-17 AS builder

# 作業ディレクトリを /app に設定
# 以降のコマンドはこのディレクトリで実行されます
WORKDIR /app

# pom.xmlをコピー
# 先にpom.xmlだけをコピーする理由：
# Dockerはレイヤーをキャッシュするため、pom.xmlが変更されない限り
# 次のRUNコマンド（依存関係のダウンロード）がキャッシュから再利用されます
# これにより、ソースコード変更時のビルド時間が大幅に短縮されます
COPY pom.xml .

# 依存関係を事前にダウンロード
# mvn dependency:go-offline は以下を実行します：
# 1. pom.xmlに記載された全ての依存ライブラリをダウンロード
# 2. ローカルのMavenリポジトリ（~/.m2）にキャッシュ
# 3. オフラインでもビルドできる状態にする
#
# -B オプション: バッチモード（対話的なプロンプトを表示しない）
RUN mvn dependency:go-offline -B

# ソースコードをコピー
# .dockerignoreで指定したファイル以外が全てコピーされます
# 主にコピーされるもの：
# - src/ ディレクトリ（Javaソースコード）
# - pom.xml（既にコピー済みだが、変更があった場合に備えて再度コピー）
COPY src ./src

# アプリケーションをビルド
# mvn package は以下を実行します：
# 1. ソースコードをコンパイル（.java → .class）
# 2. テストを実行
# 3. JARファイルを作成（target/memo-app-1.0.0-SNAPSHOT.jar）
#
# オプションの説明：
# -DskipTests: テストをスキップ（開発環境用。本番ではテストを実行すべき）
# -B: バッチモード
RUN mvn package -DskipTests -B

# ビルド結果の確認（デバッグ用）
# target/ディレクトリの内容を表示
# ビルドされたJARファイルが存在することを確認できます
RUN ls -la /app/target/

# ============================================
# ステージ2: 実行ステージ
# ============================================
# このステージでは、ビルドされたJARファイルのみを含む軽量なイメージを作成します
# Mavenなどのビルドツールは含まれません

# ベースイメージ: OpenJDK 17（JREのみ）
# eclipse-temurin:17-jre-alpine の特徴：
# - JRE（Java Runtime Environment）のみ（JDKは含まない）
# - alpineベース（軽量なLinuxディストリビューション）
# - イメージサイズが小さい（約180MB vs フルJDK 約450MB）
FROM eclipse-temurin:17-jre-alpine

# メタデータの設定（オプション）
# LABELは、イメージに関する情報を記録します
# docker inspect コマンドで確認できます
LABEL maintainer="memoapp-dev"
LABEL description="Traditional Memo Application with Spring Boot"
LABEL version="1.0.0"

# 作業ディレクトリを /app に設定
WORKDIR /app

# ビルドステージからJARファイルをコピー
# --from=builder: ビルドステージ（AS builder）からコピー
# /app/target/*.jar: ビルドされたJARファイル
# app.jar: コピー先のファイル名（わかりやすい名前に変更）
COPY --from=builder /app/target/*.jar app.jar

# 非rootユーザーの作成（セキュリティのベストプラクティス）
# rootユーザーでアプリケーションを実行するのはセキュリティリスクがあります
# 専用のユーザーを作成して、そのユーザーでアプリケーションを実行します
#
# addgroup: appgroupというグループを作成
# adduser: appuserというユーザーを作成
#   -D: パスワードなしで作成
#   -G appgroup: appgroupに所属させる
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# ファイルの所有者を変更
# app.jarの所有者をappuser:appgroupに変更
# これにより、appuserがapp.jarを読み取り・実行できるようになります
RUN chown appuser:appgroup app.jar

# 実行ユーザーを切り替え
# 以降のコマンドとアプリケーションはappuserとして実行されます
USER appuser

# ポート8080を公開
# EXPOSE命令は、コンテナが8080番ポートをリッスンすることを宣言します
# 注: これはドキュメント的な意味合いが強く、実際のポート公開は
# docker-compose.ymlのportsセクションで行います
EXPOSE 8080

# ヘルスチェック
# コンテナが正常に動作しているかを定期的に確認します
# Spring Boot Actuatorの /actuator/health エンドポイントを使用
#
# --interval=30s: 30秒ごとにチェック
# --timeout=3s: 3秒以内に応答がなければ失敗
# --start-period=40s: 起動後40秒間は失敗を無視（起動時間を考慮）
# --retries=3: 3回連続で失敗したら unhealthy とみなす
#
# wget の説明：
# -q: 静かにモード（出力を抑制）
# -O /dev/null: 出力を破棄
# http://localhost:8080/actuator/health: ヘルスチェックエンドポイント
#
# 注: このヘルスチェックを使うには、pom.xmlに
# spring-boot-starter-actuator の依存関係を追加する必要があります
# HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
#   CMD wget -q -O /dev/null http://localhost:8080/actuator/health || exit 1

# アプリケーションの起動コマンド
# ENTRYPOINTは、コンテナ起動時に実行されるコマンドです
# CMDとの違い：
# - ENTRYPOINT: 必ず実行されるコマンド（変更不可）
# - CMD: デフォルト引数（docker runで上書き可能）
#
# java -jar app.jar の説明：
# - java: Java実行コマンド
# - -jar: JARファイルを実行
# - app.jar: 実行するJARファイル
#
# 本番環境用のオプション例（必要に応じて追加）：
# -Xms256m -Xmx512m: ヒープメモリの初期サイズと最大サイズ
# -XX:+UseG1GC: G1ガベージコレクタを使用
# -Djava.security.egd=file:/dev/./urandom: 起動時間の短縮
ENTRYPOINT ["java", "-jar", "app.jar"]

# デフォルトの引数（オプション）
# Spring Bootのプロファイルを指定する場合など
# CMD ["--spring.profiles.active=docker"]

# ============================================
# ビルド方法
# ============================================
#
# 【開発環境用（docker-compose使用）】
# docker-compose up --build
#
# 【手動ビルド】
# docker build -t memo-app:latest .
#
# 【手動実行】
# docker run -p 8080:8080 \
#   -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/memoapp \
#   -e SPRING_DATASOURCE_USERNAME=memoapp_user \
#   -e SPRING_DATASOURCE_PASSWORD=memoapp_pass \
#   memo-app:latest
#
# ============================================
# トラブルシューティング
# ============================================
#
# 【ビルドが失敗する場合】
# 1. pom.xmlが正しいか確認
# 2. Mavenの依存関係が解決できるか確認
# 3. ビルドログを詳細に確認: docker-compose build --no-cache
#
# 【コンテナが起動しない場合】
# 1. ログを確認: docker-compose logs app
# 2. データベース接続設定を確認
# 3. ポートが競合していないか確認
#
# 【イメージサイズが大きい場合】
# 1. .dockerignore が正しく設定されているか確認
# 2. マルチステージビルドが使われているか確認
# 3. alpineベースのイメージを使用しているか確認
#
