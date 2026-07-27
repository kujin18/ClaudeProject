package com.back.domain.shortlink

import com.back.domain.account.Account
import com.back.domain.common.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "short_link",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_short_link_account_prefix_alias",
            columnNames = ["account_id", "domain_prefix", "alias"],
        ),
    ],
)
class ShortLink(
    @Column(name = "original_url", nullable = false, updatable = false)
    val originalUrl: String,
    @Column(name = "domain_prefix", nullable = false, updatable = false)
    val domainPrefix: String,
    @Column(nullable = false, updatable = false)
    val alias: String,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false, updatable = false)
    val account: Account,
) : BaseEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set
}
