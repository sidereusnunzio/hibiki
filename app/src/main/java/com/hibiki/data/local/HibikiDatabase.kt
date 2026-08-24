package com.hibiki.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.hibiki.data.local.dao.AudioPrototypeDao
import com.hibiki.data.local.dao.AudioSampleDao
import com.hibiki.data.local.dao.ContextDao
import com.hibiki.data.local.dao.PhraseDao
import com.hibiki.data.local.dao.SubjectDao
import com.hibiki.data.local.entity.AudioPrototypeEntity
import com.hibiki.data.local.entity.AudioSampleEntity
import com.hibiki.data.local.entity.ContextEntity
import com.hibiki.data.local.entity.PhraseEntity
import com.hibiki.data.local.entity.SubjectEntity
import com.hibiki.domain.model.BuiltInIds
import com.hibiki.domain.model.BuiltInUmamusume
import com.hibiki.domain.model.DefaultPrompts

@Database(
    entities = [ContextEntity::class, SubjectEntity::class, AudioSampleEntity::class, PhraseEntity::class, AudioPrototypeEntity::class],
    version = DatabaseConstants.SCHEMA_VERSION,
    exportSchema = true,
)
abstract class HibikiDatabase : RoomDatabase() {
    abstract fun contextDao(): ContextDao
    abstract fun subjectDao(): SubjectDao
    abstract fun audioSampleDao(): AudioSampleDao
    abstract fun audioPrototypeDao(): AudioPrototypeDao
    abstract fun phraseDao(): PhraseDao

    companion object {
        fun create(context: Context): HibikiDatabase {
            return Room.databaseBuilder(context, HibikiDatabase::class.java, DatabaseConstants.DATABASE_NAME)
                .addMigrations(
                    MIGRATION_1_2,
                    MIGRATION_2_3,
                    MIGRATION_3_4,
                    MIGRATION_4_5,
                    MIGRATION_5_6,
                    MIGRATION_6_7,
                    MIGRATION_7_8,
                    MIGRATION_8_9,
                    MIGRATION_9_10,
                    MIGRATION_10_11,
                )
                .addCallback(SeedCallback())
                .build()
        }
    }
}

private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE subjects ADD COLUMN prompt TEXT NOT NULL DEFAULT ''")
    }
}

private val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "UPDATE contexts SET prompt = ? WHERE id = ?",
            arrayOf(DefaultPrompts.GENERAL, BuiltInIds.GENERAL),
        )
        db.execSQL(
            "UPDATE contexts SET prompt = ? WHERE id = ?",
            arrayOf(DefaultPrompts.UMAMUSUME, BuiltInIds.UMAMUSUME),
        )
    }
}

private val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "UPDATE contexts SET prompt = ? WHERE id = ?",
            arrayOf(DefaultPrompts.UMAMUSUME, BuiltInIds.UMAMUSUME),
        )
        db.upsertUmamusumeSubjects()
    }
}

private val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS phrases")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `audio_samples` (
                `id` TEXT NOT NULL,
                `audioPath` TEXT,
                `audioFingerprint` BLOB,
                `durationMs` INTEGER NOT NULL,
                `japaneseRaw` TEXT NOT NULL,
                `confidence` REAL,
                `transcriptionModel` TEXT NOT NULL,
                `transcriptionPromptVersion` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `phrases` (
                `id` TEXT NOT NULL,
                `audioSampleId` TEXT NOT NULL,
                `contextId` TEXT NOT NULL,
                `subjectId` TEXT,
                `japaneseCorrected` TEXT,
                `kana` TEXT NOT NULL,
                `romaji` TEXT NOT NULL,
                `literalTranslation` TEXT NOT NULL,
                `naturalTranslation` TEXT NOT NULL,
                `verified` INTEGER NOT NULL,
                `source` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `analysisModel` TEXT NOT NULL,
                `analysisPromptVersion` INTEGER NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`audioSampleId`) REFERENCES `audio_samples`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT,
                FOREIGN KEY(`contextId`) REFERENCES `contexts`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT,
                FOREIGN KEY(`subjectId`) REFERENCES `subjects`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_phrases_audioSampleId` ON `phrases` (`audioSampleId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_phrases_contextId` ON `phrases` (`contextId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_phrases_subjectId` ON `phrases` (`subjectId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_phrases_createdAt` ON `phrases` (`createdAt`)")
        db.execSQL(
            "UPDATE contexts SET prompt = ? WHERE id = ?",
            arrayOf(DefaultPrompts.UMAMUSUME, BuiltInIds.UMAMUSUME),
        )
        db.upsertUmamusumeSubjects()
    }
}

private val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "DELETE FROM subjects WHERE contextId = ?",
            arrayOf(BuiltInIds.UMAMUSUME),
        )
        db.upsertUmamusumeSubjects()
    }
}

private val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE contexts ADD COLUMN imagePath TEXT")
        db.execSQL("ALTER TABLE subjects ADD COLUMN imagePath TEXT")
    }
}

private val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE phrases ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
        db.execSQL("UPDATE phrases SET updatedAt = createdAt WHERE updatedAt = 0")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_phrases_updatedAt` ON `phrases` (`updatedAt`)")
    }
}

private val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE phrases ADD COLUMN arashiSyncState TEXT NOT NULL DEFAULT 'DO_NOT_SYNC'",
        )
    }
}

private val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE subjects ADD COLUMN overlayEnabled INTEGER NOT NULL DEFAULT 1",
        )
    }
}

private val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `audio_prototypes` (
                `id` TEXT NOT NULL,
                `phraseId` TEXT NOT NULL,
                `audioFingerprint` BLOB,
                `durationMs` INTEGER NOT NULL,
                `pcmPreview` BLOB,
                `createdAt` INTEGER NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`phraseId`) REFERENCES `phrases`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_audio_prototypes_phraseId` ON `audio_prototypes` (`phraseId`)")
        db.execSQL(
            """
            INSERT INTO audio_prototypes (id, phraseId, audioFingerprint, durationMs, pcmPreview, createdAt)
            SELECT p.id || ':p0', p.id, s.audioFingerprint, s.durationMs, NULL, p.createdAt
            FROM phrases p
            INNER JOIN audio_samples s ON s.id = p.audioSampleId
            WHERE s.audioFingerprint IS NOT NULL
            """.trimIndent(),
        )
    }
}

private class SeedCallback : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        db.execSQL(
            """
            INSERT INTO contexts (id, name, prompt, expectedLanguage, hasSubjects, isBuiltIn, sortOrder)
            VALUES
            ('${BuiltInIds.GENERAL}', 'Generale', ?, 'ja', 0, 1, 0),
            ('${BuiltInIds.UMAMUSUME}', 'Umamusume', ?, 'ja', 1, 1, 1)
            """.trimIndent(),
            arrayOf(DefaultPrompts.GENERAL, DefaultPrompts.UMAMUSUME),
        )
        db.upsertUmamusumeSubjects()
    }
}

private fun SupportSQLiteDatabase.upsertUmamusumeSubjects() {
    BuiltInUmamusume.CHARACTERS.forEach { character ->
        execSQL(
            """
            INSERT INTO subjects (id, contextId, displayName, japaneseName, prompt)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT(id) DO UPDATE SET
                contextId = excluded.contextId,
                displayName = excluded.displayName,
                japaneseName = excluded.japaneseName,
                prompt = excluded.prompt
            """.trimIndent(),
            arrayOf(
                character.id,
                BuiltInIds.UMAMUSUME,
                character.displayName,
                character.japaneseName,
                character.prompt,
            ),
        )
    }
}
