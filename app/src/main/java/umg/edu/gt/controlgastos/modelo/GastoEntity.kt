package umg.edu.gt.controlgastos.modelo

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "gastos_tabla")
data class GastoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val titulo: String = "",
    val monto: Double = 0.0,
    val categoria: String = "",
    val tipo: String = "GASTO", // "GASTO" o "INGRESO"
    val fechaMillis: Long = System.currentTimeMillis()
)