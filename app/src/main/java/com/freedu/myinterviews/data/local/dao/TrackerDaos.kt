package com.freedu.myinterviews.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.freedu.myinterviews.data.local.entity.ApplicationEntity
import com.freedu.myinterviews.data.local.entity.CompanyEntity
import com.freedu.myinterviews.data.local.entity.ContactEntity
import com.freedu.myinterviews.data.local.entity.DocumentEntity
import com.freedu.myinterviews.data.local.entity.OfferEntity
import com.freedu.myinterviews.data.local.entity.PrepTemplateEntity
import com.freedu.myinterviews.data.local.entity.QuestionEntity
import com.freedu.myinterviews.data.local.entity.RoundEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CompanyDao {
    @Query("SELECT * FROM companies ORDER BY name ASC")
    fun observeAll(): Flow<List<CompanyEntity>>
    @Query("SELECT * FROM companies WHERE id = :id")
    fun observeById(id: Long): Flow<CompanyEntity?>
    @Query("SELECT * FROM companies WHERE id = :id")
    suspend fun getById(id: Long): CompanyEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CompanyEntity): Long
    @Delete
    suspend fun delete(entity: CompanyEntity)
    @Query("DELETE FROM companies WHERE id = :id")
    suspend fun deleteById(id: Long)
    @Query("SELECT * FROM companies WHERE name LIKE '%' || :q || '%' OR notes LIKE '%' || :q || '%' OR industry LIKE '%' || :q || '%'")
    fun search(q: String): Flow<List<CompanyEntity>>
}

@Dao
interface ApplicationDao {
    @Query("SELECT * FROM applications WHERE profileId = :profile ORDER BY updatedAt DESC")
    fun observeAll(profile: String): Flow<List<ApplicationEntity>>
    /** Unfiltered join helper so rounds/offers resolve across all profiles. */
    @Query("SELECT * FROM applications ORDER BY updatedAt DESC")
    fun observeEvery(): Flow<List<ApplicationEntity>>
    @Query("SELECT * FROM applications WHERE companyId = :companyId ORDER BY appliedDate DESC")
    fun observeByCompany(companyId: Long): Flow<List<ApplicationEntity>>
    @Query("SELECT * FROM applications WHERE id = :id")
    fun observeById(id: Long): Flow<ApplicationEntity?>
    @Query("SELECT * FROM applications WHERE id = :id")
    suspend fun getById(id: Long): ApplicationEntity?
    @Query("SELECT * FROM applications WHERE status = :status AND profileId = :profile ORDER BY updatedAt DESC")
    fun observeByStatus(status: String, profile: String): Flow<List<ApplicationEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ApplicationEntity): Long
    @Update
    suspend fun update(entity: ApplicationEntity)
    @Query("UPDATE applications SET status = :status, updatedAt = :now WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, now: Long = System.currentTimeMillis())
    @Query("DELETE FROM applications WHERE id = :id")
    suspend fun deleteById(id: Long)
    @Query("DELETE FROM applications WHERE companyId = :companyId")
    suspend fun deleteByCompany(companyId: Long)
    @Query("SELECT * FROM applications WHERE jobTitle LIKE '%' || :q || '%' OR location LIKE '%' || :q || '%'")
    fun searchApplications(q: String): Flow<List<ApplicationEntity>>
}

@Dao
interface RoundDao {
    @Query("SELECT * FROM rounds WHERE applicationId = :appId ORDER BY scheduledAt ASC")
    fun observeByApplication(appId: Long): Flow<List<RoundEntity>>
    @Query("SELECT * FROM rounds ORDER BY scheduledAt ASC")
    fun observeAll(): Flow<List<RoundEntity>>
    @Query("SELECT * FROM rounds WHERE status = 'UPCOMING' AND scheduledAt >= :now ORDER BY scheduledAt ASC")
    fun observeUpcoming(now: Long): Flow<List<RoundEntity>>
    @Query("SELECT * FROM rounds WHERE id = :id")
    suspend fun getById(id: Long): RoundEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: RoundEntity): Long
    @Update
    suspend fun update(entity: RoundEntity)
    @Query("DELETE FROM rounds WHERE id = :id")
    suspend fun deleteById(id: Long)
    @Query("DELETE FROM rounds WHERE applicationId = :appId")
    suspend fun deleteByApplication(appId: Long)
    @Query("SELECT * FROM rounds WHERE interviewers LIKE '%' || :q || '%' OR questionsAsked LIKE '%' || :q || '%'")
    fun search(q: String): Flow<List<RoundEntity>>
}

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts ORDER BY name ASC")
    fun observeAll(): Flow<List<ContactEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ContactEntity): Long
    @Delete
    suspend fun delete(entity: ContactEntity)
    @Query("SELECT * FROM contacts WHERE name LIKE '%' || :q || '%' OR role LIKE '%' || :q || '%' OR email LIKE '%' || :q || '%'")
    fun search(q: String): Flow<List<ContactEntity>>
}

@Dao
interface DocumentDao {
    @Query("SELECT * FROM documents WHERE applicationId = :appId")
    fun observeByApplication(appId: Long): Flow<List<DocumentEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DocumentEntity): Long
    @Query("DELETE FROM documents WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface OfferDao {
    @Query("SELECT * FROM offers")
    fun observeAll(): Flow<List<OfferEntity>>
    @Query("SELECT * FROM offers WHERE applicationId = :appId LIMIT 1")
    fun observeByApplication(appId: Long): Flow<OfferEntity?>
    @Query("SELECT * FROM offers WHERE applicationId = :appId LIMIT 1")
    suspend fun getByApplication(appId: Long): OfferEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: OfferEntity): Long
    @Query("DELETE FROM offers WHERE applicationId = :appId")
    suspend fun deleteByApplication(appId: Long)
}

@Dao
interface QuestionDao {
    @Query("SELECT * FROM question_bank ORDER BY id DESC")
    fun observeAll(): Flow<List<QuestionEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: QuestionEntity): Long
    @Query("DELETE FROM question_bank WHERE id = :id")
    suspend fun deleteById(id: Long)
    @Query("SELECT * FROM question_bank WHERE question LIKE '%' || :q || '%' OR answer LIKE '%' || :q || '%' OR tagsCsv LIKE '%' || :q || '%'")
    fun search(q: String): Flow<List<QuestionEntity>>
}

@Dao
interface PrepTemplateDao {
    @Query("SELECT * FROM prep_templates ORDER BY name ASC")
    fun observeAll(): Flow<List<PrepTemplateEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PrepTemplateEntity): Long
    @Query("DELETE FROM prep_templates WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface PracticeDao {
    @Query("SELECT * FROM practice_sessions WHERE applicationId = :appId ORDER BY createdAt DESC")
    fun observeByApplication(appId: Long): Flow<List<com.freedu.myinterviews.data.local.entity.PracticeSessionEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: com.freedu.myinterviews.data.local.entity.PracticeSessionEntity): Long
    @Query("DELETE FROM practice_sessions WHERE id = :id")
    suspend fun deleteById(id: Long)
}
