package com.student.performance.service.impl;

import com.student.performance.entity.User;
import com.student.performance.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        if (!user.isActive()) {
            throw new UsernameNotFoundException("User account is disabled: " + username);
        }
        String role = "ROLE_" + user.getRole().getRoleName().toUpperCase();
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(), user.getPasswordHash(),
                List.of(new SimpleGrantedAuthority(role))
        );
    }
}
