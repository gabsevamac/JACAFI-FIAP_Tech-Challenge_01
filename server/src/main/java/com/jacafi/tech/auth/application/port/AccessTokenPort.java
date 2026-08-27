package com.jacafi.tech.auth.application.port;

public interface AccessTokenPort {
    String issue(String subject);

    String parseSubject(String token);
}
