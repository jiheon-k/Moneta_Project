package com.jiheon.moneta.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jiheon.moneta.dto.UserRequest;

// 해당 클래스는 REST API를 처리하는 컨트롤러임을 나타냄
@RestController 
public class HelloController {
    // /hello 요청이 들어오면 hello() 메서드 실행
    // GET 요청을 처리
    @GetMapping("/hello")
    // RequestParam을 사용하여 쿼리 파라미터에서 name 값을 추출
    public String hello(@RequestParam("name") String userName) {
        return "Hello, " + userName + "!";
    }

    // /hello/{name} 요청이 들어오면 helloPath() 메서드 실행
    // PathVariable을 사용하여 URL 경로에서 name 값을 추출
    @GetMapping("/hello/{name}")
    public String helloPath(@PathVariable String name)
    {
        return "Hello, " + name + "!";
    }


    // RequestParam + PathVariable
    // 여러개 사용 가능
    @GetMapping("/info/{userID}/myInfo")
    public String information(
        @PathVariable String userID,
        @RequestParam String name,
        @RequestParam int age
    ){
        return "User "+userID+", name : "+name+", age : "+age;
    }
    
    /*
    // Using Map
    @PostMapping("/user")
    public String createUser(@RequestBody Map<String, Object> data){
        return "name = "+data.get("name") + ", age = "+data.get("age");
    }
    */

    // Using DTO
    @PostMapping("/user")
    public String createUser(@RequestBody UserRequest userRequest){
        return "name = "+userRequest.getName()+", age = "+userRequest.getAge();
    }
}
