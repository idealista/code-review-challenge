# Reto: Servicio para gestión de calidad de los anuncios

[![Build Status](https://travis-ci.org/idealista/coding-test-ranking.svg?branch=master)](https://travis-ci.org/idealista/coding-test-ranking)

Este repositorio contiene el API de un servicio que se encarga de medir la calidad de los anuncios. Tu objetivo será realizar un code review sobre el código contenido en el repositorio. Para ello, te proporcionamos la descripción de la tarea que habría recibido el equipo de desarrollo.

Los supuestos están basados en un hipotético *equipo de gestión de calidad de los anuncios*, que demanda una serie de verificaciones automáticas para clasificar los anuncios en base a una serie de características concretas.

## Historias de usuario

* Yo, como encargado del equipo de gestión de calidad de los anuncios quiero asignar una puntuación a un anuncio para que los usuarios de idealista puedan ordenar anuncios de más completos a menos completos. La puntuación del anuncio es un valor entre 0 y 100 que se calcula teniendo en cuenta las siguientes reglas:
  * Si el anuncio no tiene ninguna foto se restan 10 puntos. Cada foto que tenga el anuncio proporciona 20 puntos si es una foto de alta resolución (HD) o 10 si no lo es.
  * Que el anuncio tenga un texto descriptivo suma 5 puntos.
  * El tamaño de la descripción también proporciona puntos cuando el anuncio es sobre un piso o sobre un chalet. En el caso de los pisos, la descripción aporta 10 puntos si tiene entre 20 y 49 palabras o 30 puntos si tiene 50 o más palabras. En el caso de los chalets, si tiene más de 50 palabras, añade 20 puntos.
  * Que las siguientes palabras aparezcan en la descripción añaden 5 puntos cada una: Luminoso, Nuevo, Céntrico, Reformado, Ático.
  * Que el anuncio esté completo también aporta puntos. Para considerar un anuncio completo este tiene que tener descripción, al menos una foto y los datos particulares de cada tipología, esto es, en el caso de los pisos tiene que tener también tamaño de vivienda, en el de los chalets, tamaño de vivienda y de jardín. Además, excepcionalmente, en los garajes no es necesario que el anuncio tenga descripción. Si el anuncio tiene todos los datos anteriores, proporciona otros 40 puntos.

* Yo como encargado de calidad quiero que los usuarios no vean anuncios irrelevantes para que el usuario siempre vea contenido de calidad en idealista. Un anuncio se considera irrelevante si tiene una puntación inferior a 40 puntos.

* Yo como encargado de calidad quiero poder ver los anuncios irrelevantes y desde que fecha lo son para medir la calidad media del contenido del portal.

* Yo como usuario de idealista quiero poder ver los anuncios ordenados de mejor a peor para encontrar fácilmente mi vivienda.

## Consideraciones importantes

Para nosotros, lo importante de este code review es entender como piensas. Queremos saber que consideras importante y que no, que crees que podría mejorarse y que crees que está bien como está. 

Si tienes dudas entre comentar algo porque es algo que en un proyecto real no comentarías, hazlo. Sabemos que en este code review falta un montón de contexto sobre consensos que tendrías con tu equipo en una situación real, por lo que cualquier comentario nos va a servir mejor para entenderte.

No queremos que dediques tiempo a refactorizar el código, pero si hay algún comentario que quieres hacer y no sabes como explicarnos, nos puedes adjuntar código en cualquier lenguaje que conozcas (o pseudocódigo).

Para facilitar las cosas, cuando quieras referirte a alguna línea en concreto del código utiliza como nomenclatura algo parecido a NombreDeClase#lineaDeCódigo o NombreDeClase#NombreDeMétodo.

## Criterios de aceptación

Debes entregarnos un fichero de texto con todos los comentarios que harías sobre el código del repositorio.
