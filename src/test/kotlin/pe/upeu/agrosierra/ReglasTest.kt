package pe.upeu.agrosierra

import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import pe.upeu.agrosierra.repositorio.*
import pe.upeu.agrosierra.servicio.CampoService
import pe.upeu.agrosierra.config.*
import pe.upeu.agrosierra.web.dto.LaborRequest
import pe.upeu.agrosierra.dominio.TipoLabor
import java.time.Instant

@SpringBootTest(properties=["spring.datasource.url=jdbc:h2:mem:reglas;DB_CLOSE_DELAY=-1","app.jwt-secret=YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXphYmNkZWY="])
@Transactional
class ReglasTest {
    @Autowired lateinit var servicio: CampoService
    @Autowired lateinit var usuarios: UsuarioRepository
    @Autowired lateinit var lotes: LoteRepository
    @Autowired lateinit var labores: LaborRepository
    @Autowired lateinit var jwt: JwtService
    private fun autenticar(nombre: String) { val u=usuarios.findByUsuario(nombre)!!; SecurityContextHolder.getContext().authentication=UsernamePasswordAuthenticationToken(nombre,null,u.rol.privilegios.map { SimpleGrantedAuthority(it.name) }) }
    private fun request(id: Long,tipo: TipoLabor=TipoLabor.RIEGO,cantidad: Double=1.0)=LaborRequest(id,tipo,"2026-10-27",cantidad,"Prueba")
    @AfterEach fun salir()=SecurityContextHolder.clearContext()
    @Test fun supervisorCorrigeAjenasPeroNoCierra() {
        autenticar("supervisor1")
        val labor=labores.findAll().first { !lotes.findById(it.loteId).get().campaniaCerrada }
        assertEquals(labor.id,servicio.corregir(labor.id,request(labor.loteId),"supervisor1").id)
        assertThrows(org.springframework.security.access.AccessDeniedException::class.java) { servicio.cerrar(labor.loteId) }
        assertThrows(org.springframework.security.access.AccessDeniedException::class.java) { servicio.crear(request(labor.loteId),"supervisor1") }
    }
    @Test fun bloqueaLaborEnCampaniaCerrada() { autenticar("tecnico1");val id=lotes.findAll().first { it.campaniaCerrada }.id;assertEquals(409,assertThrows(Fallo::class.java) { servicio.crear(request(id),"tecnico1") }.status) }
    @Test fun limitaDosisConErrorEnCantidad() { autenticar("tecnico1");val id=lotes.findAll().first { !it.campaniaCerrada }.id;val e=assertThrows(Fallo::class.java) { servicio.crear(request(id,TipoLabor.FUMIGACION,99999.0),"tecnico1") };assertEquals(400,e.status);assertTrue(e.campos.containsKey("cantidad")) }
    @Test fun noCierraSinCosecha() { autenticar("jefe1");val id=lotes.findAll().first { !it.campaniaCerrada && !labores.existsByLoteIdAndTipo(it.id,TipoLabor.COSECHA) }.id;assertEquals(409,assertThrows(Fallo::class.java) { servicio.cerrar(id) }.status) }
    @Test fun tecnicoNoCorrigeFueraDe24Horas() { autenticar("tecnico1");val l=labores.findAll().first { !lotes.findById(it.loteId).get().campaniaCerrada };l.registradaEn=Instant.now().minusSeconds(86401);labores.saveAndFlush(l);assertEquals(403,assertThrows(Fallo::class.java) { servicio.corregir(l.id,request(l.loteId),"tecnico1") }.status) }
    @Test fun tecnicoNoCierraAunqueLlameDirectamenteAlServicio() { autenticar("tecnico1");assertThrows(org.springframework.security.access.AccessDeniedException::class.java) { servicio.cerrar(lotes.findAll().first().id) } }
    @Test fun firmaJwtSeVerifica() { val token=jwt.emitir(usuarios.findByUsuario("tecnico1")!!);assertEquals("tecnico1",jwt.verificar(token));val partes=token.split('.');val firma=partes[2];val alterado=partes[0]+"."+partes[1]+"."+(if(firma[0]=='A')"B" else "A")+firma.drop(1);assertThrows(Exception::class.java) { jwt.verificar(alterado) } }
}
