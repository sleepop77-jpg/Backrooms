package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.ui.BackroomsGameCanvas
import com.example.ui.TerminalHud
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.MainGameViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: MainGameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color(0xFF14130C)
                ) { innerPadding ->
                    BackroomsSurvivalApp(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun BackroomsSurvivalApp(
    viewModel: MainGameViewModel,
    modifier: Modifier = Modifier
) {
    val dayStats by viewModel.dayStats.collectAsState()
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF14130C))
    ) {
        Box(
            modifier = Modifier
                .weight(1.2f)
                .fillMaxSize()
        ) {
            BackroomsGameCanvas(dayStats = dayStats)
        }
        TerminalHud(
            dayStats = dayStats,
            onToggleBlock = { viewModel.toggleStudyBlock() },
            onTriggerBreach = { viewModel.triggerBreachAlert() },
            onSaveGrace = { viewModel.resolveGraceSaved() },
            onResetRun = { viewModel.resetRun() },
            onFastForwardHour = { viewModel.fastForwardHour(1L) },
            modifier = Modifier.weight(1.0f)
        )
    }
}
