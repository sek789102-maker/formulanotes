package com.sek789102.jotwai

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.sek789102.jotwai.data.model.ThemeMode
import com.sek789102.jotwai.domain.RichText
import com.sek789102.jotwai.domain.TextEditing
import com.sek789102.jotwai.ui.components.FormatToolbar
import com.sek789102.jotwai.ui.theme.JotwaiTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Drives the real formatting toolbar + editor field like a user, and saves screenshots to build/rich-screens. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w400dp-h860dp-xxhdpi")
class RichEditorUiTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private var value by mutableStateOf(TextFieldValue(""))

    private fun setUp(initial: String = "", dark: Boolean = true) {
        value = TextFieldValue(initial, TextRange(initial.length))
        rule.setContent {
            JotwaiTheme(themeMode = if (dark) ThemeMode.DARK else ThemeMode.LIGHT) {
                val scheme = MaterialTheme.colorScheme
                val transformation = remember(scheme) {
                    RichText.Transformation(
                        RichText.Palette(scheme.primary, Color(0x80FFD54F), scheme.onSurfaceVariant, scheme.onSurface.copy(alpha = 0.08f), scheme.onSurfaceVariant, scheme.outlineVariant)
                    )
                }
                Column(Modifier.background(scheme.surface)) {
                    BasicTextField(
                        value = value,
                        onValueChange = { value = TextEditing.continueList(value, RichText.fixDeletion(value, it)) },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = scheme.onSurface, lineHeight = androidx.compose.ui.unit.TextUnit.Unspecified),
                        visualTransformation = transformation,
                        cursorBrush = SolidColor(scheme.primary),
                        modifier = Modifier.fillMaxWidth().padding(20.dp).testTag("body")
                    )
                    FormatToolbar(value) { t -> value = t(value) }
                }
            }
        }
    }

    private fun shot(name: String) {
        val dir = File("build/rich-screens").apply { mkdirs() }
        rule.waitForIdle()
        val view = rule.activity.window.decorView
        val bmp = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(android.graphics.Canvas(bmp))
        File(dir, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun typingWithToolbarProducesHiddenMarkers() {
        setUp()
        rule.onNodeWithTag("body").performTextInput("สวัสดี ")
        rule.onNodeWithTag("fmt_bold").performClick()
        rule.onNodeWithTag("body").performTextInput("ตัวหนา")
        rule.onNodeWithTag("fmt_bold").performClick() // leave bold
        rule.onNodeWithTag("body").performTextInput(" ปกติ")
        assertEquals("สวัสดี **ตัวหนา** ปกติ", value.text)

        // Colour the word "ปกติ"
        val s = value.text.indexOf("ปกติ")
        value = value.copy(selection = TextRange(s, s + 4))
        rule.onNodeWithTag("fmt_color").performClick()
        rule.onNodeWithTag("color_E53935").performClick()
        assertEquals("สวัสดี **ตัวหนา** ⟦c:E53935⟧ปกติ⟦/c⟧", value.text)
        assertEquals("สวัสดี ตัวหนา ปกติ", RichText.parse(value.text).display)
        rule.waitForIdle()
        shot("01-typing-color-panel")
    }

    @Test fun sizeAndFontButtonsWorkWithoutASelection() {
        setUp()
        rule.onNodeWithTag("body").performTextInput("ปกติ ")
        rule.onNodeWithTag("fmt_bigger").performClick()
        rule.onNodeWithTag("fmt_bigger").performClick()
        rule.onNodeWithTag("body").performTextInput("ใหญ่")
        assertEquals("ปกติ ⟦z:1.25⟧ใหญ่⟦/z⟧", value.text)
        assertEquals("ปกติ ใหญ่", RichText.parse(value.text).display)

        // Size chip on a selection
        val s = value.text.indexOf("ปกติ")
        value = value.copy(selection = TextRange(s, s + 4))
        rule.onNodeWithTag("fmt_size").performClick()
        rule.onNodeWithTag("size_10").performClick()
        assertTrue(value.text.startsWith("⟦z:0.625⟧ปกติ⟦/z⟧"))

        // Font at a bare cursor, then typing
        value = value.copy(selection = TextRange(value.text.length))
        rule.onNodeWithTag("fmt_font").performScrollTo().performClick()
        rule.onNodeWithTag("font_hand").performScrollTo().performClick()
        rule.onNodeWithTag("body").performTextInput("มือ")
        assertTrue(value.text.endsWith("⟦f:hand⟧มือ⟦/f⟧"))
        assertFalse(RichText.parse(value.text).display.contains("⟦"))
    }

    @Test fun showcase() {
        setUp(
            "# หัวข้อใหญ่\n" +
                "## หัวข้อกลาง\n" +
                "**ตัวหนา** *ตัวเอียง* ⟦u⟧ขีดเส้นใต้⟦/u⟧ ~~ขีดฆ่า~~\n" +
                "⟦c:E53935⟧สีแดง⟦/c⟧ ⟦c:1E88E5⟧สีน้ำเงิน⟦/c⟧ ⟦c:43A047⟧สีเขียว⟦/c⟧ ==ไฮไลต์== ⟦h:90CAF9⟧ไฮไลต์ฟ้า⟦/h⟧\n" +
                "ขนาด ⟦z:0.75⟧เล็ก⟦/z⟧ ปกติ ⟦z:1.5⟧ใหญ่⟦/z⟧ ⟦z:2.5⟧ใหญ่มาก⟦/z⟧\n" +
                "⟦f:serif⟧ฟอนต์มีหัว⟦/f⟧ ⟦f:mono⟧พิมพ์ดีด⟦/f⟧ H⟦sub⟧2⟦/sub⟧O x⟦sup⟧2⟦/sup⟧ `code`\n" +
                "- รายการจุด\n" +
                "1. รายการตัวเลข\n" +
                "[ ] ยังไม่ทำ\n" +
                "[x] ทำแล้ว\n" +
                "> ข้อความอ้างอิง\n" +
                "---\n" +
                "⟦al:c⟧ข้อความกึ่งกลาง\n" +
                "⟦al:r⟧ข้อความชิดขวา"
        )
        rule.waitForIdle()
        shot("02-showcase-dark")
        rule.onNodeWithTag("fmt_size").performClick()
        rule.waitForIdle()
        shot("03-size-panel")
    }

    @Test fun showcaseLightAndFontPanel() {
        setUp("**จดไว้** ⟦c:8E24AA⟧ม่วง⟦/c⟧ ⟦z:2⟧ใหญ่⟦/z⟧\n⟦al:c⟧## กึ่งกลาง", dark = false)
        rule.onNodeWithTag("fmt_font").performScrollTo().performClick()
        rule.waitForIdle()
        shot("04-light-font-panel")
    }
}
