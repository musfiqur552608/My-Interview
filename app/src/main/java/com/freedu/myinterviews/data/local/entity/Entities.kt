package com.freedu.myinterviews.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "companies")
data class CompanyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val website: String = "",
    val industry: String = "",
    val notes: String = "",
    val source: String = "",
    val tagsCsv: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    // Feature: company dossier + per-company accent (DB v2, all defaulted for auto-migration)
    @ColumnInfo(defaultValue = "0") val accentColor: Long = 0,
    @ColumnInfo(defaultValue = "0") val rating: Float = 0f,
    @ColumnInfo(defaultValue = "") val size: String = "",
    @ColumnInfo(defaultValue = "") val funding: String = "",
    @ColumnInfo(defaultValue = "") val difficulty: String = ""
)

@Entity(tableName = "applications")
data class ApplicationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val companyId: Long,
    val jobTitle: String,
    val jobDescription: String = "",
    val jobLink: String = "",
    val appliedDate: Long = System.currentTimeMillis(),
    val status: String = "APPLIED",
    val salaryMin: Long = 0,
    val salaryMax: Long = 0,
    val location: String = "",
    val workMode: String = "HYBRID",
    val resumeVersion: String = "",
    val coverLetter: String = "",
    @ColumnInfo(defaultValue = "") val resumeText: String = "",
    val profileId: String = "default",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "rounds")
data class RoundEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val applicationId: Long,
    val roundType: String = "TECHNICAL",
    val scheduledAt: Long = System.currentTimeMillis(),
    val durationMin: Int = 60,
    val mode: String = "Video",
    val interviewers: String = "",
    val platformLink: String = "",
    val status: String = "UPCOMING",
    val outcome: String = "PENDING",
    val selfRating: Int = 0,
    val questionsAsked: String = "",
    val yourAnswers: String = "",
    val wentWell: String = "",
    val toImprove: String = "",
    val thankYouSent: Boolean = false,
    val prepNotes: String = "",
    val prepChecklistJson: String = "",
    val resources: String = "",
    @ColumnInfo(defaultValue = "") val voiceMemoUri: String = ""
)

@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val companyId: Long? = null,
    val name: String,
    val role: String = "",
    val email: String = "",
    val phone: String = "",
    val linkedIn: String = "",
    val notes: String = ""
)

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val applicationId: Long,
    val name: String,
    val uri: String = "",
    val type: String = "resume",
    val isSentVersion: Boolean = false
)

@Entity(tableName = "offers")
data class OfferEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val applicationId: Long,
    val baseSalary: Long = 0,
    val bonus: Long = 0,
    val equity: String = "",
    val benefits: String = "",
    val deadline: Long = 0,
    val notes: String = "",
    val decision: String = "PENDING"
)

@Entity(tableName = "question_bank")
data class QuestionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val question: String,
    val answer: String = "",
    val tagsCsv: String = ""
)

@Entity(tableName = "prep_templates")
data class PrepTemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val itemsCsv: String = ""
)
