package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.model.DayStats
import com.example.model.RunPhase
import com.example.ui.theme.BackroomsAmber
import com.example.ui.theme.BackroomsBorder
import com.example.ui.theme.BackroomsDarkPanel
import com.example.ui.theme.BackroomsDarkSurface
import com.example.ui.theme.BackroomsGreenSafe
import com.example.ui.theme.BackroomsRedBreach
import com.example.ui.theme.BackroomsYellow
import com.example.ui.theme.BackroomsYellowDim

@Composable
fun TerminalHud(
    dayStats: DayStats,
    onToggleBlock: () -> Unit,
    onTriggerBreach: () -> Unit,
    onSaveGrace: () -> Unit,
    onResetRun: () -> Unit,
    onFastForwardHour: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BackroomsDarkSurface, RoundedCornerShape(4.dp))
                .border(1.dp, BackroomsBorder, RoundedCornerShape(4.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "BACKROOMS FOCUS LAB // V1.0",
                color = BackroomsYellow,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "STATUS: ${dayStats.runPhase.name}",
                color = if (dayStats.runPhase == RunPhase.STUDY_ACTIVE) BackroomsGreenSafe else BackroomsYellowDim,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .background(BackroomsDarkPanel, RoundedCornerShape(4.dp))
                    .border(1.dp, BackroomsBorder, RoundedCornerShape(4.dp))
                    .padding(10.dp)
            ) {
                Text("CURRENT LEVEL:", color = BackroomsYellowDim, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                Text("${dayStats.currentLevel} (The Lobby)", color = BackroomsYellow, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(4.dp))
                Text("FOCUS STREAK:", color = BackroomsYellowDim, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                Text("${dayStats.streakDays} days", color = BackroomsYellow, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(4.dp))
                Text("SHAME BREACHES:", color = BackroomsYellowDim, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                Text("${dayStats.shameBreaches}", color = if (dayStats.shameBreaches > 0) BackroomsRedBreach else BackroomsYellow, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .background(BackroomsDarkPanel, RoundedCornerShape(4.dp))
                    .border(1.dp, BackroomsBorder, RoundedCornerShape(4.dp))
                    .padding(10.dp)
            ) {
                Text("DAILY GOAL (8:00:00):", color = BackroomsYellowDim, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                val totalBanked = dayStats.bankedSeconds + if (dayStats.runPhase == RunPhase.STUDY_ACTIVE) dayStats.currentBlockSeconds else 0L
                Text(formatDuration(totalBanked), color = BackroomsYellow, fontSize = 15.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(4.dp))
                Text("CURRENT BLOCK:", color = BackroomsYellowDim, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                val blockSec = dayStats.currentBlockSeconds
                val blockColor = if (blockSec >= 900L) BackroomsGreenSafe else BackroomsYellowDim
                Text(
                    text = "${formatDuration(blockSec)} ${if (blockSec < 900L) "(min 15m)" else "[QUALIFIED]"}",
                    color = blockColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
        AnimatedVisibility(visible = dayStats.isBlacklistBreachGraceActive) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BackroomsRedBreach.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                    .border(1.dp, BackroomsRedBreach, RoundedCornerShape(4.dp))
                    .padding(10.dp)
                    .clickable { onSaveGrace() }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = "Breach Alert", tint = BackroomsRedBreach)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            "WARNING: THE WIREFRAME APPROACHING!",
                            color = BackroomsRedBreach,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            "Tap here to cancel within ${String.format("%.1f", dayStats.graceSecondsRemaining)}s",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
        AnimatedVisibility(visible = dayStats.runPhase == RunPhase.BREACH_KILLED) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BackroomsDarkSurface, RoundedCornerShape(4.dp))
                    .border(2.dp, BackroomsRedBreach, RoundedCornerShape(4.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = "[FATAL BREACH: RUN TERMINATED]",
                    color = BackroomsRedBreach,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = dayStats.killMessage,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onResetRun,
                    colors = ButtonDefaults.buttonColors(containerColor = BackroomsRedBreach),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text("ACCEPT FAILURE (DAY TERMINATED)", color = Color.White, fontFamily = FontFamily.Monospace)
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val isStudying = dayStats.runPhase == RunPhase.STUDY_ACTIVE
            val isTerminal = dayStats.runPhase == RunPhase.BREACH_KILLED ||
                dayStats.runPhase == RunPhase.DAY_COMPLETED ||
                dayStats.runPhase == RunPhase.DAY_OVER
            Button(
                onClick = onToggleBlock,
                enabled = !isTerminal,
                modifier = Modifier.weight(1.5f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isStudying) BackroomsAmber else BackroomsYellow
                ),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = when {
                        isStudying -> "STOP STUDY BLOCK"
                        isTerminal -> "RUN TERMINATED TODAY"
                        else -> "ENTER BACKROOMS (START)"
                    },
                    color = Color.Black,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp
                )
            }
            if (isStudying && BuildConfig.DEBUG) {
                OutlinedButton(
                    onClick = onFastForwardHour,
                    modifier = Modifier.weight(0.9f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = BackroomsYellow),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BackroomsBorder),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text("+1H MAP", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onTriggerBreach,
                    modifier = Modifier.weight(0.9f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = BackroomsRedBreach),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BackroomsRedBreach),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text("TEST TRAP", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun formatDuration(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return String.format("%02d:%02d:%02d", h, m, s)
}
