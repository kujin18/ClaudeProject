package com.back.web

import com.back.domain.account.Account
import com.back.domain.account.AccountRepository
import com.back.domain.shortlink.ShortLinkRepository
import com.back.security.AppOAuth2User
import com.back.support.AbstractIntegrationTest
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.security.oauth2.core.user.DefaultOAuth2User
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@AutoConfigureMockMvc
class ShortLinkCreationTest @Autowired constructor(
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
    fun `도메인축약어를 비우면 원본 URL 호스트에서 자동 추출된다`() {
        val account = accountWithHandle("sl-sub-1", "sl-1@example.com", "sl-handle-1")

        mockMvc.post("/links") {
            with(oauth2Login().oauth2User(principalFor(account)))
            with(csrf())
            param("originalUrl", "https://op.gg/summoners/kr/Faker-KR1")
            param("alias", "Faker-KR1")
        }.andExpect {
            status { isOk() }
            content { string(containsString("/sl-handle-1/op/Faker-KR1")) }
        }

        assertTrue(shortLinkRepository.existsByAccountAndDomainPrefixAndAliasAndDeletedFalse(account, "op", "Faker-KR1"))
    }

    @Test
    fun `도메인축약어를 직접 입력하면 그 값을 사용한다`() {
        val account = accountWithHandle("sl-sub-2", "sl-2@example.com", "sl-handle-2")

        mockMvc.post("/links") {
            with(oauth2Login().oauth2User(principalFor(account)))
            with(csrf())
            param("originalUrl", "https://op.gg/summoners/kr/Faker-KR1")
            param("domainPrefix", "custom")
            param("alias", "Faker-KR1")
        }.andExpect {
            status { isOk() }
            content { string(containsString("/sl-handle-2/custom/Faker-KR1")) }
        }
    }

    @Test
    fun `별칭을 비우면 에러와 함께 폼이 다시 보인다`() {
        val account = accountWithHandle("sl-sub-3", "sl-3@example.com", "sl-handle-3")

        mockMvc.post("/links") {
            with(oauth2Login().oauth2User(principalFor(account)))
            with(csrf())
            param("originalUrl", "https://op.gg/summoners/kr/Faker-KR1")
            param("alias", "   ")
        }.andExpect {
            status { isOk() }
            content { string(containsString("별칭을 입력")) }
        }
    }

    @Test
    fun `이미 사용 중인 도메인축약어+별칭 조합은 거부된다`() {
        val account = accountWithHandle("sl-sub-4", "sl-4@example.com", "sl-handle-4")

        mockMvc.post("/links") {
            with(oauth2Login().oauth2User(principalFor(account)))
            with(csrf())
            param("originalUrl", "https://op.gg/summoners/kr/Faker-KR1")
            param("domainPrefix", "op")
            param("alias", "dup-alias")
        }.andExpect { status { isOk() } }

        mockMvc.post("/links") {
            with(oauth2Login().oauth2User(principalFor(account)))
            with(csrf())
            param("originalUrl", "https://op.gg/summoners/kr/OtherPage")
            param("domainPrefix", "op")
            param("alias", "dup-alias")
        }.andExpect {
            status { isOk() }
            content { string(containsString("이미 사용 중")) }
        }

        assertEquals(
            1,
            shortLinkRepository.findAll().count { it.account.id == account.id && it.alias == "dup-alias" },
        )
    }

    @Test
    fun `같은 원본 URL을 다른 별칭으로 다시 단축할 수 있다`() {
        val account = accountWithHandle("sl-sub-5", "sl-5@example.com", "sl-handle-5")
        val originalUrl = "https://op.gg/summoners/kr/Faker-KR1"

        mockMvc.post("/links") {
            with(oauth2Login().oauth2User(principalFor(account)))
            with(csrf())
            param("originalUrl", originalUrl)
            param("alias", "alias-one")
        }.andExpect { status { isOk() } }

        mockMvc.post("/links") {
            with(oauth2Login().oauth2User(principalFor(account)))
            with(csrf())
            param("originalUrl", originalUrl)
            param("alias", "alias-two")
        }.andExpect { status { isOk() } }

        assertTrue(shortLinkRepository.existsByAccountAndDomainPrefixAndAliasAndDeletedFalse(account, "op", "alias-one"))
        assertTrue(shortLinkRepository.existsByAccountAndDomainPrefixAndAliasAndDeletedFalse(account, "op", "alias-two"))
    }
}
