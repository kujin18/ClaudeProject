package com.back.domain.shortlink

import com.back.domain.account.Account
import org.springframework.data.jpa.repository.JpaRepository

interface ShortLinkRepository : JpaRepository<ShortLink, Long> {
    fun existsByAccountAndDomainPrefixAndAlias(account: Account, domainPrefix: String, alias: String): Boolean
}
