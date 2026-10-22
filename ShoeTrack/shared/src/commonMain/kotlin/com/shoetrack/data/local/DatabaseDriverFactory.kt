package com.shoetrack.data.local

import app.cash.sqldelight.db.SqlDriver

const val DATABASE_NAME = "shoetrack.db"

/**
 * Crea el driver SQLite de cada plataforma. Es una interfaz (y no un expect/actual)
 * porque cada plataforma necesita datos distintos para construirlo (en Android, el
 * Context) y así se inyecta desde el punto de entrada de cada app.
 */
interface DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}
