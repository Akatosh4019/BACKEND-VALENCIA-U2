package pe.upeu.agrosierra.config

import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.*
import org.springframework.web.bind.annotation.*
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.security.access.AccessDeniedException
import org.springframework.http.converter.HttpMessageNotReadableException
import java.time.LocalDateTime

class Fallo(val status: Int, override val message: String, val campos: Map<String, String> = emptyMap()) : RuntimeException(message)
data class ErrorRespuesta(val timestamp: String = LocalDateTime.now().toString(), val status: Int, val error: String, val message: String, val path: String, val validationErrors: Map<String,String> = emptyMap())
fun errorRespuesta(status: Int, mensaje: String, path: String, campos: Map<String,String> = emptyMap()) = ErrorRespuesta(status = status, error = HttpStatus.valueOf(status).reasonPhrase, message = mensaje, path = path, validationErrors = campos)

@RestControllerAdvice
class ManejadorDeErrores {
    @ExceptionHandler(Fallo::class)
    fun negocio(e: Fallo, r: HttpServletRequest) = ResponseEntity.status(e.status).body(errorRespuesta(e.status, e.message, r.requestURI, e.campos))
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun validacion(e: MethodArgumentNotValidException, r: HttpServletRequest) = ResponseEntity.badRequest().body(errorRespuesta(400, "Datos inválidos", r.requestURI, e.bindingResult.fieldErrors.associate { it.field to (it.defaultMessage ?: "Valor inválido") }))
    @ExceptionHandler(HttpMessageNotReadableException::class, IllegalArgumentException::class)
    fun formato(e: Exception, r: HttpServletRequest) = ResponseEntity.badRequest().body(errorRespuesta(400, "Formato de solicitud inválido", r.requestURI))
    @ExceptionHandler(AccessDeniedException::class)
    fun permiso(e: Exception, r: HttpServletRequest) = ResponseEntity.status(403).body(errorRespuesta(403, "No tiene el privilegio necesario", r.requestURI))
    @ExceptionHandler(Exception::class)
    fun inesperado(e: Exception, r: HttpServletRequest): ResponseEntity<ErrorRespuesta> {
        org.slf4j.LoggerFactory.getLogger(javaClass).error("Fallo del servicio en {}", r.requestURI, e)
        return ResponseEntity.internalServerError().body(errorRespuesta(500, "No se pudo completar la operación", r.requestURI))
    }
}
