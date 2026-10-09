package com.motovault.app.ui.vault.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motovault.app.domain.model.GovernmentDocument
import com.motovault.app.theme.*

@Composable
fun SkeuomorphicSmartCard(
    document: GovernmentDocument,
    modifier: Modifier = Modifier,
    onInspectRawScan: () -> Unit = {}
) {
    var isFlipped by remember { mutableStateOf(false) }

    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 280f)
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp)
                .graphicsLayer {
                    rotationY = rotation
                    cameraDistance = 14 * density
                }
                .clickable { isFlipped = !isFlipped }
        ) {
            if (rotation <= 90f) {
                // FRONT SIDE OF SMART CARD
                SmartCardFront(document)
            } else {
                // BACK SIDE OF SMART CARD (mirrored so text reads correctly)
                Box(modifier = Modifier.graphicsLayer { rotationY = 180f }) {
                    SmartCardBack(document)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Tap Card to Flip ↻",
                style = MotoVaultTypography.bodyMedium.copy(color = KtmApexOrange, fontSize = 12.sp)
            )
            Text(
                text = "•",
                style = MotoVaultTypography.bodyMedium.copy(color = TextMuted)
            )
            Text(
                text = "Inspect Raw Scan",
                style = MotoVaultTypography.bodyMedium.copy(
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                modifier = Modifier.clickable { onInspectRawScan() }
            )
        }
    }
}

@Composable
private fun SmartCardFront(doc: GovernmentDocument) {
    val cardBrush = Brush.linearGradient(
        colors = listOf(Color(0xFF0F402C), Color(0xFF1A5F43), Color(0xFF0A3120))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(cardBrush, RoundedCornerShape(18.dp))
            .border(1.5.dp, Color(0xFF34D399).copy(alpha = 0.5f), RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Card Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(Color(0xFFDC2626), CircleShape)
                        .border(1.dp, Color(0xFFFACC15), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "BD",
                        color = Color(0xFFFACC15),
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp
                    )
                }
                Column {
                    Text(
                        text = "PEOPLE'S REPUBLIC OF BANGLADESH",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        color = Color(0xFFA7F3D0)
                    )
                    Text(
                        text = "Bangladesh Road Transport Authority",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }
            }
            Text(
                text = "SMART CARD",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF6EE7B7)
            )
        }

        // Chip and Registration Number
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Gold Microchip Graphic
            Box(
                modifier = Modifier
                    .size(width = 44.dp, height = 32.dp)
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFFFDE68A), Color(0xFFF59E0B), Color(0xFFD97706))
                        ),
                        RoundedCornerShape(6.dp)
                    )
                    .border(1.dp, Color(0xFFFBBF24), RoundedCornerShape(6.dp))
            )

            Column {
                Text(
                    text = "VEHICLE REGISTRATION NUMBER",
                    fontSize = 9.sp,
                    color = Color(0xFF6EE7B7)
                )
                Text(
                    text = doc.documentNumber,
                    fontSize = 17.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
        }

        // Owner & Class Footer
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column {
                Text(
                    text = "OWNER NAME",
                    fontSize = 8.sp,
                    color = Color(0xFFA7F3D0)
                )
                Text(
                    text = doc.holderName,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "ISSUE DATE",
                    fontSize = 8.sp,
                    color = Color(0xFFA7F3D0)
                )
                Text(
                    text = doc.issueDateFormatted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFFD1FAE5)
                )
            }
        }
    }
}

@Composable
private fun SmartCardBack(doc: GovernmentDocument) {
    val cardBrush = Brush.linearGradient(
        colors = listOf(Color(0xFF122019), Color(0xFF1A382C), Color(0xFF0D1D16))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(cardBrush, RoundedCornerShape(18.dp))
            .border(1.5.dp, Color(0xFF34D399).copy(alpha = 0.5f), RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "TECHNICAL SPECIFICATIONS",
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.8.sp,
                color = Color(0xFF6EE7B7)
            )
            Text(
                text = "BRTA CHIP ENCRYPTED",
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFFA7F3D0)
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SpecRow(label = "Chassis No", value = doc.chassisNumber ?: "MD925BK44L1092819")
            SpecRow(label = "Engine No", value = doc.engineNumber ?: "0-928*88319*")
            SpecRow(label = "Engine CC", value = "124.7 CC / Petrol")
            SpecRow(label = "Color / Weight", value = "Orange-Black / 159 kg")
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "||||| |||| |||||| |||||",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                letterSpacing = 2.sp,
                color = Color.LightGray
            )
            Text(
                text = "Tap to flip back ↻",
                fontSize = 10.sp,
                color = Color(0xFF34D399)
            )
        }
    }
}

@Composable
private fun SpecRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF064E3B).copy(alpha = 0.4f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 10.sp, color = Color(0xFFA7F3D0))
        Text(
            text = value,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}
