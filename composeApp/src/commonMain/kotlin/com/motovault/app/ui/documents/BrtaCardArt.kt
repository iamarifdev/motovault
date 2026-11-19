package com.motovault.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motovault.app.domain.model.DocumentKinds
import com.motovault.app.domain.model.VaultDocument
import kotlinx.datetime.LocalDate
import motovault.composeapp.generated.resources.Res
import motovault.composeapp.generated.resources.brta_licence_bg
import motovault.composeapp.generated.resources.brta_registration_bg
import motovault.composeapp.generated.resources.doc_art_expiry
import motovault.composeapp.generated.resources.doc_art_licence_number
import motovault.composeapp.generated.resources.doc_art_missing_name
import motovault.composeapp.generated.resources.doc_art_missing_number
import motovault.composeapp.generated.resources.doc_art_name
import motovault.composeapp.generated.resources.doc_art_owner_name
import motovault.composeapp.generated.resources.doc_art_reg_number
import motovault.composeapp.generated.resources.vault_doc_no_expiry
import org.jetbrains.compose.resources.painterResource

private val CardInkPrimary = Color(0xFF141821)
private val CardInkMuted = Color(0xFF5B636E)
private val LicenceBgFallback = Color(0xFFF5EFE6)
private val LicenceLabelColor = Color(0xFF8B1D27)
private val LicenceScrimColor = Color(0x94FDF9F3)
private val LicencePhotoTint = Color(0xFF4C6178)
private val RegBgFallback = Color(0xFFEEF6F2)
private val RegLabelColor = Color(0xFF1C5A44)
private val RegScrimColor = Color(0xA3FBFDFA)
private val RegPhotoTint = Color(0xFF3F6659)

private data class BrtaFieldEntry(val label: String, val value: String, val muted: Boolean)

@Composable
internal fun BrtaCardArt(
    doc: VaultDocument,
    kind: String,
    modifier: Modifier = Modifier,
    large: Boolean = false
) {
    val isLicence = kind == DocumentKinds.LICENCE
    val shape = RoundedCornerShape(14.dp)
    val entries = brtaFieldEntries(doc, isLicence)
    val photoTint = (if (isLicence) LicencePhotoTint else RegPhotoTint).copy(alpha = 0.45f)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.586f)
            .clip(shape)
            .background(if (isLicence) LicenceBgFallback else RegBgFallback)
            .border(1.dp, Color.Black.copy(alpha = 0.08f), shape)
    ) {
        Image(
            painter = painterResource(if (isLicence) Res.drawable.brta_licence_bg else Res.drawable.brta_registration_bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )
        Box(
            modifier = Modifier
                .offset(x = maxWidth * (if (isLicence) 0.048f else 0.055f), y = maxHeight * (if (isLicence) 0.255f else 0.275f))
                .size(width = maxWidth * (if (isLicence) 0.28f else 0.245f), height = maxHeight * (if (isLicence) 0.59f else 0.48f)),
            contentAlignment = Alignment.Center
        ) {
            V3Icon(path = "feather_icon/fi_user-round.svg", description = null, modifier = Modifier.size(if (large) 40.dp else 34.dp), tint = photoTint)
        }
        BrtaFieldsPanel(entries, isLicence, large, maxWidth, maxHeight)
    }
}

@Composable
private fun brtaFieldEntries(doc: VaultDocument, isLicence: Boolean): List<BrtaFieldEntry> {
    val f = currentFmt
    val holderText = doc.holder.ifBlank { appStringResource(Res.string.doc_art_missing_name) }
    val numberText = doc.number.ifBlank { appStringResource(Res.string.doc_art_missing_number) }
    val parsedExpiry = runCatching { LocalDate.parse(doc.expiry) }.getOrNull()
    val expiryText = parsedExpiry?.let(f::dateFull)
        ?: doc.expiry.ifBlank { appStringResource(Res.string.vault_doc_no_expiry) }
    return if (isLicence) {
        listOf(
            BrtaFieldEntry(appStringResource(Res.string.doc_art_name), holderText, doc.holder.isBlank()),
            BrtaFieldEntry(appStringResource(Res.string.doc_art_licence_number), numberText, doc.number.isBlank()),
            BrtaFieldEntry(appStringResource(Res.string.doc_art_expiry), expiryText, false)
        )
    } else {
        listOf(
            BrtaFieldEntry(appStringResource(Res.string.doc_art_reg_number), numberText, doc.number.isBlank()),
            BrtaFieldEntry(appStringResource(Res.string.doc_art_owner_name), holderText, doc.holder.isBlank()),
            BrtaFieldEntry(appStringResource(Res.string.doc_art_expiry), expiryText, false)
        )
    }
}

@Composable
private fun BrtaFieldsPanel(
    entries: List<BrtaFieldEntry>,
    isLicence: Boolean,
    large: Boolean,
    cardWidth: Dp,
    cardHeight: Dp
) {
    val labelColor = if (isLicence) LicenceLabelColor else RegLabelColor
    val scrimColor = if (isLicence) LicenceScrimColor else RegScrimColor
    Column(
        modifier = Modifier
            .offset(x = cardWidth * (if (isLicence) 0.355f else 0.34f), y = cardHeight * (if (isLicence) 0.25f else 0.245f))
            .size(width = cardWidth * (if (isLicence) 0.42f else 0.545f), height = cardHeight * (if (isLicence) 0.61f else 0.655f))
            .clip(RoundedCornerShape(6.dp))
            .background(scrimColor)
            .padding(horizontal = if (large) 8.dp else 7.dp, vertical = if (large) 6.dp else 4.dp),
        verticalArrangement = Arrangement.spacedBy(if (large) 5.dp else 4.dp, Alignment.CenterVertically)
    ) {
        entries.forEach { entry ->
            Column {
                Text(
                    text = entry.label,
                    fontSize = if (large) 12.sp else 11.sp,
                    lineHeight = if (large) 14.sp else 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = labelColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = entry.value,
                    fontSize = if (large) 15.sp else 13.sp,
                    lineHeight = if (large) 19.sp else 16.sp,
                    fontWeight = if (entry.muted) FontWeight.Medium else FontWeight.Bold,
                    color = if (entry.muted) CardInkMuted else CardInkPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
