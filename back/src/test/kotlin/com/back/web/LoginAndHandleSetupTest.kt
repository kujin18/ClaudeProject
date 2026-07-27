package com.back.web

import com.back.domain.account.Account
import com.back.domain.account.AccountRepository
import com.back.security.AppOAuth2User
import com.back.support.AbstractIntegrationTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.security.oauth2.core.user.DefaultOAuth2User
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import kotlin.test.assertEquals
import kotlin.test.assertNull

@AutoConfigureMockMvc
class LoginAndHandleSetupTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val accountRepository: AccountRepository,
) : AbstractIntegrationTest() {

    private fun oauth2UserFor(account: Account): OAuth2User =
        DefaultOAuth2User(
            emptyList(),
            mapOf("sub" to account.googleSubject, "email" to account.email),
            "sub",
        )

    private fun principalFor(account: Account) =
        AppOAuth2User(oauth2UserFor(account), account.id!!, account.handle)

    @Test
    fun `로그인 안 한 사용자는 홈 화면을 볼 수 있다`() {
        mockMvc.get("/").andExpect {
            status { isOk() }
        }
    }

    @Test
    fun `로그인 안 한 사용자는 setup-handle에 접근할 수 없다`() {
        mockMvc.get("/setup-handle").andExpect {
            status { is3xxRedirection() }
        }
    }

    @Test
    fun `핸들이 없는 신규 계정은 setup-handle 폼을 볼 수 있다`() {
        val account = accountRepository.save(Account(googleSubject = "sub-1", email = "a@example.com"))

        mockMvc.get("/setup-handle") {
            with(oauth2Login().oauth2User(principalFor(account)))
        }.andExpect {
            status { isOk() }
        }
    }

    @Test
    fun `유효한 핸들을 제출하면 저장되고 홈으로 리다이렉트된다`() {
        val account = accountRepository.save(Account(googleSubject = "sub-2", email = "b@example.com"))

        mockMvc.post("/setup-handle") {
            with(oauth2Login().oauth2User(principalFor(account)))
            param("handle", "kujin")
        }.andExpect {
            status { is3xxRedirection() }
            redirectedUrl("/")
        }

        val saved = accountRepository.findById(account.id!!).orElseThrow()
        assertEquals("kujin", saved.handle)
    }

    @Test
    fun `이미 사용 중인 핸들은 거부되고 폼이 다시 보인다`() {
        accountRepository.save(
            Account(googleSubject = "sub-taken", email = "taken@example.com").apply { assignHandle("kujin") },
        )
        val account = accountRepository.save(Account(googleSubject = "sub-3", email = "c@example.com"))

        mockMvc.post("/setup-handle") {
            with(oauth2Login().oauth2User(principalFor(account)))
            param("handle", "kujin")
        }.andExpect {
            status { isOk() }
        }

        val saved = accountRepository.findById(account.id!!).orElseThrow()
        assertNull(saved.handle)
    }

    @Test
    fun `이미 핸들이 있는 계정은 setup-handle 접속 시 홈으로 리다이렉트된다`() {
        val account = accountRepository.save(
            Account(googleSubject = "sub-4", email = "d@example.com").apply { assignHandle("already") },
        )

        mockMvc.get("/setup-handle") {
            with(oauth2Login().oauth2User(principalFor(account)))
        }.andExpect {
            status { is3xxRedirection() }
            redirectedUrl("/")
        }
    }
}
