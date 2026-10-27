package pe.upeu.agrosierra.repositorio

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import pe.upeu.agrosierra.dominio.*
import pe.upeu.agrosierra.seguridad.*

interface UsuarioRepository : JpaRepository<Usuario, Long> { fun findByUsuario(usuario: String): Usuario? }
interface RolRepository : JpaRepository<Rol, String>
interface LoteRepository : JpaRepository<Lote, Long>, JpaSpecificationExecutor<Lote>
interface LaborRepository : JpaRepository<Labor, Long> {
    fun findByLoteIdOrderByFechaDescIdDesc(loteId: Long, pageable: Pageable): Page<Labor>
    fun existsByLoteIdAndTipo(loteId: Long, tipo: TipoLabor): Boolean
    fun findByClaveCliente(claveCliente: String): Labor?
}
