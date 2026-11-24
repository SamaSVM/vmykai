package com.example.vmykai.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.vmykai.R
import com.example.vmykai.ui.theme.Spacing

/** Shared "back arrow + title" header row for secondary screens (Settings, change-queue). */
@Composable
fun BackTitleRow(title: String, onBack: (() -> Unit)?) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back_action))
            }
            Spacer(Modifier.width(Spacing.xs))
        }
        Text(text = title, style = MaterialTheme.typography.headlineSmall)
    }
}
