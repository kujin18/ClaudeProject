package com.back.web

import com.back.domain.account.AccountRepository
import com.back.security.AppOAuth2User
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
class HandleController(
    private val accountRepository: AccountRepository,
) {

    @GetMapping("/setup-handle")
    fun showForm(
        @AuthenticationPrincipal principal: AppOAuth2User,
    ): String {
        val account = accountRepository.findById(principal.accountId).orElseThrow()
        if (account.hasHandle()) return "redirect:/"
        return "setup-handle"
    }

    @PostMapping("/setup-handle")
    fun submit(
        @AuthenticationPrincipal principal: AppOAuth2User,
        @RequestParam handle: String,
        model: Model,
    ): String {
        val account = accountRepository.findById(principal.accountId).orElseThrow()
        if (account.hasHandle()) return "redirect:/"

        val trimmed = handle.trim()
        if (trimmed.isBlank()) {
            model.addAttribute("error", "핸들을 입력해주세요.")
            return "setup-handle"
        }
        if (accountRepository.existsByHandle(trimmed)) {
            model.addAttribute("error", "이미 사용 중인 핸들입니다.")
            model.addAttribute("handle", trimmed)
            return "setup-handle"
        }

        account.assignHandle(trimmed)
        try {
            accountRepository.save(account)
        } catch (e: DataIntegrityViolationException) {
            model.addAttribute("error", "이미 사용 중인 핸들입니다.")
            model.addAttribute("handle", trimmed)
            return "setup-handle"
        }
        return "redirect:/"
    }
}
