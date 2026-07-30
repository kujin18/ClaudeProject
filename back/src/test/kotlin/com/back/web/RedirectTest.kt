package com.back.web

import com.back.domain.account.Account
import com.back.domain.account.AccountRepository
import com.back.domain.shortlink.ShortLink
import com.back.domain.shortlink.ShortLinkRepository
import com.back.support.AbstractIntegrationTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import kotlin.test.assertEquals

@AutoConfigureMockMvc
class RedirectTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val accountRepository: AccountRepository,
    private val shortLinkRepository: ShortLinkRepository,
) : AbstractIntegrationTest() {

    private fun accountWithHandle(subject: String, email: String, handle: String): Account =
        accountRepository.save(Account(googleSubject = subject, email = email).apply { assignHandle(handle) })

    @Test
    fun `존재하는 링크는 원본 URL로 302 리다이렉트되고 클릭수가 증가한다`() {
        val account = accountWithHandle("rd-sub-1", "rd-1@example.com", "rd-handle-1")
        val shortLink = shortLinkRepository.save(
            ShortLink(
                originalUrl = "https://op.gg/summoners/kr/Faker-KR1",
                domainPrefix = "op",
                alias = "Faker-KR1",
                account = account,
            ),
        )

        mockMvc.get("/rd-handle-1/op/Faker-KR1").andExpect {
            status { isFound() }
            header { string(HttpHeaders.LOCATION, "https://op.gg/summoners/kr/Faker-KR1") }
        }

        val reloaded = shortLinkRepository.findById(shortLink.id!!).orElseThrow()
        assertEquals(1, reloaded.clickCount)
    }

    @Test
    fun `클릭할 때마다 클릭수가 계속 증가한다`() {
        val account = accountWithHandle("rd-sub-2", "rd-2@example.com", "rd-handle-2")
        val shortLink = shortLinkRepository.save(
            ShortLink(
                originalUrl = "https://op.gg/summoners/kr/Faker-KR1",
                domainPrefix = "op",
                alias = "click-count",
                account = account,
            ),
        )

        repeat(3) {
            mockMvc.get("/rd-handle-2/op/click-count").andExpect { status { isFound() } }
        }

        val reloaded = shortLinkRepository.findById(shortLink.id!!).orElseThrow()
        assertEquals(3, reloaded.clickCount)
    }

    @Test
    fun `존재하지 않는 링크는 404를 반환한다`() {
        mockMvc.get("/no-such-handle/op/no-such-alias").andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun `로그인하지 않은 사용자도 리다이렉트를 사용할 수 있다`() {
        val account = accountWithHandle("rd-sub-3", "rd-3@example.com", "rd-handle-3")
        shortLinkRepository.save(
            ShortLink(
                originalUrl = "https://op.gg/summoners/kr/Faker-KR1",
                domainPrefix = "op",
                alias = "anon-access",
                account = account,
            ),
        )

        // 인증 없이(oauth2Login() 없이) 접속
        mockMvc.get("/rd-handle-3/op/anon-access").andExpect {
            status { isFound() }
        }
    }
}
