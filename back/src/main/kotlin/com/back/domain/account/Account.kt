package com.back.domain.account

import com.back.domain.common.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "account",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_account_handle", columnNames = ["handle"]),
        UniqueConstraint(name = "uk_account_google_subject", columnNames = ["google_subject"]),
    ],
)
class Account(
    @Column(name = "google_subject", nullable = false, updatable = false)
    val googleSubject: String,
    @Column(nullable = false)
    val email: String,
) : BaseEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    @Column
    var handle: String? = null
        protected set

    fun hasHandle(): Boolean = handle != null

    fun assignHandle(handle: String) {
        check(!hasHandle()) { "handle is already set" }
        this.handle = handle
    }
}
