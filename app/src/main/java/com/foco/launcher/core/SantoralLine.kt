package com.foco.launcher.core

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
@Composable
fun SantoralLine(
    day: SantoralDay,
    label: String,
    modifier: Modifier = Modifier,
    novus: Boolean = false,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    val detail = santoralDetail(day, novus)
    val spoken = buildString {
        append(label).append(". ").append(day.name)
        if (detail.isNotBlank()) append(". ").append(detail)
        if (day.note.isNotBlank()) append(' ').append(day.note)
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable { open = !open }
            .clearAndSetSemantics {
                contentDescription = spoken
                role = Role.Button
                onClick { open = !open; true }
            }
            .padding(horizontal = 12.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            style = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Normal,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                color = FocoPaperDim,
                letterSpacing = 0.6.sp,
            ),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = day.name,
            style = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Normal,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                color = FocoPaper,
            ),
            textAlign = TextAlign.Center,
        )
        if (detail.isNotBlank()) Text(
            text = detail,
            modifier = Modifier.padding(top = 2.dp),
            style = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Normal,
                fontSize = 13.sp,
                lineHeight = 17.sp,
                color = FocoPaperDim,
            ),
            textAlign = TextAlign.Center,
            maxLines = if (open) 4 else 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (open && day.note.isNotBlank()) {
            Text(
                text = day.note,
                modifier = Modifier.padding(top = 2.dp),
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Normal,
                    fontSize = 13.sp,
                    lineHeight = 17.sp,
                    color = FocoPaperDim,
                ),
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun santoralDetail(day: SantoralDay, novus: Boolean): String {
    if (!novus) {
        return if (day.rank != null) "${day.rank} clase · ${day.summary}" else day.summary
    }
    val rankWord = when (day.rank) {
        "S" -> "Solemnidad"
        "F" -> "Fiesta"
        "M" -> "Memoria"
        "O" -> "Memoria libre"
        "A" -> "Conmemoración"
        else -> null
    }
    return when {
        rankWord != null && day.summary.isNotBlank() -> "$rankWord · ${day.summary}"
        rankWord != null -> rankWord
        else -> day.summary
    }
}
