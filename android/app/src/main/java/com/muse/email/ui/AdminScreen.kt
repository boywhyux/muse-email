@file:OptIn(ExperimentalMaterial3Api::class)

package com.muse.email.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muse.email.MailState
import com.muse.email.bool
import com.muse.email.formatDate
import com.muse.email.int
import com.muse.email.long
import com.muse.email.obj
import com.muse.email.str
import org.json.JSONArray
import org.json.JSONObject

/** 字段名按服务端实际列名取，缺失一律按空处理，不会因为一个字段对不上就整页崩掉。 */
private fun JSONObject.pick(vararg keys: String): String =
    keys.firstNotNullOfOrNull { key -> str(key).takeIf { it.isNotBlank() } } ?: ""

/** 管理后台：只读总览 + 发件箱重试/删除。入口在设置页，仅管理员账号可见。 */
@Composable
fun AdminScreen(state: MailState) {
    val palette = muse()
    LaunchedEffect(Unit) { if (!state.adminLoaded) state.loadAdmin() }
    BackHandler { state.backToList() }

    Scaffold(
        containerColor = palette.appBg,
        topBar = {
            TopAppBar(
                title = { Text("管理后台", color = palette.ink, fontSize = 17.sp) },
                navigationIcon = {
                    IconButton(onClick = { state.backToList() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回", tint = palette.ink)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = palette.appBg),
                actions = {
                    IconButton(onClick = { state.loadAdmin() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新", tint = palette.ink2)
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
            // ---- 统计 ----
            MuseCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    MuseSectionTitle("统计")
                    val s = state.adminStats
                    InfoRow("用户", s.int("users").toString())
                    InfoRow("域名", s.int("domains").toString())
                    InfoRow("邮件总数", s.int("messages").toString())
                    InfoRow("今日新增", s.int("todayMessages").toString())
                    InfoRow("存储占用", "${s.int("storageMb")} MB")
                }
            }

            // ---- 域名 ----
            Spacer(Modifier.height(12.dp))
            MuseCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    MuseSectionTitle("域名 (${state.adminDomains.size})")
                    if (state.adminDomains.isEmpty()) {
                        Text("暂无域名", fontSize = 13.sp, color = palette.muted)
                    }
                    state.adminDomains.forEach { d ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(d.str("name"), fontSize = 14.sp, color = palette.ink, fontWeight = FontWeight.SemiBold)
                                Text(
                                    listOfNotNull(
                                        if (d.bool("active")) "启用" else "停用",
                                        d.str("catch_all").takeIf { it.isNotBlank() }?.let { "catch-all: $it" },
                                        if (d.bool("dkim_enabled")) "DKIM 已开" else "DKIM 未开"
                                    ).joinToString(" · "),
                                    fontSize = 12.sp, color = palette.muted
                                )
                            }
                            Text(formatDate(d.long("created_at")), fontSize = 11.sp, color = palette.muted)
                        }
                    }
                }
            }

            // ---- 用户 ----
            Spacer(Modifier.height(12.dp))
            MuseCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    MuseSectionTitle("用户 (${state.adminUsers.size})")
                    if (state.adminUsers.isEmpty()) {
                        Text("暂无用户", fontSize = 13.sp, color = palette.muted)
                    }
                    state.adminUsers.forEach { u ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(u.str("email"), fontSize = 14.sp, color = palette.ink, fontWeight = FontWeight.SemiBold)
                                Text(
                                    listOfNotNull(
                                        u.str("display_name").takeIf { it.isNotBlank() },
                                        if (u.bool("is_admin")) "管理员" else null,
                                        if (u.bool("active")) null else "已停用",
                                        "配额 ${u.long("quota_mb")} MB"
                                    ).joinToString(" · "),
                                    fontSize = 12.sp, color = palette.muted
                                )
                            }
                            Text(formatDate(u.long("last_login_at")), fontSize = 11.sp, color = palette.muted)
                        }
                    }
                }
            }

            // ---- 别名 ----
            Spacer(Modifier.height(12.dp))
            MuseCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    MuseSectionTitle("别名 (${state.adminAliases.size})")
                    if (state.adminAliases.isEmpty()) {
                        Text("暂无别名", fontSize = 13.sp, color = palette.muted)
                    }
                    state.adminAliases.forEach { a ->
                        Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                            Text(a.str("address"), fontSize = 14.sp, color = palette.ink, fontWeight = FontWeight.SemiBold)
                            Text(
                                "→ ${aliasTargets(a.str("targets"))}",
                                fontSize = 12.sp, color = palette.muted
                            )
                        }
                    }
                }
            }

            // ---- 发件箱 ----
            Spacer(Modifier.height(12.dp))
            MuseCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    MuseSectionTitle("发件箱 (${state.adminOutbox.size})")
                    if (state.adminOutbox.isEmpty()) {
                        Text("队列为空", fontSize = 13.sp, color = palette.muted)
                    }
                    state.adminOutbox.forEach { row ->
                        Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                            Text(
                                row.pick("subject").ifBlank { "(无主题)" },
                                fontSize = 14.sp, color = palette.ink, fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "${row.pick("from", "fromAddress", "from_address")} → ${row.pick("to", "toAddress", "to_address")}",
                                fontSize = 12.sp, color = palette.muted
                            )
                            Text(
                                "状态 ${row.pick("status")} · 尝试 ${row.int("attempts")} 次 · ${formatDate(row.long("created_at"))}",
                                fontSize = 12.sp, color = palette.muted
                            )
                            Spacer(Modifier.height(6.dp))
                            Row {
                                OutlinedButton(onClick = { state.adminRetryOutbox(row.long("id")) }) {
                                    Text("重试", color = palette.blue)
                                }
                                Spacer(Modifier.width(8.dp))
                                OutlinedButton(onClick = { state.adminDeleteOutbox(row.long("id")) }) {
                                    Text("删除", color = palette.danger)
                                }
                            }
                        }
                    }
                }
            }

            // ---- 审计 ----
            Spacer(Modifier.height(12.dp))
            MuseCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    MuseSectionTitle("审计（最近 ${state.adminAudit.size} 条）")
                    if (state.adminAudit.isEmpty()) {
                        Text("暂无记录", fontSize = 13.sp, color = palette.muted)
                    }
                    state.adminAudit.take(30).forEach { row ->
                        Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                            Text(
                                row.pick("action").ifBlank { "(未知操作)" },
                                fontSize = 14.sp, color = palette.ink, fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                listOfNotNull(
                                    row.pick("actor", "user").takeIf { it.isNotBlank() },
                                    row.pick("target").takeIf { it.isNotBlank() },
                                    formatDate(row.long("created_at")).takeIf { it != "—" }
                                ).joinToString(" · "),
                                fontSize = 12.sp, color = palette.muted
                            )
                        }
                    }
                }
            }

            // ---- 配置 ----
            Spacer(Modifier.height(12.dp))
            MuseCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    MuseSectionTitle("服务器配置")
                    val c = state.adminConfig
                    val tls = c.obj("tls")
                    val relay = c.obj("relay")
                    InfoRow("主机名", c.str("hostname"))
                    InfoRow("品牌", c.str("brand"))
                    InfoRow("SMTP", c.int("smtpPort").toString())
                    InfoRow("IMAP", c.int("imapPort").toString())
                    InfoRow("HTTP", c.int("httpPort").toString())
                    InfoRow("TLS", if (tls.bool("enabled")) "已启用" else "未启用")
                    InfoRow("中继", if (relay.bool("enabled")) "${relay.str("type")} ${relay.str("host")}" else "未启用")
                    InfoRow("开放注册", if (c.bool("allowSelfRegister")) "是" else "否")
                    InfoRow("DKIM 选择器", c.str("dkimSelector"))
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

/** 别名 targets 在库里是 JSON 字符串，解析失败就原样显示。 */
private fun aliasTargets(raw: String): String {
    if (raw.isBlank()) return "—"
    return try {
        val arr = JSONArray(raw)
        (0 until arr.length()).mapNotNull { arr.optString(it).takeIf { s -> s.isNotBlank() } }
            .joinToString(", ").ifBlank { raw }
    } catch (e: Exception) {
        raw
    }
}
