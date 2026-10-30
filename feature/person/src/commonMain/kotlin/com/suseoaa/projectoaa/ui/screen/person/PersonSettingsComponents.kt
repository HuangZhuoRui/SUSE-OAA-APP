package com.suseoaa.projectoaa.ui.screen.person

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.suseoaa.projectoaa.presentation.update.getAppVersionName
import com.suseoaa.projectoaa.shared.domain.model.person.EnrollmentGrade
import com.suseoaa.projectoaa.ui.theme.*

// 「我的」页面复用的卡片与设置行控件。

@Composable
fun UserInfoCard(
    userInfo: com.suseoaa.projectoaa.shared.domain.model.person.PersonData?,
    onLogout: () -> Unit,
    onAvatarClick: () -> Unit,
    onEditInfo: (username: String, email: String, grade: String) -> Unit = { _, _, _ -> },
    onCancelDeleteClick: (() -> Unit)? = null
) {
    var showEditDialog by remember { mutableStateOf(false) }

    if (showEditDialog && userInfo != null) {
        EditInfoDialog(
            initialUsername = userInfo.username,
            name = userInfo.name,
            initialEmail = userInfo.email,
            initialGrade = userInfo.resolvedGrade,
            onDismiss = { showEditDialog = false },
            onConfirm = { username, email, grade ->
                onEditInfo(username, email, grade)
                showEditDialog = false
            }
        )
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 头像区域
                Box(
                    modifier = Modifier.size(64.dp)
                ) {
                    // 头像主体
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .clickable { onAvatarClick() }
                    ) {
                        if (userInfo?.avatarUrl.isNullOrBlank()) {
                            // 无头像时显示默认图标
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(SoftBlueWait)
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.outlineVariant,
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = ElectricBlue,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        } else {
                            // 有头像时加载图片
                            AsyncImage(
                                model = userInfo.avatarUrl,
                                contentDescription = "用户头像",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(SoftBlueWait)
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.outlineVariant,
                                        CircleShape
                                    )
                            )
                        }
                    }

                    // 编辑图标提示 - 放在头像外层
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier
                            .size(24.dp)
                            .align(Alignment.BottomEnd)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                            .padding(4.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // 用户信息 (可点击编辑)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showEditDialog = true }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = userInfo?.name ?: "请登录",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = userInfo?.department?.ifBlank { null } ?: "暂未加入任何部门",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val roleAndGrade = listOfNotNull(
                        userInfo?.role?.ifBlank { null },
                        userInfo?.resolvedGrade?.let { "${it}级" },
                        userInfo?.isGraduated?.let { if (it) "已毕业" else "在校" }
                    ).joinToString(" · ").ifBlank { "未加入协会" }
                    Text(
                        text = roleAndGrade,
                        style = MaterialTheme.typography.bodySmall,
                        color = ElectricBlue
                    )
                }

                // 退出登录按钮
                IconButton(onClick = onLogout) {
                    Icon(
                        Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = "退出登录",
                        tint = AlertRed
                    )
                }
            }

            if (userInfo?.isDeleting == true) {
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.75f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.WarningAmber,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "账号正处于注销冷静期",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            val deadlineText = userInfo.scheduledDeleteAt
                                ?.takeIf { it.isNotBlank() }
                                ?.let { "将于 $it 永久注销" }
                                ?: "冷静期结束后将永久注销"
                            Text(
                                text = deadlineText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f)
                            )
                        }
                        if (onCancelDeleteClick != null) {
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(
                                onClick = onCancelDeleteClick,
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                )
                            ) {
                                Text("撤销注销", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** 游客模式下的个人卡片：说明哪些功能需要账号，并提供登录 / 注册入口。 */
@Composable
fun GuestCard(onLogin: () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(SoftBlueWait),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = ElectricBlue,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "未登录",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "登录协会账号后可查看公告、参与招新",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = onLogin) {
                Text("登录 / 注册")
            }
        }
    }
}

/**
 * 注销账号对话框：说明后果 → 向绑定邮箱发验证码 → 输入验证码确认。
 *
 * 用邮箱验证码而不是密码确认，是后端的设计；验证码同时防止手机借给别人时被误删。
 */
@Composable
fun DeleteAccountDialog(
    email: String,
    isCodeSent: Boolean,
    isSendingCode: Boolean,
    isDeleting: Boolean,
    onSendCode: () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (code: String) -> Unit,
    onSwitchToCancelDelete: (() -> Unit)? = null
) {
    var code by remember { mutableStateOf("") }
    val busy = isSendingCode || isDeleting

    AlertDialog(
        containerColor = MaterialTheme.colorScheme.surface,
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("注销账号") },
        text = {
            Column {
                Text(
                    "注销后，你的协会账号及个人资料（姓名、学号、邮箱、头像、招新申请等）将被删除。" +
                        "提交后账号进入冷静期，冷静期结束后删除不可恢复。",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "本机保存的课表、教务账号等数据不受影响，可在 App 内单独删除。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (email.isBlank()) "验证码将发送到你绑定的邮箱" else "验证码将发送到 ${email.maskEmail()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { input -> code = input.filter { it.isDigit() }.take(6) },
                        label = { Text("邮箱验证码") },
                        singleLine = true,
                        enabled = isCodeSent && !busy,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = onSendCode, enabled = !busy) {
                        if (isSendingCode) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Text(if (isCodeSent) "重新发送" else "发送验证码")
                        }
                    }
                }
                if (onSwitchToCancelDelete != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    TextButton(
                        onClick = onSwitchToCancelDelete,
                        enabled = !busy,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            "已提交过注销处于冷静期？点此撤销注销",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(code) },
                enabled = isCodeSent && code.length == 6 && !busy,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                if (isDeleting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onError
                    )
                } else {
                    Text("确认注销")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !busy) {
                Text("取消")
            }
        }
    )
}

/**
 * 取消冷静期对话框：向绑定邮箱发送 scene=cancel_delete 验证码，验证后调用 POST /user/delete/cancel 恢复账号。
 */
@Composable
fun CancelDeleteAccountDialog(
    email: String,
    scheduledDeleteAt: String? = null,
    isCodeSent: Boolean,
    isSendingCode: Boolean,
    isSubmitting: Boolean,
    onSendCode: () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (code: String) -> Unit
) {
    var code by remember { mutableStateOf("") }
    val busy = isSendingCode || isSubmitting

    AlertDialog(
        containerColor = MaterialTheme.colorScheme.surface,
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("撤销账号注销") },
        text = {
            Column {
                val promptText = if (!scheduledDeleteAt.isNullOrBlank()) {
                    "你的账号正处于注销冷静期，预计将于 $scheduledDeleteAt 永久删除。验证绑定邮箱后即可撤销注销、恢复账号正常使用。"
                } else {
                    "你的账号正处于注销冷静期，验证绑定邮箱后即可撤销注销、恢复账号正常使用。"
                }
                Text(
                    promptText,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (email.isBlank()) "验证码将发送到你绑定的邮箱" else "验证码将发送到 ${email.maskEmail()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { input -> code = input.filter { it.isDigit() }.take(6) },
                        label = { Text("邮箱验证码") },
                        singleLine = true,
                        enabled = isCodeSent && !busy,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = onSendCode, enabled = !busy) {
                        if (isSendingCode) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Text(if (isCodeSent) "重新发送" else "发送验证码")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(code) },
                enabled = isCodeSent && code.length == 6 && !busy
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("确认恢复")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !busy) {
                Text("取消")
            }
        }
    )
}

/** zhangsan@example.com -> zh****@example.com */
private fun String.maskEmail(): String {
    val at = indexOf('@')
    if (at <= 0) return this
    val name = substring(0, at)
    return name.take(2) + "****" + substring(at)
}

/**
 * 编辑信息对话框
 */
@Composable
fun EditInfoDialog(
    initialUsername: String,
    name: String,
    initialEmail: String,
    initialGrade: Int? = null,
    onDismiss: () -> Unit,
    onConfirm: (username: String, email: String, grade: String) -> Unit
) {
    var username by remember { mutableStateOf(initialUsername) }
    var email by remember { mutableStateOf(initialEmail) }
    var gradeText by remember { mutableStateOf(initialGrade?.toString().orEmpty()) }
    val gradeValid = EnrollmentGrade.parse(gradeText) != null ||
        (initialGrade == null && gradeText.isEmpty())

    AlertDialog(
        containerColor = MaterialTheme.colorScheme.surface,
        onDismissRequest = onDismiss,
        title = { Text("修改个人信息") },
        text = {
            Column {
                // 姓名与学号绑定，v2 不允许自行修改
                OutlinedTextField(
                    value = name,
                    onValueChange = {},
                    label = { Text("姓名（不可修改）") },
                    readOnly = true,
                    enabled = false,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("用户名") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("邮箱") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = gradeText,
                    onValueChange = { input -> gradeText = input.filter { it.isDigit() }.take(4) },
                    label = { Text("年级（如 2024）") },
                    placeholder = { Text("四位入学年份") },
                    isError = gradeText.isNotEmpty() && EnrollmentGrade.parse(gradeText) == null,
                    supportingText = { Text("填写大于 2000 的四位入学年份") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(username.trim(), email.trim(), gradeText.trim()) },
                enabled = username.isNotBlank() && email.isNotBlank() && gradeValid
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
fun AppInfoCard(
    isUnlocked: Boolean = false,
    onSecretUnlocked: () -> Unit = {}
) {
    // 连续点击计数和时间追踪（仅在未解锁时使用）
    var clickCount by remember { mutableIntStateOf(0) }
    var lastClickTime by remember { mutableStateOf(0L) }
    val resetTimeoutMs = 2000L // 2秒内需完成5次点击

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "青蟹",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            // 版本号 - 未解锁时可点击解锁隐藏功能
            Text(
                text = "版本 ${getAppVersionName()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = if (!isUnlocked) {
                    Modifier.clickable {
                        val currentTime =
                            com.suseoaa.projectoaa.shared.util.OaaClock.now().toEpochMilliseconds()
                        // 如果距上次点击超过超时时间，重置计数
                        if (currentTime - lastClickTime > resetTimeoutMs) {
                            clickCount = 1
                        } else {
                            clickCount++
                        }
                        lastClickTime = currentTime

                        // 达到5次点击，触发隐藏功能
                        if (clickCount >= 5) {
                            clickCount = 0
                            onSecretUnlocked()
                        }
                    }
                } else {
                    Modifier
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "四川轻化工大学开放原子开源协会",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
