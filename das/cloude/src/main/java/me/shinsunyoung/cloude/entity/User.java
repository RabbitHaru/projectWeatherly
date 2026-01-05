package me.shinsunyoung.cloude.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter @Setter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Column(nullable = false, unique = true, length = 100)
    private String userEmail;

    @Column(length = 255)
    private String userPassword;

    @Column(nullable = false, length = 50)
    private String userName;

    @Column(length = 255)
    private String profileImage;

    @Column(length = 20)
    private String userRole = "USER";

    @Column(length = 20)
    private String authProvider = "local";

    @Column(length = 100)
    private String providerId;

    private Boolean isActive = true;

    private LocalDateTime lastLoginAt;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
