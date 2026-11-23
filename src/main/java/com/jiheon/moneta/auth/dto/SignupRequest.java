package com.jiheon.moneta.auth.dto;

import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class SignupRequest {
    @NotBlank(message = "로그인 아이디는 필수입니다.")
    @Size(min = 4, max = 20, message = "로그인 아이디는 4~20자여야 합니다.")
    @Pattern(regexp = "[A-Za-z0-9_]+", message = "로그인 아이디는 영문, 숫자, 밑줄만 사용할 수 있습니다.")
    private String loginId;

    @NotBlank(message = "비밀번호는 필수입니다.")
    @Size(min = 15, max = 64, message = "비밀번호는 15~64자여야 합니다.")
    private String password;

    @NotBlank(message = "이름은 필수입니다.")
    @Size(min = 1, max = 50, message = "이름은 1~50자여야 합니다.")
    @Pattern(regexp = "[\\p{L}\\p{M}]+(?:[ '·-][\\p{L}\\p{M}]+)*",
            message = "이름은 문자와 문자 사이의 공백, 작은따옴표, 가운뎃점, 하이픈만 사용할 수 있습니다.")
    private String name;

    @NotNull(message = "나이는 필수입니다.")
    @Min(value = 1, message = "나이는 1세 이상이어야 합니다.")
    @Max(value = 150, message = "나이는 150세 이하여야 합니다.")
    private Integer age;

    // 기본 PasswordEncoder의 BCrypt는 UTF-8 입력을 최대 72바이트까지 지원한다.
    // 누락 여부는 @NotBlank에서 검사하며, 비밀번호 자체를 자르거나 변경하지 않는다.
    @JsonIgnore
    @AssertTrue(message = "비밀번호는 UTF-8 기준 72바이트 이하여야 합니다.")
    public boolean isPasswordWithinByteLimit() {
        return password == null || password.getBytes(StandardCharsets.UTF_8).length <= 72;
    }

    public String getLoginId() {
        return loginId;
    }

    public void setLoginId(String loginId) {
        this.loginId = loginId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }
}
