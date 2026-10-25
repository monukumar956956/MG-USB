package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.UsbOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.HistoryEntity
import com.example.ui.MgAppUiState
import com.example.ui.MgDestination
import com.example.ui.components.MgNeonLogoBadge
import com.example.ui.components.StorageGaugeCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class QuickAccessCardSpec(
    val destination: MgDestination,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accentColor: Color,
    val testTag: String
)

@Composable
fun HomeScreen(
    uiState: MgAppUiState,
    recentHistory: List<HistoryEntity>,
    onNavigate: (MgDestination) -> Unit,
    onSelectSafFolder: () -> Unit,
    onRefreshStatus: () -> Unit
) {
    val neonGreen = MaterialTheme.colorScheme.primary
    val cyberCyan = MaterialTheme.colorScheme.secondary
    val amber = MaterialTheme.colorScheme.tertiary

    val quickCards = listOf(
        QuickAccessCardSpec(
            destination = MgDestination.FILES,
            title = "Files",
            subtitle = "SAF USB Browser & File Ops",
            icon = Icons.Default.Folder,
            accentColor = neonGreen,
            testTag = "quick_card_files"
        ),
        QuickAccessCardSpec(
            destination = MgDestination.MUSIC,
            title = "Music",
            subtitle = "Media3 ExoPlayer Deck",
            icon = Icons.Default.LibraryMusic,
            accentColor = cyberCyan,
            testTag = "quick_card_music"
        ),
        QuickAccessCardSpec(
            destination = MgDestination.BACKUP,
            title = "Backup",
            subtitle = "Batch Copy & Progress",
            icon = Icons.Default.Backup,
            accentColor = neonGreen,
            testTag = "quick_card_backup"
        ),
        QuickAccessCardSpec(
            destination = MgDestination.STORAGE_TEST,
            title = "Storage Test",
            subtitle = "SHA-256 & Hardware R/W",
            icon = Icons.Default.Speed,
            accentColor = amber,
            testTag = "quick_card_storage_test"
        ),
        QuickAccessCardSpec(
            destination = MgDestination.ANDROID_TOOLS,
            title = "Android Tools",
            subtitle = "APK Installer & Safe ZIP",
            icon = Icons.Default.Android,
            accentColor = neonGreen,
            testTag = "quick_card_android_tools"
        ),
        QuickAccessCardSpec(
            destination = MgDestination.PC_TOOLS,
            title = "PC Tools",
            subtitle = "Portable EXE & ISO Guide",
            icon = Icons.Default.Computer,
            accentColor = cyberCyan,
            testTag = "quick_card_pc_tools"
        ),
        QuickAccessCardSpec(
            destination = MgDestination.HISTORY,
            title = "History",
            subtitle = "Local Action Audit Log",
            icon = Icons.Default.History,
            accentColor = amber,
            testTag = "quick_card_history"
        ),
        QuickAccessCardSpec(
            destination = MgDestination.SETTINGS,
            title = "Settings",
            subtitle = "Theme, Security & About",
            icon = Icons.Default.Settings,
            accentColor = neonGreen,
            testTag = "quick_card_settings"
        )
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_dashboard_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Hero MG Gaming Header Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.5.dp, neonGreen.copy(alpha = 0.5f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    neonGreen.copy(alpha = 0.14f),
                                    cyberCyan.copy(alpha = 0.06f),
                                    Color.Transparent
                                )
                            )
                        )
                        .padding(18.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        MgNeonLogoBadge(size = 60.dp)
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "MG USB MULTI-TOOL",
                                style = MaterialTheme.typography.headlineMedium,
                                color = neonGreen
                            )
                            Text(
                                text = "USB-C OTG Storage, SHA-256 & Media Suite",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, neonGreen.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = "NO ROOT REQUIRED • SAF SANDBOXED",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = neonGreen,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                        IconButton(
                            onClick = onRefreshStatus,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("refresh_dashboard_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh storage and USB status",
                                tint = neonGreen
                            )
                        }
                    }
                }
            }
        }

        // 2. Connected USB Drive Status Card (Hardware + SAF Verification)
        item {
            UsbStatusDashboardCard(
                uiState = uiState,
                onSelectSafFolder = onSelectSafFolder,
                onOpenFiles = { onNavigate(MgDestination.FILES) }
            )
        }

        // 3. Phone Storage Card & Optional Verified USB Volume Capacity
        item {
            StorageGaugeCard(
                stats = uiState.phoneStorage,
                icon = Icons.Default.Memory,
                accentColor = cyberCyan,
                actionButtonText = "Test",
                onActionClick = { onNavigate(MgDestination.STORAGE_TEST) }
            )
        }

        if (uiState.usbVolumeStorage != null && uiState.usbVolumeStorage.isReliable) {
            item {
                StorageGaugeCard(
                    stats = uiState.usbVolumeStorage,
                    icon = Icons.Default.Usb,
                    accentColor = neonGreen,
                    actionButtonText = "Browse",
                    onActionClick = { onNavigate(MgDestination.FILES) }
                )
            }
        }

        // 4. Quick-Access 2x4 Grid
        item {
            Text(
                text = "QUICK-ACCESS COMMAND DECK",
                style = MaterialTheme.typography.labelLarge,
                color = neonGreen,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        items(quickCards.chunked(2).size) { rowIndex ->
            val pair = quickCards.chunked(2)[rowIndex]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                pair.forEach { cardSpec ->
                    QuickAccessTile(
                        spec = cardSpec,
                        onClick = { onNavigate(cardSpec.destination) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (pair.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        // 5. Recent Local Activity Preview
        if (recentHistory.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "RECENT LOCAL ACTIVITY",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = "View All",
                                style = MaterialTheme.typography.labelMedium,
                                color = neonGreen,
                                modifier = Modifier
                                    .clickable { onNavigate(MgDestination.HISTORY) }
                                    .padding(6.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        val formatter = SimpleDateFormat("HH:mm:ss", Locale.US)
                        recentHistory.take(3).forEach { entry ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = entry.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = entry.details,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                                Text(
                                    text = formatter.format(Date(entry.timestamp)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UsbStatusDashboardCard(
    uiState: MgAppUiState,
    onSelectSafFolder: () -> Unit,
    onOpenFiles: () -> Unit
) {
    val hasHardwareUsb = uiState.connectedUsbDevices.isNotEmpty()
    val hasSafFolder = uiState.selectedTreeUri != null
    val statusColor = when {
        hasHardwareUsb && hasSafFolder -> MaterialTheme.colorScheme.primary
        hasHardwareUsb || hasSafFolder -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("usb_status_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.45f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(statusColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (hasHardwareUsb || hasSafFolder) Icons.Default.Usb else Icons.Default.UsbOff,
                        contentDescription = "USB Connection Status",
                        tint = statusColor
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (hasHardwareUsb) {
                            "USB OTG Hardware Verified (${uiState.connectedUsbDevices.size} device)"
                        } else {
                            "No Physical USB OTG Device Verified"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    val hardwareDesc = if (hasHardwareUsb) {
                        uiState.connectedUsbDevices.joinToString { dev ->
                            "${dev.productName} (${dev.manufacturerName})"
                        }
                    } else {
                        "Connect a USB-C OTG flash drive or select an accessible folder via Android SAF."
                    }
                    Text(
                        text = hardwareDesc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (hasSafFolder) {
                                "Active SAF Folder: ${uiState.selectedTreeName}"
                            } else {
                                "SAF Document Tree: Not Selected"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = if (hasSafFolder) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (hasSafFolder) {
                                "${uiState.currentDirectoryFiles.size} item(s) indexed in current directory"
                            } else {
                                "Grant folder access through the system picker to browse & manage files."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onSelectSafFolder,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("select_usb_saf_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (hasSafFolder) "Change USB/Folder" else "Select USB / Folder")
                }
                if (hasSafFolder) {
                    OutlinedButton(
                        onClick = onOpenFiles,
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("open_files_from_home_button")
                    ) {
                        Text("Open Files")
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickAccessTile(
    spec: QuickAccessCardSpec,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag(spec.testTag),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, spec.accentColor.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(spec.accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = spec.icon,
                    contentDescription = spec.title,
                    tint = spec.accentColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = spec.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = spec.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}
