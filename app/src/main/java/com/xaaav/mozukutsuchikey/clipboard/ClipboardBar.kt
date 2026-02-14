package com.xaaav.mozukutsuchikey.clipboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val BarBackground = Color(0xFF1A1A1E)
private val ChipBackground = Color(0xFF2E2E34)
private val PinnedChipBackground = Color(0xFF2A4A4A)

@Composable
fun ClipboardBar(
    items: List<ClipboardEntity>,
    onItemSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(36.dp)
            .background(BarBackground)
            .padding(horizontal = 6.dp)
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items.forEach { item ->
            val displayText = if (item.text.length > 30) {
                item.text.take(30) + "\u2026"
            } else {
                item.text
            }
            Box(
                modifier = Modifier
                    .background(
                        if (item.pinned) PinnedChipBackground else ChipBackground,
                        RoundedCornerShape(4.dp),
                    )
                    .clickable { onItemSelected(item.text) }
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = displayText,
                    color = Color.White,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
