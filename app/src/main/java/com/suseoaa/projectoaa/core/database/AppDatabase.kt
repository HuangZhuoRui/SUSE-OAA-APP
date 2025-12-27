package com.suseoaa.projectoaa.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.suseoaa.projectoaa.core.database.dao.CourseDao
import com.suseoaa.projectoaa.core.database.entity.ClassTimeEntity
import com.suseoaa.projectoaa.core.database.entity.CourseEntity

@Database(
    entities = [
        CourseEntity::class,
        ClassTimeEntity::class
    ],
//    如果数据库结构有更新，要增加这个数字
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    //    提供Dao接口
    abstract fun courseDao(): CourseDao
}