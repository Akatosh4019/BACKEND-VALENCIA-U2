package pe.upeu.agrosierra.web

import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*
import org.springframework.http.HttpStatus
import pe.upeu.agrosierra.servicio.CampoService
import pe.upeu.agrosierra.web.dto.LaborRequest
import java.security.Principal

@RestController
@RequestMapping("/api/v1")
class CampoController(private val servicio: CampoService) {
    @GetMapping("/lotes") fun lotes(@RequestParam(defaultValue="0") pagina: Int,@RequestParam(defaultValue="20") tamanio: Int,@RequestParam(required=false) valle: String?,@RequestParam(required=false) campaniaCerrada: Boolean?) = servicio.listar(pagina,tamanio,valle,campaniaCerrada)
    @GetMapping("/lotes/{id}") fun lote(@PathVariable id: Long) = servicio.obtener(id)
    @GetMapping("/lotes/{id}/labores") fun labores(@PathVariable id: Long,@RequestParam(defaultValue="0") pagina: Int,@RequestParam(defaultValue="20") tamanio: Int) = servicio.labores(id,pagina,tamanio)
    @PostMapping("/labores") @ResponseStatus(HttpStatus.CREATED)
    fun crear(@Valid @RequestBody r: LaborRequest,p: Principal) = servicio.crear(r,p.name)
    @PutMapping("/labores/{id}") fun corregir(@PathVariable id: Long,@Valid @RequestBody r: LaborRequest,p: Principal) = servicio.corregir(id,r,p.name)
    @DeleteMapping("/labores/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) fun eliminar(@PathVariable id: Long) = servicio.eliminar(id)
    @PostMapping("/lotes/{id}/cerrar-campania") fun cerrar(@PathVariable id: Long) = servicio.cerrar(id)
}
