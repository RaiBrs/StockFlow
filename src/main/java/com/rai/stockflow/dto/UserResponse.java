package com.rai.stockflow.dto;

import com.rai.stockflow.entity.User;
import com.rai.stockflow.security.Role;

/** Expõe apenas os dados públicos de administração da conta. */
public record UserResponse(Long id, String email, Role role, boolean active) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getRole(), user.isActive());
    }
}
