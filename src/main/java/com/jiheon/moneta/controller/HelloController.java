package com.jiheon.moneta.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jiheon.moneta.dto.UserRequest;
import com.jiheon.moneta.service.UserService;



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
    //@PostMapping("/user")
    //public String createUser(@RequestBody UserRequest userRequest){
    //    return "name = "+userRequest.getName()+", age = "+userRequest.getAge();
    //}

    // Service에 DTO 넘기기
    // @Service 안했을때 -> new 로 객체 생성
    //private UserService userService=new UserService();

    // Spring이 가지고 있는 UserService 객체를 나한테 넣어달라 요청 -> DI
    // DI = 의존성 주입
    private final UserService userService;

    public HelloController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/user")
    public String createUser(@RequestBody UserRequest userRequest){
        //return userService.createUser(userRequest.getName(), userRequest.getAge());
        userService.createUser(userRequest.getName(), userRequest.getAge());
        return "User Created";
    }

    /*
        HTTP에서 post 요청 -> DTO를 받아서 Service 호출 -> Service에서 createUser 호출
        -> createUser에서 User Entity 객체 생성 후 정보 입력 -> Repository 호출하여 실제 DB에 저장
    */ 
}
