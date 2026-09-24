package com.suseoaa.projectoaa.shared.domain.model.recruitment

import kotlinx.datetime.LocalDate

/** 某一天相对于一个周期所处的阶段。 */
enum class TermPhase {
    /** 还没到填写期 */
    NotStarted,
    /** 填写期内：可以提交、修改申请 */
    Editing,
    /** 填写期结束、公示期未开始：面试进行中 */
    Reviewing,
    /** 公示期内：可以查看录取结果 */
    Querying,
    /** 公示期已结束 */
    Ended
}

/** 解析 yyyy-MM-dd（也兼容带时间的 ISO 串，只取日期部分）。 */
fun parsePeriodDate(raw: String): LocalDate? =
    runCatching { LocalDate.parse(raw.trim().take(10)) }.getOrNull()

/** 两端都包含；任一端解析失败都视为不在窗口内。 */
fun Period.contains(date: LocalDate): Boolean {
    val start = parsePeriodDate(startAt) ?: return false
    val end = parsePeriodDate(endAt) ?: return false
    return date in start..end
}

fun Term.phaseOn(date: LocalDate): TermPhase {
    if (editPeriod.contains(date)) return TermPhase.Editing
    if (queryPeriod.contains(date)) return TermPhase.Querying
    val editStart = parsePeriodDate(editPeriod.startAt)
    val queryEnd = parsePeriodDate(queryPeriod.endAt)
    return when {
        editStart != null && date < editStart -> TermPhase.NotStarted
        queryEnd != null && date > queryEnd -> TermPhase.Ended
        else -> TermPhase.Reviewing
    }
}

/**
 * 选出申请页默认展示的周期：优先正在填写的，其次正在公示或面试中的，
 * 再次是即将开始的；都没有时取最近的一个。
 */
fun List<Term>.pickCurrent(date: LocalDate): Term? {
    val byPriority = listOf(
        TermPhase.Editing,
        TermPhase.Querying,
        TermPhase.Reviewing,
        TermPhase.NotStarted
    )
    for (phase in byPriority) {
        firstOrNull { it.phaseOn(date) == phase }?.let { return it }
    }
    return maxByOrNull { parsePeriodDate(it.editPeriod.startAt) ?: LocalDate(1970, 1, 1) }
}
