package pe.upeu.agrosierra.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.*
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter
import pe.upeu.agrosierra.repositorio.UsuarioRepository
import tools.jackson.databind.ObjectMapper

class JwtAuthenticationFilter(private val jwt: JwtService, private val usuarios: UsuarioRepository, private val json: ObjectMapper) : OncePerRequestFilter() {
    override fun shouldNotFilter(r: HttpServletRequest) = r.requestURI == "/api/v1/auth/login"
    override fun doFilterInternal(r: HttpServletRequest, s: HttpServletResponse, chain: FilterChain) {
        val header = r.getHeader("Authorization")
        if (header != null) {
            try {
                if (!header.startsWith("Bearer ")) throw IllegalArgumentException()
                val u = usuarios.findByUsuario(jwt.verificar(header.removePrefix("Bearer "))) ?: throw IllegalArgumentException()
                if (!u.habilitado) throw IllegalArgumentException()
                SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken(u.usuario, null, u.rol.privilegios.map { SimpleGrantedAuthority(it.name) })
            } catch (_: Exception) {
                SecurityContextHolder.clearContext()
                s.status = 401; s.contentType = "application/json"; s.characterEncoding = "UTF-8"
                json.writeValue(s.writer, errorRespuesta(401, "Sesión inválida o expirada. Inicie sesión nuevamente", r.requestURI))
                return
            }
        }
        chain.doFilter(r, s)
    }
}
