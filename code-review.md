# Code Review - Sistema de Puntuación de Anuncios

He realizado una revisión del código actual. Deberia tratarse de un proyecto legacy, aunque cuesta creerlo por la falta de api.yml y que el application.yml esté vacio, ademas de tener decisiones técnicas que se consideran obsoletas o que no encajan del todo con una arquitectura hexagonal limpia 

En un los primeros commits he dejado mis anotaciones en sucio, por si quereis saber más acerca de como pienso. 

A continuación dejo los puntos de mejora separados por capas, incluyendo ejemplos de lo que he visto y cómo podríamos solucionarlo.

---

## Configuración y General (pom.xml)

Antes de entrar en las capas, hay un par de cosas en la base que chirrían un poco:

*   **Versiones obsoletas:** Estamos usando versiones muy viejas de Spring y Java. Deberíamos considerar subir a la última versión estable (Java 17/21 y Spring Boot 3.x o incluso 4.x) .
*   **Dependencias redundantes:** He visto `JUnit` y `Mockito` declarados por separado. Spring Boot ya incluye ambos en `spring-boot-starter-test`, así que podemos limpiar eso.
*   **Falta de Lombok:** Estamos escribiendo muchísimos getters, setters y constructores a mano. Meter Lombok nos ahorraría muchisimas líneas de código
---

## Domain Layer

La capa de dominio debería ser la más limpia, pero tiene bastante ruido:

### Entidades y Modelos (Ad.java)
Estamos usando `java.util.Date`, que es una pesadilla para manejar zonas horarias y cálculos. Deberíamos pasar a `LocalDate` o `LocalDateTime`

Además, la lógica de isComplete() está metida con calzador en la entidad:
```java
// Ad.java - Línea 50
public boolean isComplete() {
    return (Typology.GARAGE.equals(typology) && !pictures.isEmpty())
            || (Typology.FLAT.equals(typology) && !pictures.isEmpty() && description != null && !description.isEmpty() && houseSize != null)
            || ...
}
```
**Sugerencia:** Este tipo de lógica podría meterse en el propio enum `Typology` o en una clase de validación específica

### Naming y Constantes (Constants.java)
Los nombres de las constantes actuales no dicen nada. `Constants.FORTY` no explica *por qué* es 40
```java
public static final int TEN = 10;
public static final int TWENTY = 20;
```
**Sugerencia:** Deberían ser descriptivos: `NO_PHOTO_SCORE_PENALTY = -10`, `PHOTO_HD_SCORE = 20` por ej

---

## Application Layer

Aquí es donde está el "grueso" del problema, especialmente en la implementación del servicio:

### Inyección de Dependencias y Naming
Seguimos usando `@Autowired` directamente en el campo
```java
// AdsServiceImpl.java
@Autowired
private AdRepository adRepository;
```
**Sugerencia:** Pasar a **inyección por constructor**. También, renombrar `AdsService` a `AdService` (singular), siguiendo las convenciones habituales

### Refactorización de calculateScore()
El método `calculateScore` en `AdsServiceImpl` es una "función Dios". Hace de todo: puntúa fotos, analiza descripciones, comprueba palabras clave... es demasiado largo

Además, hay errores de lógica graves:
```java
// Error: Está sobreescribiendo el score acumulado en vez de sumarlo
if (ad.isComplete()) {
    score = Constants.FORTY;
}
```
**Sugerencia:** Dividir en métodos privados con una única responsabilidad: `calculatePhotoScore()`, `calculateDescriptionScore()`... Para el tema de las palabras clave, en lugar de mil `if`, usar una lista y normalizar el texto (quitar acentos y pasar a minúsculas) para que la búsqueda sea robusta

---

## Infrastructure Layer

### Controladores (AdsController.java)
Estamos usando verbos `GET` para acciones que hacen modificaciones(`calculateScore`)
```java
@GetMapping("/ads/score")
public ResponseEntity<Void> calculateScore() { ... }
```
**Sugerencia:** Debería ser un `POST`. Además, falta un manejo global de excepciones (un `@ControllerAdvice` vendría geniial)

### Persistencia y Mapeo
He visto que en `InMemoryPersistence` se cuelan constantes de puntuación (`Constants.FORTY`). La persistencia no debería saber nada de las reglas de negocio

También, los DTOs `PublicAd` y `QualityAd` son casi iguales. Podríamos usar herencia o una estructura común tipo `CommonAd` para no repetir campos

---

## Testing y Calidad

Actualmente el proyecto está "desnudo" en cuanto a tests:
*   Solo hay **un test unitario**. Necesitamos cubrir todos los casos borde del scoring (anuncios sin fotos, descripciones nulas, puntuaciones máximas...)
*   **Cero tests de integración.** No sabemos si el flujo completo desde el Controller hasta el Repo funciona bien
*   **Falta documentación de API.** Un `api.yml` o Swagger es fundamental para que el Front sepa que esperar, de hecho si se trabaja con API First deberia ser lo primero en crearse

---
##
*Resumen:* La base está ahí, pero hace falta una limpieza profunda de lógica de negocio que se ha quedado desperdigada por donde no debería y modernizar un poco las herramientas que usamos
---

## Mejoras adicionales si fuera un proyecto real

He enfocado la revisión entendiendo que esto es una especie de prototipo, por lo que he intentado ser práctico. Pero si este codigo fuera a pasar a un entorno de producción real, ya sea como está (incluso con los cambios sugeridos) o con más entidades, un tráfico alto y una bbdd de verdad, habría que hacer unas refactorizaciones mucho más importantes que ahora mismo, pensando que es un prototipo considero que es sobreingenieria.

* **Casos de Uso**: En este caso deberia haber solo 3 metodos en `ServiceImpl` lo cual es manejable pero en un caso real y ante la posibilidad de que crezca lo interesante sería tener diferentes casos de uso tipo  `CalculateScoresUseCase`, `FindPublicAdsUseCase`...
  **¿Por qué no ahora?**: Porque para lo que parece un prototipo con 3 metodos no merece la pena crear 3 interfaces y 3 clases, sería sobreingenieria
* **Mapeos automáticos**: El Mapeo lo he dejado como estaba y no he sugerido cambiarlo porque en el prototipo trabaja solo con 6 campos, no cuesta nada cambiarlo y es muy explicativo. Lo normal sería tener un `AdMapper.java` pero insisto en que para un prototipo no hay que comlicar demasiado las cosas.
  **A tener en cuenta**: Si el proyecto crece incluiria la libreria MapStructur, que hace los mapeos automaticamente y ahoraria muchisimo código repetido. 
* **Persistencia y findAll()**: La persistencia actual en memoria es una trampa mortal, hacer un `findAll()` en un List funciona con 8 registros, pero en producción con datos reales tumbaria la aplicación.
  **¿Qué cambiaria?**: Usaría JPA y doveolveria el listado paginado con `Pageable`, no creo que nadie quiera traer 10.000 resultados de golpe. Además seria necesario añadir `@Transactional` para asegurar que se guardan todos los scores o ninguno si hay un fallo. 
* **Alto tráfico**: Imaginando que este prototipo recibiera un trafico grande habría que hacer algunos cambios en la lógica. El endpoint actual para calcular el score, que deberia ser post, recalcula todos los resultados de forma sincrona, si fueran muchos resultados esto daria un timeout. Lo ideal seria devolver un 202 y que el calculo se hiciera asincrono con @Async, sin embargo parece más coherente que el score se calcule individualmente cuando un anuncio se cree o se edite en lugar de el calculo masivo.

*Resumen:* Para esta prueba, lo que propuse anteriormente cumple su función sin volverse loco, pero si no es un prototipo y esto deberia ir a producción para millones de usuarios el primer paso sería quitar el findAll(), meter paginación y sacar el calculo pesado a un proceso asincrono.

