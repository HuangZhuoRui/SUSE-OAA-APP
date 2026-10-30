package com.suseoaa.projectoaa.shared.domain

import com.suseoaa.projectoaa.shared.domain.model.org.DepartmentTypes
import com.suseoaa.projectoaa.shared.domain.model.org.Role
import com.suseoaa.projectoaa.shared.domain.model.person.CurrentUser
import com.suseoaa.projectoaa.shared.domain.model.person.EnrollmentGrade
import com.suseoaa.projectoaa.shared.domain.model.person.PersonData
import com.suseoaa.projectoaa.shared.domain.model.person.UserListItem
import com.suseoaa.projectoaa.shared.domain.model.recruitment.InterviewDecisionRules
import com.suseoaa.projectoaa.shared.domain.permission.OaaPermission
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GraduationRulesTest {
    @Test
    fun gradeAcceptsOnlyFourDigitYearsAbove2000() {
        listOf("", "2000", "1999", "02024", "10000", "2e03", "+202", "２０２４").forEach {
            assertNull(EnrollmentGrade.parse(it), it)
        }
        assertEquals(2001, EnrollmentGrade.parse("2001"))
        assertEquals(9999, EnrollmentGrade.parse("9999"))
        assertEquals(2024, EnrollmentGrade.parse(" 2024 "))
    }

    @Test
    fun graduateDeveloperKeepsDevelopmentRightsButCannotManageRecruitment() {
        val developer = CurrentUser(PersonData(role = "开发者", isGraduated = true), 100, 1, 1)
        assertTrue(OaaPermission.canManageOrganization(developer))
        assertTrue(OaaPermission.canAccessUserManagement(developer))
        assertTrue(OaaPermission.canEditGrades(developer))
        assertFalse(OaaPermission.canManageTerms(developer))
        assertFalse(OaaPermission.canReviewApplications(developer))
        assertTrue(OaaPermission.canManageTerms(developer.copy(person = developer.person.copy(isGraduated = false))))
        assertFalse(OaaPermission.canEditGrades(developer.copy(level = 79)))
    }

    @Test
    fun graduateCannotBePromotedByChangingTheirGradeInTheSameRequest() {
        val graduate = UserListItem(role = "会员", grade = 2024, isGraduated = true)
        assertTrue(OaaPermission.canAssignRole(graduate, Role(name = "会员", level = 10)))
        assertFalse(OaaPermission.canAssignRole(graduate, Role(name = "部长", level = 50)))
        assertFalse(OaaPermission.canAssignRole(graduate, Role(name = "开发者", level = 100)))
        assertTrue(OaaPermission.canAssignRole(graduate.copy(isGraduated = false), Role(name = "部长", level = 50)))
        assertTrue(OaaPermission.canAssignRole(graduate.copy(role = "开发者"), Role(name = "开发者", level = 100)))
    }

    @Test
    fun graduateMayOnlyReceiveRejectionAndNotPendingOrAdmission() {
        for (decision in listOf("淘汰", "未通过", "不录取", "未录取")) {
            assertTrue(InterviewDecisionRules.isAllowed(true, decision), decision)
            assertTrue(InterviewDecisionRules.isNonAdmission(decision))
        }
        for (decision in listOf("", "待定", "录取第一志愿", "录取第二志愿", "已调剂")) {
            assertFalse(InterviewDecisionRules.isAllowed(true, decision), decision)
        }
        assertTrue(InterviewDecisionRules.isAllowed(false, "录取第一志愿"))
        assertTrue(InterviewDecisionRules.isAllowed(null, "录取第一志愿"))
        assertEquals(listOf("部门", "协会", "理事会"), DepartmentTypes.all)
    }
}
