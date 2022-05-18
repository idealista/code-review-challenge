# Análisis del proyecto 

¡Buenas! Soy Alejandro Cancelo Correia y se me ha adjuntado esta prueba de código como
entrevista técnica para el puesto de desarrollador de backend en idealista.
Puesto mencionado https://www.linkedin.com/jobs/view/3062108675/?refId=8f2c31f8-ece3-4366-b7b3-876dddd9b285

Cada sección la usaré para explicar mi visión sobre cómo podría mejorarse el proyecto, 
qué cosas me parecen bien y qué cosas se podrían hacer distintas. 

## Estructura del código 

La organización del repositorio es importante y creo que este proyecto se podría haberse
beneficiado del uso de una arquitectura hexagonal.
La principal ventaja de implementar dicha arquitectura es que es fácil de entender, 
extender y de navegar sobre ella. Con el añadido de que si se usa como estándar en los 
varios productos que la empresa pueda tener, los desarrolladores serían más versátiles ya
que tendría un estándar que seguir y podrían cambiarse de proyecto en proyecto de forma 
rápida y sencilla.

Ejemplo:

```
com.idealista
            \_ adapter
                    \_ in
                        \_dto
                            PublicAd
                            QualityAd
                        AdsController
                    \_ out
                        InMemoryPersistence
                        \_dto
                            AdVO
                            PictureVO
            \_ boot
                \_ config (estaría vacío pero aquí se guardarían todas aquellas 
                           clases que queramos que spring genere un bean
                           por ejemplo, deberíamos guardar AdsService y 
                           AdsServiceImpl dentro de domain.application y crear
                           aquí una clase que genere un bean para la implementación
                           de la interfaz)
                Main
            \_ domain
                    \_ application
                            AdsService
                            AdsServiceImpl
                    \_ repository
                            AdRepository
                    \_ entity
                            Ad
                            Picture
                    \_ valueobject
                            Constants
                            Quality
                            Typology
```

Como se puede observar, esta organización también sigue el Domain Driven Design por lo 
que para un desarrollador nuevo, adecuarse al Ubiquitous Language será más fácil 
permitiendo que identifique al vuelo toda la terminología.

## API

La API que se expone al frontend, o al público en general, requiere de una tecnología que
permita llevar un seguimiento de dichos endpoints, con la que he trabajado yo se denomina 
Swagger u OpenApi cuyo uso se haría a través de un documento en formato YAML. 
La idea de este documento es que sirva como guía para revisar y entender la API de manera
rápida y precisa. Esta tecnología porta el valor añadido de que podemos integrar un plugin
con maven que nos permita generar automáticamente los dtos que especifiquemos en él.

La forma en la que integraría la tecnología sería para:
1. Especificar la variedad de endpoints que tenemos, los dtos que esperan recibir, los que
  se esperan devolver y el formato de los dtos en caso de errores.
2. Especificar todos los dtos que podemos recibir y devolver puesto que estos van a ser
  generados automáticamente
3. Para realizar un uso correcto de la arquitectura hexagonal, necesitamos que los 
  controladores sean implementaciones de interfaces, dichas interfaces se generarán 
  automáticamente gracias a la integración de OpenApi.


De manera añadida, Spring tiene una dependencia denominada SpringFox, la cual genera un 
html denominado swagger-ui.html con el que podemos ver en el navegador lo que se especifica
en el YAML de la API, lo que permite a los desarrolladores realizar llamadas rápidas en 
entornos bajos (entornos que no son de producción debido a que se puede desactivar la 
generación de este html) y no solo eso, sino que permite a los
QAs o testers saber en todo momento qué puede entrar en los endpoints y qué puede salir 
y automatizar los test acorde a esto.


## com.idealista.application

No veo conveniente el uso de los dtos que está en com.idealista.infrastructure.api, tanto 
la interfaz como la implementación deberían usar y devolver las entidades del dominio ya
que aquí es donde se emplea la business logic del producto, con la arquitectura hexagonal
habríamos capturado este fallo de organización al vuelo.

Todos los mapeos que se hacen de la clase Ad a las clases PublicAd y QualityAd se tienen
que realizar en el controlador o, si usamos la arquitectura hexagonal, en un mapper 
almacenado en el paquete adapter.in. 
PublicAd y QualityAd son las estructuras que vamos a devolver a los consumidores de la 
api, no es correcto que la usemos en durante la business logic.

Si usamos una clase mapper para el for each que se aplica en `AdsServiceImpl#findPublicAds`
y `AdsServiceImpl#findQualityAds`, podríamos simplificar el código a algo como 
```
ads.stream().map(call-to-mapper-method).collect(Collector.toList())
```

Y así poder tener más control sobre lo que se mapea sin tener que modificar la 
AdsServiceImpl cada vez que queramos modificar el mapeo de una clase a otra. Pero de nuevo,
esta clase de lógica debería de estar en el controlador o en adapter.in, si usamos la 
arquitectura hexagonal.

## com.idealista.domain

La clase `Constants` me hace dudar de su utilidad, tanto el nombre de las constantes como
su valor son algo fijo, es decir, que si en algún momento querríamos penalizar con -15 
puntos a anuncios que no tienen fotos, tendríamos que realizar una refactorización de
código extensa ya que tendríamos que sustituir el nombre de la constante (no tiene sentido
que se llame TEN) y cambiar el valor a 15. 
Lo que propongo en vez de tener una clase llamada `Constants`, podemos tener una clase 
utility (estática o no, ya depende del gusto de cada uno) en la que se aplique una 
penalización o un premio a la clase Ad.
Ejemplo:
En AdsServiceImpl#calculateScore
```
El valor de Ad#score tendría que ser 0 por defecto
if (ad.getPictures.isEmpty()) {
    ScoreJudge.applyPenalizationForEmptyPictures(ad)
}
```
Otra posible idea es que se llame al `ScoreJudge` dentro del calculateScore y que sea el
`ScoreJudge` el que haga las comprobaciones y asigne los valores.
Ejemplo 2:
En AdsServiceImpl#calculateScore
```
private void calculateScore(Ad ad) {
    ScoreJudge.testForPictures(ad)
    ScoreJudge.testForDescriptionLength(ad)
    ...
}
```

Las penalizaciones serían valores extraidas del application.yml, de esta forma la 
aplicación, es más extendible, testeable y nos permite realizar cambios sin tocar código.

## com.idealista.infrastructure.api

Como ya bien he mencionado, `AdsController` debería recibir de la business logic los 
objetos del dominio (Ad en este caso) y realizar los mapeos correspondientes (de Ad a 
PublicAd o QualityAd) con la ayuda de un mapper. 
También hay que tener en cuenta el control de errores por lo que propongo el uso de una
clase que tenga la anotación `@RestHandlerAdvice`, así si tenemos problemas como un fallo
de conexión con la Base de Datos, podríamos capturar el error y enviar mensajes correctos
a los consumidores. 
Ejemplo:
Si falla la conexión con la Base de Datos porque da un timeout devolveríamos un HTTP 503.

## com.idealista.persistence

Aquí, con la clase `InMemoryPersistence` vemos que se aplica un mapeo de los objectos que
van y vienen de la base de datos al dominio de la aplicación, este es un ejemplo en donde 
podemos ver el fuerte de la arquitectura hexagonal y a lo que me refiero con que la api
puesta en `AdsController` debe recibir objetos del dominio y devolver los objectos que van
a los consumidores.
Una mejora que veo aquí es delegar el mapeo de `AdVO` y `PictureVO` a una clase mapper
que se encargue de esto.

El filtro que se aplica en `InMemoryPersistence#findRelevantAds` y `InMemoryPersistence#findIrrelevantAds` 
debería de extraerse a un método predicado que sea algo como lo siguiente:
```
private Predicate<Ad> isRelevantAd() {
    return ad -> ad.getScore >= FORTY;
}
```
Para el uso en `InMemoryPersistence#findIrrelevantAds` simplemente filtramos con la 
negación del predicado anterior. 
FORTY sería una constante que podríamos sacar de una configuación almacenada en 
application.yml. Lo que nos permite realizar cambios en el valor de la relevancia sin 
tener que modificar código.

## Tests

Echo en falta test para `InMemoryPersistence` y para `AdsController`
