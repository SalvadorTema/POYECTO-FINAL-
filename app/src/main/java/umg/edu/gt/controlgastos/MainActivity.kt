package umg.edu.gt.controlgastos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import umg.edu.gt.controlgastos.datos.GastoDbHelper
import umg.edu.gt.controlgastos.datos.ReporteHelper
import umg.edu.gt.controlgastos.modelo.GastoEntity
import umg.edu.gt.controlgastos.ui.theme.ControlGastosTheme
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class MainActivity : ComponentActivity() {

    private fun formatearFecha(millis: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy • HH:mm", Locale.getDefault())
        sdf.timeZone = TimeZone.getDefault()
        return sdf.format(Date(millis))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val dbHelper = GastoDbHelper(this)
        val reporteHelper = ReporteHelper(this)

        setContent {
            ControlGastosTheme {
                val context = LocalContext.current

                // Paleta de Colores
                val fondoGeneral = Color(0xFFF3F4F6)
                val azulPrimario = Color(0xFF1E3A8A)
                val azulAcento = Color(0xFF2563EB)
                val verdeExito = Color(0xFF059669)
                val naranjaAlerta = Color(0xFFD97706)
                val rojoCritico = Color(0xFFDC2626)

                // Estado del Tipo de Cambio Dinámico
                var tasaCambioUsd by remember { mutableStateOf(7.6419) }
                var estadoRedLabel by remember { mutableStateOf("🔄 Consultando tasa oficial...") }

                LaunchedEffect(Unit) {
                    withContext(Dispatchers.IO) {
                        try {
                            val url = URL("https://open.er-api.com/v6/latest/USD")
                            val connection = url.openConnection() as HttpURLConnection
                            connection.requestMethod = "GET"
                            connection.connectTimeout = 3000
                            connection.readTimeout = 3000

                            if (connection.responseCode == 200) {
                                val response = connection.inputStream.bufferedReader().use { it.readText() }
                                val jsonObject = JSONObject(response)
                                val rates = jsonObject.getJSONObject("rates")
                                if (rates.has("GTQ")) {
                                    val tasaReal = rates.getDouble("GTQ")
                                    val tasaFormateada = String.format(Locale.US, "%.4f", tasaReal)
                                    withContext(Dispatchers.Main) {
                                        tasaCambioUsd = tasaReal
                                        estadoRedLabel = "🌐 Tasa en vivo: 1 USD = Q$tasaFormateada"
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            val respaldoFormateado = String.format(Locale.US, "%.4f", tasaCambioUsd)
                            withContext(Dispatchers.Main) {
                                estadoRedLabel = "⚠️ Tasa de respaldo Banguat: 1 USD = Q$respaldoFormateado"
                            }
                        }
                    }
                }

                // Estados de Datos Financieros
                var totalGastosQ by remember { mutableStateOf(dbHelper.obtenerTotalGastos()) }
                var totalIngresosQ by remember { mutableStateOf(dbHelper.obtenerTotalIngresos()) }
                val saldoDisponible = totalIngresosQ - totalGastosQ
                var listaActualizada by remember { mutableStateOf(dbHelper.obtenerGastos()) }

                // Estado de Filtro de Historial ("TODOS", "INGRESO", "GASTO")
                var filtroSeleccionado by remember { mutableStateOf("TODOS") }

                // Lista Filtrada Dinámicamente
                val listaFiltrada = when (filtroSeleccionado) {
                    "INGRESO" -> listaActualizada.filter { it.tipo == "INGRESO" }
                    "GASTO" -> listaActualizada.filter { it.tipo == "GASTO" }
                    else -> listaActualizada
                }

                // Lista de Categorías Predefinidas
                val categoriasGasto = listOf("🍔 Alimentos", "🚗 Transporte", "💡 Servicios", "🎭 Entretenimiento", "📦 Varios")
                val categoriasIngreso = listOf("💼 Salario / Nómina", "📈 Inversión", "🎁 Regalo / Extra", "📦 Varios")
                var categoriaSeleccionada by remember { mutableStateOf(categoriasGasto[0]) }
                var menuCategoriaExpandido by remember { mutableStateOf(false) }

                // Formulario
                var descripcionInput by remember { mutableStateOf("") }
                var montoInput by remember { mutableStateOf("") }
                var esIngreso by remember { mutableStateOf(false) } // false = Gasto, true = Ingreso
                var esEnDolares by remember { mutableStateOf(false) }
                var mensajeError by remember { mutableStateOf<String?>(null) }
                var mostrarDialogoLimpiar by remember { mutableStateOf(false) }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = fondoGeneral
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // ENCABEZADO
                        Text(
                            text = "SmartWallet Pro 💡",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = azulPrimario
                        )

                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = estadoRedLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF4B5563)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // PANEL DE TRES TARJETAS
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = azulAcento)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("SALDO", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    Text("Q %.2f".format(saldoDisponible), fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.ExtraBold)
                                }
                            }

                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFE0F2FE))
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("INGRESOS", fontSize = 10.sp, color = verdeExito, fontWeight = FontWeight.Bold)
                                    Text("+Q %.2f".format(totalIngresosQ), fontSize = 13.sp, color = verdeExito, fontWeight = FontWeight.ExtraBold)
                                }
                            }

                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2))
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("GASTOS", fontSize = 10.sp, color = rojoCritico, fontWeight = FontWeight.Bold)
                                    Text("-Q %.2f".format(totalGastosQ), fontSize = 13.sp, color = rojoCritico, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // MEJORA 1: BARRA DE PROGRESO DE CONSUMO DEL PRESUPUESTO
                        val porcentajeGastado = if (totalIngresosQ > 0) (totalGastosQ / totalIngresosQ).toFloat() else 0f
                        val colorBarra = when {
                            porcentajeGastado >= 0.9f -> rojoCritico
                            porcentajeGastado >= 0.7f -> naranjaAlerta
                            else -> verdeExito
                        }

                        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Uso del Ingreso Total", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4B5563))
                                Text("${(porcentajeGastado * 100).toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colorBarra)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { porcentajeGastado.coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth().height(8.dp),
                                color = colorBarra,
                                trackColor = Color(0xFFE5E7EB),
                                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // FORMULARIO DE REGISTRO
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Registrar Movimiento",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = azulPrimario
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = descripcionInput,
                                    onValueChange = { descripcionInput = it },
                                    label = { Text("Descripción del movimiento") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = montoInput,
                                    onValueChange = { montoInput = it },
                                    label = { Text("Monto") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // BOTONES GASTO / INGRESO
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            esIngreso = false
                                            categoriaSeleccionada = categoriasGasto[0]
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (!esIngreso) rojoCritico else Color(0xFFE5E7EB),
                                            contentColor = if (!esIngreso) Color.White else Color.Black
                                        )
                                    ) {
                                        Text("GASTO", fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = {
                                            esIngreso = true
                                            categoriaSeleccionada = categoriasIngreso[0]
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (esIngreso) verdeExito else Color(0xFFE5E7EB),
                                            contentColor = if (esIngreso) Color.White else Color.Black
                                        )
                                    ) {
                                        Text("INGRESO", fontWeight = FontWeight.Bold)
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // MEJORA 2: MENÚ DESPLEGABLE DE CATEGORÍAS (DROPDOWN)
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedButton(
                                        onClick = { menuCategoriaExpandido = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("Categoría: $categoriaSeleccionada", color = Color(0xFF374151), fontWeight = FontWeight.SemiBold)
                                    }
                                    DropdownMenu(
                                        expanded = menuCategoriaExpandido,
                                        onDismissRequest = { menuCategoriaExpandido = false }
                                    ) {
                                        val opcionesActuales = if (esIngreso) categoriasIngreso else categoriasGasto
                                        opcionesActuales.forEach { opcion ->
                                            DropdownMenuItem(
                                                text = { Text(opcion) },
                                                onClick = {
                                                    categoriaSeleccionada = opcion
                                                    menuCategoriaExpandido = false
                                                }
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // CONVERSIÓN DE MONEDA
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = if (esEnDolares) "Moneda: Dólares ($)" else "Moneda: Quetzales (Q)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF374151)
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    Switch(
                                        checked = esEnDolares,
                                        onCheckedChange = { esEnDolares = it },
                                        colors = SwitchDefaults.colors(checkedThumbColor = azulAcento)
                                    )
                                }

                                if (esEnDolares) {
                                    Text(
                                        text = "Tasa aplicada: 1 USD = Q${String.format(Locale.US, "%.4f", tasaCambioUsd)}",
                                        fontSize = 10.sp,
                                        color = Color(0xFF6B7280)
                                    )
                                }

                                if (mensajeError != null) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = mensajeError!!,
                                        color = rojoCritico,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Button(
                                    onClick = {
                                        val montoParsed = montoInput.toDoubleOrNull()
                                        if (descripcionInput.isBlank()) {
                                            mensajeError = "⚠️ Escribe una descripción."
                                        } else if (montoParsed == null || montoParsed <= 0) {
                                            mensajeError = "⚠️ Ingresa un monto mayor a 0."
                                        } else {
                                            mensajeError = null
                                            val montoFinal = if (esEnDolares) montoParsed * tasaCambioUsd else montoParsed
                                            val tipoMovimiento = if (esIngreso) "INGRESO" else "GASTO"
                                            val etiquetaMoneda = if (esEnDolares) " [USD]" else ""
                                            val categoriaFinal = "$categoriaSeleccionada$etiquetaMoneda"

                                            val nuevoGasto = GastoEntity(
                                                titulo = descripcionInput,
                                                monto = montoFinal,
                                                categoria = categoriaFinal,
                                                tipo = tipoMovimiento
                                            )

                                            dbHelper.insertarGasto(nuevoGasto)
                                            totalGastosQ = dbHelper.obtenerTotalGastos()
                                            totalIngresosQ = dbHelper.obtenerTotalIngresos()
                                            listaActualizada = dbHelper.obtenerGastos()

                                            descripcionInput = ""
                                            montoInput = ""
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = azulAcento)
                                ) {
                                    Text("Guardar Movimiento", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // ENCABEZADO DE HISTORIAL Y EXPORTACIÓN
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Historial Reciente",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = azulPrimario
                            )

                            Row {
                                IconButton(onClick = { mostrarDialogoLimpiar = true }) {
                                    Text("🗑️", fontSize = 16.sp)
                                }
                                Button(
                                    onClick = {
                                        reporteHelper.exportarGastosCSV(
                                            listaGastos = listaActualizada,
                                            presupuesto = totalIngresosQ,
                                            total = totalGastosQ
                                        )
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A))
                                ) {
                                    Text("📄 CSV", fontSize = 12.sp)
                                }
                            }
                        }

                        // MEJORA 3: FILTROS RÁPIDOS DE HISTORIAL (TODOS / INGRESOS / GASTOS)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("TODOS", "INGRESO", "GASTO").forEach { filtro ->
                                FilterChip(
                                    selected = filtroSeleccionado == filtro,
                                    onClick = { filtroSeleccionado = filtro },
                                    label = { Text(filtro, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // LISTA DE REGISTROS FILTRADOS
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(listaFiltrada) { gasto ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = gasto.titulo,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = Color(0xFF1F2937)
                                            )
                                            Text(
                                                text = "${gasto.tipo} • ${gasto.categoria} • ${formatearFecha(gasto.fechaMillis)}",
                                                fontSize = 11.sp,
                                                color = Color(0xFF6B7280)
                                            )
                                        }
                                        Text(
                                            text = "${if (gasto.tipo == "INGRESO") "+" else "-"}Q %.2f".format(gasto.monto),
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 15.sp,
                                            color = if (gasto.tipo == "INGRESO") verdeExito else rojoCritico
                                        )
                                    }
                                }
                            }
                        }

                        // DIÁLOGO DE ELIMINAR DATOS
                        if (mostrarDialogoLimpiar) {
                            AlertDialog(
                                onDismissRequest = { mostrarDialogoLimpiar = false },
                                title = { Text("¿Eliminar todos los datos?", fontWeight = FontWeight.Bold) },
                                text = { Text("Esta acción borrará todo el historial guardado en SQLite de manera permanente.") },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            dbHelper.eliminarTodosLosGastos()
                                            totalGastosQ = dbHelper.obtenerTotalGastos()
                                            totalIngresosQ = dbHelper.obtenerTotalIngresos()
                                            listaActualizada = dbHelper.obtenerGastos()
                                            mostrarDialogoLimpiar = false
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = rojoCritico)
                                    ) {
                                        Text("Eliminar Todo")
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { mostrarDialogoLimpiar = false }) {
                                        Text("Cancelar")
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}