package pe.upeu.agrosierra.config

import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.security.crypto.password.PasswordEncoder
import pe.upeu.agrosierra.seguridad.*
import pe.upeu.agrosierra.seguridad.Privilegio.*
import pe.upeu.agrosierra.dominio.*
import pe.upeu.agrosierra.repositorio.*
import java.time.LocalDate

@Component
class DatosSemilla(private val roles: RolRepository,private val usuarios: UsuarioRepository,private val lotes: LoteRepository,private val labores: LaborRepository,private val encoder: PasswordEncoder) : CommandLineRunner {
    @Transactional
    override fun run(vararg args: String) {
        val tecnico=roles.findById("TECNICO").orElseGet { roles.save(Rol("TECNICO",mutableSetOf(LOTE_LEER,LABOR_LEER,LABOR_CREAR,LABOR_EDITAR))) }
        val jefe=roles.findById("JEFE_CAMPO").orElseGet { roles.save(Rol("JEFE_CAMPO",mutableSetOf(LOTE_LEER,LABOR_LEER,LABOR_CREAR,LABOR_EDITAR,LABOR_ELIMINAR,CAMPANIA_CERRAR),true)) }
        val supervisor=roles.findById("SUPERVISOR_CALIDAD").orElseGet { roles.save(Rol("SUPERVISOR_CALIDAD",mutableSetOf(LOTE_LEER,LABOR_LEER,LABOR_EDITAR),true)) }
        val admin=roles.findById("ADMINISTRADOR").orElseGet { roles.save(Rol("ADMINISTRADOR",mutableSetOf(LOTE_LEER,LABOR_LEER,USUARIO_GESTIONAR))) }
        listOf(Triple("tecnico1","Técnico de campo",tecnico),Triple("jefe1","Jefe de campo",jefe),Triple("admin","Administrador",admin),Triple("supervisor1","Supervisor de calidad",supervisor)).forEach { (login,nombre,rol) ->
            if(usuarios.findByUsuario(login)==null) usuarios.save(Usuario(usuario=login,nombre=nombre,passwordHash=requireNotNull(encoder.encode("Agro2026*")),rol=rol))
        }
        if(lotes.count()>0) return
        val autor=usuarios.findByUsuario("tecnico1")!!
        val valles=listOf("Mantaro","Sagrado","Cañete")
        (1..8).forEach { n ->
            val lote=lotes.save(Lote(codigo="LOT-${n.toString().padStart(3,'0')}",nombre="Parcela $n",hectareas=2.0+n,cultivo=if(n%2==0) "Papa" else "Maíz",valle=valles[(n-1)%3],campaniaCerrada=n>=7,dosisMaximaPorHectarea=5.0))
            val cantidad=if(n<=4) 3 else 2
            (1..cantidad).forEach { k ->
                labores.save(Labor(loteId=lote.id,tipo=if(n==1 && k==1) TipoLabor.COSECHA else if(k==2) TipoLabor.FUMIGACION else TipoLabor.RIEGO,fecha=LocalDate.now().minusDays(k.toLong()).toString(),responsable=autor.nombre,autorId=autor.id,cantidad=2.0,jornales=1.5,observacion="Labor semilla $n-$k"))
            }
        }
    }
}


