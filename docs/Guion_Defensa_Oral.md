# 🎤 GUION DEFENSA ORAL: EXPLICACIÓN MÉTODO A MÉTODO Y LÍNEA POR LÍNEA

Este documento contiene la explicación completa, método a método y línea por línea, de todo el código del proyecto en un lenguaje sencillo, claro y humano. Está pensado para que puedas defender la práctica con total soltura en la corrección oral.

---

## 📑 ÍNDICE
1. [Estrategia de la Corrección Oral (Las 5 Fases)](#1-estrategia-de-la-corrección-oral)
2. [Explicación Línea por Línea: `ActualizarSalarios.java`](#2-explicación-línea-por-línea-actualizarsalariosjava)
3. [Explicación Línea por Línea: `ConexionBD.java`](#3-explicación-línea-por-línea-conexionbdjava)
4. [Explicación Línea por Línea: `RecrearBaseDatos.java`](#4-explicación-línea-por-línea-recrearbasedatosjava)
5. [Preguntas Frecuentes del Profesor y Respuestas](#5-preguntas-frecuentes-del-profesor)
6. [Tabla Resumen de Comprobación de Empleados](#6-tabla-resumen-de-comprobación)
7. [Qué Entregar en el Archivo ZIP](#7-qué-entregar-en-el-archivo-zip)

---

## 1. Estrategia de la Corrección Oral

En la corrección oral sigue este guion de 5 pasos:

1. **Introducción (30 segundos):**
   > *"Esta práctica actualiza fila a fila el salario de los empleados de la tabla `emp` en SQLite según las condiciones del enunciado (oficio, departamento y antigüedad), asegurando que todas las actualizaciones se realicen dentro de una **única transacción atómica**."*

2. **Presentación de la arquitectura:**
   > *"He organizado el proyecto en tres clases:*
   > * `ConexionBD.java`: Centraliza la conexión JDBC y permite resetear los datos al estado original.
   > * `ActualizarSalarios.java`: Clase principal con el método `main`, la consulta `ResultSet`, la evaluación de subidas y el control transaccional (`commit` / `rollback`).
   > * `RecrearBaseDatos.java`: Acceso directo para restaurar la base de datos limpia con un solo clic."*

3. **Prueba 1 (Flujo con éxito):**
   Pulsas **Run** en `ActualizarSalarios.java`. Muestras la tabla con los sueldos originales, el desglose de cada empleado con el motivo de su subida y la tabla final con los salarios actualizados tras el commit (10 empleados suben y 4 se mantienen).

4. **Prueba 2 (Demostración de Rollback):**
   Ejecutas `RecrearBaseDatos.java` para dejar la BD limpia. Luego vas a `ActualizarSalarios.java`, descomentas la línea de la excepción simulada y ejecutas `ActualizarSalarios.java`.
   > *"Como salta un error antes del commit, se ejecuta el catch con `conn.rollback()`. Como se ve en la tabla final, ningún salario se ha modificado."*
   Vuelves a comentar la línea.

5. **Prueba 3 (Gestión de errores):**
   Explicas que los bloques `try-catch` capturan cualquier fallo de conexión o sentencia SQL mal escrita, impidiendo que la aplicación reviente.

---

## 2. Explicación Línea por Línea: `ActualizarSalarios.java`

---

### A) Importaciones y Constante de Fecha

```java
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
```
* **Líneas 1-8:** Importamos las clases de JDBC para la base de datos (`Connection`, `Statement`, `PreparedStatement`, `ResultSet`, `SQLException`), la clase `LocalDate` para manejar y comparar fechas, y `BigDecimal` con `RoundingMode` para redondear el dinero a 2 decimales sin fallos de coma flotante.

```java
public class ActualizarSalarios {
    private static final LocalDate FECHA_CORTE = LocalDate.of(1981, 9, 1);
```
* **Línea 15:** Creamos una constante con la fecha `1 de septiembre de 1981`.
* **Por qué está ahí:** El enunciado pide evaluar la antigüedad anterior al 01/09/1981. Ponerla en una constante evita escribir la fecha a mano varias veces y hace el código más limpio.

---

### B) Estructura Auxiliar `DatosSubida`

```java
    private static class DatosSubida {
        final double porcentaje;
        final String motivo;

        DatosSubida(double porcentaje, String motivo) {
            this.porcentaje = porcentaje;
            this.motivo = motivo;
        }
    }
```
* **Líneas 18-26:** Es una pequeña clase interna que guarda dos datos juntos: el porcentaje numérico de la subida (ej: `10.0`) y el motivo en texto (ej: `"Puesto de gerente (MANAGER)"`).
* **Por qué está ahí:** Permite que el método de cálculo devuelva tanto el número como la explicación humana de por qué le corresponde esa subida.

---

### C) Método `main(String[] args)`

```java
    public static void main(String[] args) {
        System.out.println("===============================================================================");
        System.out.println("         ACTUALIZACIÓN DE SALARIOS EN LA TABLA EMP (JDBC + SQLite)             ");
        System.out.println("===============================================================================");

        try (Connection conn = ConexionBD.getConnection()) {
```
* **Líneas 28-33:** Imprimimos la cabecera e iniciamos la conexión con `ConexionBD.getConnection()` dentro de un bloque `try-with-resources`.
* **Qué decirle al profesor:** *"Uso try-with-resources para que la conexión se cierre automáticamente al terminar, evitando fugas de memoria o bloqueos en el archivo SQLite."*

```java
            // Paso 1: Vemos los salarios que hay en la base de datos antes de hacer nada
            System.out.println("\n[1] Empleados antes de la actualización:");
            mostrarEmpleados(conn);

            // Paso 2: Recorremos los empleados fila a fila y aplicamos las subidas
            System.out.println("\n[2] Procesando empleados fila a fila...");
            actualizarSalarios(conn);

            // Paso 3: Volvemos a consultar para comprobar cómo han quedado los salarios
            System.out.println("\n[3] Empleados tras la transacción:");
            mostrarEmpleados(conn);

        } catch (SQLException e) {
            System.err.println("No se pudo conectar con la base de datos: " + e.getMessage());
        }
    }
```
* **Líneas 35-50:** Estructura visual para la Prueba 1: muestra la tabla original, ejecuta la actualización fila a fila y muestra la tabla final resultante. Si falla la conexión, el `catch` captura la excepción e informa del error.

---

### D) Método `actualizarSalarios(Connection conn)`

```java
    public static void actualizarSalarios(Connection conn) {
        String consultaSelect = "SELECT empno, ename, job, hiredate, sal, deptno FROM emp";
        String sentenciaUpdate = "UPDATE emp SET sal = ? WHERE empno = ?";
```
* **Líneas 54-55:** Definimos la consulta `SELECT` para leer todos los datos de los empleados y la plantilla `UPDATE` con interrogaciones `?` para modificar el salario de un empleado concreto por su `empno`.

```java
        try {
            // Desactivamos el autocommit para que todas las filas formen una única transacción
            conn.setAutoCommit(false);
```
* **Líneas 57-59:** Desactivamos el autocommit.
* **Qué decirle al profesor (CLAVE):** *"Esta línea cumple la exigencia del enunciado de que la actualización de todas las filas constituya una **única transacción**. Con `setAutoCommit(false)` le indicamos a JDBC que retenga los cambios en memoria y no los guarde fila a fila de forma aislada."*

```java
            try (Statement stmtSelect = conn.createStatement();
                 ResultSet rs = stmtSelect.executeQuery(consultaSelect);
                 PreparedStatement stmtUpdate = conn.prepareStatement(sentenciaUpdate)) {

                int subidasAplicadas = 0;
                int sinCambios = 0;
```
* **Líneas 61-67:** Creamos el `Statement` para ejecutar el `SELECT`, obtenemos el `ResultSet`, preparamos el `PreparedStatement` y creamos dos contadores para el resumen final.
* **Por qué PreparedStatement:** *"Es mucho más eficiente porque la base de datos compila la sentencia una sola vez y luego solo le pasamos los parámetros dentro del bucle, además de proteger contra inyecciones SQL."*

```java
                // Recorremos los empleados uno a uno con el ResultSet
                while (rs.next()) {
                    int empno = rs.getInt("empno");
                    String ename = rs.getString("ename");
                    String job = rs.getString("job");
                    String fechaStr = rs.getString("hiredate");
                    double salActual = rs.getDouble("sal");
                    int deptno = rs.getInt("deptno");
```
* **Líneas 69-76:** El bucle `while (rs.next())` avanza el cursor fila a fila sobre el `ResultSet` y extrae los datos de cada empleado con `getInt`, `getString` y `getDouble`.
* **Qué decirle al profesor:** *"Cumplo el requisito de recorrer repetitivamente el objeto ResultSet fila a fila."*

```java
                    LocalDate hiredate = (fechaStr != null) ? LocalDate.parse(fechaStr) : null;

                    // Evaluamos qué subida le corresponde según las reglas y su orden
                    DatosSubida subida = calcularSubida(job, hiredate, deptno);
```
* **Líneas 78-81:** Convertimos la fecha de texto ISO (`YYYY-MM-DD`) a `LocalDate` y llamamos al método que evalúa las condiciones.

```java
                    if (subida.porcentaje > 0) {
                        // Calculamos el nuevo sueldo y lo redondeamos a 2 decimales
                        double nuevoSalario = salActual * (1.0 + (subida.porcentaje / 100.0));
                        nuevoSalario = BigDecimal.valueOf(nuevoSalario)
                                                 .setScale(2, RoundingMode.HALF_UP)
                                                 .doubleValue();

                        // Actualizamos este empleado concreto en la base de datos
                        stmtUpdate.setDouble(1, nuevoSalario);
                        stmtUpdate.setInt(2, empno);
                        stmtUpdate.executeUpdate();

                        subidasAplicadas++;
                        System.out.printf("  • %-7s (%-9s) -> +%4.1f%% [%s]%n",
                                ename, job, subida.porcentaje, subida.motivo);
                        System.out.printf("    Sueldo: %8.2f €  --->  %8.2f €%n",
                                salActual, nuevoSalario);
                    } else {
                        sinCambios++;
                        System.out.printf("  • %-7s (%-9s) -> Sin cambios [%s]%n",
                                ename, job, subida.motivo);
                        System.out.printf("    Sueldo: %8.2f € (se mantiene)%n",
                                salActual);
                    }
                }
```
* **Líneas 83-111:**
  * Si hay subida (`porcentaje > 0`): calculamos el sueldo nuevo, lo redondeamos con `BigDecimal` a 2 decimales, sustituimos los interrogantes del `PreparedStatement` (`setDouble(1, nuevoSalario)` y `setInt(2, empno)`), y ejecutamos el `executeUpdate()`.
  * Si no hay subida (`porcentaje == 0`): no tocamos la base de datos e informamos de que el empleado mantiene su sueldo.

```java
                // =========================================================================
                // PARA LA CORRECCIÓN ORAL (PRUEBA 2: DEMOSTRACIÓN DE ROLLBACK)
                // Si descomentas la siguiente línea, simulamos un fallo antes de guardar
                // para demostrar que el rollback deja todos los sueldos exactamente igual:
                // =========================================================================
                // if (true) throw new SQLException("Fallo simulado para comprobar que el rollback funciona.");

                // Si hemos llegado aquí sin errores, guardamos todos los cambios juntos
                conn.commit();
                System.out.println("\n-> Transacción confirmada (commit con éxito).");
                System.out.println("   Resumen: " + subidasAplicadas + " empleados con subida y " + sinCambios + " empleados sin cambios.");
```
* **Líneas 113-125:** La línea comentada de la Prueba 2. Si no hay error, se llama a `conn.commit()`, guardando todas las actualizaciones en disco a la vez.

```java
            } catch (SQLException e) {
                // Si salta cualquier error, cancelamos la transacción entera
                System.err.println("\n-> Ha ocurrido un error durante la actualización: " + e.getMessage());
                conn.rollback();
                System.err.println("-> Rollback ejecutado: no se ha modificado ningún dato en la base de datos.");
            } finally {
                // Volvemos a dejar el autocommit activado por defecto
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            System.err.println("Error en la transacción: " + e.getMessage());
        }
    }
```
* **Líneas 127-140:** Si salta una excepción en cualquier momento antes del commit, entramos al `catch` y ejecutamos `conn.rollback()`, cancelando todo. En el `finally` restauramos `setAutoCommit(true)` por buena práctica.

---

### E) Método `calcularSubida(String job, LocalDate hiredate, int deptno)`

```java
    public static DatosSubida calcularSubida(String job, LocalDate hiredate, int deptno) {
        if (job == null) {
            return new DatosSubida(0.0, "Sin puesto asignado");
        }

        // 1ª regla: Gerentes (MANAGER)
        if ("MANAGER".equalsIgnoreCase(job)) {
            return new DatosSubida(10.0, "Puesto de gerente (MANAGER)");
        }

        // 2ª regla: Antigüedad anterior al 01/09/1981
        // Nota: Si un vendedor o analista entró antes de esta fecha, entra aquí (15%)
        if (hiredate != null && hiredate.isBefore(FECHA_CORTE)) {
            return new DatosSubida(15.0, "Antigüedad anterior al 01/09/1981");
        }

        // 3ª regla: Para el resto de empleados
        if ("ANALYST".equalsIgnoreCase(job) && deptno == 20) {
            return new DatosSubida(5.0, "Analista del departamento 20");
        }

        if ("SALESMAN".equalsIgnoreCase(job)) {
            return new DatosSubida(8.0, "Puesto de vendedor (SALESMAN)");
        }

        // Si no cumple ninguna de las anteriores, mantiene su salario
        return new DatosSubida(0.0, "No cumple condiciones de subida");
    }
```
* **Líneas 147-176:** Evalúa las condiciones en el orden estricto del enunciado:
  1. Si es `MANAGER` &rarr; 10%. (BLAKE, CLARK y JONES suben el 10% aquí).
  2. Si entró antes del 01/09/1981 &rarr; 15%. (Vendedores antiguos como ALLEN y WARD entran aquí y suben el 15%, no el 8%).
  3. Para el resto: analistas del departamento 20 (5%) y vendedores (8%, como MARTIN y TURNER).
  4. Si no cumple nada (como KING o los oficinistas modernos) &rarr; 0%.
* **Qué decirle al profesor:** *"Al usar `return` en cada condición, garantizo que a cada empleado solo se le aplica una única subida, respetando la prioridad indicada."*

---

### F) Método `mostrarEmpleados(Connection conn)`

```java
    public static void mostrarEmpleados(Connection conn) throws SQLException {
        String sql = "SELECT empno, ename, job, hiredate, sal, deptno FROM emp ORDER BY empno";

        System.out.println("  -------------------------------------------------------------------------");
        System.out.printf("   %-5s | %-9s | %-10s | %-11s | %-11s | %-6s%n",
                "ID", "Nombre", "Oficio", "Fecha Alta", "Salario", "Depto");
        System.out.println("  -------------------------------------------------------------------------");

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                System.out.printf("   %-5d | %-9s | %-10s | %-11s | %9.2f € | %-6d%n",
                        rs.getInt("empno"),
                        rs.getString("ename"),
                        rs.getString("job"),
                        rs.getString("hiredate"),
                        rs.getDouble("sal"),
                        rs.getInt("deptno"));
            }
        }
        System.out.println("  -------------------------------------------------------------------------");
    }
```
* **Líneas 180-205:** Consulta y muestra por consola la tabla formateada con columnas alineadas mediante `printf`.

---

## 3. Explicación Línea por Línea: `ConexionBD.java`

* **`buscarArchivo(String nombre)`:** Busca un archivo en `db/`, en la raíz o en carpetas superiores. Sirve para que la ruta de `empdept.db` y del script SQL se encuentre sin importar desde dónde ejecute el editor.
* **`asegurarDriver()`:** Carga el driver de SQLite con `Class.forName("org.sqlite.JDBC")`. Si el entorno no tuviera configuradas las librerías, busca el `.jar` en `librerias/` y lo registra por código para que funcione siempre.
* **`getConnection()`:** Abre y devuelve la conexión activa usando la URL `jdbc:sqlite:...`.
* **`reiniciarBaseDatos()`:** Lee el archivo `emp_dept_salgrade_SQLITE.sql`, separa cada sentencia por el punto y coma (`;`) y la ejecuta en la BD para restaurar las tablas y los datos originales en 1 segundo.

---

## 4. Explicación Línea por Línea: `RecrearBaseDatos.java`

```java
public class RecrearBaseDatos {
    public static void main(String[] args) {
        System.out.println("Restaurando los datos iniciales de la base de datos...");
        ConexionBD.reiniciarBaseDatos();
        System.out.println("Listo. Ya puedes ejecutar ActualizarSalarios.");
    }
}
```
* **Qué decir:** *"Es un lanzador ejecutable directo para resetear la base de datos con un clic sobre el botón Run, ideal para repetir las pruebas de la corrección oral."*

---

## 5. Preguntas Frecuentes del Profesor

1. **¿Por qué usas `PreparedStatement` en vez de `Statement` para el `UPDATE`?**
   > *"Porque se ejecuta dentro de un bucle. `PreparedStatement` precompila la sentencia SQL una sola vez en el motor de base de datos y solo actualiza los parámetros en cada vuelta, siendo mucho más rápido y seguro contra inyección SQL."*

2. **¿Por qué pones `conn.setAutoCommit(false)`?**
   > *"Porque por defecto JDBC guarda cada modificación inmediatamente. Al desactivar el autocommit, agrupamos todas las filas en una única transacción atómica."*

3. **¿Cómo funciona el `rollback`?**
   > *"Si ocurre cualquier error antes del `commit()`, el bloque `catch` ejecuta `conn.rollback()`. Esto ordena a SQLite descartar todos los cambios acumulados y dejar la tabla exactamente como estaba antes de empezar."*

4. **¿Por qué la regla de antigüedad va antes que la de vendedor?**
   > *"Porque el enunciado dice textualmente que se respete el orden propuesto y se aplique solo una subida. Vendedores como ALLEN y WARD fueron contratados en febrero de 1981; al evaluar la antigüedad antes, reciben el 15% que les corresponde por ser antiguos en lugar del 8% de vendedor."*

5. **¿Por qué usas `BigDecimal`?**
   > *"Para evitar imprecisiones de céntimos causadas por la aritmética de coma flotante del tipo `double`. Con `BigDecimal.setScale(2, RoundingMode.HALF_UP)` aseguramos un redondeo bancario exacto."*

---

## 6. Tabla Resumen de Comprobación

| ID | Empleado | Puesto | Fecha Alta | Sueldo Inicial | Regla Aplicada | % Subida | Sueldo Final |
| :---: | :--- | :--- | :---: | :---: | :--- | :---: | :---: |
| **7839** | KING | PRESIDENT | 1981-11-17 | 5000,00 € | Ninguna | 0% | **5000,00 €** |
| **7698** | BLAKE | MANAGER | 1981-05-01 | 2850,00 € | 1ª Regla: Gerente | +10% | **3135,00 €** |
| **7782** | CLARK | MANAGER | 1981-06-09 | 2450,00 € | 1ª Regla: Gerente | +10% | **2695,00 €** |
| **7566** | JONES | MANAGER | 1981-04-02 | 2975,00 € | 1ª Regla: Gerente | +10% | **3272,50 €** |
| **7369** | SMITH | CLERK | 1980-12-17 | 800,00 € | 2ª Regla: Antigüedad | +15% | **920,00 €** |
| **7499** | ALLEN | SALESMAN | 1981-02-20 | 1600,00 € | 2ª Regla: Antigüedad | +15% | **1840,00 €** |
| **7521** | WARD | SALESMAN | 1981-02-22 | 1250,00 € | 2ª Regla: Antigüedad | +15% | **1437,50 €** |
| **7788** | SCOTT | ANALYST | 1987-07-13 | 3000,00 € | 3ª Regla: Analista dpto 20 | +5% | **3150,00 €** |
| **7902** | FORD | ANALYST | 1981-12-03 | 3000,00 € | 3ª Regla: Analista dpto 20 | +5% | **3150,00 €** |
| **7654** | MARTIN | SALESMAN | 1981-09-28 | 1250,00 € | 3ª Regla: Vendedor | +8% | **1350,00 €** |
| **7844** | TURNER | SALESMAN | 1981-09-08 | 1500,00 € | 3ª Regla: Vendedor | +8% | **1620,00 €** |
| **7876** | ADAMS | CLERK | 1987-07-13 | 1100,00 € | Ninguna | 0% | **1100,00 €** |
| **7900** | JAMES | CLERK | 1981-12-03 | 950,00 € | Ninguna | 0% | **950,00 €** |
| **7934** | MILLER | CLERK | 1982-01-23 | 1300,00 € | Ninguna | 0% | **1300,00 €** |

---

## 7. Qué Entregar en el Archivo ZIP

### Nombre exacto del archivo:
```text
apellido1_apellido2_nombre_3J_Tarea.zip
```
*(Sin tildes, sin letra 'ñ', sin espacios ni caracteres especiales).*

### Contenido obligatorio del ZIP:
* `src/`: con las 3 clases Java (`ActualizarSalarios.java`, `ConexionBD.java`, `RecrearBaseDatos.java`).
* `librerias/`: con los `.jar` de SQLite y logging (exigido en el enunciado).
* `db/`: con `empdept.db` y los scripts SQL.
* `docs/`: con los enunciados.
* Archivos `.vscode/`, `.classpath`, `.project` y `README.md`.

> **No incluir `bin/`:** Los archivos compilados `.class` no deben incluirse en el archivo de entrega.

### Comando para generar el ZIP:
```bash
# 1. Dejar la base de datos limpia en su estado inicial
java -cp "bin:librerias/*" RecrearBaseDatos

# 2. Generar el archivo ZIP de entrega
zip -r apellido1_apellido2_nombre_3J_Tarea.zip src librerias db docs .vscode .classpath .project README.md Guion.md
```
