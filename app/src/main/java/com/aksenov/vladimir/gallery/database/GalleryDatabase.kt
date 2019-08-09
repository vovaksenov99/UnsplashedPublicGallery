package com.aksenov.vladimir.gallery.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.aksenov.vladimir.gallery.database.tables.Favourites
import org.jetbrains.anko.db.AUTOINCREMENT
import org.jetbrains.anko.db.INTEGER
import org.jetbrains.anko.db.ManagedSQLiteOpenHelper
import org.jetbrains.anko.db.PRIMARY_KEY
import org.jetbrains.anko.db.TEXT
import org.jetbrains.anko.db.createTable
import org.jetbrains.anko.db.dropTable

/**
 * Start database initializations
 */

const val DATABASE_NAME = "GalleryDatabase.db"
const val DATABASE_VERSION = 3

/**
 * @param context parent activity context
 */
class GalleryDatabase(context: Context) :
        ManagedSQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private var instance: GalleryDatabase? = null

        /**
         * Get current database exemplar
         */
        @Synchronized
        fun getInstance(context: Context): GalleryDatabase {
            if (instance == null) {
                instance = GalleryDatabase(context.applicationContext)
            }
            return instance!!
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.createTable(Favourites.NAME, true,
                Favourites.FIELDS._ID to INTEGER + PRIMARY_KEY + AUTOINCREMENT,
                Favourites.FIELDS.ID to INTEGER,
                Favourites.FIELDS.SERIALIZED_IMAGE to TEXT)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.dropTable(Favourites.NAME, true)
        onCreate(db)
    }
}
