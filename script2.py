import os

file_path = "composeApp/src/commonMain/kotlin/com/suseoaa/projectoaa/ui/screen/gpa/GpaScreen.kt"

with open(file_path, "r") as f:
    content = f.read()

missing_imports = [
    "import androidx.compose.foundation.background",
    "import androidx.compose.foundation.BorderStroke",
    "import androidx.compose.foundation.shape.RoundedCornerShape",
    "import androidx.compose.material.icons.filled.Edit",
    "import androidx.compose.material.icons.filled.Info"
]

for imp in missing_imports:
    if imp not in content:
        content = content.replace("import androidx.compose.foundation.layout.*", f"{imp}\nimport androidx.compose.foundation.layout.*")

target_idx = content.find("@Composable\nprivate fun GpaContent(")
if target_idx != -1:
    new_content = content[:target_idx] + """@Composable
fun GpaContent(
    courseList: List<CourseRaw>,
    totalGpa: String,
    totalCredits: String,
    degreeGpa: String,
    degreeCredits: String,
    sortOrder: SortOrder,
    filterType: FilterType,
    onSortOrderChange: (SortOrder) -> Unit,
    onFilterTypeChange: (FilterType) -> Unit,
    onScoreChange: (String, Double) -> Unit
) {
    val courseUiList by remember(courseList) {
        derivedStateOf { courseList.map { it.toUiModel() } }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // 1. 顶部统计卡片
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            shape = RoundedCornerShape(24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatItem("总绩点", totalGpa, totalCredits)
                Box(
                    modifier = Modifier
                        .height(48.dp)
                        .width(1.dp)
                        .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
                )
                StatItem("学位绩点", degreeGpa, degreeCredits)
            }
        }

        // 2. 筛选和排序操作栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 筛选分类
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = filterType == FilterType.ALL,
                    onClick = { onFilterTypeChange(FilterType.ALL) },
                    label = { Text("全部") },
                    shape = RoundedCornerShape(16.dp)
                )
                FilterChip(
                    selected = filterType == FilterType.DEGREE_ONLY,
                    onClick = { onFilterTypeChange(FilterType.DEGREE_ONLY) },
                    label = { Text("学位课") },
                    shape = RoundedCornerShape(16.dp)
                )
            }

            // 排序按钮
            TextButton(
                onClick = {
                    val newOrder = if (sortOrder == SortOrder.DESCENDING)
                        SortOrder.ASCENDING else SortOrder.DESCENDING
                    onSortOrderChange(newOrder)
                },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = if (sortOrder == SortOrder.DESCENDING) "分从高到低" else "分从低到高",
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = if (sortOrder == SortOrder.DESCENDING)
                        Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                "点击课程可修改成绩进行重修、补考等模拟",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }

        // 3. 课程列表
        val navBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

        Box(modifier = Modifier.weight(1f)) {
            AdaptiveLayout { config ->
                LazyVerticalGrid(
                    columns = GridCells.Fixed(config.getListColumns()),
                    contentPadding = PaddingValues(
                        start = config.horizontalPadding,
                        end = config.horizontalPadding,
                        bottom = 16.dp + navBarHeight,
                        top = 8.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = courseUiList,
                        key = { "${it.courseId}_${it.termCode}" },
                        contentType = { "gpa_course_item" }
                    ) { item ->
                        GpaCourseItem(
                            item = item,
                            onScoreChange = onScoreChange
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatItem(label: String, gpa: String, credit: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = gpa,
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = f"共 {credit} 学分",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun CourseTag(text: String, containerColor: Color, contentColor: Color) {
    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.padding(end = 6.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun GpaCourseItem(
    item: GpaCourseUiModel,
    onScoreChange: (String, Double) -> Unit
) {
    var showDialog by remember(item.courseId, item.termCode) { mutableStateOf(false) }

    val containerColor = MaterialTheme.colorScheme.surface
    val borderColor = if (item.isDegreeCourse) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, borderColor),
        onClick = { showDialog = true }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.courseName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (item.isDegreeCourse) {
                        CourseTag(
                            text = "学位课",
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    if (item.isGradeLevel) {
                        CourseTag(
                            text = "等级制",
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    if (item.isPassOnly) {
                        CourseTag(
                            text = "通过制",
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                    Text(
                        text = "${item.creditText} 学分",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = item.displayScore,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "GPA: ${item.displayGpa}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }

    if (showDialog) {
        EditScoreDialog(
            initialScore = item.displayScore,
            isGradeLevel = item.isGradeLevel,
            onDismiss = { showDialog = false },
            onConfirm = { scoreStr ->
                scoreStr.toDoubleOrNull()?.let { score ->
                    onScoreChange(item.courseId, score)
                }
                showDialog = false
            }
        )
    }
}

@Composable
fun EditScoreDialog(
    initialScore: String,
    isGradeLevel: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(if (isGradeLevel) "" else initialScore) }
    var selectedGrade by remember { mutableStateOf<String?>(if (isGradeLevel) initialScore else null) }

    // 等级制成绩选项及对应的分数
    val gradeOptions = listOf(
        "优" to "95",
        "良" to "85",
        "中" to "75",
        "及格" to "65",
        "差" to "55"
    )

    AlertDialog(
        containerColor = MaterialTheme.colorScheme.surface,
        onDismissRequest = onDismiss,
        title = { Text("修改模拟成绩") },
        text = {
            Column {
                if (isGradeLevel) {
                    Text(
                        "等级制成绩 (点击选择):",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        gradeOptions.forEach { (grade, score) ->
                            FilterChip(
                                selected = selectedGrade == grade,
                                onClick = {
                                    selectedGrade = grade
                                    text = score
                                },
                                label = { Text(grade, fontSize = 12.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Text(
                    text = "或直接输入分数:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = {
                        if (it.length <= 4) {
                            text = it
                            selectedGrade = null
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    label = { Text("分数 (0-100)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(text) }) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
"""
    # Fix the f-string issue in Kotlin code representation
    new_content = new_content.replace('f"共 {credit} 学分"', '"共 ${credit} 学分"')
    
    with open(file_path, "w") as f:
        f.write(new_content)
    print("Successfully updated")
else:
    print("Error finding GpaContent")
