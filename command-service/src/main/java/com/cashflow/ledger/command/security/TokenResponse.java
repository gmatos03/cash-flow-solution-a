package com.cashflow.ledger.command.security;

public record TokenResponse(String tokenType, String accessToken, long expiresInSeconds) {

    public static TokenResponse bearer(String token, long expiresInSeconds) {
        return new TokenResponse("Bearer", token, expiresInSeconds);
    }
}
