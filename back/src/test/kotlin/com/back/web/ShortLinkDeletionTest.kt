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
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import kotlin.test.assertTrue

@AutoConfigureMockMvc
class ShortLinkDeletionTest @Autowired constructor(
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
    fun `삭제하면 대시보드 목록에서 사라지고 리다이렉트는 404가 된다`() {
        val account = accountWithHandle("del-sub-1", "del-1@example.com", "del-handle-1")
        val shortLink = shortLinkRepository.save(
            ShortLink(
                originalUrl = "https://op.gg/summoners/kr/Faker-KR1",
                domainPrefix = "op",
                alias = "del-alias-1",
                account = account,
            ),
        )

        mockMvc.post("/links/${shortLink.id}/delete") {
            with(oauth2Login().oauth2User(principalFor(account)))
            with(csrf())
        }.andExpect { status { is3xxRedirection() } }

        mockMvc.get("/dashboard") {
            with(oauth2Login().oauth2User(principalFor(account)))
        }.andExpect {
            status { isOk() }
            content { string(not(containsString("del-alias-1"))) }
        }

        mockMvc.get("/del-handle-1/op/del-alias-1").andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun `삭제된 도메인축약어+별칭 조합은 같은 계정에서 새 원본 URL로 재사용할 수 있다`() {
        val account = accountWithHandle("del-sub-2", "del-2@example.com", "del-handle-2")
        val shortLink = shortLinkRepository.save(
            ShortLink(
                originalUrl = "https://op.gg/summoners/kr/Faker-KR1",
                domainPrefix = "op",
                alias = "reuse-alias",
                account = account,
            ),
        )

        mockMvc.post("/links/${shortLink.id}/delete") {
            with(oauth2Login().oauth2User(principalFor(account)))
            with(csrf())
        }.andExpect { status { is3xxRedirection() } }

        mockMvc.post("/links") {
            with(oauth2Login().oauth2User(principalFor(account)))
            with(csrf())
            param("originalUrl", "https://op.gg/summoners/kr/AnotherPlayer")
            param("domainPrefix", "op")
            param("alias", "reuse-alias")
        }.andExpect { status { isOk() } }

        assertTrue(
            shortLinkRepository.existsByAccountAndDomainPrefixAndAliasAndDeletedFalse(account, "op", "reuse-alias"),
        )
    }

    @Test
    fun `다른 계정 소유 링크는 삭제할 수 없다`() {
        val owner = accountWithHandle("del-sub-3", "del-3@example.com", "del-handle-3")
        val stranger = accountWithHandle("del-sub-4", "del-4@example.com", "del-handle-4")
        val shortLink = shortLinkRepository.save(
            ShortLink(
                originalUrl = "https://op.gg/summoners/kr/Faker-KR1",
                domainPrefix = "op",
                alias = "not-yours",
                account = owner,
            ),
        )

        mockMvc.post("/links/${shortLink.id}/delete") {
            with(oauth2Login().oauth2User(principalFor(stranger)))
            with(csrf())
        }.andExpect { status { isNotFound() } }

        assertTrue(
            shortLinkRepository.existsByAccountAndDomainPrefixAndAliasAndDeletedFalse(owner, "op", "not-yours"),
        )
    }
}
