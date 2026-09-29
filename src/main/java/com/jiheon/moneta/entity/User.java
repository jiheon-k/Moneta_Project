package com.jiheon.moneta.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

// 이 클래스는 DB 테이블과 연결해서 관리할 객체
@Entity 
public class User {
    // 해당 속성 -> PK
    // DB가 자동으로 id 생성
    @Id 
    @GeneratedValue 
    private Long id;

    private String name;
    private int age;

    public void setName(String name){
        this.name=name;
    }
    public void setAge(int age){
        this.age=age;
    }

    public Long getId(){
        return id;
    }

    public String getName(){
        return name;
    }

    public int getAge(){
        return age;
    }
}
