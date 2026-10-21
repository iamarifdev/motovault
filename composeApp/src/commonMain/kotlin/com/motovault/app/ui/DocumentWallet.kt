package com.motovault.app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import com.motovault.app.domain.model.DocumentExpiryStatus
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
        Text("কাগজপত্রের ভল্ট", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, modifier = Modifier.weight(1f))
        AppearanceButton(onAppearance)
        DocumentBadge("এই ডিভাইসে")
        Box(Modifier.size(36.dp).clip(CircleShape).background(SurfaceColor).border(1.dp, BorderColor, CircleShape).clickable(onClick = onAdd), contentAlignment = Alignment.Center) { Text("+", fontSize = 23.sp) }
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { Eyebrow("গুরুত্বপূর্ণ কাগজ এক জায়গায়"); DocumentHeading("রাস্তায় বের হওয়ার আগে প্রস্তুত থাকুন"); DocumentCopy("আসল কপি ও দরকারি তথ্য গুছিয়ে রাখুন।") }
    if (documents.isEmpty()) Panel { Text("আপনার কাগজপত্র এখানে রাখুন", fontSize = 16.sp, fontWeight = FontWeight.Bold); DocumentCopy("রেজিস্ট্রেশন, ড্রাইভিং লাইসেন্স বা ট্যাক্স টোকেন স্ক্যান করুন। কাগজের আসল রং ও লেখা অপরিবর্তিত থাকবে।") }
    else Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy((-36).dp)) {
        documents.forEachIndexed { index, document ->
            val status = document.expiryStatus
            val marker = when (document.title) { "Registration smart card" -> Color(0xFF33795B); "Driving licence" -> Color(0xFF4567AC); else -> Color(0xFFA37B38) }
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp)).background(SurfaceColor).border(1.dp, BorderColor, RoundedCornerShape(17.dp)).clickable { onDocument(document) }) {
                Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.width(4.dp).height(48.dp).background(marker))
                    Row(Modifier.weight(1f).padding(horizontal = 15.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(documentDisplayName(document.title), fontWeight = FontWeight.SemiBold, fontSize = 12.sp, modifier = Modifier.weight(1f))
                        DocumentBadge(document.expiryLabel, warning = status == DocumentExpiryStatus.EXPIRED || status == DocumentExpiryStatus.EXPIRING_SOON, good = status == DocumentExpiryStatus.VALID)
                    }
                }
                val height = if (index == 0 && isCardDocument(document.title)) null else 80.dp
                Column(Modifier.fillMaxWidth().then(if (height != null) Modifier.height(height) else Modifier).clip(RoundedCornerShape(0.dp)).padding(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 18.dp)) {
                    if (isCardDocument(document.title)) {
                        val page = document.pages.firstOrNull()
                        if (page != null) DocumentImage(page, Modifier.clip(RoundedCornerShape(9.dp)))
                        else document.frontPhoto?.let { Attachment(it, "Original ${document.title}") } ?: Column(Modifier.fillMaxWidth().aspectRatio(1.586f).background(LocalVaultPalette.current.raised, RoundedCornerShape(9.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) { DocumentNote("শুধু তথ্য আছে। আসল কপি নেই"); Text(document.holder, fontSize = 14.sp); DocumentNote(document.number) }
                    } else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { DocumentNote("আসল সনদ · ${document.pages.size.coerceAtLeast(1)} পৃষ্ঠা"); DocumentNote(document.details["registration"] ?: document.number) }
                }
            }
        }
    }
    val next = documents.filter { it.daysRemaining != null }.minByOrNull { it.daysRemaining!! }
    if (next != null) Panel {
        val status = next.expiryStatus
        Text("পরের নবায়ন", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { DocumentNote("${documentDisplayName(next.title)} · ${next.expiry}"); DocumentBadge(next.expiryLabel, warning = status == DocumentExpiryStatus.EXPIRED || status == DocumentExpiryStatus.EXPIRING_SOON, good = status == DocumentExpiryStatus.VALID) }
    }
    DocumentPrimary("কাগজ স্ক্যান করুন  ＋", onClick = onAdd)
    RenewalReminderControl()
    DocumentLink("রোডসাইড দেখানোর মোড খুলুন ↗", onInspection)
    DocumentNote("আসল স্ক্যান ও যাচাই করা তথ্য একসঙ্গে থাকবে।")
}

@Composable internal fun DocumentInspection(documents: List<VaultDocument>, onDismiss: () -> Unit, onDocument: (VaultDocument) -> Unit) {
    VaultTheme(LocalVaultBrandId.current, Appearance.LIGHT) {
        DocumentScreen("রাস্তায় কাগজ দেখান", onDismiss) {
            Eyebrow("সংরক্ষিত কাগজের কপি"); DocumentHeading("গাড়ির কাগজপত্র"); DocumentCopy("যে কাগজ দেখাবেন তা বেছে নিন। এখানে শুধু কাগজের কপি দেখানো হবে।")
            if (documents.isEmpty()) DocumentCopy("কোনো কাগজ রাখা নেই। কাগজপত্রে ফিরে প্রথম কাগজ স্ক্যান করুন।")
            documents.forEach { document -> Panel(onClick = { onDocument(document) }) { Text(documentDisplayName(document.title), fontSize = 14.sp, fontWeight = FontWeight.Bold); DocumentNote(document.number); DocumentBadge(document.expiryLabel) } }
            DocumentSecondary("ফিরে যান", Modifier.fillMaxWidth(), onClick = onDismiss)
        }
    }
}
