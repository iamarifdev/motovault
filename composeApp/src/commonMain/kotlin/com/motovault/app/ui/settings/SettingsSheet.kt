package com.motovault.app.ui

import androidx.compose.runtime.Composable
import motovault.composeapp.generated.resources.Res
import motovault.composeapp.generated.resources.settings_appearance
import motovault.composeapp.generated.resources.settings_reminders
import motovault.composeapp.generated.resources.settings_row_language
import motovault.composeapp.generated.resources.settings_row_lock
import motovault.composeapp.generated.resources.settings_row_more
import motovault.composeapp.generated.resources.settings_row_sale_pack
import motovault.composeapp.generated.resources.settings_widget_title

@Composable
internal fun SettingsSheet(
    state: VaultState,
    section: SettingsSection,
    onUpdateState: (VaultState) -> Unit,
    onOpenSheet: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val ride = state.ride ?: return
    val title = appStringResource(
        when (section) {
            SettingsSection.APPEARANCE -> Res.string.settings_appearance
            SettingsSection.LANGUAGE -> Res.string.settings_row_language
            SettingsSection.MORE -> Res.string.settings_row_more
            SettingsSection.REMINDERS -> Res.string.settings_reminders
            SettingsSection.LOCK -> Res.string.settings_row_lock
            SettingsSection.SALE -> Res.string.settings_row_sale_pack
            SettingsSection.WIDGET -> Res.string.settings_widget_title
        }
    )

    Sheet(title = title, onDismiss = onDismiss) {
        when (section) {
            SettingsSection.APPEARANCE -> AppearanceSettingsContent(state, ride.brandId, onUpdateState)
            SettingsSection.LANGUAGE -> LanguageSettingsContent(state, onUpdateState)
            SettingsSection.MORE -> MoreSettingsContent(state, onUpdateState)
            SettingsSection.REMINDERS -> RemindersSettingsContent(state, onUpdateState, onOpenSheet)
            SettingsSection.LOCK -> LockSettingsContent(state, onUpdateState, onOpenSheet)
            SettingsSection.SALE -> SaleSettingsContent()
            SettingsSection.WIDGET -> WidgetPreviewContent(state)
        }
    }
}
