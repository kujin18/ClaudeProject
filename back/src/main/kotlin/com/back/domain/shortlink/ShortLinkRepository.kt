package com.back.domain.shortlink

import com.back.domain.account.Account
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface ShortLinkRepository : JpaRepository<ShortLink, Long> {
    fun existsByAccountAndDomainPrefixAndAliasAndDeletedFalse(
        account: Account,
        domainPrefix: String,
        alias: String,
    ): Boolean

    fun findByAccount_HandleAndDomainPrefixAndAliasAndDeletedFalse(
        handle: String,
        domainPrefix: String,
        alias: String,
    ): ShortLink?

    fun findByAccountAndDeletedFalseOrderByCreatedDateDesc(account: Account): List<ShortLink>

    fun findByAccountAndVisibilityAndDeletedFalseOrderByCreatedDateDesc(
        account: Account,
        visibility: Visibility,
    ): List<ShortLink>

    fun findByIdAndAccount_Id(id: Long, accountId: Long): ShortLink?

    @Modifying
    @Query("update ShortLink s set s.clickCount = s.clickCount + 1 where s.id = :id")
    fun incrementClickCount(id: Long): Int
}
