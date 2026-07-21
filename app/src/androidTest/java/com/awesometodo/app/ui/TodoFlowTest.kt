package com.awesometodo.app.ui

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.awesometodo.app.MainActivity
import org.junit.Rule
import org.junit.Test

class TodoFlowTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test fun createTodoFromMainScreen() {
        rule.onNodeWithText("＋").performClick()
        rule.onNodeWithText("待办名称").performTextInput("完成测试")
        rule.onNodeWithText("5 分").performClick()
        rule.onNodeWithText("保存").performClick()
        rule.onNodeWithText("完成测试").assertExists()
    }
}
