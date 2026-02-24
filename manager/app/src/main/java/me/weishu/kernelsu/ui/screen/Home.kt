package me.weishu.kernelsu.ui.screen

import android.content.Context
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.system.Os
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Shape
import androidx.core.content.pm.PackageInfoCompat
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.NavGraphs
import com.ramcosta.composedestinations.generated.destinations.InstallScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ModuleScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SuperUserScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.blur
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.AutoAwesomeMotion
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Security
import androidx.compose.ui.draw.rotate
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.IntSize
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.layout.windowInsetsPadding
import me.weishu.kernelsu.ui.util.getHeaderImage
import me.weishu.kernelsu.ui.util.saveHeaderImage
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import me.weishu.kernelsu.KernelVersion
import me.weishu.kernelsu.Natives
import me.weishu.kernelsu.R
import me.weishu.kernelsu.getKernelVersion
import me.weishu.kernelsu.ui.component.RebootListPopup
import me.weishu.kernelsu.ui.component.rememberConfirmDialog
import me.weishu.kernelsu.ui.util.checkNewVersion
import me.weishu.kernelsu.ui.util.getLayoutStyle
import me.weishu.kernelsu.ui.util.getModuleCount
import me.weishu.kernelsu.ui.util.getSELinuxStatus
import me.weishu.kernelsu.ui.util.getSuperuserCount
import me.weishu.kernelsu.ui.util.module.LatestVersionInfo
import me.weishu.kernelsu.ui.util.rootAvailable

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>(start = true)
@Composable
fun HomeScreen(navigator: DestinationsNavigator) {
    val context = LocalContext.current
    val kernelVersion = getKernelVersion()

    val isManager = Natives.isManager
    val ksuVersion = if (isManager) Natives.version else null
    val lkmMode = ksuVersion?.let {
        if (kernelVersion.isGKI()) Natives.isLkmMode else null
    }
    val fullFeatured = isManager && !Natives.requireNewKernel() && rootAvailable()

    var headerImageUri by rememberSaveable { mutableStateOf(context.getHeaderImage()) }
    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            context.saveHeaderImage(uri.toString())
            headerImageUri = uri.toString()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            HomeBannerSection(
                ksuVersion = ksuVersion,
                lkmMode = lkmMode,
                headerImageUri = headerImageUri,
                onEditClick = { imagePicker.launch(arrayOf("image/*")) },
                onClickInstall = { navigator.navigate(InstallScreenDestination) }
            )
            HomeSystemInfoCard(
                context = context,
                lkmMode = lkmMode,
                ksuVersion = ksuVersion
            )
            Spacer(Modifier.height(16.dp))
            if (fullFeatured) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    HomeCountCard(
                        title = stringResource(R.string.superuser),
                        count = getSuperuserCount().toString(),
                        onClick = {
                            navigator.navigate(SuperUserScreenDestination) {
                                popUpTo(NavGraphs.root) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        modifier = Modifier.weight(1f),
                        elevated = false
                    )
                    HomeCountCard(
                        title = stringResource(R.string.module),
                        count = getModuleCount().toString(),
                        onClick = {
                            navigator.navigate(ModuleScreenDestination) {
                                popUpTo(NavGraphs.root) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        modifier = Modifier.weight(1f),
                        elevated = true
                    )
                }
            }
            if (isManager && Natives.requireNewKernel()) {
                Spacer(Modifier.height(16.dp))
                WarningCard(
                    stringResource(id = R.string.require_kernel_version).format(
                        ksuVersion, Natives.MINIMAL_SUPPORTED_KERNEL
                    )
                )
            }
            if (ksuVersion != null && !rootAvailable()) {
                Spacer(Modifier.height(16.dp))
                WarningCard(stringResource(id = R.string.grant_root_failed))
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun UpdateCard() {
    val context = LocalContext.current
    val latestVersionInfo = LatestVersionInfo()
    val newVersion by produceState(initialValue = latestVersionInfo) {
        value = withContext(Dispatchers.IO) {
            checkNewVersion()
        }
    }

    val currentVersionCode = getManagerVersion(context).second
    val newVersionCode = newVersion.versionCode
    val newVersionUrl = newVersion.downloadUrl
    val changelog = newVersion.changelog

    val uriHandler = LocalUriHandler.current
    val title = stringResource(id = R.string.module_changelog)
    val updateText = stringResource(id = R.string.module_update)

    AnimatedVisibility(
        visible = newVersionCode > currentVersionCode,
        enter = fadeIn() + expandVertically(),
        exit = shrinkVertically() + fadeOut()
    ) {
        val updateDialog = rememberConfirmDialog(onConfirm = { uriHandler.openUri(newVersionUrl) })
        WarningCard(
            message = stringResource(id = R.string.new_version_available).format(newVersionCode),
            MaterialTheme.colorScheme.outlineVariant
        ) {
            if (changelog.isEmpty()) {
                uriHandler.openUri(newVersionUrl)
            } else {
                updateDialog.showConfirm(
                    title = title,
                    content = changelog,
                    markdown = true,
                    confirm = updateText
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
    scrollBehavior: TopAppBarScrollBehavior? = null
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    val isOfficialEnabled = prefs.getBoolean("enable_official_launcher", false)
    val appNameId = if (isOfficialEnabled) R.string.app_name else R.string.app_name_mambo

    TopAppBar(
        title = {
            Text(
                text = stringResource(appNameId),
                fontWeight = FontWeight.Bold
            )
        },
        actions = { RebootListPopup() },
        windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
        scrollBehavior = scrollBehavior
    )
}

@Composable
private fun HomeBannerSection(
    ksuVersion: Int?,
    lkmMode: Boolean?,
    headerImageUri: String?,
    onEditClick: () -> Unit,
    onClickInstall: () -> Unit
) {
    val context = LocalContext.current
    val cs = MaterialTheme.colorScheme

    val workingMode = when (lkmMode) {
        null -> "LEGACY"
        true -> "LKM"
        else -> "GKI"
    }
    val statusText = if (ksuVersion != null)
        stringResource(R.string.home_working)
    else
        stringResource(R.string.home_not_installed)
    val versionText = when {
        ksuVersion != null -> "Version: $ksuVersion - $workingMode"
        else -> stringResource(R.string.home_click_to_install)
    }

    val imageLoader = remember(context) {
        ImageLoader.Builder(context)
            .components {
                if (SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .clickable { onClickInstall() }
    ) {
        if (headerImageUri != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(headerImageUri)
                    .crossfade(true)
                    .build(),
                imageLoader = imageLoader,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
        } else {
            Image(
                painter = painterResource(R.drawable.header_bg),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
        }

        // Gradient overlay
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, cs.surface.copy(alpha = 0.8f))
                    )
                )
        )

        // Edit icon at top-right
        IconButton(
            onClick = onEditClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
                .padding(8.dp)
                .size(40.dp)
                .background(Color.Black.copy(alpha = 0.35f), shape = RoundedCornerShape(50))
        ) {
            Icon(
                imageVector = Icons.Filled.Edit,
                contentDescription = stringResource(R.string.home_change_banner),
                tint = Color.White
            )
        }

        // Status and version text at bottom-left
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(24.dp)
        ) {
            Box(modifier = Modifier.clip(RoundedCornerShape(50))) {
                Box(modifier = Modifier.matchParentSize().blur(16.dp).background(cs.secondaryContainer.copy(alpha = 0.6f)))
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = cs.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            Box(modifier = Modifier.clip(RoundedCornerShape(50))) {
                Box(modifier = Modifier.matchParentSize().blur(16.dp).background(cs.secondaryContainer.copy(alpha = 0.6f)))
                Text(
                    text = versionText,
                    style = MaterialTheme.typography.labelMedium,
                    color = cs.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun HomeSystemInfoCard(
    context: Context,
    lkmMode: Boolean?,
    ksuVersion: Int?
) {
    val cs = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = 24.dp, bottomEnd = 24.dp),
        colors = CardDefaults.cardColors()
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            val managerVersion = getManagerVersion(context)
            HomeInfoRow(
                icon = Icons.Filled.AutoAwesomeMotion,
                label = stringResource(R.string.home_manager_version),
                value = "${managerVersion.first} (${managerVersion.second})"
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            val uname = Os.uname()
            HomeInfoRow(
                icon = Icons.Filled.Memory,
                label = stringResource(R.string.home_kernel),
                value = uname.release
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            val hookMethod = when (lkmMode) {
                null -> "LEGACY"
                true -> "LKM"
                else -> "GKI"
            }
            HomeInfoRow(
                icon = Icons.Filled.Security,
                label = stringResource(R.string.home_hook_method),
                value = hookMethod
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            HomeInfoRow(
                icon = Icons.Filled.Fingerprint,
                label = stringResource(R.string.home_susfs_version),
                value = stringResource(R.string.home_not_supported),
                valueColor = cs.outline
            )
        }
    }
}

@Composable
private fun HomeInfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = valueColor
        )
    }
}

@Composable
private fun HomeCountCard(
    title: String,
    count: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    elevated: Boolean = false
) {
    val cs = MaterialTheme.colorScheme
    Card(
        modifier = modifier,
        colors = if (elevated)
            CardDefaults.elevatedCardColors(
                containerColor = cs.surfaceColorAtElevation(4.dp)
            )
        else
            CardDefaults.elevatedCardColors(),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = if (elevated) 8.dp else 4.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(4.dp))
            Text(text = count, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
        }
    }
}

@Composable
fun WarningCard(
    message: String, color: Color = MaterialTheme.colorScheme.error, onClick: (() -> Unit)? = null
) {
    TonalCard(containerColor = color) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(onClick?.let { Modifier.clickable { it() } } ?: Modifier)
                .padding(24.dp)
        ) {
            Text(
                text = message, style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun TonalCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
    shape: Shape = RoundedCornerShape(20.dp),
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = shape
    ) {
        content()
    }
}

@Composable
fun LearnMoreCard() {
    val uriHandler = LocalUriHandler.current
    val url = stringResource(R.string.home_learn_kernelsu_url)

    TonalCard {
        Row(modifier = Modifier
            .fillMaxWidth()
            .clickable {
                uriHandler.openUri(url)
            }
            .padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(
                    text = stringResource(R.string.home_learn_kernelsu),
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.home_click_to_learn_kernelsu),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
fun DonateCard() {
    val uriHandler = LocalUriHandler.current

    TonalCard {
        Row(modifier = Modifier
            .fillMaxWidth()
            .clickable {
                uriHandler.openUri("https://patreon.com/weishu")
            }
            .padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(
                    text = stringResource(R.string.home_support_title),
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.home_support_content),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
private fun InfoCard() {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    val isManager = Natives.isManager
    val ksuVersion = if (isManager) Natives.version else null

    // State expand
    var expanded by rememberSaveable { mutableStateOf(false) }
    val developerOptionsEnabled = prefs.getBoolean("enable_developer_options", false)

    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow), // Putaran smooth pelan
        label = "arrowRotation"
    )

    TonalCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardDefaults.shape)
            .clickable { expanded = !expanded }
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            @Composable
            fun InfoCardItem(label: String, content: String, icon: Any? = null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (icon != null) {
                        val modifier = Modifier.padding(end = 20.dp)
                        val tint = MaterialTheme.colorScheme.primary
                        when (icon) {
                            is ImageVector -> Icon(icon, null, modifier, tint = tint)
                            is Painter -> Icon(icon, null, modifier, tint = tint)
                        }
                    }
                    Column {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = content,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp),
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            Column {
                val managerVersion = getManagerVersion(context)
                val uidText = if (developerOptionsEnabled) " | UID: ${android.os.Process.myUid()}" else ""

                InfoCardItem(
                    label = stringResource(R.string.home_manager_version),
                    content = "${managerVersion.first} (${managerVersion.second})$uidText",
                    icon = Icons.Filled.AutoAwesomeMotion,
                )

                Spacer(Modifier.height(16.dp))

                val uname = Os.uname()
                InfoCardItem(
                    label = stringResource(R.string.home_kernel),
                    content = "${uname.release} (${uname.machine})",
                    icon = Icons.Filled.Memory,
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(animationSpec = tween(400)) + expandVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ),
                exit = fadeOut(animationSpec = tween(200)) + shrinkVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            ) {
                Column {
                    Spacer(Modifier.height(16.dp))

                    InfoCardItem(
                        label = stringResource(R.string.home_android),
                        content = "${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})",
                        icon = Icons.Filled.Android,
                    )

                    Spacer(Modifier.height(16.dp))

                    InfoCardItem(
                        label = stringResource(R.string.home_selinux_status),
                        content = getSELinuxStatus(),
                        icon = Icons.Filled.Security,
                    )

                    Spacer(Modifier.height(16.dp))

                    InfoCardItem(
                        label = stringResource(R.string.home_fingerprint),
                        content = Build.FINGERPRINT,
                        icon = Icons.Filled.Fingerprint,
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = "Show more",
                        modifier = Modifier.rotate(arrowRotation) // Rotasi Smooth
                    )
                }
            }
        }
    }
}

fun getManagerVersion(context: Context): Pair<String, Long> {
    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)!!
    val versionCode = PackageInfoCompat.getLongVersionCode(packageInfo)
    return Pair(packageInfo.versionName!!, versionCode)
}

@Preview
@Composable
private fun StatusCardPreview() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Preview 1: Classic Layout (Working)
        StatusCard(
            kernelVersion = KernelVersion(5, 10, 101),
            ksuVersion = 1,
            lkmMode = null,
            fullFeatured = false,
            useClassicLayout = true // Classic
        )

        // Preview 2: Modern Layout (Working + Full Featured)
        StatusCard(
            kernelVersion = KernelVersion(5, 10, 101),
            ksuVersion = 20000,
            lkmMode = true,
            fullFeatured = true,
            useClassicLayout = false // Modern
        )

        // Preview 3: Classic Layout (Not Installed)
        StatusCard(
            kernelVersion = KernelVersion(5, 10, 101),
            ksuVersion = null,
            lkmMode = true,
            fullFeatured = true,
            useClassicLayout = true // Classic
        )

        // Preview 4: Modern Layout (Unsupported)
        StatusCard(
            kernelVersion = KernelVersion(4, 10, 101),
            ksuVersion = null,
            lkmMode = false,
            fullFeatured = false,
            useClassicLayout = false // Modern
        )
    }
}

@Preview
@Composable
private fun WarningCardPreview() {
    Column {
        WarningCard(message = "Warning message")
        WarningCard(
            message = "Warning message ",
            MaterialTheme.colorScheme.outlineVariant,
            onClick = {})
    }
}