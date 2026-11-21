package com.sek789102.jotwai

import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.text.input.TextFieldValue
import com.sek789102.jotwai.domain.TextEditing
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Runs the list-continuation through a real Compose text field, like the editor does. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ListContinuationUiTest {
    @get:Rule val rule = createComposeRule()

    private var latest = ""

    private fun setUp() {
        rule.setContent {
            var v by remember { mutableStateOf(TextFieldValue("")) }
            BasicTextField(
                value = v,
                onValueChange = { v = TextEditing.continueList(v, it); latest = v.text },
                modifier = Modifier.testTag("body")
            )
        }
        rule.onNodeWithTag("body").performClick()
    }

    @Test fun `newline typed by the keyboard continues the list`() {
        setUp()
        rule.onNodeWithTag("body").performTextInput("- first")
        rule.onNodeWithTag("body").performTextInput("\n")
        rule.onNodeWithTag("body").performTextInput("second")
        rule.waitForIdle()
        assertEquals("- first\n- second", latest)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test fun `hardware enter key continues the list`() {
        setUp()
        rule.onNodeWithTag("body").performTextInput("1. one")
        rule.onNodeWithTag("body").performKeyInput { pressKey(Key.Enter) }
        rule.onNodeWithTag("body").performTextInput("two")
        rule.waitForIdle()
        assertEquals("1. one\n2. two", latest)
    }
}
