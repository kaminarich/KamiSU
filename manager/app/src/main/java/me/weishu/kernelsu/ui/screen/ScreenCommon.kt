package me.weishu.kernelsu.ui.screen

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape

@Composable
fun TonalCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    shape: Shape = MaterialTheme.shapes.medium,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        color = containerColor,
        shape = shape,
        content = content
    )
}

@Suppress("DEPRECATION")
fun getManagerVersion(context: Context): String {
    val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    return "${pInfo.versionName} (${pInfo.versionCode})"
}
