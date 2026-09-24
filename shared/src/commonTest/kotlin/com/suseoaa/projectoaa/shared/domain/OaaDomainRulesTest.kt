package com.suseoaa.projectoaa.shared.domain

import com.suseoaa.projectoaa.shared.domain.model.person.CurrentUser
import com.suseoaa.projectoaa.shared.domain.model.person.PersonData
import com.suseoaa.projectoaa.shared.domain.model.recruitment.Period
import com.suseoaa.projectoaa.shared.domain.model.recruitment.Term
import com.suseoaa.projectoaa.shared.domain.model.recruitment.TermPhase
import com.suseoaa.projectoaa.shared.domain.model.recruitment.phaseOn
import com.suseoaa.projectoaa.shared.domain.model.recruitment.pickCurrent
import com.suseoaa.projectoaa.shared.domain.permission.OaaPermission
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OaaDomainRulesTest {

    private val autumn = Term(
        termId = 1,
        title = "2026 秋季招新",
        editPeriod = Period("2026-09-01", "2026-09-15"),
        queryPeriod = Period("2026-09-20", "2026-09-30")
    )

    @Test
    fun termPhaseFollowsPeriodsWithInclusiveEnds() {
        assertEquals(TermPhase.NotStarted, autumn.phaseOn(LocalDate(2026, 8, 31)))
        assertEquals(TermPhase.Editing, autumn.phaseOn(LocalDate(2026, 9, 1)))
        assertEquals(TermPhase.Editing, autumn.phaseOn(LocalDate(2026, 9, 15)))
        assertEquals(TermPhase.Reviewing, autumn.phaseOn(LocalDate(2026, 9, 18)))
        assertEquals(TermPhase.Querying, autumn.phaseOn(LocalDate(2026, 9, 30)))
        assertEquals(TermPhase.Ended, autumn.phaseOn(LocalDate(2026, 10, 1)))
    }

    @Test
    fun pickCurrentPrefersTheTermOpenForEditing() {
        val election = Term(
            termId = 2,
            editPeriod = Period("2026-09-10", "2026-09-25"),
            queryPeriod = Period("2026-10-01", "2026-10-05")
        )
        val terms = listOf(autumn, election)

        assertEquals(2, terms.pickCurrent(LocalDate(2026, 9, 22))?.resolvedId)
        assertEquals(1, terms.pickCurrent(LocalDate(2026, 9, 5))?.resolvedId)
    }

    @Test
    fun permissionsFollowLevelThresholds() {
        fun user(level: Int, department: String = "算法竞赛部") =
            CurrentUser(PersonData(department = department), level, null, null)

        assertFalse(OaaPermission.canManageAnnouncements(user(10), "算法竞赛部"))
        assertTrue(OaaPermission.canManageAnnouncements(user(30), "算法竞赛部"))
        assertFalse(OaaPermission.canManageAnnouncements(user(70), "秘书处"))
        assertTrue(OaaPermission.canManageAnnouncements(user(80), "秘书处"))
        assertTrue(OaaPermission.canEditUser(user(70), targetLevel = 40))
        assertFalse(OaaPermission.canEditUser(user(70), targetLevel = 70))
        assertFalse(OaaPermission.canManageTerms(user(79)))
        assertTrue(OaaPermission.canManageTerms(user(80)))
    }
}
