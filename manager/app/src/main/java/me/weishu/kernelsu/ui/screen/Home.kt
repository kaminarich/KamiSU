package me.weishu.kernelsu.ui.screen

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
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

/**
 * Custom shape that clips the banner with a concave (inward-curving) bottom edge.
 * The entire bottom of the shape bows upward at the center, creating a smooth arc
 * that visually complements rounded elements placed below the banner.
 */
private class ConcaveBottomShape(private val archDepthDp: Dp) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val archDepth = with(density) { archDepthDp.toPx() }
        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(size.width, 0f)
            lineTo(size.width, size.height)
            // Concave bottom arc: entire bottom edge curves inward (bows upward at center)
            quadraticBezierTo(size.width / 2f, size.height - archDepth, 0f, size.height)
            close()
        }
        return Outline.Generic(path)
    }
}

@Destination<RootGraph>(start = true)
@Composable
fun HomeScreen(navigator: DestinationsNavigator) {
    val isManager = Natives.isManager
    val ksuVersion = if (isManager) Natives.version else null
    val kernelVersion = getKernelVersion()
    val scrollState = rememberScrollState()

    // Menghilangkan TopAppBar bawaan (Header)
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                // Hanya gunakan bottom padding. Top dibiarkan agar banner full ke atas layar
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            // 1. BANNER CUSTOM (Full atas, sudut bawah cekung, pakai shadow)
            HomeBanner(ksuVersion = ksuVersion, navigator = navigator)

            // 2. STATUS INDICATOR & INSTALL BUTTON
            StatusSection(ksuVersion = ksuVersion, navigator = navigator)

            // 3. SUSFS VERSION CARD
            SusfsVersionCard()

            // 4. SATU CARDVIEW UNTUK SEMUA INFO (GRID)
            UnifiedInfoGrid(
                kernelVersion = kernelVersion,
                managerVersionName = BuildConfig.VERSION_NAME,
                managerVersionCode = BuildConfig.VERSION_CODE
            )

            // 5. SOMETHING WRONG CARD
            SomethingWrongCard()

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun HomeBanner(ksuVersion: Int?, navigator: DestinationsNavigator) {
    val context = LocalContext.current
    val contentResolver = context.contentResolver
    var headerImageUri by remember { mutableStateOf(context.getHeaderImage()) }
    var showMenu by remember { mutableStateOf(false) }

    // Menghitung tinggi status bar agar teks/tombol tidak tertimpa jam/baterai HP
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    // Launcher untuk ganti gambar banner (klik banner)
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

    val currentDate = remember {
        SimpleDateFormat("EEE, dd/MM/yy", Locale.getDefault()).format(Date())
    }

    val iconTint = Color.White
    val bannerShape = ConcaveBottomShape(32.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            // Tinggi total = tinggi base banner + tinggi status bar HP
            .height(220.dp + statusBarPadding)
            // Shadow mengikuti bentuk cekung
            .shadow(elevation = 8.dp, shape = bannerShape)
            // Clip konten sesuai bentuk cekung
            .clip(bannerShape)
            // Klik banner untuk ganti gambar
            .clickable { launcher.launch(arrayOf("image/*")) }
    ) {
        // Latar Belakang Banner (Gambar atau Default Warna Bawaan)
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
            // Overlay warna gelap tipis supaya teks selalu terbaca kalau pakai gambar
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
            )
        } else {
            // Default banner image (header_bg.webp) when no custom image is set
            Image(
                painter = painterResource(id = R.drawable.header_bg),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Dark overlay to ensure text remains readable over the image
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
            )
        }

        // Tombol Tiga Titik (Kanan Atas) untuk menu install & power
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

        // Teks KamiSU (Kiri Bawah) + Tanggal atau "Not Installed" di bawahnya
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
            } else {
                Text(
                    text = currentDate,
                    fontSize = 13.sp,
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
        // CardView "Running Flawlessly" (Working) di bawah banner
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
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Verified, // Ikon Centang (ic_work)
                    contentDescription = "Working",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = stringResource(id = R.string.running_flawlessly),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    } else {
        // Tombol "Click to Install" (Not Installed) di luar banner
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

    // Satu CardView Utama
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
            
            // --- BARIS 1: KERNEL & MANAGER ---
            Row(modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)) {
                
                // Blok: Kernel Version
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
                    Text(text = kernelVersion.toString(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 1, modifier = Modifier.basicMarquee())
                }

                VerticalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Blok: Manager Version
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

            // --- BARIS 2: SUPPORT & LEARN ---
            Row(modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)) {
                
                // Blok: Support Us (monet color, links to Telegram channel)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { uriHandler.openUri("https://t.me/Kaminarich_HeavenlyArchive/328") }
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Filled.Favorite, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = stringResource(id = R.string.home_support_us), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }

                VerticalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Blok: Learn KamiSU (links to KamiSU repo)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { uriHandler.openUri("https://github.com/kaminarich/KamiSU") }
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

@Composable
fun SomethingWrongCard() {
    val uriHandler = LocalUriHandler.current

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                text = stringResource(id = R.string.home_something_wrong),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(id = R.string.home_something_wrong_content),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = { uriHandler.openUri("https://github.com/kaminarich") }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_github),
                        contentDescription = "GitHub",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = { uriHandler.openUri("https://t.me/kaminarivh") }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_telegram),
                        contentDescription = "Telegram",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

