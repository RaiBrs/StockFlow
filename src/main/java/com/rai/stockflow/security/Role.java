package com.rai.stockflow.security;

/** Os perfis são persistidos pelo nome para não dependerem da ordem do enum. */
public enum Role {
    ADMIN,
    MANAGER,
    OPERATOR
}
