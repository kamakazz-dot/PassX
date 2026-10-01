package ru.kk.passx

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.kk.passx.domain.generator.PasswordGenerator

class PasswordGeneratorTest {

    @Test
    fun testGeneratePasswordLength() {
        val config = PasswordGenerator.GeneratorConfig(length = 20)
        val password = PasswordGenerator.generate(config)
        assertEquals(20, password.length)
    }

    @Test
    fun testGeneratePasswordCharacterTypes() {
        val config = PasswordGenerator.GeneratorConfig(
            length = 16,
            useUppercase = true,
            useLowercase = true,
            useDigits = true,
            useSymbols = true
        )
        val password = PasswordGenerator.generate(config)

        assertTrue(password.any { it.isUpperCase() })
        assertTrue(password.any { it.isLowerCase() })
        assertTrue(password.any { it.isDigit() })
        assertTrue(password.any { "!@#$%^&*()_+-=[]{}|;:,.<>?".contains(it) })
    }

    @Test
    fun testEvaluateStrength() {
        val weak = PasswordGenerator.evaluateStrength("12345")
        val strong = PasswordGenerator.evaluateStrength("P@ssw0rd12345678!")

        assertEquals(PasswordGenerator.Strength.WEAK, weak)
        assertEquals(PasswordGenerator.Strength.STRONG, strong)
    }
}
