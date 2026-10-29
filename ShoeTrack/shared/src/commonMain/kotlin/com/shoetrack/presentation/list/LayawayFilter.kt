package com.shoetrack.presentation.list

import com.shoetrack.domain.model.LayawayState
import com.shoetrack.domain.model.LayawaySummary

/** Quick filters above the list. Cancelled layaways only show under [All]. */
enum class LayawayFilter {
    All,
    Active,
    Overdue,
    Paid,
    ;

    fun accepts(layaway: LayawaySummary): Boolean = when (this) {
        All -> true
        Active -> layaway.state == LayawayState.Active
        Overdue -> layaway.state == LayawayState.Overdue
        Paid -> layaway.state == LayawayState.Completed
    }
}
