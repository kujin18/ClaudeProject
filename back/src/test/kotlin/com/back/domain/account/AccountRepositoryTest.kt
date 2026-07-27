package com.back.domain.account

import com.back.support.AbstractIntegrationTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import kotlin.test.assertFailsWith

class AccountRepositoryTest @Autowired constructor(
    private val accountRepository: AccountRepository,
) : AbstractIntegrationTest() {

    @Test
    fun `같은 핸들을 가진 두 계정은 저장할 수 없다`() {
        accountRepository.save(
            Account(googleSubject = "repo-test-sub-a", email = "repo-test-a@example.com")
                .apply { assignHandle("repo-test-duplicate-handle") },
        )
        val second = Account(googleSubject = "repo-test-sub-b", email = "repo-test-b@example.com")
            .apply { assignHandle("repo-test-duplicate-handle") }

        assertFailsWith<DataIntegrityViolationException> {
            accountRepository.saveAndFlush(second)
        }
    }
}
