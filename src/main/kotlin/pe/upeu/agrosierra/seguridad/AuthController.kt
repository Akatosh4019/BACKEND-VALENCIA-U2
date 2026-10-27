package pe.upeu.agrosierra.seguridad

import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*
import org.springframework.security.crypto.password.PasswordEncoder
import java.security.Principal
import pe.upeu.agrosierra.repositorio.UsuarioRepository
import pe.upeu.agrosierra.config.*
import pe.upeu.agrosierra.web.dto.*

@RestController
@RequestMapping("/api/v1")
class AuthController(private val usuarios: UsuarioRepository, private val encoder: PasswordEncoder, private val jwt: JwtService) {
    @PostMapping("/auth/login")
    fun login(@Valid @RequestBody request: LoginRequest): LoginResponse {
        val u = usuarios.findByUsuario(request.usuario)
        if (u == null || !u.habilitado || !encoder.matches(request.password, u.passwordHash)) throw Fallo(401,"Usuario o contraseña incorrectos")
        return LoginResponse(jwt.emitir(u),u.usuario,u.nombre,u.rol.nombre,u.rol.privilegios.map { it.name }.toSet(),u.id,u.rol.puedeEditarAjenas)
    }
    @GetMapping("/usuarios/me")
    fun me(principal: Principal) = (usuarios.findByUsuario(principal.name) ?: throw Fallo(401,"Sesión inválida")).aRespuesta()
}
