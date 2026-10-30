package umg.edu.gt.controlgastos.modelo

import java.util.Date

data class Gasto(
    val id: Long = System.currentTimeMillis(),
    val titulo: String,
    val monto: Double,
    val categoria: String,
    val fecha: Date = Date()
)