package com.shoetrack.data.local

import app.cash.sqldelight.db.SqlDriver

const val DATABASE_NAME = "shoetrack.db"

/**
 * Creates the SQLite driver for each platform. It is an interface rather than
 * expect/actual because platforms need different inputs (Android needs a Context),
 * so each app injects its own implementation.
 */
interface DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}
