package com.suseoaa.projectoaa.core.database.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation


//课程基本信息表，主表
@Entity(
    tableName = "课程表",
    primaryKeys = ["studentId", "courseName", "xnm", "xqm", "isCustom"]
)
data class CourseEntity(
    val studentId: String,
    val courseName: String,
    val xnm: String, // 学年 (2024)
    val xqm: String, // 学期 (3 或 12)
    val isCustom: Boolean = false, // 是否是用户自己添加的
    // 选填信息
    val teacher: String = "", // 老师名字
    val credit: String = "",  // 学分 (totalHours)
    val nature: String = "",  // 课程性质 (必修/选修)
    val category: String = "",// 课程类别
    val assessment: String = "" // 考核方式 (考试/查)
)

@Entity(
    tableName = "class_times",
    foreignKeys = [
        ForeignKey(
            entity = CourseEntity::class,
            parentColumns = ["studentId", "courseName", "xnm", "xqm", "isCustom"],
            childColumns = ["studentId", "courseOwnerName", "xnm", "xqm", "isCustom"],
            onDelete = ForeignKey.CASCADE // 删了课程，时间表自动删
        )
    ],
    indices = [
        // 加索引，查询更快
        Index("studentId", "courseOwnerName", "xnm", "xqm", "isCustom")
    ]
)
//上课时间表，从表
data class ClassTimeEntity(
    @PrimaryKey(autoGenerate = true)
    val uniqueId: Long = 0, // 自增ID

    // 外键关联字段
    val studentId: String,
    val courseOwnerName: String, // 对应 CourseEntity 的 courseName
    val xnm: String,
    val xqm: String,
    val isCustom: Boolean,

    // 时间地点详情
    val weekday: Int, // 1-7 (周一到周日)
    val startNode: Int, // 第几节课开始 (1, 3, 5...)
    val step: Int,      // 上几节课 (通常是 2)
    val weeks: String,  // 原始周次字符串 "1-16周"
    val weeksMask: Long, // 位运算掩码
    val location: String, // 教室
    val teacher: String = "" // 如果不同时间段老师不同，这里存
)

// 3. 聚合类,给UI显示用的
// 当你查课表时，你需要“课程信息”+“它所有的上课时间”
data class CourseWithTimes(
    @Embedded val course: CourseEntity,

    @Relation(
        parentColumn = "courseName",
        entityColumn = "courseOwnerName"
    )
    val times: List<ClassTimeEntity>
)
