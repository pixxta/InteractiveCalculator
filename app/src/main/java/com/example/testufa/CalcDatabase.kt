package com.example.testufa

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.*

@Database(entities = [CalcData::class, DragBoxes::class, CalcField::class], version = 1)
abstract class CalcDatabase : RoomDatabase() {
    abstract fun calcDataDao(): CalcDataDao
    abstract fun dragBoxesDao(): DragBoxesDao
    abstract fun calcFieldDao(): CalcFieldDao

    companion object {
        @Volatile
        private var INSTANCE: CalcDatabase? = null


        fun getDatabase(context: Context): CalcDatabase {
            val tempInstance = INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }
            synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CalcDatabase::class.java,
                    "calc_database"
                )
                    .build()
                INSTANCE = instance
                return instance
            }
        }
    }
}




@Entity(tableName = "calc_data")
data class CalcData(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "string1")
    val s1: String,

    @ColumnInfo(name = "string2")
    val s2: String
)

@Dao
interface CalcDataDao {
    @Query("SELECT * FROM calc_data")
    fun getAllCalcData(): List<CalcData>

    @Insert
    fun insertCalcData(calcData: CalcData)

    @Query("DELETE FROM calc_data")
    fun deleteAllCalcData()
}

@Entity(tableName = "drag_boxes")
data class DragBoxes(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "id_box")
    val idbox: String,

    @ColumnInfo(name = "digit")
    val digit: String,

    @ColumnInfo(name = "comment")
    val comment: String,

    @ColumnInfo(name = "position_x")
    val positionX: String,

    @ColumnInfo(name = "position_y")
    val positionY: String

)


@Dao
interface DragBoxesDao {
    @Query("SELECT * FROM drag_boxes")
    fun getAllDragBoxes(): List<DragBoxes>

    @Insert
    fun insertDragBoxes(dragBoxes: DragBoxes)

    @Query("DELETE FROM drag_boxes")
    fun deleteAllDragBoxes()

    @Update
    fun updateDragBoxes(dragBoxes: DragBoxes)
}

@Entity(tableName = "calc_field")
data class CalcField(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "exp")
    val exp: String
)

@Dao
interface CalcFieldDao {
    @Query("SELECT * FROM calc_field")
    fun getAllCalcFields(): List<CalcField>

    @Insert
    fun insertCalcField(calcField: CalcField)

    @Query("DELETE FROM calc_field")
    fun deleteAllCalcFields()
}