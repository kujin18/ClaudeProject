package com.back.domain.shortlink

import com.back.domain.account.Account
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface ShortLinkRepository : JpaRepository<ShortLink, Long> {
    fun existsByAccountAndDomainPrefixAndAlias(account: Account, domainPrefix: String, alias: String): Boolean

    fun findByAccount_HandleAndDomainPrefixAndAlias(handle: String, domainPrefix: String, alias: String): ShortLink?

    @Modifying
    @Query("update ShortLink s set s.clickCount = s.clickCount + 1 where s.id = :id")
    fun incrementClickCount(id: Long): Int
}
