package com.motovault.app.domain.model

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.random.Random

fun today() = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

fun recordId() = Clock.System.now().toEpochMilliseconds().toString() + "-" + Random.nextLong().toString()
