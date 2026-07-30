package com.back.web

import com.back.domain.account.AccountRepository
import com.back.domain.shortlink.ShortLinkRepository
import com.back.domain.shortlink.Visibility
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.server.ResponseStatusException
import java.time.format.DateTimeFormatter

private val CREATED_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

@Controller
class ProfileController(
    private val accountRepository: AccountRepository,
    private val shortLinkRepository: ShortLinkRepository,
) {

    @GetMapping("/{handle}")
    fun profile(
        @PathVariable handle: String,
        model: Model,
    ): String {
        val account = accountRepository.findByHandle(handle) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)

        val rows = shortLinkRepository
            .findByAccountAndVisibilityAndDeletedFalseOrderByCreatedDateDesc(account, Visibility.PUBLIC)
            .map {
                ProfileLinkRow(
                    originalUrl = it.originalUrl,
                    shortLinkPath = "/${account.handle}/${it.domainPrefix}/${it.alias}",
                    createdDate = CREATED_DATE_FORMAT.format(it.createdDate),
                )
            }

        model.addAttribute("handle", account.handle)
        model.addAttribute("links", rows)
        return "profile"
    }
}

data class ProfileLinkRow(
    val originalUrl: String,
    val shortLinkPath: String,
    val createdDate: String,
)
