package com.clubnexus.android.data.model

data class LoginRequest(val email: String, val password: String)
data class RegisterRequest(val name: String, val email: String, val password: String, val department: String, val position: String)
data class AuthResponse(val token: String, val user: User)
data class User(val id: String, val name: String, val email: String, val role: String, val member: ClubMember? = null)
data class ClubMember(val id: String)
data class DashboardData(val stats: DashboardStats = DashboardStats(), val activity: List<ActivityItem> = emptyList())
data class DashboardStats(val members: Int = 0, val upcomingEvents: Int = 0, val borrowed: Int = 0, val activeProjects: Int = 0)
data class ActivityItem(val id: String, val type: String, val text: String, val at: String)
data class Member(
    val id: String, val user: User, val position: String = "", val department: String = "", val isActive: Boolean = true,
    val attendanceRate: Int = 0, val projectMemberships: List<MemberProjectMembership> = emptyList(),
    val borrowedRequests: List<BorrowRequest> = emptyList(), val attendanceRecords: List<AttendanceRecord> = emptyList()
)
data class MemberRequest(val name: String, val email: String, val password: String, val role: String, val position: String, val department: String)
data class MemberRef(val id: String, val user: User)
data class AttendanceRecord(
    val id: String, val memberId: String, val member: MemberRef, val event: ClubEvent? = null,
    val markedBy: MemberRef? = null, val date: String, val status: String
)
data class MemberAttendance(val records: List<AttendanceRecord>, val stats: AttendanceStats)
data class AttendanceStats(val total: Int = 0, val attended: Int = 0, val percentage: Int = 0, val streak: Int = 0)
data class MarkAttendanceRequest(val memberId: String, val eventId: String?, val date: String, val status: String)
data class InventoryItem(
    val id: String, val name: String, val description: String, val category: String,
    val totalQuantity: Int, val availableQuantity: Int, val imageUrl: String? = null
)
data class InventoryItemRequest(
    val name: String, val description: String, val category: String, val totalQuantity: Int,
    val availableQuantity: Int, val imageUrl: String = ""
)
data class BorrowRequest(
    val id: String, val item: InventoryItem, val borrower: MemberRef, val quantity: Int,
    val dueDate: String, val purpose: String, val notes: String? = null, val status: String
)
data class BorrowItemRequest(val itemId: String, val quantity: Int, val days: Int, val purpose: String, val notes: String)
data class ApproveBorrowRequest(val approved: Boolean)
data class Project(val id: String, val name: String, val description: String, val status: String, val startDate: String,
    val endDate: String? = null, val githubUrl: String? = null, val tags: List<String> = emptyList(),
    val progress: Int = 0, val members: List<ProjectMembership> = emptyList())
data class ProjectMembership(val id: String, val role: String, val member: MemberRef)
data class MemberProjectMembership(val id: String, val role: String, val project: Project)
data class ProjectRequest(val name: String, val description: String, val status: String, val startDate: String,
    val endDate: String?, val githubUrl: String?, val tags: List<String>)
data class ProjectMemberRequest(val memberId: String, val role: String)
data class ClubEvent(val id: String, val name: String, val description: String, val type: String, val venue: String,
    val startDate: String, val endDate: String, val status: String, val registrationLink: String? = null,
    val bannerUrl: String? = null, val maxParticipants: Int? = null, val teamFormation: String? = null,
    val projectSubmission: String? = null, val prizeDetails: String? = null,
    val participants: List<EventParticipant> = emptyList())
data class EventParticipant(val id: String, val member: MemberRef, val attended: Boolean = false)
data class EventRequest(val name: String, val description: String, val type: String, val venue: String,
    val startDate: String, val endDate: String, val status: String, val registrationLink: String,
    val bannerUrl: String, val maxParticipants: Int?, val teamFormation: String, val projectSubmission: String, val prizeDetails: String)
data class EventAttendanceRequest(val memberId: String, val attended: Boolean)
data class EmailSettings(val clubName: String = "ClubNexus", val collegeName: String = "Your College", val hodEmail: String = "")
data class EmailLog(val id: String, val subject: String, val body: String, val recipients: List<String>,
    val sentAt: String, val status: String, val category: String, val aiGenerated: Boolean)
data class GenerateEmailRequest(val category: String, val subject: String, val notes: String, val tone: String, val eventDetails: String = "")
data class GeneratedEmail(val body: String)
data class SendEmailRequest(val subject: String, val body: String, val recipients: List<String>, val category: String, val aiGenerated: Boolean)
data class SendEmailResponse(val message: String, val providerResult: ProviderResult? = null)
data class ProviderResult(val accepted: List<String> = emptyList())
