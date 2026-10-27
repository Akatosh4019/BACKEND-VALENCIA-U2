package pe.upeu.agrosierra.config

import org.springframework.context.annotation.*
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.http.HttpMethod
import pe.upeu.agrosierra.repositorio.UsuarioRepository
import tools.jackson.databind.ObjectMapper

@Configuration
@EnableMethodSecurity
class SecurityConfig {
    @Bean fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()
    @Bean fun seguridad(http: HttpSecurity, jwt: JwtService, usuarios: UsuarioRepository, json: ObjectMapper): SecurityFilterChain {
        http.csrf { it.disable() }.formLogin { it.disable() }.httpBasic { it.disable() }.logout { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests {
                it.requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                    .anyRequest().authenticated()
            }
            .exceptionHandling {
                it.authenticationEntryPoint { r,s,_ ->
                    s.status=401; s.contentType="application/json"; s.characterEncoding="UTF-8"
                    json.writeValue(s.writer,errorRespuesta(401,"Debe iniciar sesión",r.requestURI))
                }.accessDeniedHandler { r,s,_ ->
                    s.status=403; s.contentType="application/json"; s.characterEncoding="UTF-8"
                    json.writeValue(s.writer,errorRespuesta(403,"No tiene el privilegio necesario",r.requestURI))
                }
            }
            .addFilterBefore(JwtAuthenticationFilter(jwt, usuarios, json), UsernamePasswordAuthenticationFilter::class.java)
        return http.build()
    }
}
