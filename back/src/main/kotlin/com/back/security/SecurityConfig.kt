package com.back.security

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
) {

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers("/", "/login/**", "/oauth2/**", "/css/**", "/js/**").permitAll()
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
            val redirectUrl = if (principal.handle == null) "/setup-handle" else "/"
            response.sendRedirect(redirectUrl)
        }
}
