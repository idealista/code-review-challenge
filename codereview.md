# ESTRUCTURA DEL CÓDIGO

Lo primero que cambiaria sería la estructura de carpetas, ya que no se entiende muy bien

Pondría una jerarquía de carpetas más clara, por ejemplo:

    com.idealista
        .controller
        .iservice
        .service
        .model
        .repository
        .config

Integraría librerías como lombok para evitar todos los getters y setters que hay en las clases 
y spring-data para poder utilizar hibernate.



También crearía una clase de configuración para la base de datos, 
ya que no se entiende muy bien como se conecta a la base de datos.


# SERVICE

* En cuanto a servicio, eliminaría el autowired de la clase service y lo haría por constructor, 
ya que es una buena práctica y se puede hacer de forma más sencilla.

* Se están utilizando muchos ifs, lo que hace que el código sea poco legible y difícil de mantener.
También se están utilizando muchos bucles for que se podrían sustituir perfectamente por stream.
Las constantes las metería en variables del entorno, para poder cambiarlo cuando se quiera de manera más
sencilla. También meter las palabras "luminoso", "nuevo", "ático", etc.

* El método calculateScore es demasiado complejo y debería ser
dividido en varios todos.

* Se debería crear una clase de excepciones para poder manejar mejor los errores.


# CONTROLLER

* En cuanto al controller, lo primero que haría sería eliminar el autowired y hacerlo por constructor.

* Habría que hacer que la ruta de la clase sea directamente "/ads" asi no tendríamos que repetir la 
palabra adds en cada getMapping como en "@GetMapping("/ads/quality")" 

* Hacer un control de los resultados que se devuelven, ya que si no se devuelve nada se devuelve un 200, 
lo cambiaría por un 204 NO_CONTENT.

* No me tiene mucho sentido el método calculateScore, ¿por qué utilizar un getMapping para calcular 
la puntuación y que siempre devuelva un 202?



# REPOSITORY

* En cuanto al repository lo reharía completamente de nuevo añadiendo spring data para poder utilizar
hibernate y no tener que hacer las consultas a mano, esto haría el repositorio mucho más legible y no tendríamos
que añadir a mano cada cosa que se vaya a consultar.


# MODEL 

* Añadiria las etiquetas de lombok a las clases para evitar todos los getters y setters que hay en las clases.

* También añadiría la etiqueta @Entity a las clases que son entidades de la base de datos.

* También añadiría la etiqueta @Table a las clases que son entidades de la base de datos para poder
hacer las consultas de forma más sencilla.

* También añadiría la etiqueta @Id a los atributos que son claves primarias de la base de datos.
* También añadiría la etiqueta @GeneratedValue a los atributos que son claves primarias de la base de datos
para poder hacer las consultas de forma más sencilla.

# OTRO TIPO DE MEJORAS

* Añadiría un logger para poder ver los errores que se producen en la aplicación y poder hacer un seguimiento. 

# TESTING 

Los test unitarios son bastante pobres, haría un test por cada uno de los módulos que se quieren probar
quedando algo parecido a:

    (test)com.idealista
            .controller
            .service

Con este cambio podríamos probar cada uno de los módulos de forma independiente.








