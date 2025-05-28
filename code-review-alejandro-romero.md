# Code Review - Servicio para gestión de calidad de los anuncios - Alejandro Romero

## Proyecto Java - Microservicio Spring Boot

- El proyecto está desarrollado con Java 8 y con la versión 2.x de Spring Boot. Es altamente recomendable utilizar
las versiones más recientes en cuanto a tecnologías (Java, framework, librerías...) por temas seguridad, vulnerabilidades,
compatibilidad, rendimiento, eficiencia y mantenibilidad. Modificaría la versión de Java a la última versión LTS (Java 21)
y la versión de Spring Boot a la 3.x (subversión compatible con Java 21).

- En el `pom.xml` añadiría la dependencia `spring-boot-starter-data-jpa` para hacer uso de las librerías de Spring de
acceso y tratamiento de los datos de una base de datos, y la dependencia `spring-boot-starter-test` para no tener que
declarar todas las librerías relacionadas con testing y conseguir una mejor abstracción de las dependencias. También
es clave incluir el plugin `spring-boot-maven-plugin` para facilitar el empaquetado, ejecución y despliegue de la
aplicación. A su vez, sería recomendable añadir perfiles para distinguir qué plugins, configuración y variables hay en
función de los distintos entornos de ejecución (development, staging, production, etc).

- No existe un fichero `Dockerfile` y es recomandable añadirlo porque ofrece a la aplicación portabilidad, un fácil
despliegue, aislamiento del entorno, escalabilidad, consistencia entre diferentes entornos y compatibilidad con
orquestadores (como Kubernetes).

- El fichero `application.yml` se encuentra vacío. Lo correcto es añadir en este fichero todos los parámetros de configuración
relacionados con la aplicación (configuración de Spring, conexión a base de datos...). También se puede crear un fichero
distinto para cada tipo de entorno con los valores propios del entorno (`application-dev.yml`, `application-prod.yml`, etc).
Todos los valores que impliquen información secreta deben establecerse fuera de este fichero mediante variables de entorno
definidas en el servidor o utilizando servicios cloud como secrets manager.

- No existen clases de configuración personalizadas. Esto hace que Spring cargue configuraciones por defecto que pueden
provocar comportamientos indeseados en la aplicación. Todas las clases de configuración irían dentro de un paquete
nuevo: `com.idealista.config`.

## Arquitectura

La arquitectura actual del proyecto se basa en un arquitectura hexagonal aplicando también DDD (Domain Driven Design).
Sin embargo, se están mezclando conceptos de arquitectura hexagonal con conceptos o estructuras más tradicionales como
la estructura MVC (modelo-vista-controlador) separado en tres capas.

Para conseguir implementar de manera correcta la arquitectura hexagonal en el proyecto, hay que tener en cuenta estos
aspectos clave:

- Separación de responsabilidades. Definir bien las capas de dominio (core), puertos (interfaces) y adaptadores (implementaciones).
- Inversión de dependencias. El dominio no debe depender de la infraestructura.
- Consistencia en nombres y dependencias.
- Aislar lógica de negocio de tecnología. Establecer el dominio como núcleo de la aplicación.

La estructura del proyecto quedaría de la siguiente manera si implementamos la arquitectura hexagonal pura:

```
com.idealista
├── Main
├── application
│   └── ad
│       └── AdsServiceImpl
├── config
├── domain
│   ├── ad
│   │   ├── Ad
│   │   ├── AdRepository
│   │   ├── AdVO
│   │   ├── AdsService
│   │   ├── Picture
│   │   └── PictureVO
│   ├── constant
│   │   └── Constants
│   └── enum
│       └── ad
│           ├── Quality
│           └── Typology
├── infrastructure
│   ├── in
│   │   └── rest
│   │       └── ad
│   │           ├── AdsController
│   │           ├── PublicAd
│   │           └── QualityAd
│   └── out
│       └── db
│           └── ad
│               └── InMemoryPersistence
```

Las clases que se han movido en la nueva estructura respecto a la original son las siguientes:

- **AdsService:** Esta clase es un puerto (interfaz) y se encontraba en la capa de aplicación. Se ha reubicado en la capa
de dominio porque define qué se necesita y no cómo o forma la cual se necesita (implementación).

- **AdVO:** Esta clase se encontraba en la capa de infraestructura y se ha movido a la capa de dominio porque representa
un concepto del negocio que es fundamental para modelar la lógica y las reglas del dominio.

- **PictureVO:** Esta clase se encontraba en la capa de infraestructura y se ha movido a la capa de dominio porque representa
un concepto del negocio que es fundamental para modelar la lógica y las reglas del dominio.

## Análisis del código (solo aspectos a mejorar)

### Capa Application

- **AdsService:** Esta clase tiene demasiadas responsabilidades y lo correcto sería separar cada responsabilidad en una
clase diferente. Además se está haciendo uso de las clases DTO pertenecientes a la capa de infraestructura, lo que
rompe por completo la separación de la infraestructura de la lógica de negocio.

- **AdsServiceImpl:** Hay varios puntos a tener en cuenta en esta clase:
  - La clase implementa demasiadas responsabilidades, lo cual rompe los principios de arquitectura hexagonal y SOLID.
  El caso de uso de calcular la puntuación de los anuncios debe ir en la capa de aplicación, y el uso de DTOs y el mapeo
  de éstos deben ir en la capa de infraestructura.
  - Evitar el uso de `@Autowired`, ya que reduce la testeabilidad y rompe la inmutabilidad. Es preferible inyección por
  constructor.
  - Hay código duplicado que actúa como mapper entre las entidades y los DTOs. Lo suyo es separarlo en clases específicas
  que actúen como mappers y hacer uso de librerías como MapStruct.
  - Es recomendable utilizar Value Objects en vez de tipos primitivos como Integer o String para definir elementos con
  valor e inmutables.
  - Para la creación de objetos con constructores con muchos parámetros es recomendable hacer uso de patrones como
  Builder o Factory.
  - Evitar la lógica de negoción en la capa de aplicación. En este caso, la lógica de negocio (calcular puntuación)
  debería ir dentro de la entidad `Ad` (en la capa de dominio).

- **AdsServiceImpl#67-139:** Este método hace demasiadas cosas a la vez:
  - Lógica de puntuación por imágenes.
  - Lógica de puntuación por descripción.
  - Completitud.
  - Normalización del Score.
  - Asignación de `irrelevantSince`.
  - Guardado.

  Lo correcto es separar todas estas funcionalidades en métodos distintos o clases funcionales específicas (si procede) para mejorar la legibilidad, mantenibilidad y cohesión del código.

- **AdsServiceImpl#110-114:** Hay muchos `if` anidados. Hay que evitar la lógica condicional repetida.

- **AdsServiceImpl#119:** En esta sentencia de código se está sobrescribiendo la puntuación hasta el momento calcula por
un valor constante en vez de sumarse a la puntuación acumulada. Esto sería un error si atendemos a las historias de
usuario definidas.

### Capa Domain

- **Ad:** Se puede simplificar el código de está clase haciendo uso de la librería Lombok, que permite auto-generar
en tiempo de compilación los constructores o Builders, getters/setters, y los métodos `equals`, `hashCode` y `toString`. El método
`isComplete` mezcla varias reglas específicas de negocio, por lo que sería recomendable separar esas comprobaciones
en una clase aparte.

- **Constants:** Los nombres de las constantes son ambiguos y carecen de significado. Habría que poner nombres
significativos a las constantes o colocarlas en un contexto para mejorar su reutilización.

- **Picture:** Se puede simplificar el código de está clase haciendo uso de la librería Lombok, que permite auto-generar
en tiempo de compilación los constructores o Builders, getters/setters, y los métodos `equals`, `hashCode` y `toString`.

- **Quality:** Este enum es correcto en su forma básica pero podría mejorarse añadiendo metadatos de negocio en el caso
de que los hubiera.

- **Typology:** Este enum es correcto en su forma básica pero podría mejorarse añadiendo metadatos de negocio en el caso
de que los hubiera.

### Capa Infrastructure

- **AdsController:** Hay varios puntos a tener en cuenta en esta clase:
  - Habría que utilizar un prefijo común para los endpoints (path a nivel de clase) y evitar repetir `/ads`.
  - Evitar el uso de `@Autowired`, ya que reduce la testeabilidad y rompe la inmutabilidad. Es preferible inyección por
  constructor.
  - No existe validación de los datos de entrada y no se manejan errores en caso de datos erróneos.
  - No hay ningún tipo de documentación del API. Lo ideal sería definir el API y documentarla con estándares como
  OpenAPI o Swagger.
  - El método `calculateScore` utilizar GET como método HTTP, lo cual es incorrecto. Siguiendo una convención más
  RESTful, lo suyo sería utilizar POST o PUT ya que implica cambiar el estado de uno o varios recursos en el servidor.
  - El método `calculateScore` no devuelve un mensaje de respuesta claro al usuario. Habría que definir un DTO de
  respuesta específico con una estructura predefinida para informar al usuario de cómo ha ido el proceso de puntuar
  los anuncios.
  - En un entorno real o de producción, tener el cálculo de la puntuación de los anuncios de manera síncrona puede
  ocasionar grandes ejecuciones en tiempo, ser costoso en rendimiento y puede bloquear otras peticiones entrantes.
  Una posible solución sería hacer una implementación asíncrona o utilizar la programación reactiva como paradigma
  para hacer la aplicación más concurrente y escalable.

- **InMemoryPersistence:** Esta clase implementa los métodos de `AdRepository` y actúa como repositorio para la gestión
de datos de la aplicación. Como se trata de una prueba técnica y no es un entorno real, es comprensible que se haga uso
de la memoria para almacenar los datos y la información de los anuncios. Sin embargo, es recomendable utilizar sistemas
o alternativas para trabajar con bases de datos en memoria que ofrecen una mejor implementación, configuración, rendimiento
y abstracción, para no tener que utilizar la memoria de JVM mediante variables. Una de estas posibles alternativas sería
usar H2 como base de datos en memoria. Este tipo de bases de dato es también ideal para realizar tests con datos ficticios
y no depender de conexiones a entornos de tests o desarrollo.

- **InMemoryPersistence#80-115:** Haciendo referencia a la arquitectura hexagonal y a los principios SOLID, los métodos
que actúan como mappers habría que separarlos en clases específicas, para separar responsabilidades. También como
recomendación, se puede utilizar la librería MapStruct para que el código relacionado con los mappers sea más legible.

## Consideraciones importantes a tener en cuenta

1. La parte de testing del proyecto es muy pobre. Habría que desarrollar tests unitarios, de integración y end-to-end
(si hiciera falta) para probar correctamente todas las capas, servicios y componentes de la aplicación. También es
recomendable tener una cobertura de test de al menos un 75-80% del código.

2. No existe ningún mecanismo de control de errores ni excepciones. Primero habría que definir excepciones específicas
para los casos de uso, situaciones erróneas y control de la infraestructura tanto en los puntos de entrada como de salida.
Una vez definido esto, habría que implementar un `ControllerAdvice` que maneje estas excepciones y las respuestas
correctas al usuario con códigos HTTP y mensajes claros y concisos, como por ejemplo 400 (Bad Request), 403 (Forbidden)
o 500 (Internal Server Error).

3. La aplicación no cuenta con logs en ninguna parte del código. Sería interesante implementarlos para tener mayor
trazabilidad del código y facilidad de depuración en caso de errores o bugs.

4. Es recomendable hacer uso de librerías como Lombok y MapStruct para hacer el código más legible, simple, limpio y
mantenible, además de evitar al desarrollador escribir código innecesario.

5. En un entorno de producción real, si la aplicación tiene muchas peticiones por minuto o mucho tráfico de entrada,
es bueno considerar aplicar la programación reactiva para ofrecer mayor concurrencia y escalabilidad a la aplicación
y evitar así posibles bloqueos.

6. Es una buena práctica definir y especificar el API de un aplicación utilizando estándares como OpenAPI o Swagger,
lo que permite tener centralizado los endpoints y los DTOs de las peticiones, con posibles ejemplos y zona de pruebas
para el desarrollador.

7. Es altamente recomendable usar librerías como JPA e Hibernate para abstraerse de la implementación en la gestión
de los datos de la base de datos.

8. Hay que tener mucho cuidado con los valores a `null` en todo el código para evitar excepciones o errores indeseados.
Para ello, se recomienda hacer comprobaciones de que los valores no son `null` o utilizar mecanismos propios de Java
como `Optional` para evitar estos errores.

## Conclusión

Con el análisis de este proyecto, se han propuesto una gran cantidad de mejoras que se alinean lo máximo posbile con
el uso de la arquitectura hexagonal, DDD, principios SOLID y Clean Code, y la implementación de buenas prácticas de
software. Aplicando todas estas mejoras se consigue tener una aplicación más mantenible, estructurada, limpia,
modularizada, escalable y robusta.
