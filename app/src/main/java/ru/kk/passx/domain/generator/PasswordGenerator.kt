package ru.kk.passx.domain.generator

import java.security.SecureRandom

/**
 * Генератор стойких паролей с настройкой параметров и оценкой надежности.
 */
object PasswordGenerator {

    private const val UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    private const val LOWERCASE = "abcdefghijklmnopqrstuvwxyz"
    private const val DIGITS = "0123456789"
    private const val SYMBOLS = "!@#$%^&*()_+-=[]{}|;:,.<>?"

    enum class Strength(val label: String, val score: Int) {
        WEAK("Слабый", 1),
        MEDIUM("Средний", 2),
        STRONG("Надежный", 3)
    }

    data class GeneratorConfig(
        val length: Int = 16,
        val useUppercase: Boolean = true,
        val useLowercase: Boolean = true,
        val useDigits: Boolean = true,
        val useSymbols: Boolean = true
    )

    fun generate(config: GeneratorConfig): String {
        val charPool = StringBuilder().apply {
            if (config.useUppercase) append(UPPERCASE)
            if (config.useLowercase) append(LOWERCASE)
            if (config.useDigits) append(DIGITS)
            if (config.useSymbols) append(SYMBOLS)
        }.toString()

        if (charPool.isEmpty()) return ""

        val random = SecureRandom()
        val result = StringBuilder()

        // Гарантируем наличие хотя бы одного символа каждого выбранного типа
        val mandatoryChars = mutableListOf<Char>()
        if (config.useUppercase) mandatoryChars.add(UPPERCASE[random.nextInt(UPPERCASE.length)])
        if (config.useLowercase) mandatoryChars.add(LOWERCASE[random.nextInt(LOWERCASE.length)])
        if (config.useDigits) mandatoryChars.add(DIGITS[random.nextInt(DIGITS.length)])
        if (config.useSymbols) mandatoryChars.add(SYMBOLS[random.nextInt(SYMBOLS.length)])

        // Заполняем оставшуюся длину
        val remainingLength = (config.length - mandatoryChars.size).coerceAtLeast(0)
        for (i in 0 until remainingLength) {
            result.append(charPool[random.nextInt(charPool.length)])
        }

        mandatoryChars.forEach { result.append(it) }

        // Перемешиваем символы
        val chars = result.toString().toCharArray()
        for (i in chars.indices) {
            val j = random.nextInt(chars.size)
            val temp = chars[i]
            chars[i] = chars[j]
            chars[j] = temp
        }

        return String(chars)
    }

    /**
     * Оценивает уровень надежности пароля.
     */
    fun evaluateStrength(password: String): Strength {
        if (password.length < 8) return Strength.WEAK

        var score = 0
        if (password.length >= 12) score += 1
        if (password.length >= 16) score += 1
        if (password.any { it in UPPERCASE }) score += 1
        if (password.any { it in LOWERCASE }) score += 1
        if (password.any { it in DIGITS }) score += 1
        if (password.any { it in SYMBOLS }) score += 1

        return when {
            score <= 3 -> Strength.WEAK
            score <= 4 -> Strength.MEDIUM
            else -> Strength.STRONG
        }
    }
}
