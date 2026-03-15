1️⃣ Limpieza inicial del entorno Docker

Antes de comenzar se limpia el entorno para evitar conflictos de contenedores o puertos ocupados.

Detener contenedores:

* docker stop $(docker ps -aq)
Eliminar contenedores:
* docker rm $(docker ps -aq)
Limpiar recursos no utilizados:
* docker system prune -a

Qué explicar
Este paso garantiza que:
no existan contenedores antiguos
no existan puertos ocupados
el entorno sea reproducible
Esto permite cumplir con el principio:
“Infraestructura portable — lo que funciona en una máquina funciona en todas”.

2️⃣ Verificar conflicto de PostgreSQL (problema que tuvimos)

En nuestro caso el puerto 5432 estaba ocupado por PostgreSQL del sistema.

Verificar:

sudo lsof -i :5432

Si aparece postgres, se detiene:

sudo systemctl stop postgresql

Desactivar temporalmente:

sudo systemctl disable postgresql

Si aún sigue activo:

sudo pkill postgres

Volver a verificar:

sudo lsof -i :5432
Qué explicar

Docker necesita usar el puerto 5432 para PostgreSQL.
Si el sistema operativo ya lo está usando, los contenedores no pueden iniciar.

2️⃣ Ir a la carpeta del proyecto
Entramos al directorio donde está el archivo de arquitectura.

* cd gestion-pedidos-backend-main
* mvn clean package -DskipTests
- auth-service
- catalog-service
- order-service
- api-gateway
Aquí se encuentra:
docker-compose.yml

Este archivo define:
microservicios
redes docker
bases de datos
mensajería

3️⃣ Desmontar microservicios previos

* docker compose down

Qué explicar
Este comando elimina:
contenedores activos
redes creadas
dependencias del entorno anterior
Esto asegura que el sistema se vuelva a montar desde cero.

4️⃣ Construir y montar microservicios

* docker compose up --build

Qué hace este comando
Construye las imágenes Docker
Crea los contenedores
Conecta todos los servicios en una red interna
Servicios que se levantan:
api_gateway
auth_service
order_service
catalog_service
gestion_pedidos_db
rabbitmq

5️⃣ Verificar contenedores activos
Abrimos otra terminal.

* docker ps

Ejemplo esperado:
api_gateway
auth_service
order_service
catalog_service
gestion_pedidos_db
rabbitmq
Qué explicar
Esto demuestra que:
todos los microservicios están activos
cada servicio está aislado en su contenedor
la arquitectura distribuida está funcionando.


- PRIMERA TAREA
Filtros de Gateway + Skeletons

6️⃣ Filtro Global en API Gateway (Validación JWT)
El Gateway implementa un filtro global que intercepta todas las solicitudes.
Flujo:
Cliente
   ↓
API Gateway (Filtro JWT)
   ↓
Microservicios
El filtro valida:
autenticación
token JWT
permisos de acceso
Qué explicar
El Gateway actúa como:
punto de entrada único del sistema.
Si el token JWT no es válido:
la petición es bloqueada
no llega a los microservicios.

7️⃣ Skeletons de Catalog y Orders
Se crearon proyectos base para:
catalog-service
order-service
Estos servicios inicialmente contienen:
estructura del proyecto
dependencias
endpoint de prueba
Ejemplo:
GET /health

Prueba desde terminal:

* curl http://localhost:8082/health

Respuesta esperada:

Catalog service running
Qué explicar

Estos servicios funcionan como skeletons, es decir:

proyectos base vacíos que permiten:
validar rutas
probar comunicación
verificar que el Gateway enruta correctamente.

8️⃣ Documentación técnica del sistema
Se documentaron:
Puertos
Gateway → 8080
Auth Service → 8081
Catalog Service → 8082
Order Service → 8083
RabbitMQ → 5672
RabbitMQ Panel → 15672
PostgreSQL → 5432
Variables de entorno
Ejemplo:
SPRING_DATASOURCE_URL
SPRING_DATASOURCE_USERNAME
SPRING_DATASOURCE_PASSWORD
JWT_SECRET
RABBITMQ_HOST
Qué explicar
Documentar puertos y variables permite:
facilitar despliegues
mantener consistencia entre entornos
evitar conflictos de configuración.
- SEGUNDA TAREA
Integración Orders → Catalog

9️⃣ Cliente HTTP en Order Service
El servicio Orders implementa un cliente HTTP para consultar productos en Catalog.
Flujo:
Orders Service
      ↓ REST
Catalog Service
Ejemplo de llamada:
GET /catalog/products/{id}

🔟 Probar integración entre servicios
Ejecutar una petición:

* curl http://localhost:8080/orders

Ver logs:
* docker logs -f order_service

Ejemplo:
Calling catalog-service
Product retrieved successfully
Qué explicar
Orders usa un cliente REST para obtener información de productos desde Catalog.
Esto demuestra comunicación síncrona entre microservicios.

1️⃣1️⃣ Manejo de fallos (Timeout)

Si Catalog no responde:
Orders implementa un timeout básico.
Simulación:

* docker stop catalog_service

Luego ejecutar:

* curl http://localhost:8080/orders

Logs esperados:

Calling catalog-service
Timeout after 3000ms
Catalog unavailable
Qué explicar

Esto permite que el sistema:

no quede bloqueado

maneje fallos entre servicios

mejore la resiliencia del sistema.

- TERCERA TAREA
Implementación de X-Correlation-Id

1️⃣2️⃣ Crear identificador de trazabilidad

El sistema implementa un identificador llamado:
X-Correlation-Id
Este identificador permite rastrear una petición a través de todos los microservicios.
Ejemplo de petición:

* curl http://localhost:8080/orders \
-H "X-Correlation-Id: demo-123"

1️⃣3️⃣ Propagación del ID entre servicios REST
El ID viaja entre:
Gateway
 ↓
Orders
 ↓
Catalog
Ver logs de Orders:

* docker logs -f order_service

Ejemplo:

Received X-Correlation-Id: demo-123
Calling catalog-service with X-Correlation-Id: demo-123

Ver logs de Catalog:

* docker logs -f catalog_service

Ejemplo:

Request received with X-Correlation-Id: demo-123
Qué explicar

Esto permite trazabilidad completa de solicitudes en arquitectura distribuida.

1️⃣4️⃣ Incluir Correlation ID en eventos de RabbitMQ

Cuando Orders publica un evento:

Orders Service
      ↓
RabbitMQ

El evento incluye el mismo identificador.

Logs:

Publishing event to RabbitMQ
X-Correlation-Id: demo-123
1️⃣5️⃣ Verificar RabbitMQ

Abrir panel:

http://localhost:15672

Usuario:
admin
guest

Contraseña:
admin
guest

Ir a:

Queues
Qué explicar

RabbitMQ gestiona comunicación asíncrona entre microservicios.

Los eventos incluyen el X-Correlation-Id, lo que permite rastrear también los eventos.

1️⃣6️⃣ Flujo final del sistema

Arquitectura completa:

Cliente
   ↓
API Gateway (valida JWT)
   ↓
Orders Service
   ↓ REST
Catalog Service
   ↓
Orders Service
   ↓
RabbitMQ

El X-Correlation-Id viaja en todo el flujo del sistema.

✅ Con esta demostración se valida

✔ Gateway con filtro JWT
✔ Skeletons de Catalog y Orders
✔ Documentación técnica de puertos
✔ Integración Orders → Catalog
✔ Cliente HTTP REST
✔ Manejo de fallos con timeout
✔ Propagación de X-Correlation-Id
✔ Eventos en RabbitMQ

💡 Púlico, si quieres también te puedo preparar la versión corta de la demo (10 comandos exactos) para que puedas hacer toda la demostración en menos de 3 minutos sin perderte en la terminal, que es como muchos profesores esperan verlo.
