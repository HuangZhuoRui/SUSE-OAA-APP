package com.suseoaa.projectoaa.feature.course

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable

const val COURSE_ROUTE = "course_route"

//定义去课程页的NavController
fun NavController.navigateToCourse(navOptions: NavOptions? = null) {
    this.navigate(COURSE_ROUTE, navOptions)
}

fun NavGraphBuilder.courseScreen() {
    composable(route = COURSE_ROUTE) {
        CourseScreen()
    }
}