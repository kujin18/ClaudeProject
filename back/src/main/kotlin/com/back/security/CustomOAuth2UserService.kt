package com.back.security

import com.back.domain.account.Account
import com.back.domain.account.AccountRepository
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CustomOAuth2UserService(
    private val accountRepository: AccountRepository,
) : DefaultOAuth2UserService() {

    @Transactional
    override fun loadUser(userRequest: OAuth2UserRequest): OAuth2User {
        val oAuth2User = super.loadUser(userRequest)
        val googleSubject = oAuth2User.getAttribute<String>("sub")
            ?: throw OAuth2AuthenticationException("Google 계정에서 sub(subject)를 찾을 수 없습니다")
        val email = oAuth2User.getAttribute<String>("email")
            ?: throw OAuth2AuthenticationException("Google 계정에서 email을 찾을 수 없습니다")

        val account = accountRepository.findByGoogleSubject(googleSubject)
            ?: accountRepository.save(Account(googleSubject = googleSubject, email = email))

        return AppOAuth2User(oAuth2User, account.id!!, account.handle)
    }
}
