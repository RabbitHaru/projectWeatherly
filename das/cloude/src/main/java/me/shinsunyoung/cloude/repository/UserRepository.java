package me.shinsunyoung.cloude.repository;


import me.shinsunyoung.cloude.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByUserEmail(String userEmail);
}

