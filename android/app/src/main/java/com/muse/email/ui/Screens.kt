@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
package com.muse.email.ui

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentActivity
import com.muse.email.Alias
import com.muse.email.R
import com.muse.email.MailState
import com.muse.email.Mailbox
import com.muse.email.MessageSummary
import com.muse.email.PickedFile
import com.muse.email.Screen
import com.muse.email.folderLabel
import com.muse.email.formatDate
import com.muse.email.formatDateFull
import com.muse.email.long
import com.muse.email.obj
import com.muse.email.str
import java.io.File
import java.io.FileInputStream

@Composable
fun MuseEmailApp() {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    val state = remember { MailState(context, scope) }
    LaunchedEffect(Unit) { state.start() }

    val dark = when (state.themeMode) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }
    MuseTheme(dark = dark, dynamicColor = state.dynamicColor) {
        val palette = muse()
        Surface(modifier = Modifier.fillMaxSize(), color = palette.appBg) {
            Box(Modifier.fillMaxSize()) {
                // 指纹锁只拦冷启动恢复会话；登录页本身不拦（密码刚验过也不拦）。
                if (state.screen !is Screen.Login && !state.unlocked) {
                    LockScreen(state)
                } else {
                    when (state.screen) {
                        is Screen.Login -> LoginScreen(state)
                        is Screen.Detail -> DetailScreen(state)
                        is Screen.Compose -> ComposeScreen(state)
                        is Screen.Admin -> AdminScreen(state)
                        is Screen.List -> MainScreen(state)
                    }
                }

                if (state.busy) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                        color = palette.blue
                    )
                }

                val error = state.error
                if (error != null) {
                    Snackbar(
                        modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                        containerColor = palette.danger,
                        contentColor = Color.White,
                        action = { TextButton(onClick = { state.dismissError() }) { Text("关闭", color = Color.White) } }
                    ) { Text(error) }
                } else {
                    val message = state.toast
                    if (message != null) {
                        Snackbar(
                            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                            action = { TextButton(onClick = { state.dismissToast() }) { Text("好") } }
                        ) { Text(message) }
                    }
                }
            }
        }
    }
}

// ---------------- 指纹锁 ----------------

/** 锁屏用的验证方式：Android 12+ 允许指纹/面容或锁屏密码二选一；以下版本只认强生物识别。 */
private fun lockAuthenticators(): Int =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
    } else {
        BiometricManager.Authenticators.BIOMETRIC_STRONG
    }

@Composable
private fun LockScreen(state: MailState) {
    val palette = muse()
    val activity = LocalContext.current as? FragmentActivity
    var errorText by remember { mutableStateOf<String?>(null) }
    var showClearDialog by remember { mutableStateOf(false) }

    fun authenticate() {
        val act = activity ?: return
        val prompt = BiometricPrompt(
            act,
            ContextCompat.getMainExecutor(act),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    state.unlock()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                        errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON
                    ) {
                        errorText = errString.toString()
                    }
                }
            }
        )
        val infoBuilder = BiometricPrompt.PromptInfo.Builder()
            .setTitle("验证后继续")
            .setSubtitle("使用指纹、面容或锁屏密码解锁 muse-email")
            .setAllowedAuthenticators(lockAuthenticators())
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            infoBuilder.setNegativeButtonText("取消")
        }
        prompt.authenticate(infoBuilder.build())
    }

    LaunchedEffect(Unit) { authenticate() }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("清除登录状态？", color = palette.ink) },
            text = { Text("将清除本机保存的登录会话，需要重新输入密码登录；指纹锁也会一并关闭。", color = palette.ink2) },
            confirmButton = {
                TextButton(onClick = {
                    showClearDialog = false
                    state.updateBiometricLock(false)
                    state.logout()
                }) { Text("清除并重新登录", color = palette.danger) }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("取消", color = palette.blue) }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.appBg)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        BrandMark(size = 64)
        Spacer(Modifier.height(20.dp))
        Text("muse-email 已锁定", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = palette.ink)
        Spacer(Modifier.height(8.dp))
        Text("验证后继续查看邮件", fontSize = 13.sp, color = palette.muted)
        errorText?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, fontSize = 13.sp, color = palette.danger)
        }
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { authenticate() },
            colors = ButtonDefaults.buttonColors(containerColor = palette.blue),
            modifier = Modifier.fillMaxWidth()
        ) { Text("验证") }
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = { showClearDialog = true }) {
            Text("清除登录状态并重新登录", color = palette.muted)
        }
    }
}

// ---------------- 品牌头 ----------------

/**
 * 品牌位：与启动图标共用同一张表情包位图 R.drawable.mascot。
 * 用 Image + ContentScale.Crop，避免 Icon 的 tint 覆盖原图配色。
 */
@Composable
private fun BrandMark(size: Int = 34) {
    Image(
        painter = painterResource(R.drawable.mascot),
        contentDescription = "muse-email",
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .size(size.dp)
            .clip(RoundedCornerShape(10.dp))
    )
}

/**
 * 统一的「复制邮箱地址」按钮：线性图标、单色、与顶栏刷新按钮同规格。
 * 用 IconButton 自行消费点击，不会触发外层卡片 / 身份行的切换动作。
 */
@Composable
private fun CopyAddressButton(address: String, label: String = "已复制邮箱地址") {
    val palette = muse()
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    IconButton(
        onClick = {
            if (address.isBlank()) return@IconButton
            clipboard.setText(AnnotatedString(address))
            Toast.makeText(context, label, Toast.LENGTH_SHORT).show()
        },
        modifier = Modifier.size(36.dp)
    ) {
        Icon(
            Icons.Default.ContentCopy,
            contentDescription = "复制邮箱地址",
            tint = palette.muted,
            modifier = Modifier.size(18.dp)
        )
    }
}

// ---------------- 登录 ----------------

@Composable
private fun LoginScreen(state: MailState) {
    val palette = muse()
    val saved = state.savedCredentials
    var base by remember { mutableStateOf(saved?.baseUrl?.ifBlank { "https://" } ?: state.savedBaseUrl.ifBlank { "https://" }) }
    var email by remember { mutableStateOf(saved?.email ?: "") }
    var password by remember { mutableStateOf(saved?.password ?: "") }
    var insecure by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(saved != null) }

    LaunchedEffect(state.savedBaseUrl) {
        if (state.savedBaseUrl.isNotBlank() && base == "https://") base = state.savedBaseUrl
    }

    // 冷启动时记住的账号密码在 start() 里才读出来，到了就填上（用户已输入的不覆盖）。
    LaunchedEffect(state.savedCredentials) {
        val c = state.savedCredentials ?: return@LaunchedEffect
        if (email.isBlank() && password.isBlank()) {
            if (base == "https://" || base.isBlank()) base = c.baseUrl.ifBlank { "https://" }
            email = c.email
            password = c.password
            rememberMe = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))
        BrandMark(56)
        Spacer(Modifier.height(16.dp))
        Text("muse-email", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = palette.ink)
        Text("你的自建域名邮箱", fontSize = 13.sp, color = palette.muted)
        Spacer(Modifier.height(28.dp))

        MuseCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) {
                MuseSectionTitle("服务器")
                OutlinedTextField(
                    value = base,
                    onValueChange = { base = it },
                    label = { Text("地址") },
                    placeholder = { Text("https://mail.example.com:8080") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(14.dp))
                MuseSectionTitle("账号")
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("邮箱") },
                    placeholder = { Text("you@example.com") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("密码") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { insecure = !insecure },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.Checkbox(checked = insecure, onCheckedChange = { insecure = it })
                    Text("允许自签证书 / 不安全 TLS", fontSize = 13.sp, color = palette.ink2)
                }
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { rememberMe = !rememberMe },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.Checkbox(checked = rememberMe, onCheckedChange = { rememberMe = it })
                    Text("记住账号密码", fontSize = 13.sp, color = palette.ink2)
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = { state.login(base, email, password, insecure, rememberMe) },
            enabled = !state.busy,
            colors = ButtonDefaults.buttonColors(containerColor = palette.blue),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Icon(Icons.Default.MailOutline, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("登录", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(14.dp))
        Text("不勾选只保存会话；勾选后账号密码加密存在本机", fontSize = 11.sp, color = palette.muted)
    }
}

// ---------------- 主框架 ----------------

@Composable
private fun MainScreen(state: MailState) {
    val palette = muse()
    var showMailboxSheet by remember { mutableStateOf(false) }

    // 在别名 / 设置页按返回，先回邮件页，不直接退出 App
    BackHandler(enabled = state.tab != 0) {
        state.tab = 0
    }

    Scaffold(
        containerColor = palette.appBg,
        topBar = {
            // 顶栏承载「当前身份」：品牌 + 常驻的完整邮箱地址 + 线性图标切换入口。
            // 与底栏统一使用单色线性图标，不再出现彩色圆形头像那种异类元素。
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showMailboxSheet = true },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BrandMark(30)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "muse-email",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.ink,
                                lineHeight = 22.sp
                            )
                            // 副标题常驻当前邮箱，行尾箭头提示「可点开切换」
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    state.currentMailbox?.takeIf { it.isNotBlank() }
                                        ?: state.user?.email?.takeIf { it.isNotBlank() }
                                        ?: "未登录",
                                    fontSize = 11.sp,
                                    color = palette.muted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    lineHeight = 14.sp,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Spacer(Modifier.width(2.dp))
                                Icon(
                                    Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = palette.muted,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = palette.appBg),
                actions = {
                    CopyAddressButton(
                        address = state.currentMailbox?.takeIf { it.isNotBlank() }
                            ?: state.user?.email.orEmpty()
                    )
                    IconButton(onClick = { state.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新", tint = palette.ink2)
                    }
                }
            )
        },
        bottomBar = {
            // 四格完全同构：单色线性图标 + 两字名词 label + 同一套选中态。
            // 与 domainmail/public 移动端规范一致：邮件 / 写信 / 邮箱 / 设置。
            NavigationBar(containerColor = palette.cardBg, tonalElevation = 0.dp) {
                MuseNavItem(
                    selected = state.tab == 0,
                    onClick = { state.tab = 0 },
                    icon = Icons.Default.Inbox,
                    label = "邮件",
                    palette = palette
                )
                MuseNavItem(
                    selected = false,
                    onClick = { state.openCompose() },
                    icon = Icons.Default.Edit,
                    label = "写信",
                    palette = palette
                )
                MuseNavItem(
                    selected = state.tab == 1,
                    onClick = { state.tab = 1 },
                    icon = Icons.Default.MailOutline,
                    label = "邮箱",
                    palette = palette
                )
                MuseNavItem(
                    selected = state.tab == 2,
                    onClick = { state.tab = 2 },
                    icon = Icons.Default.Settings,
                    label = "设置",
                    palette = palette
                )
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (state.tab) {
                0 -> MailListScreen(state)
                1 -> MailboxManageScreen(state)
                else -> SettingsScreen(state)
            }
        }
    }

    if (showMailboxSheet) {
        MailboxSheet(state) { showMailboxSheet = false }
    }
}

/**
 * 底栏统一格子：所有页面共用，保证图标、字号、配色完全一致。
 * 必须是 RowScope 扩展——Material3 的 NavigationBarItem 只在 RowScope 上下文可用。
 */
@Composable
private fun RowScope.MuseNavItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    palette: MusePalette
) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = {
            Icon(
                icon,
                contentDescription = null,
                tint = if (selected) palette.blue else palette.muted
            )
        },
        label = {
            Text(
                label,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) palette.blue else palette.muted
            )
        },
        alwaysShowLabel = true
    )
}

// ---------------- 邮件列表 ----------------

@Composable
private fun MailListScreen(state: MailState) {
    val palette = muse()
    val pullState = rememberPullToRefreshState()

    PullToRefreshBox(
        isRefreshing = state.refreshing,
        onRefresh = { state.refresh() },
        state = pullState,
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
        item {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                FolderRow(state)
                Spacer(Modifier.height(10.dp))
                FilterRow(state)
            }
        }

        items(state.messages, key = { it.id }) { message ->
            MessageRow(state, message)
        }

        item {
            when {
                state.messages.isEmpty() -> EmptyState("还没有邮件", "下拉刷新或切换文件夹看看")
                state.messages.size < state.total -> TextButton(
                    onClick = { state.loadMore() },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                ) { Text("加载更多 (${state.messages.size}/${state.total})", color = palette.blue) }

                else -> Box(
                    Modifier.fillMaxWidth().padding(20.dp),
                    contentAlignment = Alignment.Center
                ) { Text("— 到底了 —", color = palette.muted, fontSize = 12.sp) }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
    }
}

/** 底部弹层：邮箱列表完整铺开、可滚动，长地址不再被 DropdownMenu 裁掉。 */
@Composable
private fun MailboxSheet(state: MailState, onDismiss: () -> Unit) {
    val palette = muse()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = palette.cardBg,
        contentColor = palette.ink
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            Text("切换邮箱", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = palette.ink)
            Text(
                "共 ${state.mailboxes.size} 个身份 · 列表、未读数与发件人随之切换",
                fontSize = 12.sp,
                color = palette.muted
            )
            Spacer(Modifier.height(14.dp))
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 460.dp)
                .padding(bottom = 28.dp)
        ) {
            items(state.mailboxes, key = { it.address }) { mailbox ->
                MailboxSheetRow(state, mailbox, onDismiss)
            }
            if (state.mailboxes.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("暂无可用邮箱", color = palette.muted)
                    }
                }
            }
            item {
                HorizontalDivider(
                    color = palette.lineLight,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            state.tab = 1
                            onDismiss()
                        }
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.AlternateEmail, contentDescription = null, tint = palette.blue)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("管理邮箱与别名", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = palette.ink)
                        Text("查看全部身份、创建或删除别名", fontSize = 11.sp, color = palette.muted)
                    }
                    Icon(Icons.Default.ExpandMore, contentDescription = null, tint = palette.muted)
                }
            }
        }
    }
}

@Composable
private fun MailboxSheetRow(state: MailState, mailbox: Mailbox, onDismiss: () -> Unit) {
    val palette = muse()
    val selected = mailbox.address == (state.currentMailbox ?: state.user?.email)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) palette.blueSoft else Color.Transparent)
            .clickable {
                state.setMailbox(mailbox.address)
                onDismiss()
            }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MuseAvatar(mailbox.label, mailbox.address, 40.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                mailbox.label,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = palette.ink
            )
            // 完整地址，允许换行
            Text(mailbox.address, fontSize = 13.sp, color = palette.muted2, lineHeight = 17.sp)
            Text("${mailbox.total} 封 · ${mailbox.unread} 未读", fontSize = 11.sp, color = palette.muted)
        }
        if (selected) {
            Icon(Icons.Default.Check, contentDescription = "当前", tint = palette.blue)
        } else {
            MuseBadge(mailbox.unread)
        }
        CopyAddressButton(mailbox.address)
    }
    HorizontalDivider(color = palette.lineLight, modifier = Modifier.padding(horizontal = 20.dp))
}

/** 「邮箱」页里的身份行：与弹层行保持同一套视觉，但用卡片承载。 */
@Composable
private fun MailboxManageRow(state: MailState, mailbox: Mailbox) {
    val palette = muse()
    val selected = mailbox.address == (state.currentMailbox ?: state.user?.email)
    MuseCard(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        onClick = { state.setMailbox(mailbox.address) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (selected) palette.blueSoft else Color.Transparent)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MuseAvatar(mailbox.label, mailbox.address, 40.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    mailbox.label,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.ink
                )
                // 完整地址：允许换行，不截断
                Text(mailbox.address, fontSize = 12.sp, color = palette.muted2, lineHeight = 16.sp)
                Text("${mailbox.total} 封 · ${mailbox.unread} 未读", fontSize = 11.sp, color = palette.muted)
            }
            if (selected) {
                Icon(Icons.Default.Check, contentDescription = "当前", tint = palette.blue)
            } else {
                MuseBadge(mailbox.unread)
            }
            CopyAddressButton(mailbox.address)
        }
    }
}

@Composable
private fun FolderRow(state: MailState) {
    val palette = muse()
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
    ) {
        state.folders.forEach { folder ->
            FilterChip(
                selected = state.currentFolder == folder.name,
                onClick = { state.setFolder(folder.name) },
                label = {
                    Text(
                        folderLabel(folder.name) + if (folder.unread > 0) " ${folder.unread}" else ""
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = palette.blue,
                    selectedLabelColor = palette.onBlue,
                    containerColor = palette.cardBg,
                    labelColor = palette.ink2
                ),
                modifier = Modifier.padding(end = 8.dp)
            )
        }
    }
}

@Composable
private fun FilterRow(state: MailState) {
    val palette = muse()
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (state.showSearch) {
            OutlinedTextField(
                value = state.query,
                onValueChange = { state.query = it },
                placeholder = { Text("搜索邮件") },
                singleLine = true,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp)
            )
            TextButton(onClick = { state.search() }) { Text("搜索", color = palette.blue) }
        } else {
            Text(
                "共 ${state.total} 封",
                fontSize = 12.sp,
                color = palette.muted,
                modifier = Modifier.weight(1f)
            )
        }
        FilterChip(
            selected = state.unreadOnly,
            onClick = { state.toggleUnread() },
            label = { Text("未读") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = palette.blueSoft,
                selectedLabelColor = palette.blue,
                containerColor = palette.cardBg,
                labelColor = palette.ink2
            )
        )
        IconButton(onClick = { state.showSearch = !state.showSearch }) {
            Icon(
                if (state.showSearch) Icons.Default.Close else Icons.Default.Search,
                contentDescription = "搜索",
                tint = palette.ink2
            )
        }
    }
}

@Composable
private fun MessageRow(state: MailState, message: MessageSummary) {
    val palette = muse()
    val sender = message.fromName.ifBlank { message.from.ifBlank { "(未知发件人)" } }
    // 左滑进废纸篓（在废纸篓里再左滑就是彻底删除），右滑切换已读/未读。
    // 两边都是可逆操作，划完行自动弹回，不会卡在半路上。
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    if (message.seen) state.markUnread(message.id) else state.markRead(message.id)
                    false
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    if (state.currentFolder == "Trash") {
                        state.deleteMessage(message.id)
                        state.toast = "已删除"
                    } else {
                        state.moveMessage(message.id, "Trash")
                        state.toast = "已移到废纸篓"
                    }
                    false
                }
                SwipeToDismissBoxValue.Settled -> true
            }
        }
    )
    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            if (direction != SwipeToDismissBoxValue.Settled) {
                val isReadAction = direction == SwipeToDismissBoxValue.StartToEnd
                val bg = if (isReadAction) palette.blue else palette.danger
                val icon = when {
                    isReadAction && message.seen -> Icons.Default.MarkEmailUnread
                    isReadAction -> Icons.Default.MarkEmailRead
                    else -> Icons.Default.Delete
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(bg),
                    contentAlignment = if (isReadAction) Alignment.CenterStart else Alignment.CenterEnd
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.padding(horizontal = 20.dp).size(24.dp)
                    )
                }
            }
        },
        content = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (message.seen) Color.Transparent else palette.blueSoft.copy(alpha = 0.45f))
                        .clickable { state.open(message.id) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MuseAvatar(sender, message.from, 40.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                sender,
                                fontWeight = if (message.seen) FontWeight.Normal else FontWeight.Bold,
                                fontSize = 14.sp,
                                color = palette.ink2,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(formatDate(message.date), fontSize = 11.sp, color = palette.muted)
                        }
                        Spacer(Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                message.subject,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = if (message.seen) FontWeight.Normal else FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = palette.ink,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (message.hasAttachment) {
                                Spacer(Modifier.width(4.dp))
                                Icon(
                                    Icons.Default.AttachFile,
                                    contentDescription = "有附件",
                                    tint = palette.muted,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        if (message.snippet.isNotBlank()) {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                message.snippet,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 12.sp,
                                color = palette.muted2
                            )
                        }
                    }
                    Spacer(Modifier.width(6.dp))
                    if (!message.seen) {
                        Box(
                            Modifier
                                .size(7.dp)
                                .background(palette.blue, CircleShape)
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    IconButton(onClick = { state.toggleStar(message) }, modifier = Modifier.size(36.dp)) {
                        Icon(
                            if (message.flagged) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "星标",
                            tint = if (message.flagged) palette.star else palette.muted.copy(alpha = 0.6f)
                        )
                    }
                }
                HorizontalDivider(color = palette.lineLight, modifier = Modifier.padding(start = 68.dp))
            }
        }
    )
}

@Composable
private fun EmptyState(title: String, subtitle: String) {
    val palette = muse()
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(64.dp)
                .background(palette.blueSoft, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.MailOutline, null, tint = palette.blue, modifier = Modifier.size(30.dp))
        }
        Spacer(Modifier.height(14.dp))
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = palette.ink)
        Spacer(Modifier.height(4.dp))
        Text(subtitle, fontSize = 12.sp, color = palette.muted)
    }
}

// ---------------- 邮件详情 ----------------

@Composable
private fun DetailScreen(state: MailState) {
    val palette = muse()

    // 拦截系统返回键 / 边缘返回手势，否则会直接 finish() 退出 App
    BackHandler(enabled = true) {
        state.backToList()
    }

    Scaffold(
        containerColor = palette.appBg,
        topBar = {
            TopAppBar(
                title = { Text("邮件详情", color = palette.ink, fontSize = 17.sp) },
                navigationIcon = {
                    IconButton(onClick = { state.backToList() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回", tint = palette.ink)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = palette.appBg),
                actions = {
                    IconButton(onClick = { state.detail?.let { state.deleteMessage(it.id) } }) {
                        Icon(Icons.Default.Delete, contentDescription = "删除", tint = palette.danger)
                    }
                }
            )
        }
    ) { padding ->
        val detail = state.detail
        if (detail == null) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                LoadingIndicator(color = palette.blue)
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
            ) {
                item {
                    MuseCard(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(18.dp)) {
                            Text(detail.subject, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = palette.ink)
                            Spacer(Modifier.height(16.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                MuseAvatar(detail.fromName.ifBlank { detail.from }, detail.from, 40.dp)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        detail.fromName.ifBlank { detail.from },
                                        fontWeight = FontWeight.SemiBold,
                                        color = palette.ink,
                                        fontSize = 14.sp
                                    )
                                    Text(detail.from, fontSize = 12.sp, color = palette.muted2)
                                }
                                Text(formatDate(detail.date), fontSize = 11.sp, color = palette.muted)
                            }
                            Spacer(Modifier.height(14.dp))
                            InfoRow("收件人", detail.to)
                            if (detail.cc.isNotBlank()) InfoRow("抄送", detail.cc)
                            InfoRow("文件夹", folderLabel(detail.folder))
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                item {
                    MuseCard(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(18.dp)) {
                            if (detail.html.isNotBlank()) {
                                key(detail.id) {
                                    AndroidView(
                                        modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp, max = 900.dp),
                                        factory = { ctx ->
                                            WebView(ctx).apply {
                                                settings.javaScriptEnabled = false
                                                settings.allowFileAccess = false
                                                settings.blockNetworkLoads = true
                                            }
                                        },
                                        update = { webView ->
                                            webView.loadDataWithBaseURL(
                                                null, detail.html, "text/html", "UTF-8", null
                                            )
                                        }
                                    )
                                }
                            } else {
                                Text(
                                    detail.text.ifBlank { "(无正文)" },
                                    fontSize = 14.sp,
                                    color = palette.ink2,
                                    lineHeight = 25.sp
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                if (detail.attachments.isNotEmpty()) {
                    item {
                        MuseCard(modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(18.dp)) {
                                MuseSectionTitle("附件 (${detail.attachments.size})")
                                detail.attachments.forEach { attachment ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Description, null, tint = palette.blue)
                                        Spacer(Modifier.width(10.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(attachment.filename, fontSize = 13.sp, color = palette.ink)
                                            Text(
                                                "${attachment.size} B · ${attachment.mime}",
                                                fontSize = 11.sp,
                                                color = palette.muted
                                            )
                                        }
                                        TextButton(onClick = {
                                            state.downloadAttachment(attachment.id, attachment.filename)
                                        }) { Text("下载", color = palette.blue) }
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                }

                item {
                    state.savedFilePath?.let { path ->
                        Button(
                            onClick = { openDownloaded(state.appContext, path, state.savedFileMime) },
                            colors = ButtonDefaults.buttonColors(containerColor = palette.blue),
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("打开已下载文件") }
                        Spacer(Modifier.height(10.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    val ok = saveDownloadedToDownloads(state.appContext, path, state.savedFileMime)
                                    state.toast = if (ok) "已保存到下载文件夹" else "保存失败（需要 Android 10 及以上）"
                                },
                                modifier = Modifier.weight(1f)
                            ) { Text("保存到下载") }
                            OutlinedButton(
                                onClick = { shareDownloaded(state.appContext, path, state.savedFileMime) },
                                modifier = Modifier.weight(1f)
                            ) { Text("分享") }
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { state.markUnread(detail.id); state.backToList() },
                            modifier = Modifier.weight(1f)
                        ) { Text("标为未读") }
                        OutlinedButton(
                            onClick = { state.moveMessage(detail.id, "Trash") },
                            modifier = Modifier.weight(1f)
                        ) { Text("移到回收站") }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

// ---------------- 写邮件 ----------------

@Composable
private fun ComposeScreen(state: MailState) {
    val palette = muse()
    val context = LocalContext.current
    val savedDraft = remember { state.loadDraft() }
    var from by remember { mutableStateOf(savedDraft?.str("from") ?: state.currentMailbox ?: state.user?.email ?: "") }
    var to by remember { mutableStateOf(savedDraft?.str("to") ?: "") }
    var cc by remember { mutableStateOf(savedDraft?.str("cc") ?: "") }
    var subject by remember { mutableStateOf(savedDraft?.str("subject") ?: "") }
    var body by remember { mutableStateOf(savedDraft?.str("body") ?: "") }
    var picked by remember { mutableStateOf(listOf<PickedFile>()) }
    var fromExpanded by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }

    // 有内容就自动存草稿（只存文本，附件不进草稿），发送或放弃时清除。
    LaunchedEffect(from, to, cc, subject, body) {
        state.saveDraft(from, to, cc, subject, body)
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) picked = picked + copyUriToCache(context, uri)
    }

    // 顶栏箭头与系统返回手势共用同一套逻辑：有草稿先确认，空内容直接回列表。
    val hasDraft = to.isNotBlank() || cc.isNotBlank() || subject.isNotBlank() ||
        body.isNotBlank() || picked.isNotEmpty()
    fun requestBack() {
        if (hasDraft) showDiscardDialog = true else state.backToList()
    }

    BackHandler(enabled = true) {
        if (showDiscardDialog) showDiscardDialog = false else requestBack()
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("放弃这封邮件？", color = palette.ink) },
            text = { Text("草稿内容将丢失，且不会保存。", color = palette.ink2) },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardDialog = false
                    state.clearDraft()
                    state.backToList()
                }) { Text("放弃", color = palette.danger) }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("继续编辑", color = palette.blue)
                }
            }
        )
    }

    Scaffold(
        containerColor = palette.appBg,
        topBar = {
            TopAppBar(
                title = { Text("写邮件", color = palette.ink, fontSize = 17.sp) },
                navigationIcon = {
                    IconButton(onClick = { requestBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回", tint = palette.ink)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = palette.appBg),
                actions = {
                    TextButton(
                        onClick = { state.sendMail(from, to, cc, subject, body, picked) },
                        enabled = !state.busy
                    ) {
                        Icon(Icons.Default.Send, null, tint = palette.blue)
                        Spacer(Modifier.width(4.dp))
                        Text("发送", color = palette.blue, fontWeight = FontWeight.SemiBold)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            if (savedDraft != null) {
                Text(
                    "已恢复上次未发出的草稿（附件需重新添加）",
                    fontSize = 12.sp,
                    color = palette.muted,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            MuseCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Box {
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { fromExpanded = true },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Person, null, tint = palette.muted, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text("发件人", fontSize = 11.sp, color = palette.muted)
                                Text(from, fontSize = 14.sp, color = palette.ink, lineHeight = 18.sp)
                            }
                            Icon(Icons.Default.ExpandMore, null, tint = palette.muted)
                        }
                        DropdownMenu(expanded = fromExpanded, onDismissRequest = { fromExpanded = false }) {
                            state.mailboxes.forEach { mailbox ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(mailbox.label, fontSize = 14.sp)
                                            Text(mailbox.address, fontSize = 12.sp, color = palette.muted)
                                        }
                                    },
                                    onClick = { from = mailbox.address; fromExpanded = false }
                                )
                            }
                        }
                    }
                    HorizontalDivider(color = palette.lineLight, modifier = Modifier.padding(vertical = 8.dp))
                    LabeledInput("收件人", to, { to = it }, "逗号分隔多个地址", KeyboardType.Email)
                    LabeledInput("抄送", cc, { cc = it }, "可选", KeyboardType.Email)
                    LabeledInput("主题", subject, { subject = it }, "", KeyboardType.Text)
                }
            }
            Spacer(Modifier.height(12.dp))
            MuseCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = body,
                        onValueChange = { body = it },
                        placeholder = { Text("写点什么…", color = palette.muted) },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 220.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 16.sp,
                            color = palette.ink,
                            lineHeight = 25.sp
                        )
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            MuseCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    OutlinedButton(onClick = { picker.launch("*/*") }) {
                        Icon(Icons.Default.AttachFile, null)
                        Spacer(Modifier.width(6.dp))
                        Text("添加附件")
                    }
                    picked.forEach { file ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Description, null, tint = palette.muted)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                file.name,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 13.sp
                            )
                            IconButton(onClick = { picked = picked.filter { it != file } }) {
                                Icon(Icons.Default.Close, contentDescription = "移除", tint = palette.muted)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun LabeledInput(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    type: KeyboardType
) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 12.sp, color = muse().muted, modifier = Modifier.width(56.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            placeholder = { Text(placeholder) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = type),
            modifier = Modifier.weight(1f)
        )
    }
}

// ---------------- 邮箱（身份与别名管理） ----------------

@Composable
private fun MailboxManageScreen(state: MailState) {
    val palette = muse()
    LaunchedEffect(Unit) { state.loadSettings() }
    var prefix by remember { mutableStateOf("") }
    var domain by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    val current = state.currentMailbox ?: state.user?.email ?: ""
    val active: Mailbox? = state.mailboxes.firstOrNull { it.address == current }
    val activeTitle = active?.label?.takeIf { it.isNotBlank() }
        ?: state.user?.displayName?.takeIf { it.isNotBlank() }
        ?: "主邮箱"

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
    ) {
        item {
            Text("邮箱", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = palette.ink)
            Text("管理你的邮箱身份与无限别名", fontSize = 12.sp, color = palette.muted)
            Spacer(Modifier.height(16.dp))
        }

        // 当前身份卡：完整地址 + 未读数，头像只出现在内容卡片里，不进导航栏
        item {
            MuseCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MuseAvatar(activeTitle, current.ifBlank { activeTitle }, 46.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("当前身份", fontSize = 11.sp, color = palette.muted)
                        Text(
                            activeTitle,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.ink
                        )
                        Text(
                            current.ifBlank { "尚未登录" },
                            fontSize = 12.sp,
                            color = palette.muted2,
                            lineHeight = 17.sp
                        )
                    }
                    active?.let { MuseBadge(it.unread) }
                    CopyAddressButton(current)
                }
            }
            Spacer(Modifier.height(16.dp))
            MuseSectionTitle("全部邮箱身份 · ${state.mailboxes.size}")
            Spacer(Modifier.height(8.dp))
        }

        items(state.mailboxes, key = { "mb-${it.address}" }) { mailbox ->
            MailboxManageRow(state, mailbox)
        }

        if (state.mailboxes.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("暂无可用邮箱", color = palette.muted, fontSize = 13.sp)
                }
            }
        }

        item {
            Spacer(Modifier.height(20.dp))
            MuseSectionTitle("无限别名")
            Spacer(Modifier.height(8.dp))
            MuseCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    MuseSectionTitle("新建别名")
                    OutlinedTextField(
                        value = prefix,
                        onValueChange = { prefix = it },
                        label = { Text("前缀") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    Box {
                        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.ExpandMore, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(domain.ifBlank { "默认域名" })
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            DropdownMenuItem(
                                text = { Text("默认域名") },
                                onClick = { domain = ""; expanded = false }
                            )
                            state.aliasDomains.forEach { name ->
                                DropdownMenuItem(
                                    text = { Text(name) },
                                    onClick = { domain = name; expanded = false }
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = {
                            if (prefix.isNotBlank()) {
                                state.createAlias(prefix, domain)
                                prefix = ""
                            }
                        },
                        enabled = state.aliasAllowCreate && !state.busy,
                        colors = ButtonDefaults.buttonColors(containerColor = palette.blue),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("创建别名") }
                    if (!state.aliasAllowCreate) {
                        Spacer(Modifier.height(8.dp))
                        Text("管理员已关闭自助创建别名", color = palette.muted, fontSize = 12.sp)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            MuseSectionTitle("已有 ${state.aliases.size} 个别名")
            Spacer(Modifier.height(8.dp))
        }

        items(state.aliases, key = { it.id }) { alias: Alias ->
            MuseCard(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MuseAvatar(alias.address, alias.address, 38.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(alias.address, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = palette.ink)
                        Text(
                            "→ ${alias.targets.joinToString(", ")}" + if (alias.active) "" else "（已停用）",
                            fontSize = 11.sp,
                            color = palette.muted
                        )
                    }
                    CopyAddressButton(alias.address, label = "已复制别名地址")
                    IconButton(onClick = { state.deleteAlias(alias.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = "删除", tint = palette.danger)
                    }
                }
            }
        }

        if (state.aliases.isEmpty()) {
            item { EmptyState("还没有别名", "在上面输入前缀创建一个吧") }
        }
    }
}

// ---------------- 设置 ----------------

/**
 * 三选一的连体按钮组（Material 3 Expressive 的 connected button group，
 * 替代已废弃的 SegmentedButton）。选中项用主题大圆角，其余按位置连体。
 */
@Composable
private fun ConnectedChoice(
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit
) {
    val palette = muse()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
    ) {
        options.forEachIndexed { index, (value, label) ->
            ToggleButton(
                checked = selected == value,
                onCheckedChange = { onSelect(value) },
                modifier = Modifier.weight(1f).semantics { role = Role.RadioButton },
                shapes = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
                colors = ToggleButtonDefaults.colors(
                    containerColor = palette.cardBg,
                    contentColor = palette.ink2,
                    checkedContainerColor = palette.blueSoft,
                    checkedContentColor = palette.blue
                )
            ) { Text(label) }
        }
    }
}

@Composable
private fun SettingsScreen(state: MailState) {
    val palette = muse()
    val context = LocalContext.current
    LaunchedEffect(Unit) { state.loadSettings() }
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    val imap = state.clientConfig.obj("imap")
    val smtp = state.clientConfig.obj("smtp")
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) state.updateNotifyEnabled(true) else state.toast = "需要通知权限才能提醒新邮件"
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("设置", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = palette.ink)
        Spacer(Modifier.height(16.dp))

        MuseCard(modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                MuseAvatar(state.user?.displayName ?: "我", state.user?.email ?: "", 48.dp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        state.user?.displayName?.takeIf { it.isNotBlank() } ?: "muse-email 用户",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = palette.ink
                    )
                    Text(state.user?.email ?: "", fontSize = 12.sp, color = palette.muted2)
                }
                CopyAddressButton(state.user?.email.orEmpty())
            }
        }

        Spacer(Modifier.height(12.dp))
        MuseCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                MuseSectionTitle("账号信息")
                InfoRow("服务器", state.savedBaseUrl)
                InfoRow("已用容量", "${state.settings.long("usedMb")} / ${state.settings.long("quotaMb")} MB")
                InfoRow("管理员", if (state.user?.isAdmin == true) "是" else "否")
            }
        }

        Spacer(Modifier.height(12.dp))
        MuseCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                MuseSectionTitle("外观")
                ConnectedChoice(
                    options = listOf("system" to "跟随系统", "light" to "浅色", "dark" to "深色"),
                    selected = state.themeMode,
                    onSelect = { state.updateThemeMode(it) }
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("动态取色", fontSize = 14.sp, color = palette.ink)
                            Text(
                                "主色跟随系统壁纸（Android 12 及以上）",
                                fontSize = 12.sp,
                                color = palette.muted
                            )
                        }
                        Switch(
                            checked = state.dynamicColor,
                            onCheckedChange = { state.updateDynamicColor(it) }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        MuseCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                MuseSectionTitle("新邮件通知")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("收到新邮件时提醒", fontSize = 14.sp, color = palette.ink)
                        Text(
                            "服务端没有推送通道，只能定时轮询（最省电是 60 分钟）",
                            fontSize = 12.sp,
                            color = palette.muted
                        )
                    }
                    Switch(
                        checked = state.notifyEnabled,
                        onCheckedChange = { enabled ->
                            if (!enabled) {
                                state.updateNotifyEnabled(false)
                                return@Switch
                            }
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                ContextCompat.checkSelfPermission(
                                    context, Manifest.permission.POST_NOTIFICATIONS
                                ) != PackageManager.PERMISSION_GRANTED
                            ) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                state.updateNotifyEnabled(true)
                            }
                        }
                    )
                }
                if (state.notifyEnabled) {
                    Spacer(Modifier.height(12.dp))
                    ConnectedChoice(
                        options = listOf("15" to "15 分钟", "30" to "30 分钟", "60" to "60 分钟"),
                        selected = state.notifyIntervalMin.toString(),
                        onSelect = { state.updateNotifyInterval(it.toLong()) }
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        MuseCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                MuseSectionTitle("安全")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("打开应用时验证", fontSize = 14.sp, color = palette.ink)
                        Text(
                            "指纹、面容或锁屏密码；只拦冷启动恢复会话",
                            fontSize = 12.sp,
                            color = palette.muted
                        )
                    }
                    Switch(
                        checked = state.biometricLock,
                        onCheckedChange = { enabled ->
                            if (!enabled) {
                                state.updateBiometricLock(false)
                                return@Switch
                            }
                            val can = BiometricManager.from(context)
                                .canAuthenticate(lockAuthenticators())
                            if (can == BiometricManager.BIOMETRIC_SUCCESS) {
                                state.updateBiometricLock(true)
                            } else {
                                state.toast = "设备未设置可用的指纹、面容或锁屏密码"
                            }
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        MuseCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                MuseSectionTitle("客户端配置")
                InfoRow("IMAP", "${imap.str("host")}:${imap.long("port")} ${imap.str("security")}")
                InfoRow("SMTP", "${smtp.str("host")}:${smtp.long("port")} ${smtp.str("security")}")
                InfoRow("Webmail", state.clientConfig.str("webmail"))
            }
        }

        if (state.user?.isAdmin == true) {
            Spacer(Modifier.height(12.dp))
            MuseCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("管理后台", fontSize = 14.sp, color = palette.ink, fontWeight = FontWeight.SemiBold)
                        Text("域名、用户、别名、发件箱、审计", fontSize = 12.sp, color = palette.muted)
                    }
                    OutlinedButton(onClick = { state.screen = Screen.Admin }) { Text("打开", color = palette.blue) }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        MuseCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                MuseSectionTitle("修改密码")
                OutlinedTextField(
                    value = oldPassword,
                    onValueChange = { oldPassword = it },
                    label = { Text("当前密码") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("新密码") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        if (oldPassword.isNotBlank() && newPassword.isNotBlank()) {
                            state.changePassword(oldPassword, newPassword)
                            oldPassword = ""
                            newPassword = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = palette.blue),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("修改密码") }
            }
        }

        Spacer(Modifier.height(20.dp))
        OutlinedButton(
            onClick = { state.logout() },
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Icon(Icons.Default.Logout, null, tint = palette.danger)
            Spacer(Modifier.width(8.dp))
            Text("退出登录", color = palette.danger)
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "muse-email · 业务核心由 Rust 实现",
            fontSize = 11.sp,
            color = palette.muted,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    val palette = muse()
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, color = palette.muted, fontSize = 12.sp, modifier = Modifier.width(78.dp))
        Text(value, fontSize = 12.sp, color = palette.ink2, modifier = Modifier.weight(1f))
    }
}

// ---------------- 工具 ----------------

private fun copyUriToCache(context: Context, uri: Uri): PickedFile {
    var name = "attachment"
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0) name = cursor.getString(index) ?: name
        }
    }
    val safe = name.map { if ("/\\:*?\"<>|".contains(it)) '_' else it }.joinToString("")
    val dir = File(context.cacheDir, "attachments").apply { mkdirs() }
    val file = File(dir, "${System.currentTimeMillis()}_$safe")
    context.contentResolver.openInputStream(uri)?.use { input ->
        file.outputStream().use { output -> input.copyTo(output) }
    }
    return PickedFile(file.absolutePath, safe, guessMime(safe))
}

private fun guessMime(name: String): String =
    when (name.substringAfterLast('.', "").lowercase()) {
        "txt", "log", "md", "csv" -> "text/plain"
        "html", "htm" -> "text/html"
        "json" -> "application/json"
        "pdf" -> "application/pdf"
        "png" -> "image/png"
        "jpg", "jpeg" -> "image/jpeg"
        "gif" -> "image/gif"
        "zip" -> "application/zip"
        else -> "application/octet-stream"
    }

private fun openDownloaded(context: Context, path: String, mime: String?) {
    try {
        val uri = FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", File(path)
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mime?.ifBlank { "*/*" } ?: "*/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "打不开文件：${e.message}", Toast.LENGTH_SHORT).show()
    }
}

/** 把已下载的附件复制到系统下载文件夹（Android 10+，无需额外权限）。 */
private fun saveDownloadedToDownloads(context: Context, path: String, mime: String?): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
    val file = File(path)
    if (!file.exists()) return false
    val values = ContentValues().apply {
        put(MediaStore.Downloads.DISPLAY_NAME, file.name)
        put(MediaStore.Downloads.MIME_TYPE, mime ?: "application/octet-stream")
        put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
    }
    return try {
        val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: return false
        context.contentResolver.openOutputStream(uri)?.use { out ->
            FileInputStream(file).use { input -> input.copyTo(out) }
        } ?: return false
        true
    } catch (e: Exception) {
        false
    }
}

/** 用系统分享框把已下载的附件发出去（微信、QQ、邮件等）。 */
private fun shareDownloaded(context: Context, path: String, mime: String?) {
    val file = File(path)
    if (!file.exists()) return
    try {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mime ?: "application/octet-stream"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "分享附件"))
    } catch (e: Exception) {
        Toast.makeText(context, "分享失败：${e.message}", Toast.LENGTH_SHORT).show()
    }
}
