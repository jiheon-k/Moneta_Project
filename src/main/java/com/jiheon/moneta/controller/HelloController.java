package com.jiheon.moneta.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


// 해당 클래스는 REST API를 처리하는 컨트롤러임을 나타냄
@RestController 
public class HelloController {
    // /hello 요청이 들어오면 hello() 메서드 실행
    // GET 요청을 처리
    @GetMapping("/hello")
    public String hello(@RequestParam String name) {
        return "Hello, " + name + "!";
    }
}
