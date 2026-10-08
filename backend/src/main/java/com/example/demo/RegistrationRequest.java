package com.example.demo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegistrationRequest(
        @NotBlank(message = "ユーザIDを入力してください")
        @Pattern(regexp = "[A-Za-z0-9_-]{3,64}", message = "ユーザIDは半角英数字・「-」・「_」で3〜64文字にしてください") String userid,
        @NotBlank(message = "パスワードを入力してください")
        @Size(min = 8, max = 72, message = "パスワードは8〜72文字にしてください") String password1,
        @NotBlank(message = "確認用パスワードを入力してください") String password2,
        @Size(max = 100, message = "お名前は100文字以内にしてください") String username) {
}
