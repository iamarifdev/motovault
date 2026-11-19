package com.motovault.app.ui

import androidx.compose.runtime.Composable
import motovault.composeapp.generated.resources.Res
import motovault.composeapp.generated.resources.service_workshop_clear
import motovault.composeapp.generated.resources.service_workshop_picker_close
import motovault.composeapp.generated.resources.service_workshop_picker_title
import motovault.composeapp.generated.resources.service_workshop_search
import motovault.composeapp.generated.resources.service_workshop_use_typed

@Composable
internal fun ServiceWorkshopPickerSheet(
    value: String,
    recentWorkshops: List<String>,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Sheet(
        title = appStringResource(Res.string.service_workshop_picker_title),
        onDismiss = onDismiss,
        closeLabel = appStringResource(Res.string.service_workshop_picker_close)
    ) {
        NamePickerContent(
            value = value,
            recent = recentWorkshops.filter { it.isNotBlank() }.distinct().map { RecentNameOption(it) },
            placeholder = appStringResource(Res.string.service_workshop_search),
            recentHeader = "",
            useTypedLabel = { typed -> appStringResource(Res.string.service_workshop_use_typed, typed) },
            clearLabel = appStringResource(Res.string.service_workshop_clear),
            onPick = onPick
        )
    }
}
