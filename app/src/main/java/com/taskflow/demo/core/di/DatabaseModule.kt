package com.taskflow.demo.core.di

import android.content.Context
import androidx.room.Room
import com.taskflow.demo.data.local.CachedProjectDao
import com.taskflow.demo.data.local.LocalUserDao
import com.taskflow.demo.data.local.ProjectDao
import com.taskflow.demo.data.local.SearchHistoryDao
import com.taskflow.demo.data.local.TaskAttachmentDao
import com.taskflow.demo.data.local.TaskCommentDao
import com.taskflow.demo.data.local.TaskDao
import com.taskflow.demo.data.local.TaskDraftDao
import com.taskflow.demo.data.local.TaskFlowDatabase
import com.taskflow.demo.data.local.UserProfileDao
import com.taskflow.demo.data.local.WorkspaceDao
import com.taskflow.demo.data.local.WorkspaceMemberDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): TaskFlowDatabase {
        return Room.databaseBuilder(
            context,
            TaskFlowDatabase::class.java,
            TaskFlowDatabase.DATABASE_NAME
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideLocalUserDao(database: TaskFlowDatabase): LocalUserDao = database.localUserDao()

    @Provides
    fun provideUserProfileDao(database: TaskFlowDatabase): UserProfileDao = database.userProfileDao()

    @Provides
    fun provideWorkspaceDao(database: TaskFlowDatabase): WorkspaceDao = database.workspaceDao()

    @Provides
    fun provideWorkspaceMemberDao(database: TaskFlowDatabase): WorkspaceMemberDao = database.workspaceMemberDao()

    @Provides
    fun provideProjectDao(database: TaskFlowDatabase): ProjectDao = database.projectDao()

    @Provides
    fun provideTaskDao(database: TaskFlowDatabase): TaskDao = database.taskDao()

    @Provides
    fun provideTaskCommentDao(database: TaskFlowDatabase): TaskCommentDao = database.taskCommentDao()

    @Provides
    fun provideTaskAttachmentDao(database: TaskFlowDatabase): TaskAttachmentDao = database.taskAttachmentDao()

    @Provides
    fun provideTaskDraftDao(database: TaskFlowDatabase): TaskDraftDao = database.taskDraftDao()

    @Provides
    fun provideSearchHistoryDao(database: TaskFlowDatabase): SearchHistoryDao = database.searchHistoryDao()

    @Provides
    fun provideCachedProjectDao(database: TaskFlowDatabase): CachedProjectDao = database.cachedProjectDao()
}
