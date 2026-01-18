package com.suseoaa.projectoaa.core.util

import org.jsoup.Jsoup

object HtmlParser {
    /**
     * 通用通知解析器
     */
    fun htmlParse(html: String): List<String> {
        val doc = Jsoup.parse(html)

        // 策略 1：获取课程更新信息
        val strategy1 = doc.select("div#kbDiv a.list-group-item span.title")
            .map { it.text().trim() }
        if (strategy1.isNotEmpty()) return strategy1

        // 策略 2：获取调课信息
        val strategy2 = doc.select("div#home a.list-group-item")
            .map { elements ->
                val time = elements.select("span.fraction").text().trim()
                val info = elements.attr("data-tkxx").trim()
                if (time.isNotEmpty()) "$time\n$info" else info
            }
        if (strategy2.isNotEmpty()) return strategy2

        // 策略 3: 获取考试信息
        val strategy3 = doc.select("div#exam a.list-group-item").mapNotNull { element ->
            val title = element.select("span.title").text().trim()
            if (title.isEmpty()) return@mapNotNull null

            // 步骤 A: 优先尝试查找 span.fraction
            var details = element.select("span.fraction").text().trim()

            // 步骤 B: 如果没有，尝试 p.list-group-item-text
            if (details.isEmpty()) {
                details = element.select("p.list-group-item-text").text().trim()
            }

            // 步骤 C: 最后的兜底，纯文本替换
            if (details.isEmpty()) {
                val fullText = element.text().trim()
                // 移除标题，移除可能存在的 "考试安排" 等多余标签文本
                details = fullText.replace(title, "")
                    .replace("考试安排", "")
                    .trim()
            }

            // 返回 "课程名###详情" 的格式
            "$title###$details"
        }

        if (strategy3.isNotEmpty()) return strategy3

        return emptyList()
    }

    // 数据类用于返回解析结果
    data class GradeDetail(
        val regular: String = "",
        val final: String = ""
    )

    // 解析成绩详情 HTML
    fun parseGradeDetail(html: String): GradeDetail {
        val doc = Jsoup.parse(html)
        // 成绩通常在 id 为 subtab 的表格中
        val rows = doc.select("table#subtab tbody tr")

        var regular = ""
        var finalScore = ""

        for (row in rows) {
            val cols = row.select("td")
            if (cols.size >= 3) {
                // 第一列是标题（如：【 平时 】），去除特殊符号
                val title = cols[0].text().replace("【", "").replace("】", "").trim()
                // 第三列是分数
                val score = cols[2].text().trim()

                when {
                    title.contains("平时") -> regular = score
                    // 期末、补考、或者总评外的其他主要成绩都视为期末部分
                    title.contains("期末") || title.contains("补考") -> finalScore = score
                }
            }
        }
        return GradeDetail(regular, finalScore)
    }
}