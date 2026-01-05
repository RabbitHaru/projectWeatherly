package me.shinsunyoung.cloude.service;


import lombok.RequiredArgsConstructor;
import me.shinsunyoung.cloude.entity.User;
import me.shinsunyoung.cloude.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public void register(User user) {

        // local 로그인일 때만 비밀번호 암호화
        if ("local".equals(user.getAuthProvider())
                && user.getUserPassword() != null
                && !user.getUserPassword().isEmpty()) {

            user.setUserPassword(
                    passwordEncoder.encode(user.getUserPassword())
            );
        } else {
            user.setUserPassword(null);
        }

        userRepository.save(user);
    }
}
