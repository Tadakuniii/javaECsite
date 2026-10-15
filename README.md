## JavaClassECsite


### バックエンドの起動
Java 21 の JDK を用意し、`JAVA_HOME` に設定してください。

```bash
cd backend
./gradlew bootRun
```

Windows の PowerShell では `cd backend` の後に `.\gradlew.bat bootRun` を実行します。

### dockerの起動(mysql)
`backend` フォルダーで実行します。
```bash
docker compose up
```


### DBに接続
```bash
docker exec -it shop-mysql mysql -u shop_user -pshop_password shop_db
```
