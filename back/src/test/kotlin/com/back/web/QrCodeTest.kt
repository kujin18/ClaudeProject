package com.back.web

import com.back.domain.account.Account
import com.back.domain.account.AccountRepository
import com.back.domain.shortlink.ShortLink
import com.back.domain.shortlink.ShortLinkRepository
import com.back.security.AppOAuth2User
import com.back.support.AbstractIntegrationTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.security.oauth2.core.user.DefaultOAuth2User
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import kotlin.test.assertTrue

@AutoConfigureMockMvc
class QrCodeTest @Autowired constructor(
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
    fun `본인 소유 링크의 QR 코드는 PNG 이미지로 응답한다`() {
        val account = accountWithHandle("qr-sub-1", "qr-1@example.com", "qr-handle-1")
        val shortLink = shortLinkRepository.save(
            ShortLink(originalUrl = "https://op.gg/a", domainPrefix = "op", alias = "qr-alias", account = account),
        )

        val result = mockMvc.get("/links/${shortLink.id}/qr-code") {
            with(oauth2Login().oauth2User(principalFor(account)))
        }.andExpect {
            status { isOk() }
            content { contentType(MediaType.IMAGE_PNG) }
        }.andReturn()

        val bytes = result.response.contentAsByteArray
        val pngMagicNumber = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47)
        assertTrue(bytes.size > pngMagicNumber.size)
        assertTrue(bytes.sliceArray(pngMagicNumber.indices).contentEquals(pngMagicNumber))
    }

    @Test
    fun `다른 계정 소유 링크의 QR 코드는 볼 수 없다`() {
        val owner = accountWithHandle("qr-sub-2", "qr-2@example.com", "qr-handle-2")
        val stranger = accountWithHandle("qr-sub-3", "qr-3@example.com", "qr-handle-3")
        val shortLink = shortLinkRepository.save(
            ShortLink(originalUrl = "https://op.gg/a", domainPrefix = "op", alias = "not-yours-qr", account = owner),
        )

        mockMvc.get("/links/${shortLink.id}/qr-code") {
            with(oauth2Login().oauth2User(principalFor(stranger)))
        }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun `로그인하지 않으면 QR 코드에 접근할 수 없다`() {
        val account = accountWithHandle("qr-sub-4", "qr-4@example.com", "qr-handle-4")
        val shortLink = shortLinkRepository.save(
            ShortLink(originalUrl = "https://op.gg/a", domainPrefix = "op", alias = "anon-qr", account = account),
        )

        mockMvc.get("/links/${shortLink.id}/qr-code").andExpect {
            status { is3xxRedirection() }
        }
    }
}
