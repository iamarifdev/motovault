package com.motovault.app.ui.vault

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.motovault.app.domain.model.GovernmentDocument
import com.motovault.app.domain.model.GovernmentDocumentType
import com.motovault.app.theme.*

/**
 * Apple Wallet Passbook Vertical Card Stack
 * Features:
 * - Vertical overlapping physical cards that fan out on tap with fluid spring physics
 * - 3D 180° flip on the active front card
 * - Bangladeshi Government identity: BRTA Registration, Driving Licence, Tax Token
 * - One-tap Traffic Police Mode (pure black AMOLED, max contrast, privacy lock)
 */
@Composable
fun DocumentVaultScreen(
    onBackToGarage: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPoliceModeActive by remember { mutableStateOf(false) }
    var activeCardIndex by remember { mutableStateOf(0) } // 0: Reg, 1: License, 2: Tax Token
    var isFrontFlipped by remember { mutableStateOf(false) }

    val mockRegistrationDoc = remember {
        GovernmentDocument(
            id = "doc_reg_01",
            type = GovernmentDocumentType.BRTA_REGISTRATION,
            documentNumber = "DHAKA METRO-HA 45-8821",
            holderName = "MD. ARIFUR RAHMAN",
            issueDateFormatted = "14-OCT-2021",
            expiryDateFormatted = "13-OCT-2031",
            daysUntilExpiry = 1820,
            chassisNumber = "MD925BK44L1092819",
            engineNumber = "0-928*88319*"
        )
    }

    val mockLicenseDoc = remember {
        GovernmentDocument(
            id = "doc_lic_01",
            type = GovernmentDocumentType.DRIVING_LICENSE,
            documentNumber = "DK0182949CL",
            holderName = "MD. ARIFUR RAHMAN",
            issueDateFormatted = "02-MAR-2022",
            expiryDateFormatted = "01-MAR-2032",
            daysUntilExpiry = 1980,
            bloodGroup = "B+",
            authorizedClasses = "MOTORCYCLE / LIGHT"
        )
    }

    val mockTaxToken = remember {
        GovernmentDocument(
            id = "doc_tax_01",
            type = GovernmentDocumentType.TAX_TOKEN,
            documentNumber = "TT-992014-2024",
            holderName = "MD. ARIFUR RAHMAN",
            issueDateFormatted = "14-OCT-2024",
            expiryDateFormatted = "13-OCT-2026",
            daysUntilExpiry = 48
        )
    }

    val canvasBackground = if (isPoliceModeActive) Color.Black else CanvasTftDark

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(canvasBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // HEADER & POLICE MODE TOGGLE
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .background(StatusHealthyGreen, CircleShape)
                            )
                            Text(
                                text = "ENCRYPTED VAULT",
                                style = MotoVaultTypography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = StatusHealthyGreen,
                                    letterSpacing = 1.sp
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Vehicle Papers",
                            style = MotoVaultTypography.displayLarge.copy(
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black
                            ),
                            color = TextPrimary
                        )
                    }

                    // Emergency Police Mode Button
                    Button(
                        onClick = { isPoliceModeActive = !isPoliceModeActive },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isPoliceModeActive) StatusOverdueRed else Color(0x26EF4444)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isPoliceModeActive) Color.White else StatusOverdueRed.copy(alpha = 0.5f)
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .background(if (isPoliceModeActive) Color.White else StatusOverdueRed, CircleShape)
                            )
                            Text(
                                text = if (isPoliceModeActive) "EXIT POLICE MODE" else "TRAFFIC POLICE MODE",
                                style = MotoVaultTypography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (isPoliceModeActive) Color.White else StatusOverdueRed
                                )
                            )
                        }
                    }
                }
            }

            // POLICE MODE BANNER IF ACTIVE
            if (isPoliceModeActive) {
                item {
                    Surface(
                        color = Color(0x33EF4444),
                        border = androidx.compose.foundation.BorderStroke(1.dp, StatusOverdueRed),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(text = "🚨", fontSize = 18.sp)
                            Column {
                                Text(
                                    text = "OFFICIAL POLICE VERIFICATION DISPLAY",
                                    style = MotoVaultTypography.labelMedium.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color.White
                                    )
                                )
                                Text(
                                    text = "AMOLED max contrast active • Sensitive service & maintenance logs locked",
                                    style = MotoVaultTypography.bodySmall.copy(
                                        fontSize = 11.sp,
                                        color = Color(0xFFFCA5A5)
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // APPLE WALLET VERTICAL CARD STACK CONTAINER
            item {
                Text(
                    text = "TAP CARD TO EXPAND OR FLIP",
                    style = MotoVaultTypography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 1.sp
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(390.dp)
                ) {
                    val cards = listOf(
                        Triple(0, mockRegistrationDoc, "REGISTRATION"),
                        Triple(1, mockLicenseDoc, "DRIVING LICENCE"),
                        Triple(2, mockTaxToken, "TAX TOKEN")
                    )

                    cards.forEach { (index, doc, label) ->
                        val isSelected = activeCardIndex == index

                        // Vertical Passbook Stack Offset calculation
                        val targetOffsetY = when {
                            isSelected -> 0.dp
                            index > activeCardIndex -> (195 + (index - activeCardIndex - 1) * 68).dp
                            else -> (index * 50).dp
                        }

                        val targetScale = if (isSelected) 1f else (1f - (index * 0.03f))
                        val currentZIndex = if (isSelected) 30f else (20f - index)

                        val animatedY by animateDpAsState(
                            targetValue = targetOffsetY,
                            animationSpec = spring(dampingRatio = 0.82f, stiffness = 280f)
                        )

                        val animatedScale by animateFloatAsState(
                            targetValue = targetScale,
                            animationSpec = spring(dampingRatio = 0.82f, stiffness = 280f)
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .offset(y = animatedY)
                                .scale(animatedScale)
                                .zIndex(currentZIndex)
                                .clickable {
                                    if (activeCardIndex != index) {
                                        activeCardIndex = index
                                        isFrontFlipped = false
                                    } else {
                                        isFrontFlipped = !isFrontFlipped
                                    }
                                }
                        ) {
                            when (index) {
                                0 -> BrtaRegistrationCardView(
                                    doc = doc,
                                    isFlipped = isSelected && isFrontFlipped,
                                    isPoliceMode = isPoliceModeActive
                                )
                                1 -> DrivingLicenceCardView(
                                    doc = doc,
                                    isFlipped = isSelected && isFrontFlipped,
                                    isPoliceMode = isPoliceModeActive
                                )
                                2 -> TaxTokenCardView(
                                    doc = doc,
                                    isPoliceMode = isPoliceModeActive
                                )
                            }
                        }
                    }
                }
            }

            // ACTION CONTROLS BELOW STACK
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { isFrontFlipped = !isFrontFlipped },
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceElevatedDark),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSpecularDark),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text(
                            text = "↻ Flip Card (3D)",
                            style = MotoVaultTypography.titleMedium.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                    }

                    Button(
                        onClick = { /* Launch camera scanner */ },
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceElevatedDark),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSpecularDark),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text(
                            text = "📷 Scan Card",
                            style = MotoVaultTypography.titleMedium.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                    }
                }
            }

            // RETURN TO GARAGE
            item {
                Button(
                    onClick = onBackToGarage,
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceCardDark),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSpecularDark),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text(
                        text = "← Return to Garage Dashboard",
                        style = MotoVaultTypography.titleMedium.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary
                        )
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

// -------------------------------------------------------------
// CARD 1: BANGLADESH BRTA REGISTRATION SMART CARD
// -------------------------------------------------------------
@Composable
private fun BrtaRegistrationCardView(
    doc: GovernmentDocument,
    isFlipped: Boolean,
    isPoliceMode: Boolean
) {
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 280f)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 14 * density
            }
    ) {
        if (rotation <= 90f) {
            // FRONT
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(BrtaRegStart, BrtaRegMid, BrtaRegEnd)
                        )
                    )
                    .border(
                        if (isPoliceMode) 2.dp else 1.dp,
                        if (isPoliceMode) Color(0xFF34D399) else Color(0x4034D399),
                        RoundedCornerShape(22.dp)
                    )
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Header
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
                                    .size(26.dp)
                                    .background(Color(0xFFDC2626), CircleShape)
                                    .border(1.dp, Color(0xFFFACC15), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "BD",
                                    color = Color(0xFFFACC15),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 10.sp
                                )
                            }
                            Column {
                                Text(
                                    text = "PEOPLE'S REPUBLIC OF BANGLADESH",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp,
                                    color = Color(0xFFA7F3D0)
                                )
                                Text(
                                    text = "Bangladesh Road Transport Authority",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .background(Color(0x33000000), RoundedCornerShape(6.dp))
                                .border(1.dp, Color(0x4034D399), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "SMART CARD",
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF6EE7B7)
                            )
                        }
                    }

                    // Gold Microchip & Registration Number
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 42.dp, height = 30.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFFFDE68A), Color(0xFFF59E0B), Color(0xFFD97706))
                                    )
                                )
                                .border(1.dp, Color(0xFFFBBF24), RoundedCornerShape(6.dp))
                        )
                        Column {
                            Text(
                                text = "VEHICLE REGISTRATION NUMBER",
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
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

                    // Owner & Vehicle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(text = "OWNER", fontSize = 8.sp, color = Color(0xFFA7F3D0))
                            Text(
                                text = doc.holderName,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "VEHICLE", fontSize = 8.sp, color = Color(0xFFA7F3D0))
                            Text(
                                text = "KTM Duke 125",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        } else {
            // BACK
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationY = 180f }
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF0C1813), Color(0xFF122B21), Color(0xFF081510))
                        )
                    )
                    .border(1.dp, Color(0x4034D399), RoundedCornerShape(22.dp))
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
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
                            color = Color(0xFF6EE7B7)
                        )
                        Text(
                            text = "BRTA ENCRYPTED",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFFA7F3D0)
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        MicroSpecRow(label = "CHASSIS", value = doc.chassisNumber ?: "MD925BK44L1092819")
                        MicroSpecRow(label = "ENGINE", value = doc.engineNumber ?: "0-928*88319*")
                        MicroSpecRow(label = "ENGINE CC", value = "124.7 CC / Petrol")
                        MicroSpecRow(label = "KERB WEIGHT", value = "159 kg")
                    }

                    Text(
                        text = "Tap to flip front ↻",
                        fontSize = 9.sp,
                        color = Color(0xFF34D399),
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// CARD 2: BANGLADESH BRTA DRIVING LICENCE
// -------------------------------------------------------------
@Composable
private fun DrivingLicenceCardView(
    doc: GovernmentDocument,
    isFlipped: Boolean,
    isPoliceMode: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.linearGradient(
                    listOf(LicenseBlueStart, LicenseBlueMid, LicenseBlueEnd)
                )
            )
            .border(
                if (isPoliceMode) 2.dp else 1.dp,
                if (isPoliceMode) Color(0xFF60A5FA) else Color(0x4060A5FA),
                RoundedCornerShape(22.dp)
            )
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "BRTA DRIVING LICENCE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp,
                    color = Color(0xFFBFDBFE)
                )
                Box(
                    modifier = Modifier
                        .background(Color(0x33000000), RoundedCornerShape(6.dp))
                        .border(1.dp, Color(0x4060A5FA), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = doc.documentNumber,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 46.dp, height = 54.dp)
                        .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "👤", fontSize = 22.sp)
                }
                Column {
                    Text(
                        text = doc.holderName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Class: ${doc.authorizedClasses ?: "MOTORCYCLE / LIGHT"}",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF93C5FD)
                    )
                    Text(
                        text = "Blood Group: ${doc.bloodGroup ?: "B+"}",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF93C5FD)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "Valid Until: ${doc.expiryDateFormatted}",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFFBFDBFE)
                )
                Text(
                    text = "STATUS: ACTIVE",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = StatusHealthyGreen
                )
            }
        }
    }
}

// -------------------------------------------------------------
// CARD 3: BANGLADESH BRTA MOTOR VEHICLE TAX TOKEN
// -------------------------------------------------------------
@Composable
private fun TaxTokenCardView(
    doc: GovernmentDocument,
    isPoliceMode: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.linearGradient(
                    listOf(TaxTokenAmberStart, TaxTokenAmberMid, TaxTokenAmberEnd)
                )
            )
            .border(
                if (isPoliceMode) 2.dp else 1.dp,
                if (isPoliceMode) Color(0xFFFBBF24) else Color(0x40FBBF24),
                RoundedCornerShape(22.dp)
            )
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "BRTA TAX TOKEN CERTIFICATE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp,
                    color = Color(0xFFFDE68A)
                )
                Box(
                    modifier = Modifier
                        .background(Color(0x33000000), RoundedCornerShape(6.dp))
                        .border(1.dp, Color(0x40FBBF24), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = doc.documentNumber,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Column {
                Text(
                    text = "Reg: DHAKA METRO-HA 45-8821",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFFFDE68A)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "${doc.daysUntilExpiry}",
                        fontSize = 28.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Days Remaining",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFCD34D),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                Text(
                    text = "Renewal Due: ${doc.expiryDateFormatted}",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFFFDE68A)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "Payment: VERIFIED",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = StatusHealthyGreen
                )
                Text(
                    text = "Tap to bring forward",
                    fontSize = 10.sp,
                    color = Color(0xFFFDE68A).copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun MicroSpecRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0x26000000), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 9.sp, color = Color(0xFFA7F3D0))
        Text(
            text = value,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}
