package umg.edu.gt.controlgastos.datos

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import umg.edu.gt.controlgastos.modelo.GastoEntity

class GastoDbHelper(context: Context) : SQLiteOpenHelper(context, "gastos_pro.db", null, 2) {

    override fun onCreate(db: SQLiteDatabase) {
        val createTableQuery = """
            CREATE TABLE gastos_tabla (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                titulo TEXT,
                monto REAL,
                categoria TEXT,
                tipo TEXT,
                fechaMillis INTEGER
            )
        """
        db.execSQL(createTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS gastos_tabla")
        onCreate(db)
    }

    // Insertar un gasto o ingreso en la base de datos SQLite
    fun insertarGasto(gasto: GastoEntity): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("titulo", gasto.titulo)
            put("monto", gasto.monto)
            put("categoria", gasto.categoria)
            put("tipo", gasto.tipo)
            put("fechaMillis", gasto.fechaMillis)
        }
        return db.insert("gastos_tabla", null, values)
    }

    // Obtener todos los movimientos ordenados del más reciente al más antiguo
    fun obtenerGastos(): List<GastoEntity> {
        val lista = mutableListOf<GastoEntity>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM gastos_tabla ORDER BY id DESC", null)

        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow("id"))
                val titulo = cursor.getString(cursor.getColumnIndexOrThrow("titulo"))
                val monto = cursor.getDouble(cursor.getColumnIndexOrThrow("monto"))
                val categoria = cursor.getString(cursor.getColumnIndexOrThrow("categoria"))
                val tipoIndex = cursor.getColumnIndex("tipo")
                val tipo = if (tipoIndex != -1 && !cursor.isNull(tipoIndex)) cursor.getString(tipoIndex) else "GASTO"
                val fechaMillis = cursor.getLong(cursor.getColumnIndexOrThrow("fechaMillis"))

                lista.add(GastoEntity(id, titulo, monto, categoria, tipo, fechaMillis))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return lista
    }

    // Obtener total de gastos únicamente
    fun obtenerTotalGastos(): Double {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT SUM(monto) FROM gastos_tabla WHERE tipo = 'GASTO' OR tipo IS NULL", null)
        var total = 0.0
        if (cursor.moveToFirst()) {
            total = cursor.getDouble(0)
        }
        cursor.close()
        return total
    }

    // Obtener total de ingresos únicamente
    fun obtenerTotalIngresos(): Double {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT SUM(monto) FROM gastos_tabla WHERE tipo = 'INGRESO'", null)
        var total = 0.0
        if (cursor.moveToFirst()) {
            total = cursor.getDouble(0)
        }
        cursor.close()
        return total
    }

    // Mantener compatibilidad previa
    fun obtenerTotal(): Double {
        return obtenerTotalGastos()
    }

    // Elimina todos los registros
    fun eliminarTodosLosGastos() {
        val db = writableDatabase
        try {
            db.delete("gastos_tabla", null, null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}