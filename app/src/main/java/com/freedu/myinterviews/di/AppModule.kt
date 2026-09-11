package com.freedu.myinterviews.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.freedu.myinterviews.data.local.TrackerDatabase
import com.freedu.myinterviews.data.local.entity.PrepTemplateEntity
import com.freedu.myinterviews.data.preferences.SettingsDataStore
import com.freedu.myinterviews.data.preferences.appSettingsStore
import com.freedu.myinterviews.data.repository.TrackerRepositoryImpl
import com.freedu.myinterviews.domain.repository.TrackerRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): TrackerDatabase =
        Room.databaseBuilder(ctx, TrackerDatabase::class.java, "tracker.db")
            .fallbackToDestructiveMigration()
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    // Seed reusable prep templates on first install (io thread via scope).
                    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                        // Seeding happens lazily in PrepViewModel if table is empty;
                        // callback kept minimal to avoid context leaks.
                    }
                }
            })
            .build()

    @Provides
    @Singleton
    fun provideRepository(db: TrackerDatabase): TrackerRepository =
        TrackerRepositoryImpl(db)

    @Provides
    @Singleton
    fun provideSettings(@ApplicationContext ctx: Context): SettingsDataStore =
        SettingsDataStore(ctx.appSettingsStore)

    @Provides
    @Singleton
    fun provideDefaultTemplates(): List<PrepTemplateEntity> = listOf(
        PrepTemplateEntity(name = "Standard Technical Round", itemsCsv = "Review DSA patterns;;Revise project deep-dives;;Prepare 2 STAR stories;;Test audio/video setup"),
        PrepTemplateEntity(name = "System Design Round", itemsCsv = "Clarify requirements;;Draw high-level diagram;;Discuss trade-offs;;Capacity estimation;;Prepare questions for interviewer"),
        PrepTemplateEntity(name = "Behavioral / HR Round", itemsCsv = "Research company values;;Prepare salary expectations;;Prepare why-this-company pitch;;Thank-you email draft")
    )
}
