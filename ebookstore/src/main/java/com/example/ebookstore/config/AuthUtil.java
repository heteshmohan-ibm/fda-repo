package com.example.ebookstore.config;

import com.example.ebookstore.entity.User;
import com.example.ebookstore.exception.ResourceNotFoundException;
import com.example.ebookstore.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Resolves the currently authenticated user from the SecurityContext.
 * Inject this into controllers to replace the old DEMO_USER_ID constant.
 */
@Component
@RequiredArgsConstructor
public class AuthUtil {

    private final UserRepository userRepository;

    /** Returns the User entity for the currently authenticated principal. */
    public User currentUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Authenticated user not found: " + email));
    }

    /** Returns just the id of the currently authenticated user. */
    public Long currentUserId() {
        return currentUser().getId();
    }
}
