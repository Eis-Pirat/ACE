package isme.service_user.controller;

import isme.service_user.entity.User;
import isme.service_user.entity.Role;
import isme.service_user.JWT.JwtService;
import isme.service_user.repository.UserRepo;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthController(UserRepo userRepo, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public String register(@RequestBody User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // Set the default role to USER if not provided
        if (user.getRole() == null) {
            user.setRole(Role.USER); // Default role
        }

        userRepo.save(user);
        return "User registered successfully";
    }

    @PostMapping("/login")
    public String login(@RequestBody User user) {
        Optional<User> foundUser = userRepo.findByEmail(user.getEmail());
        if (foundUser.isPresent() && passwordEncoder.matches(user.getPassword(), foundUser.get().getPassword())) {
            // Add authorities based on the user's role
            SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + foundUser.get().getRole().name());

            UserDetails userDetails = org.springframework.security.core.userdetails.User
                    .withUsername(foundUser.get().getEmail())
                    .password(foundUser.get().getPassword())
                    .authorities(Collections.singletonList(authority))
                    .build();

            return jwtService.generateToken(userDetails);
        }
        return "Invalid credentials";
    }
}