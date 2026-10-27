package com.motovault.app.ui

object LaunchConfig {
    var initialTab: String? = null
    var initialSheet: String? = null
    var initialOnboarding: Boolean = false
    var initialSeed: Boolean = false
    var scrollToBottom: Boolean = false

    fun applyArgs(args: List<String>) {
        var i = 0
        while (i < args.size) {
            when (args[i]) {
                "--tab" -> {
                    if (i + 1 < args.size) {
                        initialTab = args[i + 1]
                        i++
                    }
                }
                "--sheet" -> {
                    if (i + 1 < args.size) {
                        initialSheet = args[i + 1]
                        i++
                    }
                }
                "--onboarding" -> {
                    initialOnboarding = true
                }
                "--seed" -> {
                    initialSeed = true
                }
                "--scroll-bottom" -> {
                    scrollToBottom = true
                }
                "--none" -> {
                    initialSheet = null
                }
            }
            i++
        }
    }
}
