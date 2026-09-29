package com.jiheon.moneta.service;

import org.springframework.stereotype.Service;

import com.jiheon.moneta.dto.UserResponse;
import com.jiheon.moneta.entity.User;
import com.jiheon.moneta.repository.UserRepository;

// 이 클래스를 Service 역할을 하는 객체로 관리해줘
// Spring -> Application 시작할 때 -> @Service가 붙은 클래스를 찾아서 객체로 만들어 관리
@Service
public class UserService {

    // DI
    // UserRepository 객체를 직접 생성하지 않고 외부에서 넣어줌
    // 
    private final UserRepository userRepository;
    public UserService(UserRepository userRepository){
        this.userRepository=userRepository;
    }

    public void createUser(String name, int age){
        // user -> Entity 객체
        User user=new User();
        user.setName(name);
        user.setAge(age);

        // User 객체를 DB에 저장하도록 JPA에게 요청
        userRepository.save(user);
    }

    /* 
    public User getUser(Long id){
        // Repository에서 JPA가 알아서 DB를 조회해줌
        // Repositoy.DB접근 함수 -> 반환값 => Optional -> 이걸 다시 Entity 형태로
        return userRepository.findById(id).orElse(null);
    }
    */

    public UserResponse getUser(Long id){
        // User Entity 가져오기
        User user=userRepository.findById(id).orElse(null);

        if(user==null)
            return null;

        return new UserResponse(user.getId(), user.getName(), user.getAge());
    }
}
