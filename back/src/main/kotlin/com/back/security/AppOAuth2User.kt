package com.back.security

import org.springframework.security.oauth2.core.user.OAuth2User

class AppOAuth2User(
    private val delegate: OAuth2User,
    val accountId: Long,
) : OAuth2User by delegate
