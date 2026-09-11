package com.freedu.myinterviews.domain.model

/** Pipeline stages shown on the Kanban board. Order matters (funnel order). */
enum class ApplicationStatus {
    APPLIED, SCREENING, INTERVIEWING, OFFER, REJECTED, ACCEPTED, WITHDRAWN;

    companion object {
        val funnelOrder = listOf(APPLIED, SCREENING, INTERVIEWING, OFFER, ACCEPTED)
        fun fromName(name: String?): ApplicationStatus =
            values().firstOrNull { it.name == name } ?: APPLIED
    }
}

enum class RoundType {
    PHONE_SCREEN, ONLINE_ASSESSMENT, TECHNICAL, SYSTEM_DESIGN,
    BEHAVIORAL, HR, PANEL, FINAL, OTHER;

    companion object {
        fun fromName(name: String?): RoundType =
            values().firstOrNull { it.name == name } ?: TECHNICAL
    }
}

enum class RoundStatus { UPCOMING, COMPLETED, CANCELLED, RESCHEDULED;
    companion object {
        fun fromName(name: String?): RoundStatus =
            values().firstOrNull { it.name == name } ?: UPCOMING
    }
}

enum class RoundOutcome { PASSED, FAILED, PENDING, GHOSTED;
    companion object {
        fun fromName(name: String?): RoundOutcome =
            values().firstOrNull { it.name == name } ?: PENDING
    }
}

enum class WorkMode { REMOTE, HYBRID, ONSITE;
    companion object {
        fun fromName(name: String?): WorkMode =
            values().firstOrNull { it.name == name } ?: HYBRID
    }
}

enum class ThemeMode { SYSTEM, LIGHT, DARK, FOCUS }

data class Company(
    val id: Long = 0,
    val name: String,
    val website: String = "",
    val industry: String = "",
    val notes: String = "",
    val source: String = "",
    val tags: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    // Dossier + accent
    val accentColor: Long = 0,
    val rating: Float = 0f,
    val size: String = "",
    val funding: String = "",
    val difficulty: String = ""
)

data class JobApplication(
    val id: Long = 0,
    val companyId: Long,
    val companyName: String = "",
    val jobTitle: String,
    val jobDescription: String = "",
    val jobLink: String = "",
    val appliedDate: Long = System.currentTimeMillis(),
    val status: ApplicationStatus = ApplicationStatus.APPLIED,
    val salaryMin: Long = 0,
    val salaryMax: Long = 0,
    val location: String = "",
    val workMode: WorkMode = WorkMode.HYBRID,
    val resumeVersion: String = "",
    val coverLetter: String = "",
    val resumeText: String = "",
    val profileId: String = "default",
    val updatedAt: Long = System.currentTimeMillis()
)

data class InterviewRound(
    val id: Long = 0,
    val applicationId: Long,
    val jobTitle: String = "",
    val companyName: String = "",
    val roundType: RoundType = RoundType.TECHNICAL,
    val scheduledAt: Long = System.currentTimeMillis(),
    val durationMin: Int = 60,
    val mode: String = "Video",
    val interviewers: String = "",
    val platformLink: String = "",
    val status: RoundStatus = RoundStatus.UPCOMING,
    val outcome: RoundOutcome = RoundOutcome.PENDING,
    val selfRating: Int = 0,
    val questionsAsked: String = "",
    val yourAnswers: String = "",
    val wentWell: String = "",
    val toImprove: String = "",
    val thankYouSent: Boolean = false,
    val prepNotes: String = "",
    val prepChecklistJson: String = "",
    val resources: String = "",
    val voiceMemoUri: String = ""
)

data class Contact(
    val id: Long = 0,
    val companyId: Long? = null,
    val companyName: String = "",
    val name: String,
    val role: String = "",
    val email: String = "",
    val phone: String = "",
    val linkedIn: String = "",
    val notes: String = ""
)

data class DocAttachment(
    val id: Long = 0,
    val applicationId: Long,
    val name: String,
    val uri: String = "",
    val type: String = "resume",
    val isSentVersion: Boolean = false
)

data class Offer(
    val id: Long = 0,
    val applicationId: Long,
    val companyName: String = "",
    val jobTitle: String = "",
    val baseSalary: Long = 0,
    val bonus: Long = 0,
    val equity: String = "",
    val benefits: String = "",
    val deadline: Long = 0,
    val notes: String = "",
    val decision: String = "PENDING"
)

data class QuestionBankItem(
    val id: Long = 0,
    val question: String,
    val answer: String = "",
    val tags: List<String> = emptyList()
)

data class PrepTemplate(
    val id: Long = 0,
    val name: String,
    val items: List<String> = emptyList()
)

data class PracticeSession(
    val id: Long = 0,
    val applicationId: Long,
    val role: String = "",
    val questions: List<String> = emptyList(),
    val answers: List<String> = emptyList(),
    val score: Int = 0,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class DashboardStats(
    val totalApplications: Int = 0,
    val activeCount: Int = 0,
    val interviewsUpcoming: Int = 0,
    val offersCount: Int = 0,
    val offerRate: Float = 0f,
    val statusCounts: Map<ApplicationStatus, Int> = emptyMap(),
    val passRateByRoundType: Map<RoundType, Float> = emptyMap(),
    val applicationsThisWeek: Int = 0,
    val mostCommonRejectionStage: String = "-"
)
