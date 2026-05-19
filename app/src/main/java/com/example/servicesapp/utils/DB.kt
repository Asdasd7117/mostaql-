package com.example.servicesapp.utils

object DB {

    const val USERS = "users"
    const val SERVICES = "services"
    const val MESSAGES = "messages"
    const val PROJECTS = "projects"  // ✅ أضف هذا السطر

    // ✅ ثوابت جدول users
    const val USER_ID = "id"
    const val USER_EMAIL = "email"
    const val USER_ROLE = "role"

    // ✅ ثوابت جدول services
    const val SERVICE_ID = "id"
    const val SERVICE_TITLE = "title"
    const val SERVICE_DESC = "description"
    const val SERVICE_PRICE = "price"
    const val SERVICE_USER = "user_id"

    // ✅ ثوابت جدول messages
    const val MSG_ID = "id"
    const val MSG_SENDER = "sender_id"
    const val MSG_RECEIVER = "receiver_id"
    const val MSG_TEXT = "text"

    // ✅ ثوابت جدول projects (جديد)
    const val PROJECT_ID = "id"
    const val PROJECT_NAME = "name"
    const val PROJECT_DESC = "description"
    const val PROJECT_LANGUAGE = "language"
    const val PROJECT_PREVIEW = "preview_link"
    const val PROJECT_GITHUB = "github_link"
    const val PROJECT_USER = "user_id"
    const val PROJECT_CREATED = "created_at"
    const val REVIEWS = "reviews"
    const val COMMENTS = "comments"
}