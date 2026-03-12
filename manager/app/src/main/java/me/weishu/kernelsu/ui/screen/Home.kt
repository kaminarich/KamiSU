package me.weishu.kernelsu.ui.screen

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.generated.destinations.InstallScreenDestination
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.weishu.kernelsu.BuildConfig
import me.weishu.kernelsu.KernelVersion
import me.weishu.kernelsu.Natives
import me.weishu.kernelsu.R
import me.weishu.kernelsu.getKernelVersion
import me.weishu.kernelsu.ui.component.RebootDropdownItems
import me.weishu.kernelsu.ui.util.getHeaderImage
import me.weishu.kernelsu.ui.util.reboot
import me.weishu.kernelsu.ui.util.saveHeaderImage
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@RootNavGraph(start = true)
@Destination
@Composable
fun HomeScreen(navigator: DestinationsNavigator) {
    val isManager = Natives.isManager
    val ksuVersion = if (isManager) Natives.version else null
    val kernelVersion = getKernelVersion()
    val scrollState = rememberScrollState()

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            // 1. BANNER CUSTOM
            HomeBanner(ksuVersion = ksuVersion, navigator = navigator)

            // 2. STATUS INDICATOR & INSTALL BUTTON
            StatusSection(ksuVersion = ksuVersion, navigator = navigator)

            // 3. SUSFS VERSION CARD
            SusfsVersionCard()

            // 4. INFO GRID
            UnifiedInfoGrid(
                kernelVersion = kernelVersion,
                managerVersionName = BuildConfig.VERSION_NAME,
                managerVersionCode = BuildConfig.VERSION_CODE
            )
        }
    }
}

private fun getOrSaveInstallDate(context: Context): String {
    val prefs = context.getSharedPreferences("ksu_prefs", Context.MODE_PRIVATE)
    return prefs.getString("install_date", null) ?: run {
        val sdf = SimpleDateFormat("EEE, dd/MM/yy", Locale.getDefault())
        val dateStr = sdf.format(Date())
        prefs.edit().putString("install_date", dateStr).apply()
        dateStr
    }
}

@Composable
fun HomeBanner(ksuVersion: Int?, navigator: DestinationsNavigator) {
    val context = LocalContext.current
    val contentResolver = context.contentResolver
    var headerImageUri by remember { mutableStateOf(context.getHeaderImage()) }
    var showMenu by remember { mutableStateOf(false) }

    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            try {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                context.saveHeaderImage(uri.toString())
                headerImageUri = uri.toString()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val installDate = if (ksuVersion != null) {
        remember { getOrSaveInstallDate(context) }
    } else null

    val iconTint = if (headerImageUri != null) Color.White else MaterialTheme.colorScheme.onPrimaryContainer

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp + statusBarPadding)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
            )
            .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
    ) {
        if (headerImageUri != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(Uri.parse(headerImageUri))
                    .crossfade(true)
                    .build(),
                contentDescription = "Banner",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primaryContainer)
            )
        }

        // Edit banner button (top-left)
        IconButton(
            onClick = { launcher.launch(arrayOf("image/*")) },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 16.dp + statusBarPadding, start = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Edit,
                contentDescription = "Change Banner",
                tint = iconTint
            )
        }

        // Three-dot menu button (top-right)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp + statusBarPadding, end = 8.dp)
        ) {
            IconButton(onClick = { showMenu = true }) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = "More options",
                    tint = iconTint
                )
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(id = R.string.home_install_update)) },
                    onClick = {
                        showMenu = false
                        navigator.navigate(InstallScreenDestination)
                    },
                    leadingIcon = { Icon(Icons.Filled.Build, contentDescription = null) }
                )
                HorizontalDivider()
                RebootDropdownItems { reason ->
                    showMenu = false
                    reboot(reason)
                }
            }
        }

        // Title "KamiSU" at bottom-left + subtitle below
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 24.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = stringResource(id = R.string.app_name),
                fontSize = 38.sp,
                fontWeight = FontWeight.ExtraBold,
                color = iconTint
            )
            if (ksuVersion == null) {
                Text(
                    text = stringResource(id = R.string.home_status_not_installed),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = iconTint.copy(alpha = 0.85f),
                    modifier = Modifier.padding(top = 2.dp)
                )
            } else if (installDate != null) {
                Text(
                    text = installDate,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Light,
                    color = iconTint.copy(alpha = 0.80f),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
fun SusfsVersionCard() {
    val notFoundText = stringResource(id = R.string.home_susfs_not_found)
    val enabledText = stringResource(id = R.string.home_susfs_enabled)

    val susfsStatus by produceState<String?>(initialValue = null) {
        value = withContext(Dispatchers.IO) {
            try {
                val enabledFile = File("/proc/sys/fs/susfs/enabled")
                if (enabledFile.exists() && enabledFile.readText().trim() == "1") enabledText
                else notFoundText
            } catch (e: Exception) {
                notFoundText
            }
        }
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(id = R.string.home_susfs_version),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = susfsStatus ?: stringResource(id = R.string.home_susfs_not_found),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
fun StatusSection(ksuVersion: Int?, navigator: DestinationsNavigator) {
    if (ksuVersion != null) {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Verified,
                    contentDescription = "Working",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(id = R.string.running_flawlessly),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    } else {
        Button(
            onClick = { navigator.navigate(InstallScreenDestination) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp)
                .height(54.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Filled.Build, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(id = R.string.home_install),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun UnifiedInfoGrid(
    kernelVersion: KernelVersion,
    managerVersionName: String,
    managerVersionCode: Int
) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

            // --- ROW 1: KERNEL & MANAGER ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Filled.Memory, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = stringResource(id = R.string.home_kernel_version), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = kernelVersion.toString(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                }

                VerticalDivider(modifier = Modifier.padding(vertical = 12.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Filled.PhoneAndroid, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = stringResource(id = R.string.home_manager_version), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = "$managerVersionName ($managerVersionCode)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // --- ROW 2: TELEGRAM & LEARN ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
            ) {
                // Telegram
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { uriHandler.openUri(context.getString(R.string.home_join_telegram_url)) }
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.Send,
                        contentDescription = null,
                        tint = Color(0xFF2196F3),
                        modifier = Modifier.rotate(-30f)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = stringResource(id = R.string.home_join_telegram), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }

                VerticalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Learn KamiSU
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { uriHandler.openUri("https://kernelsu.org/") }
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Filled.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = stringResource(id = R.string.learn_kamisu), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
