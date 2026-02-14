package com.xaaav.mozukutsuchikey.mozc

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val BarBackground = Color(0xFF1A1A1E)
private val ComposingColor = Color.White
private val CandidateChipBackground = Color(0xFF2E2E34)
private val DividerColor = Color(0xFF444448)

@Composable
fun MozcCandidateBar(
    composingText: String,
    candidates: List<Candidate>,
    onCandidateSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(36.dp)
            .background(BarBackground)
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (composingText.isNotEmpty()) {
            Text(
                text = composingText,
                color = ComposingColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        if (composingText.isNotEmpty() && candidates.isNotEmpty()) {
            VerticalDivider(
                modifier = Modifier.fillMaxHeight().padding(vertical = 6.dp),
                thickness = 1.dp,
                color = DividerColor
            )
        }

        if (candidates.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                candidates.forEach { candidate ->
                    Box(
                        modifier = Modifier
                            .background(CandidateChipBackground, RoundedCornerShape(4.dp))
                            .clickable { onCandidateSelected(candidate.id) }
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = candidate.value,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}
