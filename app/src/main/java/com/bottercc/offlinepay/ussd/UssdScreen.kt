package com.bottercc.offlinepay.ussd

data class MenuOption(val key: String, val label: String)

/**
 * One USSD message as shown in the system dialog, split into the free-text prompt and
 * numbered menu options ("1. Send Money", "2.Request Money", ...).
 */
data class UssdScreen(
    val text: String,
    val prompt: String,
    val options: List<MenuOption>,
    val hasInput: Boolean,
) {
    /** First option matching the keywords, trying keywords in priority order. Navigation entries are ignored. */
    fun option(vararg keywords: String): MenuOption? {
        val real = options.filterNot { o -> NAV_WORDS.any { o.label.contains(it, ignoreCase = true) } }
        for (k in keywords) {
            real.firstOrNull { it.label.contains(k, ignoreCase = true) }?.let { return it }
        }
        return null
    }

    fun asks(vararg keywords: String): Boolean = keywords.any { prompt.contains(it, ignoreCase = true) }

    /** Position of the first keyword in the prompt, or Int.MAX_VALUE. */
    fun indexOf(vararg keywords: String): Int =
        keywords.map { prompt.indexOf(it, ignoreCase = true) }.filter { it >= 0 }.minOrNull() ?: Int.MAX_VALUE

    /** Digit the menu says to send to skip an optional field, e.g. "Enter remark (1 to skip)". */
    fun skipKey(): String? =
        SKIP_BEFORE.find(text)?.groupValues?.get(1) ?: SKIP_AFTER.find(text)?.groupValues?.get(1)

    companion object {
        private val NAV_WORDS = listOf("back", "main menu", "exit", "home")
        private val OPTION = Regex("""^\s*(\d{1,2})\s*[.):\-]?\s*([^\d\s].*)$""")
        private val INLINE_SPLIT = Regex("""\s+(?=\d{1,2}[.)]\s*[A-Za-z])""")
        private val SKIP_BEFORE = Regex("""(\d)\s*(?:to|for|-|=|:)?\s*skip""", RegexOption.IGNORE_CASE)
        private val SKIP_AFTER = Regex("""skip\s*(?:-|=|:|press|enter|send)?\s*(\d)""", RegexOption.IGNORE_CASE)

        fun parse(text: String, hasInput: Boolean): UssdScreen {
            val options = mutableListOf<MenuOption>()
            val prompt = StringBuilder()
            text.replace(INLINE_SPLIT, "\n").lines().map { it.trim() }.filter { it.isNotEmpty() }.forEach { line ->
                val m = OPTION.find(line)
                if (m != null) options += MenuOption(m.groupValues[1], m.groupValues[2].trim())
                else prompt.appendLine(line)
            }
            return UssdScreen(text.trim(), prompt.toString().trim(), options, hasInput)
        }
    }
}
