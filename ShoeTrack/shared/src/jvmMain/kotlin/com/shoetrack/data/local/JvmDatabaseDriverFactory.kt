package com.shoetrack.data.local

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.shoetrack.db.ShoeTrackDatabase
import java.io.File

class JvmDatabaseDriverFactory(
    private val directory: File = File(System.getProperty("user.home"), ".shoetrack"),
) : DatabaseDriverFactory {

    override fun createDriver(): SqlDriver {
        directory.mkdirs()
        val file = File(directory, DATABASE_NAME)
        val isNew = !file.exists()
        val driver = JdbcSqliteDriver("jdbc:sqlite:${file.absolutePath}")
        if (isNew) {
            ShoeTrackDatabase.Schema.create(driver)
        }
        return driver
    }
}
