package com.jiheon.moneta.user;

// 서버가 만들어 보내는 결과 값
public class UserResponse {

    private Long id;
    
    private String name;
    
    private Integer age;

    public UserResponse(Long id, String name, Integer age) {
        this.id = id;
        this.name = name;
        this.age = age;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Integer getAge() {
        return age;
    }
}
