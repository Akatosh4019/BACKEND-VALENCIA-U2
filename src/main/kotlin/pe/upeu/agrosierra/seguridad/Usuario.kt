package pe.upeu.agrosierra.seguridad

import jakarta.persistence.*

enum class Privilegio { LOTE_LEER, LABOR_LEER, LABOR_CREAR, LABOR_EDITAR, LABOR_ELIMINAR, CAMPANIA_CERRAR, USUARIO_GESTIONAR }

@Entity
@Table(name = "roles")
class Rol(
    @Id var nombre: String = "",
    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    var privilegios: MutableSet<Privilegio> = mutableSetOf(),
    // Política de edición explícita: evita comparar nombres de rol en el servicio.
    var puedeEditarAjenas: Boolean = false,
)

@Entity
@Table(name = "usuarios")
class Usuario(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long = 0,
    @Column(unique = true) var usuario: String = "",
    var nombre: String = "",
    var passwordHash: String = "",
    @ManyToOne(fetch = FetchType.EAGER) var rol: Rol = Rol(),
    var habilitado: Boolean = true,
)
