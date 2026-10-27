package pe.upeu.agrosierra.config

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import pe.upeu.agrosierra.seguridad.Usuario
import java.time.Instant
import java.util.Date

@Service
class JwtService(@Value("\${app.jwt-secret}") secret: String) {
    private val clave = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret))
    fun emitir(u: Usuario): String = Jwts.builder().subject(u.usuario)
        .claim("usuarioId", u.id).claim("nombre", u.nombre).claim("rol", u.rol.nombre)
        .claim("privilegios", u.rol.privilegios.map { it.name })
        .claim("puedeEditarAjenas", u.rol.puedeEditarAjenas)
        .issuedAt(Date.from(Instant.now())).expiration(Date.from(Instant.now().plusSeconds(3600)))
        .signWith(clave).compact()
    fun verificar(token: String): String = Jwts.parser().verifyWith(clave).build().parseSignedClaims(token).payload.subject
}
