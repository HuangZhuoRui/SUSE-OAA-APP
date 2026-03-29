import re

file_path = "composeApp/src/commonMain/kotlin/com/suseoaa/projectoaa/ui/screen/gpa/GpaScreen.kt"
with open(file_path, "r") as f:
    content = f.read()

# 1. Imports
imports_to_add = [
    "import androidx.compose.foundation.background",
    "import androidx.compose.foundation.BorderStroke",
    "import androidx.compose.foundation.shape.RoundedCornerShape",
    "import androidx.compose.material.icons.filled.Info"
]
for imp in imports_to_add:
    if imp not in content:
        content = content.replace("import androidx.compose.foundation.layout.*", f"{imp}\nimport androidx.compose.foundation.layout.*")


# 2. Card
old_card = """        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                StatItem("总绩点", totalGpa, totalCredits)
                StatItem("学位绩点", degreeGpa, degreeCredits)
            }
        }"""
new_card = """        Card(
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
        }"""
content = content.replace(old_card, new_card)

# 3. Filters
old_filters = """        // 2. 筛选和排序操作栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 0.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 筛选分类
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = filterType == FilterType.ALL,
                    onClick = { onFilterTypeChange(FilterType.ALL) },
                    label = { Text("全部") },
                    leadingIcon = if (filterType == FilterType.ALL) {
                        { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                    } else null
                )
                FilterChip(
                    selected = filterType == FilterType.DEGREE_ONLY,
                    onClick = { onFilterTypeChange(FilterType.DEGREE_ONLY) },
                    label = { Text("学位课") },
                    leadingIcon = if (filterType == FilterType.DEGREE_ONLY) {
                        { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                    } else null
                )
            }

            // 排序按钮
            TextButton(
                onClick = {
                    val newOrder = if (sortOrder == SortOrder.DESCENDING)
                        SortOrder.ASCENDING else SortOrder.DESCENDING
                    onSortOrderChange(newOrder)
                }
            ) {
                Icon(
                    imageVector = if (sortOrder == SortOrder.DESCENDING)
                        Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(if (sortOrder == SortOrder.DESCENDING) "从高到低" else "从 低到高")
            }
        }

        Text(
            "点击课程修改成绩进行模拟",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )"""

new_filters = """        // 2. 筛选和排序操作栏
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
        }"""
content = content.replace(old_filters, new_filters)

# 4. Grid Padding
old_grid = """                    contentPadding = PaddingValues(
                        start = config.horizontalPadding,
                        end = config.horizontalPadding,
                        bottom = 16.dp + navBarHeight
                    ),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),"""
new_grid = """                    contentPadding = PaddingValues(
                        start = config.horizontalPadding,
                        end = config.horizontalPadding,
                        bottom = 16.dp + navBarHeight,
                        top = 8.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),"""
content = content.replace(old_grid, new_grid)

# 5. StatItem
old_stat = """@Composable
fun StatItem(label: String, gpa: String, credit: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Text(
            gpa,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            "总学分: $credit",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
    }
}"""
new_stat = """@Composable
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
            text = "共 " + credit + " 学分",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
        )
    }
}"""
content = content.replace(old_stat, new_stat)


# 6. GpaCourseItem
old_item = """@Composable
private fun GpaCourseItem(
    item: GpaCourseUiModel,
    onScoreChange: (String, Double) -> Unit
) {
    var showDialog by remember(item.courseId, item.termCode) { mutableStateOf(false) }

    val containerColor = if (item.isDegreeCourse) {
        MaterialTheme.colorScheme.tertiaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        onClick = { showDialog = true }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.courseName,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (item.isDegreeCourse) {
                        Surface(
                            color = MaterialTheme.colorScheme.tertiary,
                            shape = MaterialTheme.shapes.extraSmall
                        ) {
                            Text(
                                "学位课",
                                color = MaterialTheme.colorScheme.onTertiary,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    // 如果是等级制成绩，显示等级标签
                    if (item.isGradeLevel) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondary,
                            shape = MaterialTheme.shapes.extraSmall
                        ) {
                            Text(
                                "等级制",
                                color = MaterialTheme.colorScheme.onSecondary,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    // 如果是仅通过类成绩（合格/通过/免修），显示标签
                    if (item.isPassOnly) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = MaterialTheme.shapes.extraSmall
                        ) {
                            Text(
                                "通过制",
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        "学分: ${item.creditText}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    item.displayScore,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "GPA: ${item.displayGpa}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }"""

new_item = """@Composable
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
    }"""
content = content.replace(old_item, new_item)

with open(file_path, "w") as f:
    f.write(content)
print("done")
