package com.back.web

import com.back.domain.account.AccountService
import com.back.domain.shortlink.ShortLinkRepository
import com.back.security.AppOAuth2User
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import java.time.format.DateTimeFormatter

private val CREATED_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

@Controller
class DashboardController(
    private val accountService: AccountService,
    private val shortLinkRepository: ShortLinkRepository,
) {

    @GetMapping("/dashboard")
    fun dashboard(
        @AuthenticationPrincipal principal: AppOAuth2User,
        model: Model,
    ): String {
        val account = accountService.findById(principal.accountId)
        val rows = shortLinkRepository.findByAccountOrderByCreatedDateDesc(account).map {
            ShortLinkRow(
                originalUrl = it.originalUrl,
                shortLinkPath = "/${account.handle}/${it.domainPrefix}/${it.alias}",
                createdDate = CREATED_DATE_FORMAT.format(it.createdDate),
                clickCount = it.clickCount,
            )
        }
        model.addAttribute("links", rows)
        return "dashboard"
    }
}

data class ShortLinkRow(
    val originalUrl: String,
    val shortLinkPath: String,
    val createdDate: String,
    val clickCount: Long,
)
