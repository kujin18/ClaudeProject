package com.back.domain.shortlink

import com.back.domain.account.AccountRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.net.URI
import java.net.URISyntaxException

@Service
class ShortLinkService(
    private val shortLinkRepository: ShortLinkRepository,
    private val accountRepository: AccountRepository,
) {

    @Transactional
    fun create(
        accountId: Long,
        originalUrl: String,
        domainPrefixOverride: String?,
        alias: String,
    ): ShortLinkCreationResult {
        val trimmedUrl = originalUrl.trim()
        val host = parseHost(trimmedUrl) ?: return ShortLinkCreationResult.InvalidUrl

        val trimmedAlias = alias.trim()
        if (trimmedAlias.isBlank()) return ShortLinkCreationResult.BlankAlias

        val prefix = domainPrefixOverride?.trim()?.takeIf { it.isNotBlank() }
            ?: DomainPrefixExtractor.extract(host)

        val account = accountRepository.findById(accountId).orElseThrow()
        if (shortLinkRepository.existsByAccountAndDomainPrefixAndAliasAndDeletedFalse(account, prefix, trimmedAlias)) {
            return ShortLinkCreationResult.AliasTaken
        }

        val shortLink = ShortLink(
            originalUrl = trimmedUrl,
            domainPrefix = prefix,
            alias = trimmedAlias,
            account = account,
        )
        return try {
            ShortLinkCreationResult.Success(shortLinkRepository.save(shortLink))
        } catch (e: DataIntegrityViolationException) {
            ShortLinkCreationResult.AliasTaken
        }
    }

    @Transactional
    fun delete(accountId: Long, shortLinkId: Long): ShortLinkDeletionResult {
        val shortLink = shortLinkRepository.findByIdAndAccount_Id(shortLinkId, accountId)
            ?: return ShortLinkDeletionResult.NotFound
        shortLink.markDeleted()
        shortLinkRepository.save(shortLink)
        return ShortLinkDeletionResult.Success
    }

    private fun parseHost(url: String): String? {
        val uri = try {
            URI(url)
        } catch (e: URISyntaxException) {
            return null
        }
        if (uri.scheme !in setOf("http", "https")) return null
        return uri.host?.takeIf { it.isNotBlank() }
    }
}

sealed interface ShortLinkCreationResult {
    data class Success(val shortLink: ShortLink) : ShortLinkCreationResult
    data object InvalidUrl : ShortLinkCreationResult
    data object BlankAlias : ShortLinkCreationResult
    data object AliasTaken : ShortLinkCreationResult
}

sealed interface ShortLinkDeletionResult {
    data object Success : ShortLinkDeletionResult
    data object NotFound : ShortLinkDeletionResult
}
