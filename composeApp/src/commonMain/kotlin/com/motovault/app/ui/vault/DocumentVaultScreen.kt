package com.motovault.app.ui.vault

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motovault.app.domain.model.GovernmentDocument
import com.motovault.app.domain.model.GovernmentDocumentType
import com.motovault.app.theme.*
import com.motovault.app.ui.vault.components.SkeuomorphicSmartCard

@Composable
fun DocumentVaultScreen(
    onBackToGarage: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPoliceModeActive by remember { mutableStateOf(false) }
    var selectedDocType by remember { mutableStateOf(GovernmentDocumentType.BRTA_REGISTRATION) }

    val mockRegistrationDoc = GovernmentDocument(
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

    val mockLicenseDoc = GovernmentDocument(
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

    val mockTaxToken = GovernmentDocument(
        id = "doc_tax_01",
        type = GovernmentDocumentType.TAX_TOKEN,
        documentNumber = "TT-992014-2024",
        holderName = "MD. ARIFUR RAHMAN",
        issueDateFormatted = "14-OCT-2024",
        expiryDateFormatted = "13-OCT-2026",
        daysUntilExpiry = 48
    )

    val activeDocument = when (selectedDocType) {
        GovernmentDocumentType.BRTA_REGISTRATION -> mockRegistrationDoc
        GovernmentDocumentType.DRIVING_LICENSE -> mockLicenseDoc
        GovernmentDocumentType.TAX_TOKEN -> mockTaxToken
        else -> mockRegistrationDoc
    }

    val canvasBackground = if (isPoliceModeActive) Color.Black else CanvasTftDark

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(canvasBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // TOP VAULT HEADER
        item {
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
                            .size(10.dp)
                            .background(StatusHealthyGreen, CircleShape)
                    )
                    Text(
                        text = "DOCUMENT VAULT",
                        style = MotoVaultTypography.labelMedium.copy(
                            fontSize = 13.sp,
                            color = TextSecondary,
                            letterSpacing = 1.sp
                        )
                    )
                }

                // TRAFFIC POLICE CHECK MODE BUTTON
                Button(
                    onClick = { isPoliceModeActive = !isPoliceModeActive },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPoliceModeActive) StatusOverdueRed else SurfaceElevatedDark
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isPoliceModeActive) StatusOverdueRed else StatusOverdueRed.copy(alpha = 0.6f)
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (isPoliceModeActive) "🚨 EXIT POLICE MODE" else "🚨 POLICE CHECK MODE",
                        style = MotoVaultTypography.labelMedium.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isPoliceModeActive) Color.White else StatusOverdueRed
                        )
                    )
                }
            }
        }

        // POLICE MODE BANNER IF ACTIVE
        if (isPoliceModeActive) {
            item {
                Surface(
                    color = StatusOverdueRed.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, StatusOverdueRed),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Max Brightness Engaged • Private Financial & Service Records Locked",
                        style = MotoVaultTypography.bodyMedium.copy(
                            color = StatusOverdueRed,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        ),
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        // SKEUOMORPHIC 3D FLIP SMART CARD
        item {
            SkeuomorphicSmartCard(document = activeDocument)
        }

        // DOCUMENT TYPE SELECTOR PILLS
        item {
            Text(
                text = "SAVED VEHICLE PAPERS",
                style = MotoVaultTypography.labelMedium.copy(
                    fontSize = 11.sp,
                    color = TextMuted,
                    letterSpacing = 1.sp
                )
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DocumentPill(
                    title = "Registration",
                    daysLeft = mockRegistrationDoc.daysUntilExpiry,
                    isSelected = selectedDocType == GovernmentDocumentType.BRTA_REGISTRATION,
                    onClick = { selectedDocType = GovernmentDocumentType.BRTA_REGISTRATION },
                    modifier = Modifier.weight(1f)
                )
                DocumentPill(
                    title = "Driving License",
                    daysLeft = mockLicenseDoc.daysUntilExpiry,
                    isSelected = selectedDocType == GovernmentDocumentType.DRIVING_LICENSE,
                    onClick = { selectedDocType = GovernmentDocumentType.DRIVING_LICENSE },
                    modifier = Modifier.weight(1f)
                )
                DocumentPill(
                    title = "Tax Token",
                    daysLeft = mockTaxToken.daysUntilExpiry,
                    isSelected = selectedDocType == GovernmentDocumentType.TAX_TOKEN,
                    onClick = { selectedDocType = GovernmentDocumentType.TAX_TOKEN },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // BACK TO GARAGE BUTTON
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onBackToGarage,
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceCardDark),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtleDark),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text(
                    text = "← Return to Garage Dashboard",
                    style = MotoVaultTypography.titleMedium.copy(fontSize = 13.sp, color = TextPrimary)
                )
            }
        }
    }
}

@Composable
private fun DocumentPill(
    title: String,
    daysLeft: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) KtmApexOrange else BorderSubtleDark
    val bgColor = if (isSelected) SurfaceElevatedDark else SurfaceCardDark

    Column(
        modifier = modifier
            .background(bgColor, RoundedCornerShape(12.dp))
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MotoVaultTypography.titleMedium.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) KtmApexOrange else TextPrimary
            )
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "$daysLeft days left",
            style = MotoVaultTypography.bodyMedium.copy(
                fontSize = 10.sp,
                color = if (daysLeft <= 60) StatusWarningYellow else TextMuted
            )
        )
    }
}
