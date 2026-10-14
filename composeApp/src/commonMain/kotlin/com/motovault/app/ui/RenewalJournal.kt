package com.motovault.app.ui

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

/** Reservations are persisted before submission. An interrupted elapsed submission is never replayed. */
@Serializable
internal data class RenewalJournal(
    val requests: List<RenewalReminder> = emptyList(),
    val consumed: Set<String> = emptySet(),
) {
    fun recover(now: LocalDateTime, pending: Set<String>, delivered: Set<String>) = copy(
        consumed = consumed + delivered + requests.filter { it.localDateTime() <= now && it.key !in pending }.map { it.key },
    )

    fun reserve(request: RenewalReminder) = copy(requests = requests.filterNot { it.key == request.key } + request)
    fun rejected(key: String) = copy(requests = requests.filterNot { it.key == key })
    fun retain(keys: Set<String>) = copy(requests = requests.filter { it.key in keys })
}
