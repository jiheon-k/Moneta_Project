package com.jiheon.moneta.user;

import jakarta.validation.constraints.*;

// 사용자가 전송한 JSON 데이터를 DTO에 저장
public class UserRequest {
    @NotBlank
    @Size(max = 100)
    private String name;

    @NotNull
    @Positive
    @Max(199)
    private Integer age;

    public String getName() {
        return name;
    }

    public Integer getAge() {
        return age;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAge(Integer age) {
        this.age = age;
    }
}
