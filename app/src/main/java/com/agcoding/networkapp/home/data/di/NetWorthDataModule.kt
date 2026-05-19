package com.agcoding.networkapp.home.data.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.agcoding.networkapp.home.data.local.NetWorthDao
import com.agcoding.networkapp.home.data.local.NetWorthDatabase
import com.agcoding.networkapp.home.data.repository.NetWorthRepositoryImpl
import com.agcoding.networkapp.home.domain.repository.NetWorthRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// Top-level so Hilt/KSP doesn't try to process the anonymous Migration object
private val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE net_worth_entries ADD COLUMN note TEXT NOT NULL DEFAULT ''")
    }
}

private val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS `accounts` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `startingBalance` REAL NOT NULL DEFAULT 0,
                `colorHex` TEXT NOT NULL DEFAULT '#76C893'
            )"""
        )
        db.execSQL("INSERT INTO `accounts` (`id`, `name`, `startingBalance`, `colorHex`) VALUES (1, 'Main', 0, '#76C893')")
        db.execSQL("ALTER TABLE `net_worth_entries` ADD COLUMN `accountId` INTEGER NOT NULL DEFAULT 1")
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class NetWorthDataModule {

    @Binds
    @Singleton
    abstract fun bindNetWorthRepository(impl: NetWorthRepositoryImpl): NetWorthRepository

    companion object {

        @Provides
        @Singleton
        fun provideNetWorthDatabase(@ApplicationContext context: Context): NetWorthDatabase =
            Room.databaseBuilder(context, NetWorthDatabase::class.java, "net_worth_db")
                .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
                .fallbackToDestructiveMigration()
                .build()

        @Provides
        fun provideNetWorthDao(db: NetWorthDatabase): NetWorthDao = db.netWorthDao()
    }
}
