package com.example.submate

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AppFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Before fun resetAppData() = runBlocking {
        val app = compose.activity.application as SubMateApplication
        app.settings.update { it.copy(calendarSync = false, darkMode = "light") }
        app.repository.dao.all().forEach { app.repository.dao.delete(it) }
    }
    @Test fun createEditPauseDeleteAndNavigate() {
        compose.onNodeWithText("첫 구독 등록").performScrollTo().performClick()
        compose.onNodeWithText("서비스명").performScrollTo().performTextInput("검증용 구독")
        compose.onNodeWithText("결제 금액").performScrollTo().performTextInput("12000")
        compose.onNodeWithTag("editorList").performScrollToNode(hasText("구독 저장"))
        compose.onNodeWithText("구독 저장").performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithText("검증용 구독").fetchSemanticsNodes().isNotEmpty() }
        dismissMessage()
        compose.onNodeWithText("검증용 구독").performScrollTo().performClick()
        compose.onNodeWithContentDescription("구독 수정").performClick()
        compose.onNodeWithText("서비스명").performScrollTo().performTextReplacement("수정 완료 구독")
        compose.onNodeWithTag("editorList").performScrollToNode(hasText("구독 저장"))
        compose.onNodeWithText("구독 저장").performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithText("수정 완료 구독").fetchSemanticsNodes().isNotEmpty() }
        dismissMessage()
        compose.onNodeWithTag("detailList").performScrollToNode(hasText("구독 일시정지"))
        compose.onNodeWithText("구독 일시정지").performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithText("구독 다시 활성화").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("구독 다시 활성화").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("detailList").performScrollToIndex(0)
        compose.onNodeWithContentDescription("구독 삭제").performClick()
        compose.onNodeWithText("삭제").performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithText("첫 구독 등록").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("캘린더").performClick()
        compose.onNodeWithText("결제일을 놓치지 않게").assertIsDisplayed()
        compose.onNodeWithText("리포트").performClick()
        compose.onNodeWithText("구독료의 흐름을 읽어요").assertIsDisplayed()
        compose.onNodeWithText("설정").performClick()
        compose.onNodeWithTag("settingsList").performScrollToNode(hasText("백업 파일 저장"))
        compose.onNodeWithText("백업 파일 저장").assertIsDisplayed()
    }
    private fun dismissMessage() {
        compose.waitUntil(15000) { compose.onAllNodesWithText("확인").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("확인").performClick()
    }
}
