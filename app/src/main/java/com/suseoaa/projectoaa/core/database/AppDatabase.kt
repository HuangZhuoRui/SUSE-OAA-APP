package com.suseoaa.projectoaa.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.suseoaa.projectoaa.core.database.dao.AcademicDao
import com.suseoaa.projectoaa.core.database.dao.CourseDao
import com.suseoaa.projectoaa.core.database.dao.GradeDao
import com.suseoaa.projectoaa.core.database.entity.ClassTimeEntity
import com.suseoaa.projectoaa.core.database.entity.CourseAccountEntity
import com.suseoaa.projectoaa.core.database.entity.CourseEntity
import com.suseoaa.projectoaa.core.database.entity.ExamCacheEntity
import com.suseoaa.projectoaa.core.database.entity.GradeEntity
import com.suseoaa.projectoaa.core.database.entity.MessageCacheEntity

@Database(
    entities = [
        CourseEntity::class,
        ClassTimeEntity::class,
        CourseAccountEntity::class,
        GradeEntity::class,
        ExamCacheEntity::class,
        MessageCacheEntity::class
    ],
    version = 11,
    exportSchema = false
)
abstract class CourseDatabase : RoomDatabase() {
    abstract fun courseDao(): CourseDao
    abstract fun gradeDao(): GradeDao
    abstract fun academicDao(): AcademicDao

    companion object {
        @Volatile
        private var INSTANCE: CourseDatabase? = null

        // 定义迁移策略：从版本 10 升级到 11
        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // SQLite 不支持一次添加多列，需要分三条执行
                // 添加 jxbId 列，默认值为空字符串
                db.execSQL("ALTER TABLE grades ADD COLUMN jxbId TEXT NOT NULL DEFAULT ''")
                // 添加 regularScore 列
                db.execSQL("ALTER TABLE grades ADD COLUMN regularScore TEXT NOT NULL DEFAULT ''")
                // 添加 finalScore 列
                db.execSQL("ALTER TABLE grades ADD COLUMN finalScore TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getInstance(context: Context): CourseDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): CourseDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                CourseDatabase::class.java,
                "course_schedule.db"
            )
                .addMigrations(MIGRATION_10_11) // 注册迁移脚本
                .fallbackToDestructiveMigration(false) // 禁止破坏性迁移(防止数据被删)
                .build()
        }
    }
}