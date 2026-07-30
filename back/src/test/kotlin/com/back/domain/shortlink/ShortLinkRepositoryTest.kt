package com.back.domain.shortlink

import com.back.domain.account.Account
import com.back.domain.account.AccountRepository
import com.back.support.AbstractIntegrationTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import kotlin.test.assertFailsWith

class ShortLinkRepositoryTest @Autowired constructor(
    private val accountRepository: AccountRepository,
    private val shortLinkRepository: ShortLinkRepository,
) : AbstractIntegrationTest() {

    private fun accountWithHandle(subject: String, email: String, handle: String): Account =
        accountRepository.save(Account(googleSubject = subject, email = email).apply { assignHandle(handle) })

    @Test
    fun `같은 계정+도메인축약어+별칭 조합은 활성 상태로 두 개 저장할 수 없다`() {
        val account = accountWithHandle("repo-sl-sub-1", "repo-sl-1@example.com", "repo-sl-handle-1")
        shortLinkRepository.saveAndFlush(
            ShortLink(originalUrl = "https://op.gg/a", domainPrefix = "op", alias = "dup", account = account),
        )

        assertFailsWith<DataIntegrityViolationException> {
            shortLinkRepository.saveAndFlush(
                ShortLink(originalUrl = "https://op.gg/b", domainPrefix = "op", alias = "dup", account = account),
            )
        }
    }

    @Test
    fun `삭제된 레코드가 있으면 같은 조합으로 새 레코드를 저장할 수 있다`() {
        val account = accountWithHandle("repo-sl-sub-2", "repo-sl-2@example.com", "repo-sl-handle-2")
        val first = shortLinkRepository.saveAndFlush(
            ShortLink(originalUrl = "https://op.gg/a", domainPrefix = "op", alias = "reuse", account = account),
        )
        first.markDeleted()
        shortLinkRepository.saveAndFlush(first)

        shortLinkRepository.saveAndFlush(
            ShortLink(originalUrl = "https://op.gg/b", domainPrefix = "op", alias = "reuse", account = account),
        )
    }
}
