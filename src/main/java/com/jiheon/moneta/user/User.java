package com.jiheon.moneta.user;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Entity
public class User {
    // NULL 표현을 위해 Long, Integer와 같은 객체 사용

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // PK, 인조 key

    // 사용자 인증
    @Column(nullable = false, unique = true, length = 50)
    private String loginId;

    // 원본 비밀번호를 저장하지 않는다
    @Column(nullable = false)
    private String passwordHash;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String name; // 이름

    @Positive
    @Max(199)
    @Column(nullable = false)
    private int age; // 나이

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getAge() {
        return age;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getLoginId(){
        return loginId;
    }

    @JsonIgnore
    public String getPasswordHash(){
        return passwordHash;
    }

    public void setLoginId(String loginId){
        this.loginId=loginId;
    }

    public void setPasswordHash(String passwordHash){
        this.passwordHash=passwordHash;
    }
}
