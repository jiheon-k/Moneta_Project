package com.jiheon.moneta.auth.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.jiheon.moneta.auth.dto.SignupRequest;
import com.jiheon.moneta.auth.service.SignupService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

// 회원가입 입력을 검증하고 가입 처리를 Service에 맡긴다.
@RestController
@RequestMapping("/auth")
public class SignupController {

    private final SignupService signupService;
    public SignupController(SignupService signupService){
        this.signupService=signupService;
    }

    // 정상적으로 저장되면 201 Created 반환
    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public void signUp(@Valid @RequestBody SignupRequest request) {
        signupService.signUp(request);
    }

}
