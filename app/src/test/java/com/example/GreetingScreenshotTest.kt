package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.model.DayStats
import com.example.model.MascotState
import com.example.model.RunPhase
import com.example.ui.TerminalHud
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    composeTestRule.setContent {
      MyApplicationTheme {
        TerminalHud(
          dayStats = DayStats(
            currentLevel = 0,
            streakDays = 12,
            almondWaterCans = 3,
            shameBreaches = 1,
            bankedSeconds = 7200L,
            currentBlockSeconds = 2059L,
            runPhase = RunPhase.STUDY_ACTIVE,
            mascotState = MascotState.WALK
          ),
          onToggleBlock = {},
          onTriggerBreach = {},
          onSaveGrace = {},
          onResetRun = {},
          onFastForwardHour = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}

