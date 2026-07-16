<!DOCTYPE html>
<html lang="ja">
<head>
<meta charset="utf-8">
<title>HTMLフォームの作成</title>
</head>
<body>
<table border="1">
  <tr>
    <td>>ユーザID</td><td><?php echo $_POST["userid"] ?></td>
  </tr>
  <tr>
    <td>パスワード</td><td><?php echo $_POST["password"] ?></td>
  </tr>
</table>
</body>
</html>