                val boundaries = cluster.flatMap { listOf(it.startNodeIndex, it.endNodeIndex + 1) }.distinct().sorted()

                var currentSegmentStart = -1
                var currentSegmentEnd = -1
                var currentSegmentItems = emptyList<ScheduleLayoutItem>()

                fun emitSegment() {
                    if (currentSegmentItems.isEmpty()) return
                    
                    val uniqueAccountsCount = currentSegmentItems.map { it.course.course.studentId }.distinct().size
                    val status = when {
                        uniqueAccountsCount >= activeQueryCount -> CourseOverlapStatus.OVERLAP
                        uniqueAccountsCount == 1 -> CourseOverlapStatus.NO_OVERLAP
                        else -> CourseOverlapStatus.PARTIAL_OVERLAP
                    }

                    val representativeItem = currentSegmentItems.minByOrNull { it.startNodeIndex } ?: currentSegmentItems.first()
                    val unifiedItem = representativeItem.copy(
                        startNodeIndex = currentSegmentStart,
                        endNodeIndex = currentSegmentEnd
                    )
                    
                    val baseColor = overlapFilterColor(
                        when (status) {
                            CourseOverlapStatus.NO_OVERLAP -> OverlapDisplayFilter.NO_OVERLAP
                            CourseOverlapStatus.OVERLAP -> OverlapDisplayFilter.OVERLAP
                            CourseOverlapStatus.PARTIAL_OVERLAP -> OverlapDisplayFilter.PARTIAL_OVERLAP
                        }
                    )
                    
                    result.add(
                        PreparedCardItem(
                            layoutItem = unifiedItem,
                            laneIndex = 0,
                            laneCount = 1,
                            conflictGroup = currentSegmentItems,
                            color = baseColor,
                            overlapStatus = status,
                            customTitle = overlapFilterLabel(
                                when (status) {
                                    CourseOverlapStatus.NO_OVERLAP -> OverlapDisplayFilter.NO_OVERLAP
                                    CourseOverlapStatus.OVERLAP -> OverlapDisplayFilter.OVERLAP
                                    CourseOverlapStatus.PARTIAL_OVERLAP -> OverlapDisplayFilter.PARTIAL_OVERLAP
                                }
                            )
                        )
                    )
                }

                for (i in 0 until boundaries.size - 1) {
                    val segStart = boundaries[i]
                    val segEnd = boundaries[i + 1] - 1
                    if (segStart > segEnd) continue

                    val segItems = cluster.filter { it.startNodeIndex <= segStart && it.endNodeIndex >= segEnd }
                    if (segItems.isEmpty()) continue

                    // Match item sets by a naive unique identifier (e.g. object identity is fine since they are same list elements, or compare their IDs if needed. Let's just use toSet())
                    if (currentSegmentItems.toSet() == segItems.toSet() && currentSegmentEnd + 1 == segStart) {
                        currentSegmentEnd = segEnd
                    } else {
                        emitSegment()
                        currentSegmentStart = segStart
                        currentSegmentEnd = segEnd
                        currentSegmentItems = segItems
                    }
                }
                emitSegment()

