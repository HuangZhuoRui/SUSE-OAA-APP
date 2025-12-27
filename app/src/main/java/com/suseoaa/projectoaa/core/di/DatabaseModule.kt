package com.suseoaa.projectoaa.core.di

import android.content.Context
import androidx.room.Room
import com.suseoaa.projectoaa.core.database.AppDatabase
import com.suseoaa.projectoaa.core.database.dao.CourseDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
//    提供数据库实例
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context, AppDatabase::class.java,
            "course_database.db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    //    提供CourseDao实例
    @Provides
    fun provideCourseDao(database: AppDatabase): CourseDao {
        return database.courseDao()
    }
}