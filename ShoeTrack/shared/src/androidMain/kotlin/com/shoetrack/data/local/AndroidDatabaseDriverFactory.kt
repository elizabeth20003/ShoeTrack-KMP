package com.shoetrack.data.local

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.shoetrack.db.ShoeTrackDatabase

class AndroidDatabaseDriverFactory(private val context: Context) : DatabaseDriverFactory {

    override fun createDriver(): SqlDriver =
        AndroidSqliteDriver(ShoeTrackDatabase.Schema, context, DATABASE_NAME)
}
