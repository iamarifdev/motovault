package com.motovault.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motovault.app.domain.model.DocumentKinds
import com.motovault.app.domain.model.VaultDocument
import com.motovault.app.domain.model.stableKind
import kotlinx.datetime.LocalDate
import motovault.composeapp.generated.resources.Res
import motovault.composeapp.generated.resources.doc_art_expiry
import motovault.composeapp.generated.resources.doc_art_missing_name
import motovault.composeapp.generated.resources.doc_art_missing_number
import motovault.composeapp.generated.resources.doc_art_name
import motovault.composeapp.generated.resources.doc_art_number
import motovault.composeapp.generated.resources.vault_doc_no_expiry

@Composable
fun DocArt(
    doc: VaultDocument,
    modifier: Modifier = Modifier,
    large: Boolean = false,
    inWallet: Boolean = false,
    pageIndex: Int = 0
) {
    val shape = RoundedCornerShape(14.dp)
    if (doc.pages.isNotEmpty()) {
        val page = doc.pages[pageIndex.coerceIn(doc.pages.indices)]
        Box(
            modifier = modifier
                .fillMaxWidth()
                .aspectRatio(1.586f)
                .clip(shape)
                .background(SurfaceColor2)
        ) {
            DocumentImage(page, modifier = Modifier.fillMaxSize(), accepted = true, contentScale = ContentScale.Crop)
        }
        return
    }
    if (doc.frontPhoto != null || doc.backPhoto != null) {
        val encoded = if (pageIndex == 1 && doc.backPhoto != null) doc.backPhoto else (doc.frontPhoto ?: doc.backPhoto!!)
        Box(
            modifier = modifier
                .fillMaxWidth()
                .aspectRatio(1.586f)
                .clip(shape)
                .background(SurfaceColor2)
        ) {
            LegacyDocumentImage(encoded, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
        return
    }

    val kind = doc.stableKind() ?: DocumentKinds.TAX
    if (kind == DocumentKinds.LICENCE || kind == DocumentKinds.REGISTRATION) {
        BrtaCardArt(doc = doc, kind = kind, modifier = modifier, large = large)
    } else {
        GenericGradientDocCard(doc = doc, kind = kind, modifier = modifier, large = large, hideHeader = inWallet)
    }
}

@Composable
private fun GenericGradientDocCard(
    doc: VaultDocument,
    kind: String,
    modifier: Modifier = Modifier,
    large: Boolean,
    hideHeader: Boolean
) {
    val f = currentFmt
    val (gradStart, gradEnd) = when (kind) {
        DocumentKinds.TAX -> Color(0xFF8A5A1C) to Color(0xFFB8802F)
        DocumentKinds.INSURANCE -> Color(0xFF5B3C8F) to Color(0xFF7D58B8)
        else -> Color(0xFF3E4A55) to Color(0xFF5D6B78)
    }
    val padding = if (large) 18.dp else 12.dp
    val numberText = doc.number.ifBlank { appStringResource(Res.string.doc_art_missing_number) }
    val holderText = doc.holder.ifBlank { appStringResource(Res.string.doc_art_missing_name) }
    val parsedExpiry = runCatching { LocalDate.parse(doc.expiry) }.getOrNull()
    val expiryText = parsedExpiry?.let(f::dateFull)
        ?: doc.expiry.ifBlank { appStringResource(Res.string.vault_doc_no_expiry) }

    val fields = listOf(
        appStringResource(Res.string.doc_art_number) to numberText,
        appStringResource(Res.string.doc_art_name) to holderText,
        appStringResource(Res.string.doc_art_expiry) to expiryText
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.586f)
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.linearGradient(listOf(gradStart, gradEnd)))
            .padding(horizontal = if (large) 20.dp else 14.dp, vertical = padding)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            if (!hideHeader) {
                Text(
                    text = docDisplayName(kind),
                    fontSize = if (large) 16.sp else 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Box(
                modifier = Modifier
                    .size(width = if (large) 48.dp else 42.dp, height = if (large) 34.dp else 30.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFFE7D49A), Color(0xFFB99B4C))))
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                fields.forEach { (label, value) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = label,
                            modifier = Modifier.widthIn(min = 44.dp),
                            fontSize = if (large) 12.sp else 11.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Text(
                            text = value,
                            modifier = Modifier.weight(1f),
                            fontSize = if (large) 15.sp else 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
