package com.motovault.app.ui.redesign

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun ColumnScope.DocumentWallet(documents: List<VaultDocument>, onAppearance: () -> Unit, onAdd: () -> Unit, onDocument: (VaultDocument) -> Unit, onInspection: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Document vault", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, modifier = Modifier.weight(1f))
        AppearanceButton(onAppearance)
        DocumentBadge("On this device")
        Box(Modifier.size(36.dp).clip(CircleShape).background(SurfaceColor).border(1.dp, BorderColor, CircleShape).clickable(onClick = onAdd), contentAlignment = Alignment.Center) { Text("+", fontSize = 23.sp) }
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { Eyebrow("Your papers, together"); DocumentHeading("Ready for the road."); DocumentCopy("Original copies. A place for every detail.") }
    if (documents.isEmpty()) Panel { Text("Your papers, together.", fontSize = 16.sp, fontWeight = FontWeight.Bold); DocumentCopy("Scan your registration, driving licence or tax token. Their printed details stay intact.") }
    else Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy((-36).dp)) {
        documents.forEachIndexed { index, document ->
            val marker = when (document.title) { "Registration smart card" -> Color(0xFF33795B); "Driving licence" -> Color(0xFF4567AC); else -> Color(0xFFA37B38) }
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp)).background(SurfaceColor).border(1.dp, BorderColor, RoundedCornerShape(17.dp)).clickable { onDocument(document) }) {
                Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.width(4.dp).height(48.dp).background(marker))
                    Row(Modifier.weight(1f).padding(horizontal = 15.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(document.title, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, modifier = Modifier.weight(1f))
                        DocumentBadge(document.expiryLabel, warning = (document.daysRemaining ?: Int.MAX_VALUE) <= RENEWAL_WARNING_DAYS, good = (document.daysRemaining ?: 0) > RENEWAL_WARNING_DAYS)
                    }
                }
                val height = if (index == 0 && isCardDocument(document.title)) null else 80.dp
                Column(Modifier.fillMaxWidth().then(if (height != null) Modifier.height(height) else Modifier).clip(RoundedCornerShape(0.dp)).padding(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 18.dp)) {
                    if (isCardDocument(document.title)) {
                        val page = document.pages.firstOrNull()
                        if (page != null) DocumentImage(page, Modifier.clip(RoundedCornerShape(9.dp)))
                        else document.frontPhoto?.let { Attachment(it, "Original ${document.title}") } ?: Column(Modifier.fillMaxWidth().aspectRatio(1.586f).background(LocalVaultPalette.current.raised, RoundedCornerShape(9.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) { DocumentNote("Details only · No original stored"); Text(document.holder, fontSize = 14.sp); DocumentNote(document.number) }
                    } else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { DocumentNote("Original certificate · ${document.pages.size.coerceAtLeast(1)} page(s)"); DocumentNote(document.details["registration"] ?: document.number) }
                }
            }
        }
    }
    val next = documents.filter { it.daysRemaining != null }.minByOrNull { it.daysRemaining!! }
    if (next != null) Panel {
        Text("Your next renewal", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { DocumentNote("${next.title} · ${next.expiry}"); DocumentBadge(next.expiryLabel, warning = next.daysRemaining!! <= RENEWAL_WARNING_DAYS, good = next.daysRemaining!! > RENEWAL_WARNING_DAYS) }
    }
    DocumentPrimary("Scan a document  ＋", onClick = onAdd)
    DocumentLink("Open inspection mode ↗", onInspection)
    DocumentNote("Original scans and confirmed details stay together.")
}

@Composable internal fun DocumentInspection(documents: List<VaultDocument>, onDismiss: () -> Unit, onDocument: (VaultDocument) -> Unit) {
    VaultTheme(LocalVaultBrandId.current, Appearance.LIGHT) {
        DocumentScreen("Roadside inspection", onDismiss) {
            Eyebrow("Stored document copies"); DocumentHeading("Vehicle documents"); DocumentCopy("Choose the original to show. Only document copies are displayed.")
            if (documents.isEmpty()) DocumentCopy("No documents stored. Return to the vault and scan your first document.")
            documents.forEach { document -> Panel(onClick = { onDocument(document) }) { Text(document.title, fontSize = 14.sp, fontWeight = FontWeight.Bold); DocumentNote(document.number); DocumentBadge(document.expiryLabel) } }
            DocumentSecondary("Exit inspection", Modifier.fillMaxWidth(), onClick = onDismiss)
        }
    }
}
