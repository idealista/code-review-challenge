Autor: Alberto Ramírez Losilla
# Code review - Servicio para gestión de calidad de los anuncios

El presente fichero contiene la revisión sobre el código contenido en el repositorio.

Pese a entender que el código de repositorio tiene 5 años y que parece que es un POC (prueba de concepto), voy a exponer cambios que haría en un proyecto real partiendo del código del repositorio.

La revisión se organizará de lo más general a detalles más concretos.

## Versiones de Java y Spring Boot

Actualizaría Java y Spring Boot a una de las versiones más recientes: 
- Java 25
- Spring Boot 4.1.2

## Pom
Añadiría spring-boot-starter-test para evitar poner las dependencias junit y mockito a mano.

## Arquitectura

Aunque el proyecto está planteado desde la arquitectura hexagonal, haría una restructuración para desacoplar algunas capas y dividir más el código.

- Controller: 
  - Crearía un controller por endpoint. Con esto evitamos que un controller pueda crecer mucho y que distintos endpoints puedan compartir código. 
  - Además, crearía un mapper por controller (usando MapStruct), lo inyectaría en el controller, y evitaría que este trabajo lo haga la capa de servicio.

- Service: 
  - Los service no deben conocer el modelo de los controladores y debería devolver el modelo de dominio. Con esto desacoplamos esta capa y evitamos tener que tocarla si cambia el modelo del controller. 
  - Además, quitaría el mapeo del modelo de dominio al modelo del controller y añadiría esta lógica en el controller (mapper con MapStruct).
  - El service se puede cambiar por UseCase. Y un UseCase con un solo método. Con esto evitamos que una sola clase de servicio pueda crecer mucho.

- Facade: 
  - Añadiría una capa extra (Facade) entre la capa de Servicio/UseCase y los repository de base de datos. Con esto desacoplamos la capa de servicio de los repositorios, y hacemos transparente la tecnología/librería con la que se implementan estos repositorios.
  - En esta capa añadiría los mappers con MapStruct para transformar el modelo de base de datos al modelo de dominio.

- Repository:
  - Quitaría de aquí los mappers
  - Además, movería la interfaz AdRepository a la capa de persistencia.


## Librerías 

### Lombok

Añadiría lombok para evitar tener que añadir a mano código repetitivo.

- Modelo --> se pueden añadir las siguientes  anotaciones en las clases del modelo de dominio:
```
@Data
@AllArgsConstructor
@NoArgsConstructor 
@Builder
```
- Inyección de dependencias --> usar la anotación @RequiredArgsConstructor y marcar como private final las clases que necesitamos del contexto de Spring para inyectarlas por constructor

```
@RequiredArgsConstructor
@Service
public class AdsServiceImpl implements AdsService {

    private final AdRepository adRepository;
```


### MapStruct

Añadiría la librería de MapStruct para facilitar la implementación de los mappers:

```
<dependency>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct</artifactId>
    <version>1.6.3</version> 
</dependency>
```

```
@Mapper
```

## Bugs o detalles específicos

### Controller

Lo más importante en esta capa es que el método para calcular el score está implementado como un método get

```
@GetMapping("/ads/score")
public ResponseEntity<Void> calculateScore()
```
Al tener una operativa que calcula y guarda en base de datos el score, habría que cambiarlo por un método post.

Por otro lado, el método devuelve el HttpStatus 202 (Accepted). El problema es que no los calculan de forma asíncrona.

Opciones:

- Lo ideal es que el cálculo de los scores se haga de forma asíncrona.

- En caso contrarío, si no procesamos el cálculo de forma asíncrona, devolvería 200.

*No obstante, el planteamiento de procesar todos los scores mediante un endpoint no es muy escaladble. Lo ideal es que cuando se crea o modifica un anuncio, se calcule el score de forma individual.*

Por último, para facilitar la documentación y el consumo de la API, añadiría swagger (OpenAPI).

## Service

El método privado calculateScore(Ad ad) hace demasiadas cosas. Es poco mantenible, testeable, etc...

Hay dos propuestas, de menos a mas compleja:

1.En vez de tener un método que haga todos los cálculos, Lo dividiría en varios métodos para calcular la puntuación de las fotos, descripción, ... 

```
int calculatePicturesScore(Ad ad);
```

```
int calculateDescriptionScore(Ad ad);
```

*El problema es que si crece la forma de calcular el score con nuevas reglas, se hace poco mantenible*

2.Para solucionar esto, se puede aplicar el patrón estrategia.

Como podemos observar en el ejemplo de arriba, los reglas comparten una interfaz común.

```
public interface ScoreRule {
    int calculate(Ad ad);
}
```

Podemos crear una implementación para cada uno de los métodos anteriores que hemos creado.

```
@Component
public class PicturesRule implements ScoreRule {
    @Override
    public int evaluate(Ad ad) {
      ...
    }
}
```

La implementación de todas las reglas las podemos inyectar en la clase de servicio mediante un list, recorrerlas y sumar el resultado. 

```
@Service
@RequiredArgsConstructor
public class AdsServiceImpl {

    private final List<QualityRule> rules;

    public int calculateScore(Ad ad) {
        if (Objects.isNull(ad)) {
          return 0;
        }

        int score = rules.stream()
                .mapToInt(rule -> rule.evaluate(ad))
                .sum();

        return Math.max(0, Math.min(100, score));
    }
}
```

*Con esto conseguimos no tener que cambiar la clase de servicio cada vez que se añade una nueva regla.*

3.Por último y no menos importante, deberíamos cambiar la parte donde se comprueba si la descripción tiene palabras clave:

```
            if (wds.contains("luminoso")) score += Constants.FIVE;
            if (wds.contains("nuevo")) score += Constants.FIVE;
            if (wds.contains("céntrico")) score += Constants.FIVE;
            if (wds.contains("reformado")) score += Constants.FIVE;
            if (wds.contains("ático")) score += Constants.FIVE;
```

Para quitar tanto if, lo ideal es añadir las palabras clave en un list sin acentos y en minúsculas.

```
private static final List<String> WORDS = List.of("luminoso", "nuevo", "centrico", "reformado", "atico");
```

La descripción la podemos pasar a minúsculas y normalizar para poder afinar el algoritmo:

```
String normalizedDesc = Normalizer.normalize(description.toLowerCase(), Normalizer.Form.NFD);
```

Por último, recorremos el list de palabras clave y comprobamos si la descripción contiene cada una de las palabras clave:
```
long matches = KEYWORDS.stream()
                    .filter(normalizedDesc::contains)
                    .count();
```

## Model

- Cambiaría las fechas a LocalDate

- En el modelo del controller usaría herencia, ya que PublicAd y QualityAd comparten la mayoría de propiedades.

## Repository

- Para la POC  es aceptable utilizar la memoria para guardar los datos de ejemplo. De cara a un microsevicio real, usaría Spring Data. 

```
spring-boot-starter-data-jpa
```
*Esto nos ayudaríapor ejemplo, a usar paginación en las búsquedas*

- Por otro lado, para quitar lógica de negocio en el reposotory, cambiaría los métodos :
```
List<Ad> findRelevantAds() 
List<Ad> findIrrelevantAds() 
```

Por:
```
List<Ad> findByScoreGreaterThanEqual(Integer score)
List<Ad> findByScoreLessThan(Integer score)
```

## Tests

Los test tienen una cobertura insuficiente. 

Por un lado solo se testea el método calculateScores() de la clase de servicio. Además, este test no comprueba si el cálculo del score es correcto. 

Por otro lado, añadiría test para el controller y el repository.