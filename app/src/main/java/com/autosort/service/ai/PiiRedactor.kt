package com.autosort.service.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Utility class to blindly redact highly sensitive financial PII from raw text.
 * Runs on Dispatchers.Default for performance.
 */
object PiiRedactor {

    // Regex to match credit card numbers in standard 4-group format (14-19 digits with optional separators)
    private val creditCardRegex = Regex("\\b\\d{4}[- ]?\\d{4}[- ]?\\d{4}[- ]?\\d{2,7}\\b")
    
    // Regex for standard US SSN (XXX-XX-XXXX) or similar 9-digit national IDs
    private val ssnRegex = Regex("\\b\\d{3}-\\d{2}-\\d{4}\\b")
    
    // Regex for bank account numbers preceded by account-related keywords
    private val bankAccountRegex = Regex("(?i)(?:account|acct|a/c)[#:\\s-]*\\d{9,18}\\b")

    suspend fun redact(input: String): String = withContext(Dispatchers.Default) {
        if (input.isBlank()) return@withContext input

        var redacted = input
        
        // 1. Redact Credit Cards first
        redacted = creditCardRegex.replace(redacted, "[REDACTED CARD]")
        
        // 2. Redact SSNs
        redacted = ssnRegex.replace(redacted, "[REDACTED SSN]")
        
        // 3. Redact Bank Account numbers (only when preceded by account keywords)
        redacted = bankAccountRegex.replace(redacted, "[REDACTED ACCOUNT]")

        return@withContext redacted
    }
}
