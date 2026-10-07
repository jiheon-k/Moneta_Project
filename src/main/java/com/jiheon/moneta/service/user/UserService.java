package com.jiheon.moneta.service.user;

import org.springframework.stereotype.Service;

import com.jiheon.moneta.dto.user.UserRequest;
import com.jiheon.moneta.dto.user.UserResponse;
import com.jiheon.moneta.entity.User;
import com.jiheon.moneta.repository.UserRepository;

@Service 
public class UserService {

    // DI - 생성자 주입
    // -> UserService 클래스를 생성할떄 사용할 UserRepository도 전달

    // 1. 변수 선언
    private final UserRepository userRepository;
    // 2. Spring이 만든 userRepository 갹체를 생성자로 받아서 변수에 설정
    public UserService(UserRepository userRepository){
        this.userRepository=userRepository;
    }

    // User 생성
    // 사용자로부터 user 정보를 userRequest로 받아서 DB에 저장, 저장돈 사용자 정보로 userResponse 만들어서 반환
    public UserResponse createUser(UserRequest userRequest){
        User newUser=new User();
        newUser.setName(userRequest.getName());
        newUser.setAge(userRequest.getAge());

        User savedUser=userRepository.save(newUser);
        return new UserResponse(
            savedUser.getId(),
            savedUser.getName(),
            savedUser.getAge()
        );
    }

}
