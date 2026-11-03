package com.clubnexus.android.data.network

import com.clubnexus.android.data.model.*
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ClubNexusApi {
    @POST("auth/login") suspend fun login(@Body request: LoginRequest): AuthResponse
    @POST("auth/register") suspend fun register(@Body request: RegisterRequest): AuthResponse
    @GET("auth/me") suspend fun me(): User
    @GET("dashboard") suspend fun dashboard(): DashboardData
    @GET("members") suspend fun members(@Query("q") query: String? = null): List<Member>
    @GET("members/{id}") suspend fun member(@Path("id") id: String): Member
    @POST("members") suspend fun addMember(@Body request: MemberRequest): Member
    @PUT("members/{id}") suspend fun updateMember(@Path("id") id: String, @Body request: Map<String, @JvmSuppressWildcards Any?>): Member
    @DELETE("members/{id}") suspend fun deactivateMember(@Path("id") id: String): Response<Unit>
    @GET("attendance") suspend fun attendance(): List<AttendanceRecord>
    @GET("attendance/member/{memberId}") suspend fun memberAttendance(@Path("memberId") memberId: String): MemberAttendance
    @POST("attendance/mark") suspend fun markAttendance(@Body request: MarkAttendanceRequest): AttendanceRecord
    @GET("attendance/export.csv") suspend fun exportAttendance(): ResponseBody
    @GET("inventory") suspend fun inventory(@Query("q") query: String? = null): List<InventoryItem>
    @POST("inventory") suspend fun addInventoryItem(@Body request: InventoryItemRequest): InventoryItem
    @GET("inventory/borrow-requests") suspend fun borrowRequests(): List<BorrowRequest>
    @POST("inventory/borrow") suspend fun borrow(@Body request: BorrowItemRequest): BorrowRequest
    @PUT("inventory/borrow/{id}/approve") suspend fun approveBorrow(@Path("id") id: String, @Body request: ApproveBorrowRequest): BorrowRequest
    @PUT("inventory/borrow/{id}/return") suspend fun returnBorrow(@Path("id") id: String): BorrowRequest
    @GET("projects") suspend fun projects(): List<Project>
    @POST("projects") suspend fun createProject(@Body request: ProjectRequest): Project
    @PUT("projects/{id}") suspend fun updateProject(@Path("id") id: String, @Body request: ProjectRequest): Project
    @POST("projects/{id}/members") suspend fun addProjectMember(@Path("id") id: String, @Body request: ProjectMemberRequest): ProjectMembership
    @DELETE("projects/{id}/members/{memberId}") suspend fun removeProjectMember(@Path("id") id: String, @Path("memberId") memberId: String): Response<Unit>
    @GET("events") suspend fun events(): List<ClubEvent>
    @POST("events") suspend fun createEvent(@Body request: EventRequest): ClubEvent
    @PUT("events/{id}") suspend fun updateEvent(@Path("id") id: String, @Body request: EventRequest): ClubEvent
    @DELETE("events/{id}") suspend fun deleteEvent(@Path("id") id: String): Response<Unit>
    @POST("events/{id}/register") suspend fun registerForEvent(@Path("id") id: String): EventParticipant
    @POST("events/{id}/attendance") suspend fun markEventParticipant(@Path("id") id: String, @Body request: EventAttendanceRequest): EventParticipant
    @GET("email/settings") suspend fun emailSettings(): EmailSettings
    @GET("email/logs") suspend fun emailLogs(): List<EmailLog>
    @POST("email/generate") suspend fun generateEmail(@Body request: GenerateEmailRequest): GeneratedEmail
    @POST("email/send") suspend fun sendEmail(@Body request: SendEmailRequest): SendEmailResponse
}
