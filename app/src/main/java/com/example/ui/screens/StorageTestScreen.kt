package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.MgAppUiState
import com.example.ui.components.MgConfirmDialog
import com.example.ui.components.MgScreenHeader
import com.example.ui.components.StorageGaugeCard
import com.example.util.StorageAndCryptoUtils
import java.util.Locale

@Composable
fun StorageTestScreen(
    uiState: MgAppUiState,
    onBack: () -> Unit,
    onPickSingleHashFile: () -> Unit,
    onPickVerifySourceFile: () -> Unit,
    onPickVerifyTargetFile: () -> Unit,
    onRunTwoFileVerification: () -> Unit,
    onRunConfirmedHardwareBenchmark: () -> Unit,
    onSelectSafFolder: () -> Unit
) {
    BackHandler(onBack = onBack)

    var showBenchmarkConfirmDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        MgScreenHeader(
            title = "Storage Info & Verification",
            subtitle = "File Integrity (SHA-256) vs. Hardware Read/Write Test",
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("storage_test_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Phone Storage
            item {
                StorageGaugeCard(
                    stats = uiState.phoneStorage,
                    icon = Icons.Default.Memory,
                    accentColor = MaterialTheme.colorScheme.secondary
                )
            }

            // 2. Selected USB / SAF Volume Capacity
            item {
                val usbVol = uiState.usbVolumeStorage
                if (usbVol != null && usbVol.isReliable) {
                    StorageGaugeCard(
                        stats = usbVol,
                        icon = Icons.Default.Usb,
                        accentColor = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Selected USB / SAF Volume Capacity",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (uiState.selectedTreeUri == null) {
                                    "No USB or SAF folder is currently selected. Select a USB volume to inspect its capacity when supported by the document provider."
                                } else {
                                    "Selected folder '${uiState.selectedTreeName}' does not expose raw filesystem block metrics via OS fstatvfs. MG USB Multi-Tool only displays volume capacity when reliably reported by the OS."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedButton(onClick = onSelectSafFolder) {
                                Text(if (uiState.selectedTreeUri == null) "Select USB / Folder" else "Switch USB / Folder")
                            }
                        }
                    }
                }
            }

            // 3. FILE INTEGRITY CHECK (SHA-256 & Copy Verification)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "1. File Copy & SHA-256 Integrity Check",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Read-only cryptographic check — does not write to the drive",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Compute a single file's SHA-256 checksum or compare an original file against a copied file on your USB drive to verify zero byte corruption.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onPickSingleHashFile,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("compute_single_sha256_button")
                        ) {
                            Text("Select File to Compute SHA-256")
                        }

                        val singleRes = uiState.singleHashResult
                        if (singleRes != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "${singleRes.fileName} (${StorageAndCryptoUtils.formatBytes(singleRes.sizeBytes)})",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "SHA-256: ${singleRes.sha256}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(14.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CompareArrows,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Two-File Copy Verification (Size + SHA-256)",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = onPickVerifySourceFile,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("pick_verify_source_button")
                            ) {
                                Text(uiState.verifySourceName ?: "1. Source File")
                            }
                            OutlinedButton(
                                onClick = onPickVerifyTargetFile,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("pick_verify_target_button")
                            ) {
                                Text(uiState.verifyTargetName ?: "2. Copied File")
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = onRunTwoFileVerification,
                            enabled = uiState.verifySourceUri != null && uiState.verifyTargetUri != null && !uiState.isOperationInProgress,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("run_copy_verification_button")
                        ) {
                            Text("Verify Copy Integrity")
                        }

                        val verifyRes = uiState.copyVerificationResult
                        if (verifyRes != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (verifyRes.isMatch) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.errorContainer
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (verifyRes.isMatch) Icons.Default.CheckCircle else Icons.Default.Error,
                                            contentDescription = null,
                                            tint = if (verifyRes.isMatch) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = verifyRes.summary,
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Source SHA-256: ${verifyRes.sourceSha256}",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                    Text(
                                        text = "Target SHA-256: ${verifyRes.targetSha256}",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. DEDICATED HARDWARE READ/WRITE BENCHMARK TEST
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "2. Hardware Read/Write Speed & Flash Test",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                                Text(
                                    text = "Physical I/O test using dedicated temp file (mg_usb_benchmark_temp.bin)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Unlike a basic file checksum, this test performs actual physical writes and reads on the selected folder using a temporary 4 MB payload, measures real MB/s throughput, verifies that flash blocks return identical SHA-256 data, and automatically removes the test file. Never formats or erases your existing files.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { showBenchmarkConfirmDialog = true },
                            enabled = uiState.selectedTreeUri != null && !uiState.isRunningBenchmark,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("start_hardware_test_button")
                        ) {
                            Text(
                                if (uiState.selectedTreeUri == null) {
                                    "Select a USB/SAF Folder First"
                                } else {
                                    "Run 4 MB Read/Write Hardware Test (Requires Confirmation)"
                                }
                            )
                        }

                        if (uiState.isRunningBenchmark) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = uiState.benchmarkStageText,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        val bench = uiState.benchmarkResult
                        if (bench != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(
                                    1.dp,
                                    if (bench.success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = bench.message,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = if (bench.success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                    )
                                    if (bench.bytesTested > 0L) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = String.format(
                                                Locale.US,
                                                "Write Speed: %.2f MB/s  |  Read Speed: %.2f MB/s",
                                                bench.writeSpeedMbps,
                                                bench.readSpeedMbps
                                            ),
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                        Text(
                                            text = "Payload SHA-256: ${bench.actualSha256}",
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
    }

    if (showBenchmarkConfirmDialog) {
        MgConfirmDialog(
            title = stringResource(R.string.write_test_confirm_title),
            message = stringResource(R.string.write_test_confirm_body),
            confirmText = "Start R/W Test",
            isDestructive = false,
            onConfirm = {
                showBenchmarkConfirmDialog = false
                onRunConfirmedHardwareBenchmark()
            },
            onDismiss = { showBenchmarkConfirmDialog = false }
        )
    }
}
