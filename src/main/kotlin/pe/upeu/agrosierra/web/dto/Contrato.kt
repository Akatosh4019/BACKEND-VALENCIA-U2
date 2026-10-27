package pe.upeu.agrosierra.web.dto

import jakarta.validation.constraints.*
import org.springframework.data.domain.Page
import pe.upeu.agrosierra.dominio.TipoLabor
import pe.upeu.agrosierra.seguridad.Usuario

data class LoginRequest(@field:NotBlank val usuario: String = "", @field:NotBlank val password: String = "")
data class UsuarioResponse(val id: Long, val usuario: String, val nombre: String, val rol: String, val privilegios: Set<String>, val puedeEditarAjenas: Boolean)
fun Usuario.aRespuesta() = UsuarioResponse(id, usuario, nombre, rol.nombre, rol.privilegios.map { it.name }.toSet(), rol.puedeEditarAjenas)
data class LoginResponse(val token: String, val usuario: String, val nombre: String, val rol: String, val privilegios: Set<String>, val id: Long, val puedeEditarAjenas: Boolean)
data class LaborRequest(
    @field:Positive val loteId: Long = 0,
    val tipo: TipoLabor = TipoLabor.RIEGO,
    @field:NotBlank val fecha: String = "",
    @field:Positive val cantidad: Double = 0.0,
    @field:Size(max = 500) val observacion: String = "",
    @field:PositiveOrZero val jornales: Double = 0.0,
    @field:Size(max = 100) val claveCliente: String? = null,
)
data class Pagina<T>(val contenido: List<T>, val pagina: Int, val tamanio: Int, val totalElementos: Long, val totalPaginas: Int, val ultima: Boolean)
fun <T : Any> Page<T>.envolver() = Pagina(content, number, size, totalElements, totalPages, isLast)
