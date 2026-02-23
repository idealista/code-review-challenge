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

### Repositorio (AdRepository)
El repositorio en una arquitectura hexagonal debería ser un puerto de salida bastante "tonto" (guardar y recuperar). Si el repo tiene metodos como `findRelevantAds()` / `findIrrelevantAds()` (filtrando por score >= 40), esta metiendo una regla de negocio en persistencia.

**Sugerencia:** dejar el repo con cosas tipo `findAll()` / `saveAll()` y que el servicio o el dominio decidan que es relevante. Si fuese JPA, se puede traducir a SQL igual, pero sin que el dominio dependa de `Specification` de Spring

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

Además estamos devolviendo `PublicAd`/`QualityAd` que son DTOs de la capa de infraestructura, eso rompe por completo como se relaciona la arquitectura hexagonal. Debería devolver `Ad` que es el que está en dominio y que el controller haga el mapeo

**Sugerencia:** Pasar a **inyección por constructor**. También, renombrar `AdsService` a `AdService` (singular), siguiendo las convenciones habituales. Hacer que el service devuelva al controller `Ad` en lugar de los DTOs

### Refactorización de calculateScore()
El método `calculateScore` en `AdsServiceImpl` es una "función Dios". Hace de todo: puntúa fotos, analiza descripciones, comprueba palabras clave... es demasiado largo

Además, hay errores de lógica graves:
```java
// Error: Está sobreescribiendo el score acumulado en vez de sumarlo
if (ad.isComplete()) {
    score = Constants.FORTY;
}
```
**Sugerencia:** Dividir en métodos privados con una única responsabilidad: `calculatePhotoScore()`, `calculateDescriptionScore()`... Para el tema de las palabras clave, en lugar de mil `if`, usar una lista y normalizar el texto (quitar acentos y pasar a minúsculas) para que la búsqueda sea robusta utilizando `Normalizer`.
Tambien el `split(" ")` cambiarlo por `split("\\s+")` para tener en cuenta saltos de linea, doble espacio y tabulaciones.
En cuanto a la fecha no puede recalcularse siempre, habría que comprobar que no sea null
Habría que cambiar
```java
//Sobreescribe la fecha cada vez que se calcula score
if (ad.getScore() < Constants.FORTY) {
        ad.setIrrelevantSince(new Date());
        } else {
        ad.setIrrelevantSince(null);
        }
        
```
**Sugerencia:** Deberia, además de ser LocalDate, no poder sobreescribirse, es decir, comprobar en código si es null y en caso de serlo puede settearlo

Tambien hay otro bug tonto pero importante: el listado de anuncios se debe devolver de mejor a peor, y normalmente se hace un `sort(Comparator.comparing(Ad::getScore))` que ordena ascendente. Hay que hacer `reversed()`

Y las palabras clave no solo es normalizar, es que si no normalizas ahora mismo ni matchea por mayusculas ni por acentos (por ejemplo "Céntrico" vs "centrico").

Y por ultimo, `calculateScore` deberia calcular, no guardar. Si dentro hace `adRepository.save(ad)` en un bucle, eso en BBDD real es N escrituras y te puede dejar el sistema a medias si falla una. Lo normal es calcular todo y hacer `saveAll()` con una transacción (`@Transactional`).

Para que la puntuación se mantenga en rango está necesitando demasiadas lineas de codigo ineficiente:
```java
ad.setScore(score);

        if (ad.getScore() < Constants.ZERO) {
            ad.setScore(Constants.ZERO);
        }

        if (ad.getScore() > Constants.ONE_HUNDRED) {
            ad.setScore(Constants.ONE_HUNDRED);
        }
```
**Sugerencia:** Utilizar `int score = Math.max(0, Math.min(100, calculatedScore));`


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

El mappeo tiene un `...orElse(null)` en pictures, cuando no encuentra una imagen mete un null en la lista de fotos. Al no haber control de errores puede dar un NPE

Otra cosa aqui: el mapeo de `Ad` a DTOs (`PublicAd` / `QualityAd`) si se hace en el service, estas mezclando aplicación con infraestructura. Mejor que el service devuelva dominio (`Ad`) y el controller use un mapper (manual o MapStruct) y ya.

---

## Testing y Calidad

Actualmente el proyecto está "desnudo" en cuanto a tests:
*   Solo hay **un test unitario**. Necesitamos cubrir todos los casos borde del scoring (anuncios sin fotos, descripciones nulas, puntuaciones máximas...)
*   **Cero tests de integración.** No sabemos si el flujo completo desde el Controller hasta el Repo funciona bien
*   **Falta documentación de API.** Un `api.yml` o Swagger es fundamental para que el Front sepa que esperar, de hecho si se trabaja con API First deberia ser lo primero en crearse
*   **Falta Logs.** Son necesarios logs con contexto por toda la aplicación

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
  **¿Qué cambiaria?**: Usaría JPA y doveolveria el listado paginado con `Pageable`, no creo que nadie quiera traer 10.000 resultados de golpe. Además seria necesario añadir `@Transactional` para asegurar que se guardan todos los scores o ninguno si hay un fallo. Tener `WHERE score >= threshold` + `ORDER BY score DESC` + `Pageable` para optimizar la busqueda.
* **Alto tráfico**: Imaginando que este prototipo recibiera un trafico grande habría que hacer algunos cambios en la lógica. El endpoint actual para calcular el score, que deberia ser post, recalcula todos los resultados de forma sincrona, si fueran muchos resultados esto daria un timeout. Lo ideal seria devolver un 202 y que el calculo se hiciera asincrono con @Async, sin embargo parece más coherente que el score se calcule individualmente cuando un anuncio se cree o se edite en lugar de el calculo masivo.
* **Caché**: Si es un listado muy leido se debería usar caché y recargar cuando se recalculan los scores.

*Resumen:* Para esta prueba, lo que propuse anteriormente cumple su función sin volverse loco, pero si no es un prototipo y esto deberia ir a producción para millones de usuarios el primer paso sería quitar el findAll(), meter paginación y sacar el calculo pesado a un proceso asincrono.