package com.marginalia.dev.security;

import com.marginalia.dev.entities.User;
import com.marginalia.dev.repo.dao.UserDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserDao userDao;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userDao.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
        return new CustomUserDetails(user);
    }

    public UserDetails loadUserById(UUID userId) {
        User user = userDao.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userId));
        return new CustomUserDetails(user);
    }
}
