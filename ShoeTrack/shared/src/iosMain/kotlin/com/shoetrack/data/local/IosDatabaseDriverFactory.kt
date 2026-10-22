package com.shoetrack.data.local

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.shoetrack.db.ShoeTrackDatabase

class IosDatabaseDriverFactory : DatabaseDriverFactory {

    override fun createDriver(): SqlDriver =
        NativeSqliteDriver(ShoeTrackDatabase.Schema, DATABASE_NAME)
}
