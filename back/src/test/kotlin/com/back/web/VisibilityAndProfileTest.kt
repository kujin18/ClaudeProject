package com.back.web

import com.back.domain.account.Account
import com.back.domain.account.AccountRepository
import com.back.domain.shortlink.ShortLink
import com.back.domain.shortlink.ShortLinkRepository
import com.back.domain.shortlink.Visibility
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
import kotlin.test.assertEquals

@AutoConfigureMockMvc
class VisibilityAndProfileTest @Autowired constructor(
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
    fun `새로 만든 링크는 기본적으로 비공개다`() {
        val account = accountWithHandle("vis-sub-1", "vis-1@example.com", "vis-handle-1")
        val shortLink = shortLinkRepository.save(
            ShortLink(originalUrl = "https://op.gg/a", domainPrefix = "op", alias = "default-visibility", account = account),
        )

        assertEquals(Visibility.PRIVATE, shortLink.visibility)
    }

    @Test
    fun `대시보드에서 공개로 전환하면 프로필 페이지에 보이고, 다시 비공개로 전환하면 사라진다`() {
        val account = accountWithHandle("vis-sub-2", "vis-2@example.com", "vis-handle-2")
        val shortLink = shortLinkRepository.save(
            ShortLink(originalUrl = "https://op.gg/a", domainPrefix = "op", alias = "toggle-alias", account = account),
        )

        mockMvc.get("/vis-handle-2").andExpect {
            content { string(not(containsString("toggle-alias"))) }
        }

        mockMvc.post("/links/${shortLink.id}/toggle-visibility") {
            with(oauth2Login().oauth2User(principalFor(account)))
            with(csrf())
        }.andExpect { status { is3xxRedirection() } }

        mockMvc.get("/vis-handle-2").andExpect {
            status { isOk() }
            content { string(containsString("toggle-alias")) }
        }

        mockMvc.post("/links/${shortLink.id}/toggle-visibility") {
            with(oauth2Login().oauth2User(principalFor(account)))
            with(csrf())
        }.andExpect { status { is3xxRedirection() } }

        mockMvc.get("/vis-handle-2").andExpect {
            content { string(not(containsString("toggle-alias"))) }
        }
    }

    @Test
    fun `존재하지 않는 핸들의 프로필 페이지는 404다`() {
        mockMvc.get("/no-such-handle-anywhere").andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun `dashboard 같은 예약된 이름은 핸들로 설정할 수 없다`() {
        val account = accountRepository.save(Account(googleSubject = "vis-sub-3", email = "vis-3@example.com"))

        mockMvc.post("/setup-handle") {
            with(oauth2Login().oauth2User(principalFor(account)))
            with(csrf())
            param("handle", "dashboard")
        }.andExpect {
            status { isOk() }
        }

        val reloaded = accountRepository.findById(account.id!!).orElseThrow()
        assertEquals(null, reloaded.handle)
    }

    @Test
    fun `대시보드와 setup-handle은 여전히 로그인 없이 접근할 수 없다`() {
        mockMvc.get("/dashboard").andExpect { status { is3xxRedirection() } }
        mockMvc.get("/setup-handle").andExpect { status { is3xxRedirection() } }
    }
}
