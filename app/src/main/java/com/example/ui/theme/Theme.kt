package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val BackroomsColorScheme = darkColorScheme(
  primary = BackroomsYellow,
  onPrimary = Color.Black,
  secondary = BackroomsAmber,
  onSecondary = Color.Black,
  tertiary = BackroomsYellowDim,
  background = BackroomsDarkBg,
  onBackground = BackroomsYellow,
  surface = BackroomsDarkSurface,
  onSurface = BackroomsYellow,
  surfaceVariant = BackroomsDarkPanel,
  onSurfaceVariant = BackroomsYellowDim,
  error = BackroomsRedBreach,
  onError = Color.White
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = BackroomsColorScheme,
    typography = Typography,
    content = content
  )
}

