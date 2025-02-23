# Code Review

## Estructura del proyecto

Aunque sigue el estándar de la arquitectura hexagonal (capas infraestructura, aplicación y dominio), no sigue ninguna
otra forma de organización y las clases están apelotonadas, cambiaria esto con una jerarquía que distinga las clases por
tipología o funcionalidad. Por ejemplo:

```
src
├── main
│   ├── java
│   │   └── com.idealista
│   │       ├── application
│   │       │   └── service
│   │       │       ├── AdsService
│   │       │       └── AdsServiceImpl
│   │       │   
│   │       ├── domain
│   │       │   ├── constants
│   │       │   │   ├── Constants  
│   │       │   │   ├── Quality
│   │       │   │   └── Typology 
│   │       │   │
│   │       │   ├── model
│   │       │   │   ├── Ad
│   │       │   │   └── Picture
│   │       │   │       
│   │       │   └── repository
│   │       │       └── AdRepository
│   │       │        
│   │       └── infrastructure
│   │           ├── api
│   │           │   ├── adapter
│   │           │   │   └── AdsController
│   │           │   │                     
│   │           │   └── model
│   │           │       ├── PublicAd
│   │           │       └── QualityAd
│   │           │ 
│   │           └── persistence
│   │               ├── adapter
│   │               │   └── InMemoryPersistence
│   │               │                     
│   │               └── model
│   │                   ├── AdVO
│   │                   └── PictureVO
│   │            
│   └── resources
│       └── application.yml
```

## Mejoras en el código

1. En todos los modelos de datos (PictureVO, AdVO, PublicAd, QualityAd, Picture, Ad) podría reducirse significativamente
   el número de líneas de código si implementamos el uso de la libreria Lombok, esto podría ser contraproducente según
   el caso porque añadiríamos una dependencia al proyecto, pero en este caso me parece que ganaríamos bastante
   legibilidad y simpleza en el código
2. Actualmente se está empleando la anotación @Autowired para la inyección de dependencias, considero que sería mejor
   usar la inyección a través del constructor, con esto conseguimos la inmutabilidad de las dependencias.
3. En la clase AdsServiceImpl hay un error en los principios de la arquitectura hexagonal, esta clase se encuentra en la
   capa de aplicación y está haciendo uso de las clases PublicAd y QualityAd, las cuales se encuentran en la capa de
   infraestructura, la capa de aplicación no debería tener constancia alguna de la capa de infraestructura. Las
   dependencias deberían de ser:
   Infraestructura -> Aplicación -> Dominio.
   Siempre en ese sentido y nunca al revés, la capa de infraestructura puede conocer la de aplicación y la de dominio.
   La capa de aplicación solo puede conocer la de dominio y la de dominio no puede saber que existe ninguna de las dos
   anteriores.
   Para solucionar esto, habria que mover los modelos PublicAd y QualityAd a la capa de aplicación y crear sus
   correspondientes homónimos en la capa de infraestructura, luego los endpoints que hacen las llamadas correspondientes
   a la capa aplicación deberían encargarse de mapear los modelos resultantes de la llamada a la capa de aplicación a
   los modelos que se enviarán como respuesta de la llamada rest en la capa de infraestructura.
4. Con respecto al punto anterior y para asegurarnos de que cada capa solo tiene visibilidad de las que le corresponden,
   podríamos convertir el proyecto en uno multimódulo, añadiendo al pom correspodiente de cada módulo como dependencias
   aquellas capas a las que tenga acceso, así si intentamos acceder a alguna clase que no corresponde ya el propio
   compilador no nos la detectaría. Para este caso y teniendo en cuenta lo poco extenso que es el proyecto sería
   complicarlo mucho, pero existe esa posibilidad.
5. En la clase AdVo se ha declarado un atributo "pictures" de tipo List<Integer>, que se sobreentiende que hace
   referencia a los ids de las imágenes, igual sería más descriptivo algo del estilo "pictureIds", pero lo importante
   aquí es que en vez de una lista podríamos usar un Set<Integer>, para evitar duplicidades.
6. Lo mismo para las clases PublicAd y QualityAd, ambas contienen un atributo List<String> llamado pictureUrls, podemos
   sustituirlos por Set<String> para evitar duplicidades.
7. En la clase de testing AdsServiceImplTest simplemente se están verificando que se llamen un número de veces
   determinado a los métodos que se utilizan en el proceso a testear, en ningún momento se está verificando que el
   resultado o los datos de salida del proceso sean los correctos o esperados.
8. Haría falta añadir más tests unitarios para verificar todas las causuísticas y aumentar la cobertura, actualmente es
   muy deficiente y no se están contemplando casos de error.
9. En la clase InMemoryPersistence se podrían declarar como final los atributos ads y pictures, ya que en ningún momento
   parece que se vaya a hacer una reasignacion de sus referencias a otros objetos en memoria y no afectaría a la
   funcionalidad actual del programa.
10. En la clase InMemoryPersistence#80 e InMemoryPersistence#100 se podrían extraer esos dos métodos (mapToDomain y
    mapToPersistence) a una nueva clase AdMapper independiente, y si queremos hilarlo más fino, podríamos contemplar la
    posibilidad de implementar esta nueva clase usando la dependencia mapstructs para reducir el número de líneas de
    código.
11. En la clase InMemoryPersistence#91 e InMemoryPersistence#111 se podrían extraer esos dos métodos (mapToDomain y
    mapToPersistence) a una nueva clase PictureMapper independiente, e igual que en el punto anterior también se podría
    contemplar la posibilidad de usar mapstruct para ello.
12. El método mapToDomain de InMemoryPersistence#91 incumple el principio de responsabilidad única, está por un lado
    buscando una imagen concreta y por otro mapeando el resultado, lo ideal sería separarlo en dos metodos, uno que haga
    la busqueda y otro el mapeo.
13. En la clase AdsController, el path "/ads/" es común a los tres endpoints, se podría extraer usando la etiqueta
    @RequestMapping(path = "ads") a nivel de clase.
14. Se podría implementar un ControllerAdvice global para el manejo de errores.
15. En la clase AdsController se podría mejorar la documentación del controller con el uso de Spring Doc y Swagger.
16. En la clase AdsServiceImpl#findPublicAds se está haciendo un mapeo directamente en ese metodo de un objeto Ad a uno
    PublicAd, esto podría extraerse a una nueva clase mapper que permita la reutilización del código.
17. En la clase AdsServiceImpl#findQualityAds se está haciendo un mapeo directamente en ese metodo de un objeto Ad a uno
    QualityAd, esto podría extraerse a una nueva clase mapper que permita la reutilización del código.
18. En la clase AdsServiceImpl#89 esa comprobación es innecesaria, description siempre tendrá valor ya que se está
    comprobando en la línea 86 que el valor que se le asigna a esta variable esté presente en el optional, si no tiene
    valor no llegara a la línea 89.
19. En la clase AdsServiceImpl#calculateScore, aunque se están usando constantes, se podría calificar de magic numbers
    los valores utilizados, en vez de:
    ```
      //Calcular puntuación por fotos
        if (ad.getPictures().isEmpty()) {
            score -= Constants.TEN; //Si no hay fotos restamos 10 puntos
        } else {
            for (Picture picture: ad.getPictures()) {
                if(Quality.HD.equals(picture.getQuality())) {
                    score += Constants.TWENTY; //Cada foto en alta definición aporta 20 puntos
                } else {
                    score += Constants.TEN; //Cada foto normal aporta 10 puntos
                }
            }
        }
    ```
    Podríamos usar variables con un nombre más descriptivo que eviten usar comentarios y dejen un código más legible:
     ```
      private static final int PENALIZACION_SIN_FOTOS = Constants.TEN;
      private static final int BENEFICIO_FOTO_HD = Constants.TWENTY;
      private static final int BENEFICIO_FOTO_NORMAL = Constants.TEN;
    
      //Calcular puntuación por fotos
        if (ad.getPictures().isEmpty()) {
            score -= PENALIZACION_SIN_FOTOS; 
        } else {
            for (Picture picture: ad.getPictures()) {
                if(Quality.HD.equals(picture.getQuality())) {
                    score += BENEFICIO_FOTO_HD;
                } else {
                    score += BENEFICIO_FOTO_NORMAL;
                }
            }
        }
      ```
    Este es un ejemplo, pero podríamos aplicar la misma lógica para el resto del código.
20. En la clase InMemoryPersistence#constructor se están instanciando objetos de tipo Ad con el Score con valor null, no
    se está validando en ningun lado que esto pueda suceder o no y luego se llama al método AdVO#getScore() cuando se
    ejecuta el calculo de la puntuación sin verificar si el valor puede ser nulo o no, por lo que es muy probable que dé
    un NullPointerException, un ejemplo de esto sería AdsServiceImpl#124, AdsServiceImpl#128 y AdsServiceImpl#132.
21. En AdsServiceImpl#110 a AdsServiceImpl#114 intentar evitar los valores literales, crear constantes en su lugar.
22. Estaría bien añadir javadoc para explicar los diferentes métodos, su funcionalidad, sus parametros, posibles excepciones, etc.
23. No existen logs en la aplicación, estaría bien implementarlos.

### Mantenimiento, escalabilidad y estándarización.
1. Dockerizar el servicio mediante un Dockerfile y docker-compose para facilitar la ejecución del servicio en local.
2. Añadir un checkstyle para unificar criterios de estilo entre los desarrolladores del equipo.
3. Uso de maven wrapper para estándarizar una version concreta de maven entre los miembros del equipo.
4. Versión de Spring Boot y Java bastante desactualizadas, habría que mirar de actualizar.


