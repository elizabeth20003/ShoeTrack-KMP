package com.shoetrack.data.local

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.shoetrack.db.ShoeTrackDatabase
import java.io.File

/** Desktop driver; stores the database at `~/.shoetrack/shoetrack.db`. */
class JvmDatabaseDriverFactory(
    private val directorio: File = File(System.getProperty("user.home"), ".shoetrack"),
) : DatabaseDriverFactory {

    override fun createDriver(): SqlDriver {
        directorio.mkdirs()
        val archivo = File(directorio, DATABASE_NAME)
        val esNueva = !archivo.exists()
        val driver = JdbcSqliteDriver("jdbc:sqlite:${archivo.absolutePath}")
        if (esNueva) {
            ShoeTrackDatabase.Schema.create(driver)
        }
        return driver
    }
}
