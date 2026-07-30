package com.back.web

import com.back.domain.account.Account
import com.back.domain.account.AccountRepository
import com.back.domain.shortlink.ShortLink
import com.back.domain.shortlink.ShortLinkRepository
import com.back.security.AppOAuth2User
import com.back.support.AbstractIntegrationTest
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.security.oauth2.core.user.DefaultOAuth2User
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@AutoConfigureMockMvc
class DashboardTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val accountRepository: AccountRepository,
    private val shortLinkRepository: ShortLinkRepository,
) : AbstractIntegrationTest() {

    private fun oauth2UserFor(account: Account): OAuth2User =
        DefaultOAuth2User(
            emptyList(),
            mapOf("sub" to account.googleSubject, "email" to account.email),
            "sub",
        )

    private fun principalFor(account: Account) = AppOAuth2User(oauth2UserFor(account), account.id!!)

    private fun accountWithHandle(subject: String, email: String, handle: String): Account =
        accountRepository.save(Account(googleSubject = subject, email = email).apply { assignHandle(handle) })

    @Test
    fun `로그인하지 않으면 대시보드에 접근할 수 없다`() {
        mockMvc.get("/dashboard").andExpect {
            status { is3xxRedirection() }
        }
    }

    @Test
    fun `본인 소유 링크를 원본URL, 단축링크, 생성일, 클릭수와 함께 보여준다`() {
        val account = accountWithHandle("db-sub-1", "db-1@example.com", "db-handle-1")
        shortLinkRepository.save(
            ShortLink(
                originalUrl = "https://op.gg/summoners/kr/Faker-KR1",
                domainPrefix = "op",
                alias = "dash-alias",
                account = account,
            ),
        )

        mockMvc.get("/dashboard") {
            with(oauth2Login().oauth2User(principalFor(account)))
        }.andExpect {
            status { isOk() }
            content { string(containsString("https://op.gg/summoners/kr/Faker-KR1")) }
            content { string(containsString("/db-handle-1/op/dash-alias")) }
            content { string(containsString("0")) }
        }
    }

    @Test
    fun `다른 계정이 만든 링크는 보이지 않는다`() {
        val owner = accountWithHandle("db-sub-2", "db-2@example.com", "db-handle-2")
        val other = accountWithHandle("db-sub-3", "db-3@example.com", "db-handle-3")
        shortLinkRepository.save(
            ShortLink(
                originalUrl = "https://op.gg/summoners/kr/OtherOwner",
                domainPrefix = "op",
                alias = "other-owner-alias",
                account = other,
            ),
        )

        mockMvc.get("/dashboard") {
            with(oauth2Login().oauth2User(principalFor(owner)))
        }.andExpect {
            status { isOk() }
            content { string(not(containsString("other-owner-alias"))) }
        }
    }
}
