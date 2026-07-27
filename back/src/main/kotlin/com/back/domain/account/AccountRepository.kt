package com.back.domain.account

import org.springframework.data.jpa.repository.JpaRepository

interface AccountRepository : JpaRepository<Account, Long> {
    fun findByGoogleSubject(googleSubject: String): Account?
    fun existsByHandle(handle: String): Boolean
}
