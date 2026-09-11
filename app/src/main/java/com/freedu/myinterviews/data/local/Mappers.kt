package com.freedu.myinterviews.data.local

import com.freedu.myinterviews.data.local.entity.ApplicationEntity
import com.freedu.myinterviews.data.local.entity.CompanyEntity
import com.freedu.myinterviews.data.local.entity.ContactEntity
import com.freedu.myinterviews.data.local.entity.DocumentEntity
import com.freedu.myinterviews.data.local.entity.OfferEntity
import com.freedu.myinterviews.data.local.entity.PracticeSessionEntity
import com.freedu.myinterviews.data.local.entity.PrepTemplateEntity
import com.freedu.myinterviews.data.local.entity.QuestionEntity
import com.freedu.myinterviews.data.local.entity.RoundEntity
import com.freedu.myinterviews.domain.model.ApplicationStatus
import com.freedu.myinterviews.domain.model.Company
import com.freedu.myinterviews.domain.model.Contact
import com.freedu.myinterviews.domain.model.DocAttachment as DomainDoc
import com.freedu.myinterviews.domain.model.InterviewRound
import com.freedu.myinterviews.domain.model.JobApplication
import com.freedu.myinterviews.domain.model.Offer
import com.freedu.myinterviews.domain.model.PrepTemplate
import com.freedu.myinterviews.domain.model.QuestionBankItem
import com.freedu.myinterviews.domain.model.RoundOutcome
import com.freedu.myinterviews.domain.model.RoundStatus
import com.freedu.myinterviews.domain.model.RoundType
import com.freedu.myinterviews.domain.model.WorkMode

private const val SEP = ";;"

fun List<String>.toCsv(): String = joinToString(SEP) { it.replace(SEP, " ").trim() }
fun String.toStringList(): List<String> =
    if (isBlank()) emptyList() else split(SEP).map { it.trim() }.filter { it.isNotEmpty() }

// Company
fun CompanyEntity.toDomain() = Company(id, name, website, industry, notes, source, tagsCsv.toStringList(), createdAt,
    accentColor, rating, size, funding, difficulty)
fun Company.toEntity() = CompanyEntity(id, name, website, industry, notes, source, tags.toCsv(), createdAt,
    accentColor, rating, size, funding, difficulty)

// Application (joined with company name at repo layer)
fun ApplicationEntity.toDomain(companyName: String = "") = JobApplication(
    id, companyId, companyName, jobTitle, jobDescription, jobLink, appliedDate,
    ApplicationStatus.fromName(status), salaryMin, salaryMax, location,
    WorkMode.fromName(workMode), resumeVersion, coverLetter, resumeText, profileId, updatedAt
)
fun JobApplication.toEntity() = ApplicationEntity(
    id, companyId, jobTitle, jobDescription, jobLink, appliedDate, status.name,
    salaryMin, salaryMax, location, workMode.name, resumeVersion, coverLetter, resumeText, profileId, updatedAt
)

// Round
fun RoundEntity.toDomain(companyName: String = "", jobTitle: String = "") = InterviewRound(
    id, applicationId, jobTitle, companyName, RoundType.fromName(roundType),
    scheduledAt, durationMin, mode, interviewers, platformLink,
    RoundStatus.fromName(status), RoundOutcome.fromName(outcome), selfRating,
    questionsAsked, yourAnswers, wentWell, toImprove, thankYouSent,
    prepNotes, prepChecklistJson, resources, voiceMemoUri
)
fun InterviewRound.toEntity() = RoundEntity(
    id, applicationId, roundType.name, scheduledAt, durationMin, mode, interviewers,
    platformLink, status.name, outcome.name, selfRating, questionsAsked, yourAnswers,
    wentWell, toImprove, thankYouSent, prepNotes, prepChecklistJson, resources, voiceMemoUri
)

// Contact
fun ContactEntity.toDomain(companyName: String = "") =
    Contact(id, companyId, companyName, name, role, email, phone, linkedIn, notes)
fun Contact.toEntity() =
    ContactEntity(id, companyId, name, role, email, phone, linkedIn, notes)

// Document
fun DocumentEntity.toDomain() = DomainDoc(id, applicationId, name, uri, type, isSentVersion)
fun DomainDoc.toEntity() = DocumentEntity(id, applicationId, name, uri, type, isSentVersion)

// Offer
fun OfferEntity.toDomain(companyName: String = "", jobTitle: String = "") =
    Offer(id, applicationId, companyName, jobTitle, baseSalary, bonus, equity, benefits, deadline, notes, decision)
fun Offer.toEntity() = OfferEntity(id, applicationId, baseSalary, bonus, equity, benefits, deadline, notes, decision)

// Question bank / templates
fun QuestionEntity.toDomain() = QuestionBankItem(id, question, answer, tagsCsv.toStringList())
fun QuestionBankItem.toEntity() = QuestionEntity(id, question, answer, tags.toCsv())
fun PrepTemplateEntity.toDomain() = PrepTemplate(id, name, itemsCsv.toStringList())
fun PrepTemplate.toEntity() = PrepTemplateEntity(id, name, items.toCsv())

fun PracticeSessionEntity.toDomain() = com.freedu.myinterviews.domain.model.PracticeSession(
    id, applicationId, role, questionsJson.toStringList(), answersJson.toStringList(),
    score, notes, createdAt
)
fun com.freedu.myinterviews.domain.model.PracticeSession.toEntity() = PracticeSessionEntity(
    id, applicationId, role, questions.toCsv(), answers.toCsv(), score, notes, createdAt
)

// Question bank / templates
