package com.rai.stockflow.dto;

/** Token de acesso e seu prazo em segundos, sem dados da senha. */
public record LoginResponse(String accessToken, String tokenType, long expiresIn) {
}
