package umg.edu.gt.controlgastos.datos

import umg.edu.gt.controlgastos.modelo.Gasto

class GastoRepositoryEnMemoria {
    private val listaGastos = mutableListOf<Gasto>()

    fun agregarGasto(gasto: Gasto) {
        listaGastos.add(gasto)
    }

    fun obtenerGastos(): List<Gasto> {
        return listaGastos.sortedByDescending { it.fecha }
    }

    fun calcularTotal(): Double {
        return listaGastos.sumOf { it.monto }
    }
}