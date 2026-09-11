package com.freedu.myinterviews

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.freedu.myinterviews.domain.model.ApplicationStatus
import com.freedu.myinterviews.presentation.components.CompanyAvatar
import com.freedu.myinterviews.presentation.components.EmptyState
import com.freedu.myinterviews.presentation.components.FunnelChart
import com.freedu.myinterviews.presentation.components.SearchBar
import com.freedu.myinterviews.presentation.components.StatCard
import com.freedu.myinterviews.presentation.components.StatusChip
import com.freedu.myinterviews.ui.theme.MyInterviewsTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * On-device Compose UI tests for stateless shared components.
 * (Deliberately Hilt-free: screens with injected ViewModels are covered by
 * unit tests over their use cases instead. Run on an emulator/device with
 * `:app:connectedDebugAndroidTest`.)
 */
@RunWith(AndroidJUnit4::class)
class ComponentsUiTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun statusChip_showsHumanReadableLabel() {
        compose.setContent {
            MyInterviewsTheme { StatusChip(ApplicationStatus.INTERVIEWING) }
        }
        compose.onNodeWithText("Interviewing").assertIsDisplayed()
    }

    @Test
    fun emptyState_showsTitleSubtitleAndAction() {
        compose.setContent {
            MyInterviewsTheme {
                EmptyState(
                    icon = Icons.Default.Work,
                    title = "Nothing here",
                    subtitle = "Add something to begin.",
                    action = {
                        androidx.compose.material3.Button(onClick = {}) {
                            androidx.compose.material3.Text("Add now")
                        }
                    }
                )
            }
        }
        compose.onNodeWithText("Nothing here").assertIsDisplayed()
        compose.onNodeWithText("Add something to begin.").assertIsDisplayed()
        compose.onNodeWithText("Add now").assertIsDisplayed()
        compose.onNodeWithText("Add now").performClick()
    }

    @Test
    fun searchBar_typingEmitsQuery() {
        var query = ""
        compose.setContent {
            MyInterviewsTheme {
                SearchBar(query = query, onQuery = { query = it })
            }
        }
        compose.onNodeWithText("Search companies, roles, notes…").assertIsDisplayed()
        // The editable field (matched by its input action, not the placeholder text).
        compose.onNode(hasSetTextAction()).performTextInput("acme")
        assert(query == "acme")
    }

    @Test
    fun funnelChart_rendersStageLabelsAndCounts() {
        compose.setContent {
            MyInterviewsTheme {
                FunnelChart(
                    mapOf(
                        ApplicationStatus.APPLIED to 4,
                        ApplicationStatus.SCREENING to 2,
                        ApplicationStatus.INTERVIEWING to 1,
                        ApplicationStatus.OFFER to 1,
                        ApplicationStatus.ACCEPTED to 0
                    )
                )
            }
        }
        compose.onNodeWithText("Applied").assertIsDisplayed()
        compose.onNodeWithText("Screening").assertIsDisplayed()
        compose.onNodeWithText("Interviewing").assertIsDisplayed()
        compose.onNodeWithText("Offer").assertIsDisplayed()
        compose.onNodeWithText("Applied → Offer conversion: 25%").assertIsDisplayed()
    }

    @Test
    fun companyAvatar_derivesInitials() {
        compose.setContent {
            MyInterviewsTheme { CompanyAvatar("Nova Tech") }
        }
        compose.onNodeWithText("NT").assertIsDisplayed()
    }

    @Test
    fun statCard_showsValueAndLabel() {
        compose.setContent {
            MyInterviewsTheme { StatCard("12", "Applied") }
        }
        compose.onNodeWithText("12").assertIsDisplayed()
        compose.onNodeWithText("Applied").assertIsDisplayed()
    }
}
