package com.courshare.identity.config;

import com.courshare.identity.domain.Role;
import com.courshare.identity.domain.RoleRepository;
import com.courshare.identity.domain.User;
import com.courshare.identity.domain.UserRepository;
import com.courshare.identity.domain.UserRole;
import com.courshare.identity.domain.UserRoleRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Component
@Profile("!test")
public class RoleDataInitializer implements ApplicationRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;

    public RoleDataInitializer(
            RoleRepository roleRepository,
            UserRepository userRepository,
            UserRoleRepository userRoleRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedRole("STUDENT", "Learner who enrolls in courses");
        seedRole("INSTRUCTOR", "Creates and manages courses");
        seedRole("ADMIN", "Platform administrator");

        seedUser("student@courshare.io", "password123", "Student User", "STUDENT");
        seedUser("instructor@courshare.io", "password123", "Instructor User", "INSTRUCTOR");
        seedUser("admin@courshare.io", "password123", "Admin User", "ADMIN");
    }

    private void seedRole(String name, String description) {
        if (roleRepository.findByName(name).isEmpty()) {
            roleRepository.save(new Role(UUID.randomUUID().toString(), name, description));
        }
    }

    private void seedUser(String email, String rawPassword, String fullName, String roleName) {
        if (userRepository.findByEmail(email).isEmpty()) {
            User user = new User(email, passwordEncoder.encode(rawPassword));
            user.setFullName(fullName);
            userRepository.save(user);

            Role role = roleRepository.findByName(roleName)
                    .orElseThrow(() -> new IllegalStateException("Role not found: " + roleName));
            
            userRoleRepository.save(new UserRole(user.getId(), role.getId()));
        }
    }
}

