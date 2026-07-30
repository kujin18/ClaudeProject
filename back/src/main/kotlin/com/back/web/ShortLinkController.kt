package com.back.web

import com.back.domain.shortlink.ShortLinkCreationResult
import com.back.domain.shortlink.ShortLinkService
import com.back.security.AppOAuth2User
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
class ShortLinkController(
    private val shortLinkService: ShortLinkService,
) {

    @GetMapping("/links/new")
    fun newForm(): String = "links/new"

    @PostMapping("/links")
    fun create(
        @AuthenticationPrincipal principal: AppOAuth2User,
        @RequestParam originalUrl: String,
        @RequestParam(required = false) domainPrefix: String?,
        @RequestParam alias: String,
        model: Model,
    ): String {
        val result = shortLinkService.create(principal.accountId, originalUrl, domainPrefix, alias)
        return when (result) {
            is ShortLinkCreationResult.Success -> {
                val shortLink = result.shortLink
                model.addAttribute("originalUrl", shortLink.originalUrl)
                model.addAttribute(
                    "shortLinkPath",
                    "/${shortLink.account.handle}/${shortLink.domainPrefix}/${shortLink.alias}",
                )
                "links/created"
            }
            ShortLinkCreationResult.InvalidUrl -> {
                model.addAttribute("error", "올바른 URL을 입력해주세요 (http:// 또는 https://로 시작해야 합니다).")
                model.addAttribute("originalUrl", originalUrl)
                model.addAttribute("domainPrefix", domainPrefix)
                model.addAttribute("alias", alias)
                "links/new"
            }
            ShortLinkCreationResult.BlankAlias -> {
                model.addAttribute("error", "별칭을 입력해주세요.")
                model.addAttribute("originalUrl", originalUrl)
                model.addAttribute("domainPrefix", domainPrefix)
                "links/new"
            }
            ShortLinkCreationResult.AliasTaken -> {
                model.addAttribute("error", "이미 사용 중인 도메인축약어 + 별칭 조합입니다.")
                model.addAttribute("originalUrl", originalUrl)
                model.addAttribute("domainPrefix", domainPrefix)
                model.addAttribute("alias", alias)
                "links/new"
            }
        }
    }
}
