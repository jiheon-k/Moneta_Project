package com.jiheon.moneta.category;

import com.jiheon.moneta.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity 
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, length=100)
    private String name;

    // NULL 허용
    @ManyToOne 
    @JoinColumn (name="user_id")
    private User user;

    public Long getId(){
        return id;
    }

    public String getName(){
        return name;
    }

    public User getUser(){
        return user;
    }

    public void setName(String name){
        this.name=name;
    }

    public void setUser(User user){
        this.user=user;
    }
}
