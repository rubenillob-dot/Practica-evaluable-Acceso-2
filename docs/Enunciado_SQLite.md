# Enunciado Adaptado a SQLite: Actualización de Salarios con JDBC (JDBCSQLite)

**En un nuevo proyecto JDBCSQLite, crea una clase con un método `main` que actualice fila a fila el salario de los empleados de la tabla `emp` utilizando una base de datos SQLite.**

---

### Requisitos del Proyecto y Conectividad

1. **Librerías:**
   - El proyecto debe tener una carpeta llamada `librerias` dentro del directorio del proyecto con todos los `.jar` necesarios para conectar con SQLite vía JDBC (driver `sqlite-jdbc-*.jar` y librerías auxiliares de log si son requeridas).
2. **Conexión a SQLite:**
   - Base de datos en archivo local (ejemplo: `empdept.db`).
   - Driver JDBC: `org.sqlite.JDBC`.
   - Cadena de conexión: `jdbc:sqlite:empdept.db` (o ruta relativa/absoluta correspondiente).
3. **Manejo de Transacciones (Requisito fundamental):**
   - La actualización de todas las filas debe constituir una **única transacción**.
   - Se debe desactivar el autocommit (`connection.setAutoCommit(false)`), realizar todas las actualizaciones fila a fila y hacer el `connection.commit()` al finalizar todo el recorrido.
   - Debe implementarse la captura de errores en bloque `try-catch` con `connection.rollback()` en caso de excepción para garantizar la atomicidad.

---

### Lógica de Negocio y Reglas de Actualización

Utiliza un objeto `ResultSet` para recuperar los datos de los empleados, recorre de manera repetitiva este objeto (`while (rs.next())`) y, en función de las condiciones descritas a continuación, actualiza el salario (`sal`) del empleado fila a fila (por ejemplo, mediante una sentencia `PreparedStatement` de tipo `UPDATE emp SET sal = ? WHERE empno = ?`).

> [!IMPORTANT]
> **A cada empleado sólo se aplica una subida**, dependiendo del oficio (`job`), departamento (`deptno`) y antigüedad (`hiredate`).  
> **Evalúa las condiciones estrictamente en el orden siguiente:**

1. **Gerentes (`job = 'MANAGER'`):** tienen una subida del **10%**.
2. **Empleados con antigüedad anterior al 01/09/1981 (`hiredate < '1981-09-01'`):** tienen una subida del **15%**.
3. **Para el resto de los empleados:**
   - **Analistas (`job = 'ANALYST'`) del departamento 20 (`deptno = 20`):** tienen una subida del **5%**.
   - **Vendedores (`job = 'SALESMAN'`):** tienen una subida del **8%**.

*Nota sobre fechas en SQLite:* En SQLite las fechas se almacenan en formato estándar ISO-8601 (`'YYYY-MM-DD'`). Puedes recuperarla en Java mediante `rs.getDate("hiredate")` o `rs.getString("hiredate")` y compararla con el corte `'1981-09-01'`.

---

### Scripts de Base de Datos

Se adjunta el script adaptado para SQLite en la carpeta `db/`: `db/emp_dept_salgrade_SQLITE.sql`.  
Dicho script:
- Habilita claves foráneas (`PRAGMA foreign_keys = ON;`).
- Elimina tablas existentes (`DROP TABLE IF EXISTS ...`).
- Crea las tablas `dept`, `emp`, `bonus` y `salgrade`.
- Inserta los datos iniciales con fechas en formato compatible ISO (`YYYY-MM-DD`).

Para recrear la base de datos `empdept.db` rápidamente en cualquier momento:
```bash
sqlite3 db/empdept.db < db/emp_dept_salgrade_SQLITE.sql
```
*(o ejecutando la clase auxiliar `RecrearBaseDatos` desde el propio editor).*

---

### Indicaciones de Entrega (13/11/2026)

Comprime el proyecto que resuelve la tarea, incluidas todas las clases que se piden; las librerías importadas deberán estar en la carpeta `librerias` dentro del propio proyecto.  
El envío se realizará a través de la plataforma de la forma establecida para ello, y el archivo se nombrará siguiendo las siguientes pautas:

```text
apellido1_apellido2_nombre_3J_Tarea.zip
```
*Asegúrate de que el nombre no contenga la letra 'ñ', tildes ni caracteres especiales extraños.*

---

### Indicaciones de Corrección Oral (17/09/2026)

Ten preparado el entorno y los datos cargados en la base de datos `empdept.db`:

1. **Explicación técnica:** Explica el proceso, las clases y su uso más relevante (conexión JDBC, `Statement`/`PreparedStatement`, `ResultSet`, control manual de transacción).
2. **Recreación rápida de datos:** Ten preparado un script o comando (`sqlite3 empdept.db < emp_dept_salgrade_SQLITE.sql` o reset desde código) para restaurar los datos iniciales al instante entre prueba y prueba.
3. **Prueba 1 (Flujo de éxito):**
   - Con los datos originales, realiza una consulta (vía consola sqlite3, SQLiteViewer, DBeaver o desde código) para ver los salarios originales.
   - Ejecuta el programa.
   - Vuelve a consultar para verificar cómo se han incrementado los salarios según las reglas establecidas.
4. **Prueba 2 (Demostración de Transacción / Rollback):**
   - Con los datos originales cargados, prepara el código para abortar la actualización (por ejemplo, con una línea descomentada que lance una excepción artificial antes del `commit()`).
   - Comprueba mediante consulta que la base de datos ha realizado el `rollback` y **ningún salario ha sido modificado**, manteniendo la integridad.
5. **Prueba 3 (Gestión de Errores):**
   - Verificaciones de manejo robusto de excepciones (por ejemplo, ruta incorrecta de la base de datos, bloqueo de archivo, sentencias SQL mal definidas o error de sintaxis).
