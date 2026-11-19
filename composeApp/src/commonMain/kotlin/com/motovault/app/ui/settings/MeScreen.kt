package com.motovault.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motovault.app.ui.backup.BackupSheetTab
import motovault.composeapp.generated.resources.Res
import motovault.composeapp.generated.resources.settings_about
import motovault.composeapp.generated.resources.settings_appearance
import motovault.composeapp.generated.resources.settings_cloud_disconnected_short
import motovault.composeapp.generated.resources.settings_digits_short_bn
import motovault.composeapp.generated.resources.settings_digits_short_en
import motovault.composeapp.generated.resources.settings_group_app
import motovault.composeapp.generated.resources.settings_group_security
import motovault.composeapp.generated.resources.settings_language_bn
import motovault.composeapp.generated.resources.settings_language_en
import motovault.composeapp.generated.resources.settings_lock_off_value
import motovault.composeapp.generated.resources.settings_pair_value
import motovault.composeapp.generated.resources.settings_profile_title
import motovault.composeapp.generated.resources.settings_reminders
import motovault.composeapp.generated.resources.settings_reminders_on_value
import motovault.composeapp.generated.resources.settings_row_cloud_backup
import motovault.composeapp.generated.resources.settings_row_language
import motovault.composeapp.generated.resources.settings_row_local_backup
import motovault.composeapp.generated.resources.settings_row_lock
import motovault.composeapp.generated.resources.settings_row_more
import motovault.composeapp.generated.resources.settings_row_more_value
import motovault.composeapp.generated.resources.settings_row_reminders
import motovault.composeapp.generated.resources.settings_row_sale_pack
import motovault.composeapp.generated.resources.settings_theme_dark
import motovault.composeapp.generated.resources.settings_theme_light
import motovault.composeapp.generated.resources.settings_theme_system
import motovault.composeapp.generated.resources.settings_value_off
import motovault.composeapp.generated.resources.settings_value_on
import motovault.composeapp.generated.resources.settings_widget_title

@Composable
fun MeScreen(
    state: VaultState,
    onOpenSheet: (String, Any?) -> Unit
) {
    val ride = state.ride ?: return
    val brand = worlds.firstOrNull { it.id == ride.brandId } ?: worlds.first()
    val model = ride.selectedModel
    val bikeCount = 1 + state.garageBikes.count { !it.ride.archived }

    Column(
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), contentAlignment = Alignment.CenterStart) {
            Text(text = appStringResource(Res.string.settings_profile_title), fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        }
        SettingsProfileCard(brand = brand, model = model, ride = ride, bikeCount = bikeCount) {
            onOpenSheet("bikeSwitcher", null)
        }
        MeSecurityGroup(state = state, onOpenSheet = onOpenSheet)
        MeRemindersGroup(state = state, onOpenSheet = onOpenSheet)
        MeAppGroup(state = state, ride = ride, onOpenSheet = onOpenSheet)
        Text(
            text = appStringResource(Res.string.settings_about),
            fontSize = 12.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
        )
    }
}

@Composable
private fun MeSecurityGroup(state: VaultState, onOpenSheet: (String, Any?) -> Unit) {
    val f = currentFmt
    val lockValue = if (state.privacy.enabled) {
        appStringResource(Res.string.settings_value_on)
    } else {
        appStringResource(Res.string.settings_lock_off_value, f.d(state.documents.size.toString()))
    }
    val lockTone = if (state.privacy.enabled) SettingsValueTone.OK else SettingsValueTone.WARN

    SettingsGroup(title = appStringResource(Res.string.settings_group_security)) {
        SettingsRow("fi_lock.svg", appStringResource(Res.string.settings_row_lock), lockValue, lockTone) {
            onOpenSheet("settings", SettingsSection.LOCK)
        }
        SettingsRow("fi_database-backup.svg", appStringResource(Res.string.settings_row_local_backup), showDividerAbove = true) {
            onOpenSheet("backup", BackupSheetTab.CREATE)
        }
        SettingsRow("fi_cloud.svg", appStringResource(Res.string.settings_row_cloud_backup), appStringResource(Res.string.settings_cloud_disconnected_short), showDividerAbove = true) {
            onOpenSheet("cloud-backup", null)
        }
        SettingsRow("fi_package.svg", appStringResource(Res.string.settings_row_sale_pack), showDividerAbove = true) {
            onOpenSheet("settings", SettingsSection.SALE)
        }
    }
}

@Composable
private fun MeRemindersGroup(state: VaultState, onOpenSheet: (String, Any?) -> Unit) {
    val f = currentFmt
    val remindersOn = state.serviceReminders || state.docReminders
    val reminderValue = if (remindersOn) {
        appStringResource(Res.string.settings_reminders_on_value, f.d(state.reminderPolicy.warningDays.toString()))
    } else {
        appStringResource(Res.string.settings_value_off)
    }
    val reminderTone = if (remindersOn) SettingsValueTone.DEFAULT else SettingsValueTone.WARN

    SettingsGroup(title = appStringResource(Res.string.settings_reminders)) {
        SettingsRow("fi_bell.svg", appStringResource(Res.string.settings_row_reminders), reminderValue, reminderTone) {
            onOpenSheet("settings", SettingsSection.REMINDERS)
        }
    }
}

@Composable
private fun MeAppGroup(state: VaultState, ride: Ride, onOpenSheet: (String, Any?) -> Unit) {
    val themeLabel = appStringResource(
        when (state.appearance) {
            Appearance.DARK -> Res.string.settings_theme_dark
            Appearance.LIGHT -> Res.string.settings_theme_light
            Appearance.SYSTEM -> Res.string.settings_theme_system
        }
    )
    val accentBrand = worlds.firstOrNull { it.id == (state.brandOverride ?: ride.brandId) } ?: worlds.first()
    val appearanceValue = appStringResource(Res.string.settings_pair_value, themeLabel, accentBrand.name)
    val langLabel = appStringResource(if (state.language == AppLanguage.EN) Res.string.settings_language_en else Res.string.settings_language_bn)
    val digitsLabel = appStringResource(if (state.digits == Digits.BN) Res.string.settings_digits_short_bn else Res.string.settings_digits_short_en)
    val langValue = appStringResource(Res.string.settings_pair_value, langLabel, digitsLabel)

    SettingsGroup(title = appStringResource(Res.string.settings_group_app)) {
        SettingsRow("fi_palette.svg", appStringResource(Res.string.settings_appearance), appearanceValue) {
            onOpenSheet("settings", SettingsSection.APPEARANCE)
        }
        SettingsRow("fi_globe.svg", appStringResource(Res.string.settings_row_language), langValue, showDividerAbove = true) {
            onOpenSheet("settings", SettingsSection.LANGUAGE)
        }
        SettingsRow("fi_settings-2.svg", appStringResource(Res.string.settings_row_more), appStringResource(Res.string.settings_row_more_value), showDividerAbove = true) {
            onOpenSheet("settings", SettingsSection.MORE)
        }
        SettingsRow("fi_grid.svg", appStringResource(Res.string.settings_widget_title), showDividerAbove = true) {
            onOpenSheet("settings", SettingsSection.WIDGET)
        }
    }
}
