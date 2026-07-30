package com.back.security

import com.back.domain.account.AccountService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.AuthenticationSuccessHandler

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val customOAuth2UserService: CustomOAuth2UserService,
    private val accountService: AccountService,
) {

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .authorizeHttpRequests { auth ->
                auth
                    // 인증 필요 경로를 먼저 매칭시켜, 아래의 "/*"(프로필 페이지) permitAll
                    // 와일드카드가 이 경로들을 가로채지 못하게 한다(매칭 순서대로 첫 규칙이 적용됨).
                    .requestMatchers("/dashboard", "/setup-handle", "/links", "/links/**").authenticated()
                    .requestMatchers("/", "/login/**", "/oauth2/**", "/css/**", "/js/**", "/*/*/*", "/*")
                    .permitAll()
                    .anyRequest().authenticated()
            }
            .oauth2Login { oauth2 ->
                oauth2
                    .userInfoEndpoint { it.userService(customOAuth2UserService) }
                    .successHandler(loginSuccessHandler())
            }
        return http.build()
    }

    @Bean
    fun loginSuccessHandler(): AuthenticationSuccessHandler =
        AuthenticationSuccessHandler { _, response, authentication ->
            val principal = authentication.principal as AppOAuth2User
            val account = accountService.findById(principal.accountId)
            val redirectUrl = if (account.hasHandle()) "/" else "/setup-handle"
            response.sendRedirect(redirectUrl)
        }
}
