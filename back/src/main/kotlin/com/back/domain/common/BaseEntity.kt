package com.back.domain.common

import jakarta.persistence.EntityListeners
import jakarta.persistence.MappedSuperclass
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class BaseEntity {

    @CreatedDate
    var createdDate: LocalDateTime = LocalDateTime.now()
        protected set

    @LastModifiedDate
    var modifiedDate: LocalDateTime = LocalDateTime.now()
        protected set
}
