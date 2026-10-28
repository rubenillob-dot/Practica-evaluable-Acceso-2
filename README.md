# Práctica Evaluable Acceso a Datos 2: Actualización de Salarios con JDBC y SQLite

Este proyecto resuelve el ejercicio de actualización salarial fila a fila de la tabla `emp` mediante una **única transacción** con JDBC sobre **SQLite**.

---

## 📁 Estructura del Proyecto Organizada por Carpetas

```text
├── .vscode/                         # Configuración de ejecución en el editor (Run / Debug)
│   ├── launch.json
│   └── settings.json
├── bin/                             # Archivos compilados (.class)
├── db/                              # Base de datos SQLite y scripts SQL
│   ├── empdept.db                   # Base de datos SQLite inicializada
│   ├── emp_dept_salgrade_SQLITE.sql # Script SQL adaptado para SQLite
│   └── emp_dept_salgrade_MYSQL.sql  # Script SQL original para MySQL
├── docs/                            # Documentación y enunciados de la práctica
│   ├── Enunciado.md                 # Enunciado original de la tarea
│   └── Enunciado_SQLite.md          # Enunciado adaptado a SQLite
├── librerias/                       # Librerías JDBC requeridas
│   ├── sqlite-jdbc-3.49.1.0.jar
│   ├── slf4j-api-2.0.17.jar
│   └── slf4j-simple-2.0.17.jar
├── src/                             # Código fuente Java
│   ├── ActualizarSalarios.java      # Clase principal con el método main y lógica de negocio
│   ├── ConexionBD.java              # Gestión de conexión SQLite y reinicio de datos
│   └── RecrearBaseDatos.java        # Clase auxiliar para restaurar los datos iniciales
├── .classpath                       # Descriptor Eclipse/VS Code de librerías
├── .project                         # Descriptor Eclipse/VS Code de proyecto
├── .gitignore                       # Control de archivos ignorados
└── README.md                        # Documentación general
```

---

## 🚀 Ejecución Directa desde el Editor

1. Abre [`src/ActualizarSalarios.java`](file:///home/alumnot/Documentos/Practica-evaluable-Acceso-2/src/ActualizarSalarios.java) o [`src/RecrearBaseDatos.java`](file:///home/alumnot/Documentos/Practica-evaluable-Acceso-2/src/RecrearBaseDatos.java).
2. Haz clic en el enlace flotante **`Run`** justo encima de `public static void main(String[] args)` (o pulsa el botón **▶** en la esquina superior derecha).
3. También puedes pulsar **F5** o ir a la pestaña **Ejecutar y depurar** (`Ctrl + Mayús + D`).

---

## 🎯 Reglas de Negocio Implementadas

El cálculo del porcentaje salarial sigue estrictamente el orden de prioridad fijado en el enunciado (aplicando una sola subida por empleado):

1. **Gerentes (`job = 'MANAGER'`):** +10%
2. **Antigüedad anterior al 01/09/1981 (`hiredate < '1981-09-01'`):** +15%
3. **Para el resto de empleados:**
   - **Analistas (`job = 'ANALYST'`) del departamento 20:** +5%
   - **Vendedores (`job = 'SALESMAN'`):** +8%

---

## 🎤 Guía para la Corrección Oral

1. **Explicación del código:**
   - En [`ConexionBD.java`](file:///home/alumnot/Documentos/Practica-evaluable-Acceso-2/src/ConexionBD.java) se conecta mediante el driver `org.sqlite.JDBC` y la base de datos en [`db/empdept.db`](file:///home/alumnot/Documentos/Practica-evaluable-Acceso-2/db/empdept.db).
   - En [`ActualizarSalarios.java`](file:///home/alumnot/Documentos/Practica-evaluable-Acceso-2/src/ActualizarSalarios.java) se desactiva el autocommit con `conn.setAutoCommit(false)`.
   - Se consulta con `ResultSet` (`SELECT ...`), se itera fila a fila y se lanza un `PreparedStatement` de `UPDATE` por cada empleado.
   - Si todo finaliza con éxito, se ejecuta `conn.commit()`. En caso de cualquier error dentro del `try-catch`, se ejecuta `conn.rollback()` protegiendo la integridad.
2. **Prueba 1 (Demostración de cambios):**
   - Ejecuta `ActualizarSalarios` desde el editor. El programa imprime la tabla de empleados antes de la actualización, el detalle de cada fila procesada y la tabla después de la actualización.
3. **Prueba 2 (Demostración de Rollback / Transacción Atómica):**
   - En [`ActualizarSalarios.java`](file:///home/alumnot/Documentos/Practica-evaluable-Acceso-2/src/ActualizarSalarios.java), localiza el bloque de la **Prueba 2** y descomenta la línea:
     ```java
     if (true) throw new SQLException(">> SIMULACIÓN DE ERROR MANUAL: ABORTANDO ANTES DEL COMMIT <<");
     ```
   - Ejecuta desde el editor. Comprobarás que se captura la excepción, salta el `conn.rollback()` y en la tabla final ningún salario ha cambiado.
4. **Prueba 3 (Gestión de errores):**
   - Cambia el nombre de la tabla en la consulta SQL a una inexistente (ej. `SELECT * FROM empleados_falsa`) para mostrar cómo se gestiona y reporta limpiamente el error SQL.
