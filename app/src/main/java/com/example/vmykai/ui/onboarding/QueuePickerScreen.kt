package com.example.vmykai.ui.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.vmykai.R
import com.example.vmykai.data.model.QueueId
import com.example.vmykai.ui.components.BackTitleRow
import com.example.vmykai.ui.theme.AppShapes
import com.example.vmykai.ui.theme.Spacing

@Composable
fun QueuePickerScreen(
    onQueueSelected: (QueueId) -> Unit,
    modifier: Modifier = Modifier,
    selectedQueueId: QueueId? = null,
    onBack: (() -> Unit)? = null,
) {
    Column(modifier = modifier.fillMaxSize().padding(Spacing.lg)) {
        if (onBack != null) {
            BackTitleRow(title = stringResource(R.string.onboarding_title), onBack = onBack)
            Spacer(Modifier.height(Spacing.xl))
        } else {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, AppShapes.IconBadge),
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_bolt),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onPrimaryContainer),
                )
            }
            Spacer(Modifier.height(Spacing.lg))
            Text(text = stringResource(R.string.onboarding_title), style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = stringResource(R.string.onboarding_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Spacing.lg))
        }

        QueueId.ALL.chunked(3).forEach { rowQueues ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                rowQueues.forEach { queueId ->
                    QueueOption(
                        queueId = queueId,
                        isSelected = queueId == selectedQueueId,
                        onClick = { onQueueSelected(queueId) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Spacer(Modifier.height(Spacing.sm))
        }
    }
}

@Composable
private fun QueueOption(queueId: QueueId, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    // Selected state uses onSurface rather than onPrimaryContainer: onPrimaryContainer is amber,
    // reserved for icon tint on the tonal badge, and reads too low-contrast as body text here.
    val containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    val contentColor = MaterialTheme.colorScheme.onSurface

    Card(
        onClick = onClick,
        shape = AppShapes.StatusCard,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = modifier,
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.lg),
        ) {
            Text(
                text = queueId.display,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (isSelected) FontWeight.SemiBold else null,
                color = contentColor,
            )
            if (isSelected) {
                Spacer(Modifier.width(Spacing.xs))
                Image(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    colorFilter = ColorFilter.tint(contentColor),
                )
            }
        }
    }
}
