package com.back.web

import com.back.domain.account.AccountService
import com.back.domain.account.HandleAssignmentResult
import com.back.security.AppOAuth2User
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
class HandleController(
    private val accountService: AccountService,
) {

    @GetMapping("/setup-handle")
    fun showForm(
        @AuthenticationPrincipal principal: AppOAuth2User,
    ): String {
        val account = accountService.findById(principal.accountId)
        if (account.hasHandle()) return "redirect:/"
        return "setup-handle"
    }

    @PostMapping("/setup-handle")
    fun submit(
        @AuthenticationPrincipal principal: AppOAuth2User,
        @RequestParam handle: String,
        model: Model,
    ): String = when (accountService.assignHandle(principal.accountId, handle)) {
        HandleAssignmentResult.Success, HandleAssignmentResult.AlreadyAssigned -> "redirect:/"
        HandleAssignmentResult.Blank -> {
            model.addAttribute("error", "핸들을 입력해주세요.")
            "setup-handle"
        }
        HandleAssignmentResult.HandleTaken -> {
            model.addAttribute("error", "이미 사용 중인 핸들입니다.")
            model.addAttribute("handle", handle.trim())
            "setup-handle"
        }
    }
}
