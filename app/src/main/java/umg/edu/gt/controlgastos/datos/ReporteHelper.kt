package umg.edu.gt.controlgastos.datos

import android.content.Context
import android.os.Environment
import android.widget.Toast
import umg.edu.gt.controlgastos.modelo.GastoEntity
import java.io.File
import java.io.FileWriter

class ReporteHelper(private val context: Context) {

    fun exportarGastosCSV(listaGastos: List<GastoEntity>, presupuesto: Double, total: Double): String? {
        if (listaGastos.isEmpty()) {
            Toast.makeText(context, "⚠️ No hay gastos para exportar", Toast.LENGTH_SHORT).show()
            return null
        }

        return try {
            val fileName = "Reporte_Gastos_${System.currentTimeMillis()}.csv"
            // Se guarda en la carpeta pública de Descargas para que sea fácil de encontrar
            val downloadsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsFolder, fileName)

            val writer = FileWriter(file)
            writer.append("REPORTE FINANCIERO - SMARTWALLET PRO\n")
            writer.append("Presupuesto Configurado: Q $presupuesto\n")
            writer.append("Total Gastado Acumulado: Q $total\n")
            writer.append("\nID,Descripcion,Monto (GTQ),Tipo Moneda\n")

            for (gasto in listaGastos) {
                writer.append("${gasto.id},\"${gasto.titulo}\",${gasto.monto},${gasto.categoria}\n")
            }

            writer.flush()
            writer.close()

            // AVISO VISUAL EN PANTALLA
            Toast.makeText(
                context,
                "✅ CSV descargado en la carpeta 'Descargas':\n$fileName",
                Toast.LENGTH_LONG
            ).show()

            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "❌ Error al exportar: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            null
        }
    }
}