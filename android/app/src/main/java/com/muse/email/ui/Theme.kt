package com.muse.email.ui

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import androidx.compose.foundation.clickable

/**
 * 设计令牌直接对齐 domainmail/public/style.css 的 :root。
 * 包括亮/暗两套，以及 README 里标注的手机端实测尺寸。
 * 0.5.0 起走 Material 3 Expressive：主题换成 MaterialExpressiveTheme（形状与动效
 * 默认就是 Expressive 的），动效用 MotionScheme.expressive() 的弹簧方案。
 * Expressive API 在 material3 1.4.0 仍是实验性的，这里统一 opt-in。
 */
object MuseUi {
    val Blue = Color(0xFF2563EB)
    val BlueDark = Color(0xFF1D4ED8)
    val BlueSoft = Color(0xFFEDF4FF)
    val Star = Color(0xFFF0A92B)
    val Danger = Color(0xFFE5484D)
    val Ok = Color(0xFF22C34D)

    private val AvatarColors = listOf(
        Color(0xFF6B92E0), Color(0xFF7C6BD6), Color(0xFF3F9E8C),
        Color(0xFFD07A53), Color(0xFFC75B86), Color(0xFF4E8FCB),
        Color(0xFF8A7B4F), Color(0xFF5B7FB5)
    )

    fun avatarColor(key: String): Color =
        AvatarColors[abs(key.hashCode() % AvatarColors.size)]
}

data class MusePalette(
    val appBg: Color,
    val cardBg: Color,
    val ink: Color,
    val ink2: Color,
    val muted: Color,
    val muted2: Color,
    val line: Color,
    val lineLight: Color,
    val blue: Color,
    val blueSoft: Color,
    val onBlue: Color,
    val avatarFg: Color,
    val star: Color,
    val danger: Color
)

private val LightPalette = MusePalette(
    appBg = Color(0xFFF4F6F9),
    cardBg = Color(0xFFFFFFFF),
    ink = Color(0xFF202938),
    ink2 = Color(0xFF344054),
    muted = Color(0xFF71717A),
    muted2 = Color(0xFF7A8494),
    line = Color(0xFFE4E9F0),
    lineLight = Color(0xFFEDF0F4),
    blue = MuseUi.Blue,
    blueSoft = MuseUi.BlueSoft,
    onBlue = Color.White,
    avatarFg = Color(0xFF6B92E0),
    star = MuseUi.Star,
    danger = MuseUi.Danger
)

private val DarkPalette = MusePalette(
    appBg = Color(0xFF0F1117),
    cardBg = Color(0xFF161922),
    ink = Color(0xFFE7E9EE),
    ink2 = Color(0xFFC8CCD6),
    muted = Color(0xFF8B93A7),
    muted2 = Color(0xFF7D8598),
    line = Color(0xFF2A3040),
    lineLight = Color(0xFF232734),
    blue = Color(0xFF5B8CFF),
    blueSoft = Color(0xFF1C2740),
    onBlue = Color(0xFF0F1117),
    avatarFg = Color(0xFF9FB8F5),
    star = Color(0xFFF0A92B),
    danger = Color(0xFFE5484D)
)

val LocalMusePalette = staticCompositionLocalOf { LightPalette }

@Composable
fun muse(): MusePalette = LocalMusePalette.current

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MuseTheme(
    dark: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val palette = if (dark) DarkPalette else LightPalette
    val context = LocalContext.current
    // 动态取色只在 Android 12+ 可用；低于 12 即使开了开关也回落到固定配色。
    val useDynamic = dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val scheme = when {
        useDynamic && dark -> dynamicDarkColorScheme(context)
        useDynamic -> dynamicLightColorScheme(context)
        dark -> darkColorScheme(
            primary = Color(0xFF5B8CFF),
            onPrimary = Color(0xFF0F1117),
            background = palette.appBg,
            surface = palette.cardBg,
            onBackground = palette.ink,
            onSurface = palette.ink,
            outline = palette.line
        )
        else -> lightColorScheme(
            primary = MuseUi.Blue,
            onPrimary = Color.White,
            background = palette.appBg,
            surface = palette.cardBg,
            onBackground = palette.ink,
            onSurface = palette.ink,
            outline = palette.line
        )
    }
    // 动态取色只换主色：表面、文字、线条仍用自家调色盘，界面结构不走样。
    val effectivePalette = if (useDynamic) {
        palette.copy(
            blue = scheme.primary,
            blueSoft = scheme.primaryContainer,
            onBlue = scheme.onPrimary
        )
    } else {
        palette
    }
    androidx.compose.runtime.CompositionLocalProvider(LocalMusePalette provides effectivePalette) {
        // shapes / typography 不传：MaterialExpressiveTheme 默认就是 Expressive 的形状与字阶。
        MaterialExpressiveTheme(
            colorScheme = scheme,
            motionScheme = MotionScheme.expressive(),
            content = content
        )
    }
}

private fun initialOf(label: String, address: String): String {
    val source = label.trim().ifBlank { address.trim() }
    val first = source.firstOrNull()?.uppercaseChar() ?: '?'
    return first.toString()
}

@Composable
fun MuseAvatar(label: String, address: String, size: Dp = 40.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .background(MuseUi.avatarColor(address.ifBlank { label }), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            initialOf(label, address),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.42f).sp
        )
    }
}

/** 大圆角（取主题的 Expressive large 形状）+ 微阴影，不再用 1px 边框分层。 */
@Composable
fun MuseCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val palette = muse()
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = palette.cardBg,
        contentColor = palette.ink,
        shadowElevation = 1.dp,
        tonalElevation = 0.dp
    ) {
        Column(modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier) {
            content()
        }
    }
}

@Composable
fun MuseBadge(count: Int) {
    if (count <= 0) return
    val palette = muse()
    Box(
        modifier = Modifier
            .background(palette.blue, RoundedCornerShape(10.dp))
            .padding(horizontal = 7.dp, vertical = 2.dp)
    ) {
        Text(
            if (count > 99) "99+" else count.toString(),
            color = palette.onBlue,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun MuseSectionTitle(text: String) {
    Text(
        text,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = muse().muted,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}
