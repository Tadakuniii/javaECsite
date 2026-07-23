## JavaClassECsite


### バックエンドの起動
./gradlew bootRun

### dockerの起動(mysql)
docker compose up

### DBに接続
docker exec -it shop-mysql mysql -u shop_user -pshop_password shop_db