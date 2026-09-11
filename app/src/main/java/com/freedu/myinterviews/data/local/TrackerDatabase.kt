package com.freedu.myinterviews.data.local

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import com.freedu.myinterviews.data.local.dao.ApplicationDao
import com.freedu.myinterviews.data.local.dao.CompanyDao
import com.freedu.myinterviews.data.local.dao.ContactDao
import com.freedu.myinterviews.data.local.dao.DocumentDao
import com.freedu.myinterviews.data.local.dao.OfferDao
import com.freedu.myinterviews.data.local.dao.PracticeDao
import com.freedu.myinterviews.data.local.dao.PrepTemplateDao
import com.freedu.myinterviews.data.local.dao.QuestionDao
import com.freedu.myinterviews.data.local.dao.RoundDao
import com.freedu.myinterviews.data.local.entity.ApplicationEntity
import com.freedu.myinterviews.data.local.entity.CompanyEntity
import com.freedu.myinterviews.data.local.entity.ContactEntity
import com.freedu.myinterviews.data.local.entity.DocumentEntity
import com.freedu.myinterviews.data.local.entity.OfferEntity
import com.freedu.myinterviews.data.local.entity.PracticeSessionEntity
import com.freedu.myinterviews.data.local.entity.PrepTemplateEntity
import com.freedu.myinterviews.data.local.entity.QuestionEntity
import com.freedu.myinterviews.data.local.entity.RoundEntity

/**
 * Room is the single source of truth (fully offline-first).
 *
 * v2 adds (all defaulted → AutoMigration, no data loss):
 * - companies: accentColor, rating, size, funding, difficulty (dossier + accents)
 * - applications: resumeText (JD match score)
 * - rounds: voiceMemoUri (voice reflections)
 * - new table practice_sessions (AI coach)
 */
@Database(
    entities = [
        CompanyEntity::class,
        ApplicationEntity::class,
        RoundEntity::class,
        ContactEntity::class,
        DocumentEntity::class,
        OfferEntity::class,
        QuestionEntity::class,
        PrepTemplateEntity::class,
        PracticeSessionEntity::class
    ],
    version = 2,
    autoMigrations = [AutoMigration(from = 1, to = 2)],
    exportSchema = true
)
abstract class TrackerDatabase : RoomDatabase() {
    abstract fun companyDao(): CompanyDao
    abstract fun applicationDao(): ApplicationDao
    abstract fun roundDao(): RoundDao
    abstract fun contactDao(): ContactDao
    abstract fun documentDao(): DocumentDao
    abstract fun offerDao(): OfferDao
    abstract fun questionDao(): QuestionDao
    abstract fun prepTemplateDao(): PrepTemplateDao
    abstract fun practiceDao(): PracticeDao
}
