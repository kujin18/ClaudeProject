package com.back.web

import com.back.domain.shortlink.ShortLinkRepository
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.server.ResponseStatusException
import java.net.URI

@Controller
class RedirectController(
    private val shortLinkRepository: ShortLinkRepository,
) {

    @Transactional
    @GetMapping("/{handle}/{domainPrefix}/{alias}")
    fun redirect(
        @PathVariable handle: String,
        @PathVariable domainPrefix: String,
        @PathVariable alias: String,
    ): ResponseEntity<Void> {
        val shortLink = shortLinkRepository
            .findByAccount_HandleAndDomainPrefixAndAliasAndDeletedFalse(handle, domainPrefix, alias)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)

        shortLinkRepository.incrementClickCount(shortLink.id!!)

        return ResponseEntity.status(HttpStatus.FOUND)
            .location(URI.create(shortLink.originalUrl))
            .build()
    }
}
