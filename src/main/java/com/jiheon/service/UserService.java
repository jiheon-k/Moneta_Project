package com.jiheon.service;
import org.springframework.stereotype.Service;

// 이 클래스를 Service 역할을 하는 객체로 관리해줘
// Spring -> Application 시작할 때 -> @Service가 붙은 클래스를 찾아서 객체로 만들어 관리
@Service
public class UserService {
    // Controller에서 DTO를 받고있는 상황
    public String createUser(String name, int age){
        return "name = "+name+", age = "+age;
    }
}
