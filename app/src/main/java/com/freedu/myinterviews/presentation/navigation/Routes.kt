package com.freedu.myinterviews.presentation.navigation

object Routes {
    const val DASHBOARD = "dashboard"
    const val PIPELINE = "pipeline"
    const val CALENDAR = "calendar"
    const val CONTACTS = "contacts"
    const val SETTINGS = "settings"
    const val PREP = "prep"
    const val OFFERS = "offers"
    const val TODAY = "today"
    const val SMART_IMPORT = "smart_import"
    const val GMAIL = "gmail"
    const val ANALYTICS = "analytics"
    const val COACH = "coach/{appId}"
    const val COMPANY_DETAIL = "company/{companyId}"
    const val APPLICATION_DETAIL = "application/{appId}"

    fun company(companyId: Long) = "company/$companyId"
    fun application(appId: Long) = "application/$appId"
    fun coach(appId: Long) = "coach/$appId"
}
