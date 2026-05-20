package com.autosort.data.config

/**
 * ╔══════════════════════════════════════════════════════════════════╗
 * ║          EXTERNAL CONFIGURATION — CHANGE THESE LINES           ║
 * ╠══════════════════════════════════════════════════════════════════╣
 * ║                                                                ║
 * ║  To activate AI features, change these 2 values:               ║
 * ║    1. Set your Gemini API key (get it from ai.google.dev)      ║
 * ║    2. Set AI_ENABLED = true                                    ║
 * ║                                                                ║
 * ╚══════════════════════════════════════════════════════════════════╝
 */
object ExternalConfig {

    // ── LINE 1: Groq API Key securely injected from local.properties ──
    val GROQ_API_KEY = com.autosort.BuildConfig.GROQ_API_KEY

    // ── LINE 2: Set to true when you have your API key ────────────────
    const val AI_ENABLED = true

    // ── Derived (do not change) ───────────────────────────────────────
    val isAiReady: Boolean
        get() = AI_ENABLED && GROQ_API_KEY.isNotBlank() && GROQ_API_KEY != "YOUR_API_KEY_HERE"

    // ── AI Usage Limits ───────────────────────────────────────────────
    const val MAX_AI_SCANS_PER_MONTH = 10
    const val AI_MODEL_NAME = "llama-3.1-8b-instant"

    // ── Sensitive file keywords for the Privacy Scanner ───────────────
    val SENSITIVE_KEYWORDS = listOf(
        "tax", "medical", "bank", "password", "passport",
        "aadhaar", "aadhar", "pan", "salary", "payslip",
        "ssn", "insurance", "health", "credit"
    )

    // ── Granular Ad Monetization Architecture ─────────────────────────
    object AdConfig {
        // 1. Global Kill Switch (Overrides everything if false)
        const val GLOBAL_ADS_ENABLED = true

        // 2. Screen-Level Toggles
        const val SHOW_DASHBOARD_BANNER = true
        const val SHOW_SETTINGS_BANNER = false
        const val REQUIRE_AD_FOR_AI_SCAN = true

        // 3. Specific Placement Links (Google Test IDs)
        // Swap these to change specific ads on specific screens
        const val ID_BANNER_DASHBOARD = "ca-app-pub-3940256099942544/6300978111"
        const val ID_BANNER_SETTINGS = "ca-app-pub-3940256099942544/6300978111"
        const val ID_REWARDED_AI_SCAN = "ca-app-pub-3940256099942544/5224354917"
    }
}

