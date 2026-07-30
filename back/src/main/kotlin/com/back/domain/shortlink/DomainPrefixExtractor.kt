package com.back.domain.shortlink

object DomainPrefixExtractor {

    fun extract(host: String): String {
        val labels = host.lowercase().split(".").filter { it.isNotBlank() }
        val withoutWww = if (labels.size > 2 && labels.first() == "www") labels.drop(1) else labels
        return if (withoutWww.size >= 2) withoutWww[withoutWww.size - 2] else withoutWww.first()
    }
}
