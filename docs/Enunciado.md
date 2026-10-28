**En un nuevo proyecto JDBCMySQL, crea una clase con un método main que actualice fila a fila, el salario de los empleados de la tabla emp.**

El proyecto debe tener en una carpeta librerias todas los .jar necesarios.
Utiliza un objeto ResultSet para recuperar los datos de los empleados, recorre de manera repetitiva este objeto y en función de las condiciones de abajo actualiza el salario del empleado como corresponda. La actualización de todas las filas debe constituir una única transacción.
A cada empleado sólo se aplica una subida, depende del oficio, departamento y antigüedad. Evalúa las condiciones en el orden siguiente:

* Los gerentes ( job = MANAGER) tienen una subida del 10%
* Los empleados con una antigüedad anterior al 01/09/81 tienen una subida del 15%
* Para el resto de los empleados:
    * Los analistas (job= ANALYST) del departamento 20 tienen una subida del 5%.
    * Los Vendedores del 8%.

TEN EN CUENTA EL ORDEN DE EVALUACION DE LAS CONDICIONES QUE SE PROPONE Y APLICA ÚNICAMENTE UNA SUBIDA DE SALARIO.
El script de creación de las tablas esta adjunto.

Como la corrección va a ser oral puedes usar la base de datos que consideres.

**Indicaciones de entrega. (13/11/2026)**
Comprime el proyecto que resuelve la tarea, incluidas todas las clases que se piden, las librerías importadas deberán estar en una carpeta dentro del propio proyecto.
El envío se realizará a través de la plataforma de la forma establecida para ello, y el archivo se nombrará siguiendo las siguientes pautas:
`apellido1_apellido2_nombre_3J_Tarea.zip`
Asegúrate que el nombre no contenga la letra ñ, tildes ni caracteres especiales extraños.

**Indicaciones de corrección oral (17/09/2026)**

Ten preparado el entorno y los datos cargados en la base de datos.

* Explica el proceso, las clases y su uso mas relevantes.
* Ten preparado un proceso, sql, sctript o código para recrear la base de datos con los datos iniciales cargados de manera rápida, por que hay que hacer varias pruebas con esos datos.
* Prueba 1: Con los datos originales desde un cliente tendrás que hacer una consulta para ver los datos originales, ejecutarás el código y volveremos a ver con la misma consulta cómo han cambiado los datos.
* Prueba 2: Con los datos originales, prepara el código para abortar la actualización (con una línea descomentada) para abortar la ejecución antes del commit y comprobar que no se actualiza la base de datos.
* Pruebas de gestión de errores: Modificaremos el código para hacer comprobaciones de que se manejan los errores de conexión a BBDD o setencias mal definidas, etc.