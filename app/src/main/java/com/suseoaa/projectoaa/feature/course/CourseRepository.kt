package com.suseoaa.projectoaa.feature.course

import com.suseoaa.projectoaa.core.database.dao.CourseDao
import com.suseoaa.projectoaa.core.database.entity.ClassTimeEntity
import com.suseoaa.projectoaa.core.database.entity.CourseEntity
import com.suseoaa.projectoaa.core.database.entity.CourseWithTimes
import com.suseoaa.projectoaa.core.network.model.CourseResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CourseRepository @Inject constructor(
    private val dao: CourseDao
) {
    //    获取课表
    fun getCourseFlow(studentId: String, xnm: String, xqm: String): Flow<List<CourseWithTimes>> {
        return dao.getCourseWithTimes(studentId, xnm, xqm)
    }

    suspend fun updateCourseData(studentId: String, response: CourseResponse) = withContext(
        Dispatchers.Default
    ) {
        val rawList = response.kbList
        if (rawList.isEmpty()) return@withContext

//        获取学年学期
        val xnm = response.xsxx.xNM
        val xqm = response.xsxx.xQM

//        按照课程名称归类
        val groupedMap = rawList.filter { it.kcmc.isNotBlank() }
            .groupBy { it.kcmc }

        val courses = mutableListOf<CourseEntity>()
        val classTimes = mutableListOf<ClassTimeEntity>()

//        遍历每一门课
        for ((courseName, kbList) in groupedMap) {
            val info = kbList.first()

            val courseEntity = CourseEntity(
                studentId = studentId,
                courseName = courseName, // kcmc -> 课程名
                xnm = xnm,
                xqm = xqm,
                isCustom = false,
                // 字段映射
                teacher = info.xm,         // xm -> 教师姓名
                credit = info.xf,          // xf -> 学分
                assessment = info.khfsmc,  // khfsmc -> 考核方式
                nature = info.kcxz         // kcxz -> 课程性质
            )
            courses.add(courseEntity)

            //        获取这门课所有的上课时间
            for (kb in kbList) {
//                解析周次
                val mask = parseWeeksToMask(kb.zcd)
//                解析节次
                val (startNode, step) = parsePeriod(kb.jcs)

                val timeEntity = ClassTimeEntity(
                    studentId = studentId,
                    courseOwnerName = courseName, // 外键：关联回上面的 courseEntity
                    xnm = xnm,
                    xqm = xqm,
                    isCustom = false,
                    // === 字段映射 ===
                    weekday = kb.xqj.toIntOrNull() ?: 1, // xqj -> 星期几 (1-7)
                    startNode = startNode,
                    step = step,
                    location = kb.cdmc,    // cdmc -> 教室
                    weeks = kb.zcd,        // zcd -> 原始周次文本
                    weeksMask = mask       // 计算后的二进制位
                )
                classTimes.add(timeEntity)
            }
        }
        dao.updateTermCourse(studentId, xnm, xqm, courses, classTimes)
    }

    // 工具函数区

    // 1. 解析节次：把 "1-2节" 解析成 (start=1, step=2)
    // 或者是 "3-5节" -> (start=3, step=3)
    private fun parsePeriod(raw: String): Pair<Int, Int> {
        // 去掉“节”字
        val clean = raw.replace("节", "")

        if (clean.contains("-")) {
            val parts = clean.split("-")
            val start = parts[0].toIntOrNull() ?: 1
            val end = parts[1].toIntOrNull() ?: start
            return start to (end - start + 1) // 长度 = 结束 - 开始 + 1
        }
        // 如果只有一个数字 "1"
        return (clean.toIntOrNull() ?: 1) to 1
    }

    // 2. 解析周次：把 "1-16周(单)" 解析成 Long 类型的位掩码
    // 这是最核心的算法！
    private fun parseWeeksToMask(raw: String): Long {
        if (raw.isBlank()) return 0L
        var mask = 0L

        // 预处理：统一符号
        val normalized = raw.replace("，", ",").replace(";", ",")
        val isOdd = raw.contains("单") // 是否只含单周
        val isEven = raw.contains("双") // 是否只含双周

        // 分割多段周次 (比如 "1-8周,10-16周")
        val parts = normalized.split(",")
        for (part in parts) {
            // 提取纯数字部分 (比如 "1-16")
            val cleanPart = part.filter { it.isDigit() || it == '-' }
            if (cleanPart.isBlank()) continue

            if (cleanPart.contains("-")) {
                // 处理区间 "1-16"
                val range = cleanPart.split("-")
                val start = range[0].toIntOrNull() ?: 1
                val end = range.getOrNull(1)?.toIntOrNull() ?: start

                // 循环把每一周都“打卡”到 mask 上
                for (w in start..end) {
                    if (shouldInclude(w, isOdd, isEven)) {
                        mask = mask or (1L shl (w - 1)) // 核心位运算：把第 w 位设为 1
                    }
                }
            } else {
                // 处理单点 "3"
                val w = cleanPart.toIntOrNull()
                if (w != null && shouldInclude(w, isOdd, isEven)) {
                    mask = mask or (1L shl (w - 1))
                }
            }
        }
        return mask
    }

    // 辅助判断：这一周是否符合单双周要求
    private fun shouldInclude(week: Int, isOdd: Boolean, isEven: Boolean): Boolean {
        if (isOdd && week % 2 == 0) return false // 要单周，但这是双周 -> 扔掉
        if (isEven && week % 2 != 0) return false // 要双周，但这是单周 -> 扔掉
        return true
    }
}