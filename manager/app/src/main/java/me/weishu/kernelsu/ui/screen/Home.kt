package me.weishu.kernelsu.ui.screen

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.generated.destinations.InstallScreenDestination
import me.weishu.kernelsu.BuildConfig
import me.weishu.kernelsu.KernelVersion
import me.weishu.kernelsu.Natives
import me.weishu.kernelsu.R
import me.weishu.kernelsu.getKernelVersion
import me.weishu.kernelsu.ui.component.PowerMenuButton
import me.weishu.kernelsu.ui.util.getHeaderImage
import me.weishu.kernelsu.ui.util.saveHeaderImage

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
            
            // 1. BANNER CUSTOM (Full atas, sudut bawah lurus, tanpa shadow)
            HomeBanner(ksuVersion = ksuVersion)

            // 2 & 3. Blok konten dengan efek overlap di atas banner
            // layout modifier menggeser blok ini 24.dp ke atas agar menimpa banner,
            // background dengan sudut atas bulat menciptakan ilusi "concave" di banner.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        val overlapPx = 24.dp.roundToPx()
                        layout(placeable.width, placeable.height - overlapPx) {
                            placeable.placeRelative(0, -overlapPx)
                        }
                    }
                    .background(
                        color = MaterialTheme.colorScheme.background,
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                    )
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            ) {
                // STATUS INDICATOR & INSTALL BUTTON
                StatusSection(ksuVersion = ksuVersion, navigator = navigator)

                // SATU CARDVIEW UNTUK SEMUA INFO (GRID)
                UnifiedInfoGrid(
                    kernelVersion = kernelVersion,
                    managerVersionName = BuildConfig.VERSION_NAME,
                    managerVersionCode = BuildConfig.VERSION_CODE
                )

                // CARDVIEW FEEDBACK / CONTACT
                FeedbackCard()
            }
        }
    }
}

@Composable
fun HomeBanner(ksuVersion: Int?) {
    val context = LocalContext.current
    val contentResolver = context.contentResolver
    var headerImageUri by remember { mutableStateOf(context.getHeaderImage()) }
    
    // Menghitung tinggi status bar agar teks/tombol tidak tertimpa jam/baterai HP
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    // Launcher untuk ganti gambar banner
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

    // Warna ikon overlay: putih kalau ada gambar custom, ikut Material You kalau default
    val iconTint = if (headerImageUri != null) Color.White else MaterialTheme.colorScheme.onPrimaryContainer

    Box(
        modifier = Modifier
            .fillMaxWidth()
            // Tinggi total = tinggi base banner + tinggi status bar HP
            .height(220.dp + statusBarPadding)
            // Sudut bawah dibiarkan lurus; efek lengkungan ke dalam
            // dibuat oleh blok konten yang menimpa banner di bawah.
    ) {
        // Latar Belakang Banner: gunakan header_bg.webp sebagai default
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
            // Default: gunakan header_bg.webp sebagai gambar latar banner
            Image(
                painter = painterResource(id = R.drawable.header_bg),
                contentDescription = "Default Banner",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Overlay ringan agar teks tetap terbaca di atas gambar default
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
            )
        }

        // Logo KamiSU (Tengah Banner) - gambar ic_kamisu.png
        Image(
            painter = painterResource(id = R.drawable.ic_kamisu),
            contentDescription = "KamiSU Logo",
            modifier = Modifier
                .size(80.dp)
                .align(Alignment.Center)
        )

        // Teks KamiSU (Kanan Atas) + Status Not Installed (Kecil di Bawahnya)
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                // Padding atas ditambah statusBarPadding agar turun ke bawah baterai
                .padding(top = 28.dp + statusBarPadding, end = 24.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = stringResource(id = R.string.app_name), // Teks "KamiSU"
                fontSize = 38.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            
            // Teks "Not Installed" kecil jika belum terinstall
            if (ksuVersion == null) {
                Text(
                    text = stringResource(id = R.string.home_status_not_installed),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        // Tombol di kanan bawah banner: [Power] [Ganti Gambar]
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 12.dp, end = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tombol Power Menu (hanya tampil jika KSU aktif)
            PowerMenuButton(iconTint = iconTint)

            // Tombol Ganti Gambar Banner
            IconButton(onClick = { launcher.launch(arrayOf("image/*")) }) {
                Icon(
                    imageVector = Icons.Filled.Image,
                    contentDescription = "Change Banner",
                    tint = iconTint
                )
            }
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
                    Text(text = kernelVersion.toString(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
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
                
                // Blok: Support Us
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

                // Blok: Learn KernelSU/KamiSU
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

@Composable
fun FeedbackCard() {
    val uriHandler = LocalUriHandler.current

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Subtitle
            Text(
                text = stringResource(id = R.string.home_feedback_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            // Body text
            Text(
                text = stringResource(id = R.string.home_feedback_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            // Icon row: GitHub + Telegram
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // GitHub
                IconButton(onClick = { uriHandler.openUri("https://github.com/kaminarich/KamiSU") }) {
                    Icon(
                        imageVector = Icons.Filled.Code,
                        contentDescription = "GitHub",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
                // Telegram
                IconButton(onClick = { uriHandler.openUri("https://t.me/kaminarich") }) {
                    Icon(
                        imageVector = Icons.Filled.Send,
                        contentDescription = "Telegram",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}
