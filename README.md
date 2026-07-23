## JavaClassECsite


### バックエンドの起動
```bash
./gradlew bootRun
```

### dockerの起動(mysql)
```bash
docker compose up
```


### DBに接続
```bash
docker exec -it shop-mysql mysql -u shop_user -pshop_password shop_db
```
