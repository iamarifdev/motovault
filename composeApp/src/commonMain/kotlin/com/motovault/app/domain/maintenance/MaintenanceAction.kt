package com.motovault.app.domain.maintenance

import kotlinx.serialization.Serializable

/**
 * A maintenance action is deliberately separate from the part it applies to.
 * A visit can inspect a chain, adjust it and replace a sprocket without claiming
 * that every action reset the same reminder.
 */
@Serializable
enum class MaintenanceAction {
    INSPECT,
    CLEAN,
    LUBRICATE,
    ADJUST,
    REPLACE;

    companion object {
        fun fromCode(code: String): MaintenanceAction? = entries.firstOrNull { it.name.equals(code, ignoreCase = true) }
    }
}

@Serializable
enum class SchedulePhase {
    RUNNING_IN,
    RECURRING
}

@Serializable
enum class ScheduleSource {
    MANUAL,
    OWNER,
    DEALER
}

/**
 * Provenance travels with the persisted schedule. Empty manual fields mean the
 * schedule is owner-defined, never that it is silently manual-backed.
 */
@Serializable
data class ScheduleProvenance(
    val source: ScheduleSource = ScheduleSource.OWNER,
    val label: String = "",
    val url: String = "",
    val revision: String = "",
    val page: String = "",
    val applicability: String = ""
) {
    val isManualBacked: Boolean get() = source == ScheduleSource.MANUAL && label.isNotBlank()
    val displayReference: String
        get() = listOfNotNull(
            label.takeIf { it.isNotBlank() },
            revision.takeIf { it.isNotBlank() }?.let { "rev $it" },
            page.takeIf { it.isNotBlank() }?.let { "p. $it" }
        ).joinToString(" · ")
}
