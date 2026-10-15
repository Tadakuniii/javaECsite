# Java EC Site

健康商品を扱うECサイトの学習用プロジェクトです。

- フロントエンド：Vue 3 / TypeScript / Vite
- バックエンド：Java 21 / Spring Boot
- データベース：MySQL 8.0
- 実行環境：Docker Compose

## 1. 必要なもの

- Git
- Docker Desktop

macOS・Linux・Git Bash・WSLから実行できます。Java、Node.js、Bun、MySQLを個別にインストールする必要はありません。

## 2. プロジェクトを取得

```bash
git clone https://github.com/Tadakuniii/javaECsite.git
cd javaECsite
```

## 3. 環境設定

設定ファイルを作成します。

```bash
cp .env.example .env
```

商品管理機能を使う場合は、`.env`に任意の管理者情報を設定してください。

```env
ADMINISTRATOR_USER_ID=admin
ADMINISTRATOR_PASSWORD=8文字以上の安全なパスワード
IMPORT_DEMO_CUSTOMERS=false
```

通常ユーザーとして確認するだけなら、管理者IDとパスワードは空欄でも起動できます。`.env`はGitHubへ送信されません。

## 4. 起動

プロジェクト直下で実行します。

```bash
./start.sh
```

初回はDockerイメージとライブラリを取得するため、数分かかることがあります。起動後、次のURLを開いてください。

```text
http://127.0.0.1:5173/
```

| サービス | URL・接続先 |
|---|---|
| Webサイト | `http://127.0.0.1:5173/` |
| バックエンドAPI | `http://127.0.0.1:8081/` |
| MySQL | `127.0.0.1:3307` |

一般ユーザーはログイン画面の「ユーザID登録」から作成できます。`.env`に設定した管理者でログインすると「商品管理」ボタンが表示されます。

## 5. 停止

```bash
./stop.sh
```

停止してもMySQLのデータは残ります。次回も`./start.sh`で同じデータを使用できます。

## よく使うコマンド

### 最新コードを取得

```bash
git pull origin main
```

### 起動状態を確認

```bash
docker compose -p javaecsite -f backend/docker-compose.dev.yml ps
```

### ログを確認

```bash
docker compose -p javaecsite -f backend/docker-compose.dev.yml logs -f
```

### テストを実行

サイトを起動した状態で実行します。

```bash
docker compose -p javaecsite -f backend/docker-compose.dev.yml exec backend gradle --no-daemon test
docker compose -p javaecsite -f backend/docker-compose.dev.yml exec frontend bun run test
```

## 主な機能

- 会員登録、ログイン、ログアウト
- 健康商品の検索、カテゴリ絞り込み、詳細表示
- 管理者による商品の登録と更新
- 在庫数、価格、説明、画像URL、販売状態の管理
- カートと注文情報を保存するデータベース

## 困ったとき

起動しない場合はDocker Desktopが動作していることを確認し、次のコマンドで再起動してください。

```bash
./stop.sh
./start.sh
```
