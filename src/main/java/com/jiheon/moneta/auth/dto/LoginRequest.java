package com.jiheon.moneta.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class LoginRequest {
    @NotBlank(message = "로그인 아이디는 필수입니다.")
    @Size(max = 50, message = "로그인 아이디는 50자 이하여야 합니다.")
    private String loginId;
    @NotBlank(message = "비밀번호는 필수입니다.")
    @Size(max = 1024, message = "비밀번호가 너무 깁니다.")
    private String password;

    public String getLoginId(){
        return loginId;
    }

    public String getPassword(){
        return password;
    }

    public void setLoginId(String loginId){
        this.loginId=loginId;
    }
    public void setPassword(String password){
        this.password=password;
    }
}
