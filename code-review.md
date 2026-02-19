Code Review

Primero vamos a echar un vistazo al pom.xml
Versiones viejas de Spring y Java
Comprobado: version de Spring obsoleta
Podría tratarse de una app legacy, sin embargo lo trataré como si fuera un posible error y sugeriré subir la versión
JUnit y mockito? Spring incorpora ambos en otra dependencia.
Comprobado: Efectivamente viene con spring-boot-starter-test
Falta Lombok

Parece que usa arquitectura hexagonal por las capas de domain, application e infraestructure.

Vamos con dominio primero:
Empezamos con Ad.java
Definitivamente se necesita Lombok
Utiliza Date en vez de LocalDate o LocalDateTime, podria deberse a ser algo viejo pero eso hay que cambiarlo 100%

El isComplete() chirria, se podria encargar el propio enum Typology de devolver estos datos

Seguimos con Constants.java

los nombres están mal, deberian indicar el nombre a que se refieren. Supongo que segun el enunciado "Si el anuncio no tiene ninguna foto se restan 10 puntos. Cada foto que tenga el anuncio proporciona 20 puntos si es una foto de alta resolución (HD) o 10 si no lo es" querrá decir que en Constants.java deberia poner los nombres como NO_PHOTO_SCORE = 10 y que sea un valor que se resta, en caso de foto HD que sea PHOTO_HD_SCORE = 20; por ejemplo

en Quality.java de momento nada que añadir

AdRepository.java 

usar un findAll y un save mejor, posiblemente con un spec y si el volumen es alto usar un @query de jpa

Vamos con capa Application:

AdsService.java renombrar! deberia ser AdService.java e igual para la implementación
devuelve una lista de PublicAd y QualityAd pero estan en infraestructura. Deberia devolver Ad en todo caso y mapearlos en el controller

AdsServiceImpl.java nombre mal
declara el repo con @Autowired, eso está mal

de entrada mucho código duplicado, se podría extraer la parte comun 

findPublicAds hace sort, pero por defecto es de menor a mayor a si que estará mostrando los de peor puntuación primero, todo lo contrario a lo que se pide

nada de logs?

calculateScore() es larguisimo, esto no tiene una unica responsabilidad
parece que habría que separarlo en los diferentes calculos, de hecho hasta viene comentado
sugerir un scoreForPhotos() scoreFotDescription()...

el uso de las contantes mal nombradas

el calculo de fotos parece bien

al calculo de descripción le quitaba el optional y en el if comprobaba que no sea null

el array wds hay que renombrarlo por ejemplo descriptionWords y el caracter separador deberia tener en cuenta saltos de lineas
lo de los tipology de nuevo se podria meter en el enum
los ifs se podrian simplificar, si es >50 else if >30 else y no hace falta tanto

lo de las palabras clave está mal planteado, habría que coger el array de wds y ponerlo todo en minuscula para que cuadre, si no no lo va a coger y tampoco si tiene acentos, no todo el mundo los escribe. Ademas deberian estar todos en una lista, en vez de tener una linea de codigo por cada palabra, siempre y cuando sumen lo mismo. 

Comprobado: el split bueno es "\\s+" con esto cubres espacio, salto de linea, doble espacio y tabulación
Comprobado: se puede usar java.text.normalizer para los acentos

Esto:
//Calcular puntuación por completitud
if (ad.isComplete()) {
score = Constants.FORTY;
}
Está igualando el score a 40, está mal

las tres siguientes llamadas a setScore() creo que sobran, se puede establecer una constante para el umbral, una de maximo y una de minimo. Math de java lo permite
Comprobado: Math lo permite con umbrales

el set del date esta cambiando siempre la fecha, deberia comprobar que no sea null. Deberia ser LocalDate como poco.

tiene un save individual? eso puede ser demasiado si no conocemos el volumen, lo sacaria fuera y haria un saveAll para hacer solo una trnsaccion con bbdd. Se deberia añadir @Transactional por si hay algun problema

en general esta clase hay que refactorizarla bastante.

Seguimos con AdsController

@Autowired de nuevo, es un error.
calculateScore no deberia tener verbo get, pondria post

no veo manejo de errores

seguimos con PublicAd
seguimos con QualityAd

ambas son muy similares, deberia heredar qualityAd de publicAd
