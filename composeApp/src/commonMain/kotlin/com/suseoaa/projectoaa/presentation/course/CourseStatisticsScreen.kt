package com.suseoaa.projectoaa.presentation.course

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suseoaa.projectoaa.util.LockScreenOrientation
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseStatisticsScreen(
    onBack: () -> Unit,
    viewModel: CourseStatisticsViewModel = koinViewModel()
) {
    // 强制横屏
    LockScreenOrientation(landscape = true)

    val timelineData by viewModel.timelineData.collectAsStateWithLifecycle()
    val allAccounts by viewModel.allAccounts.collectAsStateWithLifecycle()
    val selectedAccountIds by viewModel.selectedAccountIds.collectAsStateWithLifecycle()
    val selectedTerm by viewModel.selectedTerm.collectAsStateWithLifecycle()
    val availableTerms by viewModel.availableTerms.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()

    var showFilterSheet by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showFilterSheet = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.AutoMirrored.Filled.List, contentDescription = "筛选")
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (timelineData.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("暂无数据，请尝试调整筛选条件", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                }
            } else {
                LazyRow(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 64.dp, vertical = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(32.dp)
                ) {
                    items(timelineData.toList()) { (termPair, teacherMap) ->
                        val (xnm, xqm) = termPair
                        val termName = "${xnm}-${xnm.toInt() + 1}学年 第${if (xqm == "3") "一" else "二"}学期"
                        
                        TermTimelineNode(termName = termName, teacherMap = teacherMap)
                    }
                }
            }

            // 悬浮返回按钮 (固定在左上角)
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .padding(top = 16.dp, start = 16.dp)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            // 同步按钮 (固定在右上角)
            IconButton(
                onClick = { viewModel.syncAllData() },
                enabled = !isSyncing,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 16.dp, end = 16.dp)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), CircleShape)
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "同步",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }

    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        text = "数据配置",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                // ===== 账号筛选 =====
                item {
                    Text(
                        text = "参与统计的账号 (可多选对比)",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }
                items(allAccounts) { account ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.toggleAccount(account.studentId) }
                            .padding(vertical = 4.dp)
                    ) {
                        Checkbox(
                            checked = selectedAccountIds.contains(account.studentId),
                            onCheckedChange = { viewModel.toggleAccount(account.studentId) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${account.name} (${account.studentId})",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                // ===== 学期筛选 =====
                item {
                    Text(
                        text = "学期范围",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                    )
                }

                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.selectTerm(isAll = true) }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = selectedTerm.isAll,
                            onClick = { viewModel.selectTerm(isAll = true) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "全部学期",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (selectedTerm.isAll) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }

                items(availableTerms) { (xnm, xqm) ->
                    val isSelected = !selectedTerm.isAll && selectedTerm.xnm == xnm && selectedTerm.xqm == xqm
                    val termName = "${xnm}-${xnm.toInt() + 1}学年 第${if (xqm == "3") "一" else "二"}学期"
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.selectTerm(isAll = false, xnm = xnm, xqm = xqm) }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { viewModel.selectTerm(isAll = false, xnm = xnm, xqm = xqm) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = termName,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
                
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
fun TermTimelineNode(
    termName: String,
    teacherMap: Map<String, List<CourseNodeData>>
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    
    Column(
        modifier = Modifier.wrapContentWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 时间轴核心绘制区域
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minWidth = 200.dp)
                .height(80.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // 绘制贯穿左右的时间主线
                drawLine(
                    color = primaryColor.copy(alpha = 0.3f),
                    start = Offset(0f, size.height / 2),
                    end = Offset(size.width, size.height / 2),
                    strokeWidth = 6f
                )
                // 绘制当前学期的节点圆圈
                drawCircle(
                    color = primaryColor,
                    radius = 16f,
                    center = Offset(size.width / 2, size.height / 2)
                )
                drawCircle(
                    color = Color.White,
                    radius = 8f,
                    center = Offset(size.width / 2, size.height / 2)
                )
            }
            // 学期文字
            Text(
                text = termName,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.offset(y = (-28).dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 教师与课程横向延展
        Row(
            modifier = Modifier.wrapContentWidth(),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            teacherMap.forEach { (teacher, courses) ->
                TeacherBranch(teacherName = teacher, courses = courses)
            }
        }
    }
}

@Composable
fun TeacherBranch(teacherName: String, courses: List<CourseNodeData>) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(IntrinsicSize.Max)
    ) {
        // 教师卡片
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.widthIn(min = 80.dp)
        ) {
            Text(
                text = teacherName,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 向下连线
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(24.dp)
                .background(MaterialTheme.colorScheme.outlineVariant)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 课程列表
        courses.forEach { node ->
            CourseChip(node)
        }
    }
}

@Composable
fun CourseChip(node: CourseNodeData) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Text(
                text = node.courseName,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            node.primaryGrade?.let { grade ->
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        if (grade.regularScore.isNotEmpty() || grade.finalScore.isNotEmpty()) {
                            Text(
                                text = "平:${grade.regularScore.ifEmpty { "-" }} | 期:${grade.finalScore.ifEmpty { "-" }}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (grade.experimentScore.isNotEmpty()) {
                            Text(
                                text = "实验:${grade.experimentScore}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Text(
                        text = grade.score,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            node.makeupGrade?.let { makeup ->
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "补考",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = makeup.score,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}
