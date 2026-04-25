import re

with open('composeApp/src/commonMain/kotlin/com/suseoaa/projectoaa/presentation/course/CourseViewModel.kt', 'r') as f:
    text = f.read()

# Extract overlapCoursesByAccount
match = re.search(r'    @OptIn\(ExperimentalCoroutinesApi::class\)\n    private val overlapCoursesByAccount: StateFlow<Map<String, List<CourseWithTimes>>> = combine\(.*?\.stateIn[^\n]+\n', text, re.DOTALL)
if match:
    overlap = match.group(0)
    
    # Remove from its original location
    text = text.replace(overlap, "")
    
    # Insert right after // ==================== 课程数据 ====================
    split_str = '    // ==================== 课程数据 ====================\n'
    parts = text.split(split_str)
    
    with open('composeApp/src/commonMain/kotlin/com/suseoaa/projectoaa/presentation/course/CourseViewModel.kt', 'w') as f:
        f.write(parts[0] + split_str + '\n' + overlap + '\n' + parts[1])

