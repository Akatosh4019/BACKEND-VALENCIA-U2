package pe.upeu.agrosierra.servicio

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import pe.upeu.agrosierra.dominio.*
import pe.upeu.agrosierra.repositorio.*
import pe.upeu.agrosierra.config.Fallo
import pe.upeu.agrosierra.web.dto.*
import java.time.*

@Service
@Transactional
class CampoService(private val lotes: LoteRepository, private val labores: LaborRepository, private val usuarios: UsuarioRepository) {
    private fun paginar(p: Int, t: Int): PageRequest {
        if (p < 0 || t !in 1..100) throw Fallo(400,"Paginación inválida", mapOf("pagina" to "Página desde cero y tamaño entre 1 y 100"))
        return PageRequest.of(p,t,Sort.by("id"))
    }
    @PreAuthorize("hasAuthority('LOTE_LEER')")
    fun listar(p: Int,t: Int,valle: String?,cerrada: Boolean?): Pagina<Lote> {
        val spec = Specification<Lote> { root,_,cb ->
            val filtros = mutableListOf<jakarta.persistence.criteria.Predicate>()
            if (!valle.isNullOrBlank()) filtros += cb.equal(root.get<String>("valle"),valle)
            if (cerrada != null) filtros += cb.equal(root.get<Boolean>("campaniaCerrada"),cerrada)
            cb.and(*filtros.toTypedArray())
        }
        return lotes.findAll(spec,paginar(p,t)).envolver()
    }
    private fun buscar(id: Long) = lotes.findById(id).orElseThrow { Fallo(404,"Lote no encontrado") }
    @PreAuthorize("hasAuthority('LOTE_LEER')") fun obtener(id: Long) = buscar(id)
    @PreAuthorize("hasAuthority('LABOR_LEER')")
    fun labores(id: Long,p: Int,t: Int): Pagina<Labor> { buscar(id); paginar(p,t); return labores.findByLoteIdOrderByFechaDescIdDesc(id,PageRequest.of(p,t)).envolver() }
    private fun validar(lote: Lote,r: LaborRequest) {
        if (lote.campaniaCerrada) throw Fallo(409,"La campaña está cerrada; no puede registrar ni corregir labores")
        try { LocalDate.parse(r.fecha) } catch (_: Exception) { throw Fallo(400,"Fecha inválida",mapOf("fecha" to "Use una fecha ISO válida: AAAA-MM-DD")) }
        if (!r.cantidad.isFinite() || r.cantidad <= 0) throw Fallo(400,"Cantidad inválida",mapOf("cantidad" to "La cantidad debe ser mayor que cero"))
        if (!r.jornales.isFinite() || r.jornales < 0) throw Fallo(400,"Jornales inválidos",mapOf("jornales" to "Ingrese cero o un valor positivo"))
        if (r.tipo == TipoLabor.FUMIGACION && r.cantidad > lote.hectareas * lote.dosisMaximaPorHectarea)
            throw Fallo(400,"La cantidad supera la dosis permitida",mapOf("cantidad" to "Máximo permitido: ${lote.hectareas * lote.dosisMaximaPorHectarea}"))
    }
    @PreAuthorize("hasAuthority('LABOR_CREAR')")
    fun crear(r: LaborRequest, username: String): Labor {
        val u = usuarios.findByUsuario(username) ?: throw Fallo(401,"Sesión inválida")
        r.claveCliente?.let { key -> labores.findByClaveCliente(key)?.let {
            if (it.autorId != u.id) throw Fallo(409,"Clave de operación ya utilizada")
            return it
        } }
        val lote=buscar(r.loteId); validar(lote,r)
        return labores.save(Labor(loteId=r.loteId,tipo=r.tipo,fecha=r.fecha,responsable=u.nombre,autorId=u.id,cantidad=r.cantidad,observacion=r.observacion,jornales=r.jornales,claveCliente=r.claveCliente))
    }
    @PreAuthorize("hasAuthority('LABOR_EDITAR')")
    fun corregir(id: Long,r: LaborRequest,username: String): Labor {
        val labor=labores.findById(id).orElseThrow { Fallo(404,"Labor no encontrada") }
        val u=usuarios.findByUsuario(username) ?: throw Fallo(401,"Sesión inválida")
        if (!u.rol.puedeEditarAjenas && (labor.autorId != u.id || !Instant.now().isBefore(labor.registradaEn.plusSeconds(86400))))
            throw Fallo(403,"Solo puede corregir sus labores dentro de las primeras 24 horas")
        if (labor.loteId != r.loteId) throw Fallo(400,"No puede cambiar el lote",mapOf("loteId" to "Conserve el lote original"))
        validar(buscar(labor.loteId),r)
        labor.tipo=r.tipo; labor.fecha=r.fecha; labor.cantidad=r.cantidad; labor.observacion=r.observacion; labor.jornales=r.jornales
        return labores.save(labor)
    }
    @PreAuthorize("hasAuthority('LABOR_ELIMINAR')")
    fun eliminar(id: Long) { if (!labores.existsById(id)) throw Fallo(404,"Labor no encontrada"); labores.deleteById(id) }
    @PreAuthorize("hasAuthority('CAMPANIA_CERRAR')")
    fun cerrar(id: Long): Lote {
        val lote=buscar(id)
        if (!labores.existsByLoteIdAndTipo(id,TipoLabor.COSECHA)) throw Fallo(409,"Registre al menos una cosecha antes de cerrar la campaña")
        lote.campaniaCerrada=true
        return lotes.save(lote)
    }
}
