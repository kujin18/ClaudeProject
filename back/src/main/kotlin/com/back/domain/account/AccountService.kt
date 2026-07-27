package com.back.domain.account

import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AccountService(
    private val accountRepository: AccountRepository,
) {

    fun findById(accountId: Long): Account = accountRepository.findById(accountId).orElseThrow()

    @Transactional
    fun assignHandle(accountId: Long, handle: String): HandleAssignmentResult {
        val trimmed = handle.trim()
        if (trimmed.isBlank()) return HandleAssignmentResult.Blank

        val account = findById(accountId)
        if (account.hasHandle()) return HandleAssignmentResult.AlreadyAssigned
        if (accountRepository.existsByHandle(trimmed)) return HandleAssignmentResult.HandleTaken

        account.assignHandle(trimmed)
        return try {
            accountRepository.save(account)
            HandleAssignmentResult.Success
        } catch (e: DataIntegrityViolationException) {
            HandleAssignmentResult.HandleTaken
        }
    }
}

sealed interface HandleAssignmentResult {
    data object Success : HandleAssignmentResult
    data object AlreadyAssigned : HandleAssignmentResult
    data object Blank : HandleAssignmentResult
    data object HandleTaken : HandleAssignmentResult
}
