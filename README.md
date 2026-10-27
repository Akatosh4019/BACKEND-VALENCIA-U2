# AgroAPI Valencia - Examen Unidad 2

**Caso:** AgroSierra  
**Estudiante:** Roberto Samuel Valencia Saavedra

AgroAPI es el servicio REST del sistema de labores agricolas. La aplicacion Android AgroCampo lo consume desde un repositorio separado. El backend valida el token y los privilegios, aplica las reglas de negocio y guarda los datos en H2.

## Arquitectura y paquetes

El servicio usa Spring Boot, Kotlin, Spring Web, Spring Security, Spring Data JPA y H2. El codigo se encuentra en `src/main/kotlin/pe/upeu/agrosierra`:

```text
web/          controladores REST y DTO de solicitud/respuesta
seguridad/    usuarios, roles, privilegios y autenticacion
servicio/     reglas del negocio y operaciones autorizadas
repositorio/  interfaces Spring Data para persistencia
dominio/      entidades Lote y Labor
config/       JWT, filtros de seguridad, errores y datos semilla
Aplicacion.kt punto de entrada Spring Boot
```

El recorrido de una peticion es: el controlador recibe y valida el contrato HTTP; la capa de servicio aplica privilegios y reglas; el repositorio lee o escribe entidades en H2; los DTO forman la respuesta JSON. `SecurityConfig`, `JwtAuthenticationFilter` y `JwtService` protegen las rutas.

## Ejecucion con Docker

Requisitos: Docker Desktop con contenedores Linux. En PowerShell, desde la raiz de este repositorio:

```powershell
$bytes = [Security.Cryptography.RandomNumberGenerator]::GetBytes(32)
$secret = [Convert]::ToBase64String($bytes)
"JWT_SECRET=$secret" | Set-Content -Encoding ascii .env
docker compose up --build -d
docker compose ps
```

La API queda en `http://localhost:8082/api/v1`; Swagger UI en `http://localhost:8082/swagger-ui/index.html`. H2 persiste en el volumen Docker `agro-datos`. Para detener el servicio conservando los datos: `docker compose down`. Para borrar tambien los datos locales del contenedor: `docker compose down -v`.

Para que el emulador Android consuma el servicio, AgroCampo usa `http://10.0.2.2:8082/api/v1/`, direccion que llega al puerto 8082 del equipo anfitrion.

## Contrato del API

Todas las rutas, salvo `POST /auth/login`, requieren `Authorization: Bearer <token>`.

| Metodo y ruta | Uso |
|---|---|
| `POST /api/v1/auth/login` | Recibe `usuario` y `password`; devuelve JWT, usuario, rol y privilegios |
| `GET /api/v1/usuarios/me` | Devuelve la identidad autenticada |
| `GET /api/v1/lotes?pagina=0&tamanio=20` | Listado paginado de lotes |
| `GET /api/v1/lotes/{id}` | Detalle de lote |
| `GET /api/v1/lotes/{id}/labores?pagina=0&tamanio=20` | Labores paginadas del lote |
| `POST /api/v1/labores` | Crea una labor; acepta lote, tipo, fecha, cantidad, jornales, observacion y `claveCliente` |
| `PUT /api/v1/labores/{id}` | Corrige una labor existente |
| `DELETE /api/v1/labores/{id}` | Elimina una labor autorizada |
| `POST /api/v1/lotes/{id}/cerrar-campania` | Cierra la campania del lote |

Los listados devuelven `contenido`, `pagina`, `tamanio`, `totalElementos`, `totalPaginas` y `ultima`. El campo `claveCliente` hace idempotentes los reintentos de altas offline. Las respuestas de error distinguen autenticacion (`401`), privilegio insuficiente (`403`), validacion (`400`), recurso ausente (`404`) y conflictos de reglas (`409`).

## Roles y privilegios

El servidor aplica estas reglas en las operaciones. La interfaz movil tambien oculta acciones no disponibles, pero esa presentacion no reemplaza el control del backend.

| Rol | Privilegios |
|---|---|
| `TECNICO` | `LOTE_LEER`, `LABOR_LEER`, `LABOR_CREAR`, `LABOR_EDITAR` |
| `JEFE_CAMPO` | `LOTE_LEER`, `LABOR_LEER`, `LABOR_CREAR`, `LABOR_EDITAR`, `LABOR_ELIMINAR`, `CAMPANIA_CERRAR`; puede editar labores ajenas |
| `SUPERVISOR_CALIDAD` | `LOTE_LEER`, `LABOR_LEER`, `LABOR_EDITAR`; puede editar labores ajenas |
| `ADMINISTRADOR` | `LOTE_LEER`, `LABOR_LEER`, `USUARIO_GESTIONAR`; no puede crear labores ni cerrar campanias |

Cuentas locales de demostracion: `tecnico1`, `jefe1`, `supervisor1` y `admin`. La contrasena semilla es `Agro2026*`.

## Pruebas y coleccion HTTP

Con Maven instalado, desde la raiz del repositorio:

```powershell
mvn -B test
python ..\probar_api.py
```

`probar_api.py` ejecuta casos contra Docker para login, respuestas `401`, `403`, `400`, paginacion, reglas agricolas, operaciones CRUD y reintentos idempotentes. La coleccion de Postman esta en `postman/AgroSierra.postman_collection.json`.

## Cambios de la Parte II

- **SC-B:** rol de supervision de calidad que puede corregir labores ajenas, pero no crear labores ni cerrar campanias.
- **SC-C:** campo de jornales en la entidad, DTO, validacion y base local compatible.
- **SC-D:** cambio de la aplicacion movil para mostrar labores pendientes por lote; el cliente se documenta y desarrolla en el repositorio EXA-VALENCIA-U2.
