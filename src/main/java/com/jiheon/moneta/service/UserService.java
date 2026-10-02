package com.jiheon.moneta.service;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void createUser(String name, int age) {
        // user -> Entity 객체
        User user = new User();
        user.setName(name);
        user.setAge(age);

        // User 객체를 DB에 저장하도록 JPA에게 요청
        userRepository.save(user);
    }

    /*
     * public User getUser(Long id){
     * // Repository에서 JPA가 알아서 DB를 조회해줌
     * // Repositoy.DB접근 함수 -> 반환값 => Optional -> 이걸 다시 Entity 형태로
     * return userRepository.findById(id).orElse(null);
     * }
     */

    public UserResponse getUser(Long id) {
        // User Entity 가져오기
        User user = userRepository.findById(id).orElse(null);

        if (user == null)
            return null;

        return new UserResponse(user.getId(), user.getName(), user.getAge());
    }

    public List<UserResponse> getUsers() {
        // List<User> userList=new ArrayList<>();
        // userList=userRepository.findAll();
        List<User> userList = userRepository.findAll();
        List<UserResponse> result = new ArrayList<>();

        for (int i = 0; i < userList.size(); i++) {
            UserResponse unit = new UserResponse(userList.get(i).getId(), userList.get(i).getName(),
                    userList.get(i).getAge());
            result.add(unit);
        }

        return result;
    }

    public List<UserResponse> getUsers_2(Integer minAge) {
        List<User> userList = userRepository.findByAgeGreaterThanEqual(minAge);
        List<UserResponse> result = new ArrayList<>();

        for (int i = 0; i < userList.size(); i++) {
            UserResponse unit = new UserResponse(userList.get(i).getId(), userList.get(i).getName(),
                    userList.get(i).getAge());
            result.add(unit);
        }

        return result;
    }

    public Map<Integer, Integer> getSummary() {
        List<User> userList = userRepository.findAll();
        // 보통은 Map으로 선언
        Map<Integer, Integer> result = new HashMap<>();

        for (int i = 0; i < userList.size(); i++) {
            int age = userList.get(i).getAge();
            if (result.containsKey(age)) {
                int count=result.get(age);
                result.put(age, count+1);
            }

            else {
                result.put(age, 1);
            }
        }

        return result;
    }

}
