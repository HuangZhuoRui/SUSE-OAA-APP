package com.suseoaa.projectoaa.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.suseoaa.projectoaa.core.database.entity.ClassTimeEntity
import com.suseoaa.projectoaa.core.database.entity.CourseEntity
import com.suseoaa.projectoaa.core.database.entity.CourseWithTimes
import kotlinx.coroutines.flow.Flow


@Dao
interface CourseDao {
    //    插入课程实体,冲突处理：如果冲突就替换
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourse(course: List<CourseEntity>)

    //    插入上课时间
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClassTime(times: List<ClassTimeEntity>)

    //    删除指定学期的旧数据,只删除获取到的数据，不删除用户自己添加的数据
    @Transaction
    @Query("DELETE FROM courses WHERE studentId = :studentId AND xnm = :xnm AND xqm = :xqm AND isCustom = 0")
    suspend fun deleteOldCourses(studentId: String, xnm: String, xqm: String)

    //     查询数据
    @Transaction
    @Query("SELECT * FROM courses WHERE studentId = :studentId AND xnm = :xnm AND xqm = :xqm")
    fun getCourseWithTimes(
        studentId: String,
        xnm: String,
        xqm: String
    ): Flow<List<CourseWithTimes>>

    //    事务处理,先删除，再插入
    @Transaction
    suspend fun updateTermCourse(
        studentId: String,
        xnm: String,
        xqm: String,
        course: List<CourseEntity>,
        times: List<ClassTimeEntity>
    ) {
        deleteOldCourses(studentId, xnm, xqm)
        insertCourse(course)
        insertClassTime(times)
    }
}