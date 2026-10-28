# 🎤 Guión para la Corrección Oral y Entrega de la Práctica

Este documento contiene el guión detallado para explicar la práctica en la defensa oral de forma natural, sencilla y paso a paso, así como las instrucciones exactas para preparar el archivo `.zip` final.

---

## 📌 Paso 0: Preparación previa (Antes de empezar)

1. Abre el editor en la carpeta del proyecto.
2. Abre en pestañas los archivos:
   - `src/ActualizarSalarios.java`
   - `src/RecrearBaseDatos.java`
   - `src/ConexionBD.java`
3. Asegúrate de que la base de datos está en su estado original ejecutando `RecrearBaseDatos.java` (pulsando el botón `Run`).

---

## 🗣️ Paso 1: Introducción breve (30 segundos)

**Qué decir:**
> *"Hola. En esta práctica he implementado la actualización salarial de los empleados de la tabla `emp` utilizando JDBC sobre SQLite.*  
> *El objetivo principal es evaluar fila a fila qué porcentaje de subida le corresponde a cada empleado en función de su puesto, departamento y fecha de alta, garantizando que todas las modificaciones se realicen dentro de una **única transacción atómica**."*

---

## 🧩 Paso 2: Explicación de las clases del proyecto (1-2 minutos)

Señala con el ratón las clases en la carpeta `src/`:

### 1. `ConexionBD.java`
**Qué decir:**
> *"Centraliza la conexión a SQLite en `db/empdept.db`. Además, incluye el método `reiniciarBaseDatos()`, que lee y ejecuta el script SQL original para restaurar las tablas y los datos en cualquier momento sin tener que hacerlo a mano."*

### 2. `RecrearBaseDatos.java`
**Qué decir:**
> *"Es una clase auxiliar muy sencilla que solo ejecuta el reinicio de la base de datos. La he preparado para poder resetear los datos al estado inicial entre prueba y prueba con un solo clic."*

### 3. `ActualizarSalarios.java` (Clase Principal)
**Qué decir:**
> *"Es donde reside la lógica principal del ejercicio. Destaco cuatro puntos clave de la implementación:*
> 1. *Desactivo el autocommit con `conn.setAutoCommit(false)` para gestionar manualmente la transacción.*
> 2. *Consulto los empleados con `Statement` y los recorro fila a fila mediante un `ResultSet` con un bucle `while (rs.next())`.*
> 3. *En el método `calcularSubida()` evalúo las condiciones en el **orden estricto** que pide el enunciado:*
>    * *1º Gerentes (`MANAGER`): subida del 10%.*
>    * *2º Antigüedad anterior al 01/09/1981: subida del 15%. (Por ejemplo, los vendedores antiguos como ALLEN o WARD entran aquí y reciben el 15%, no el 8%).*
>    * *3º Para el resto de empleados: analistas del departamento 20 un 5%, y vendedores un 8%.*
> 4. *Actualizo cada fila con un `PreparedStatement` individual (`UPDATE emp SET sal = ? WHERE empno = ?`) y, al finalizar todo el bucle, hago `conn.commit()`. Si ocurriera cualquier excepción, el bloque `catch` ejecuta `conn.rollback()` para cancelar todos los cambios."*

---

## ▶️ Paso 3: Demostración en vivo - Prueba 1 (Flujo normal)

**Qué hacer:**
1. Ve a `src/ActualizarSalarios.java`.
2. Haz clic en el texto flotante **`Run`** sobre el método `main`.
3. Deja que termine la ejecución en la consola.

**Qué decir mostrando la consola:**
> *"Como se puede ver en la consola, el programa realiza tres pasos bien diferenciados:*
> * *Primero muestra la tabla tal cual está en la base de datos antes de tocar nada.*
> * *Luego muestra el recorrido fila a fila explicando el motivo exacto de cada subida (por ejemplo, JONES sube el 10% por ser manager, SMITH sube el 15% por antigüedad previa a septiembre del 81, y KING se queda igual porque no cumple condiciones).*
> * *Al final confirma la transacción con el commit y vuelve a mostrar la tabla completa para comprobar cómo han quedado guardados los nuevos salarios (10 empleados han subido y 4 se mantienen)."*

---

## 🔄 Paso 4: Demostración en vivo - Prueba 2 (Demostración de Rollback)

**Qué hacer:**
1. Abre `src/RecrearBaseDatos.java` y pulsa **`Run`** (para dejar la base de datos limpia de nuevo).
2. Vuelve a `src/ActualizarSalarios.java` y busca la línea comentada:
   ```java
   // if (true) throw new SQLException("Fallo simulado para comprobar que el rollback funciona.");
   ```
3. **Quítale las dos barras `//`** para descomentarla.
4. Pulsa **`Run`** en `ActualizarSalarios.java`.

**Qué decir mostrando la consola:**
> *"He descomentado una excepción artificial justo antes de hacer el commit. Como vemos en la consola, el programa procesa los empleados pero salta el error antes de guardar. El bloque `catch` captura la excepción y ejecuta inmediatamente `conn.rollback()`.*  
> *En la tabla final podemos comprobar que **ningún salario ha cambiado**, demostrando la atomicidad de la transacción: o se guardan todos los empleados o no se guarda ninguno."*

5. **Vuelve a comentar la línea añadiendo `//` al principio.**

---

## 🛡️ Paso 5: Prueba 3 (Gestión de errores)

**Si el profesor te pide simular un error SQL:**
1. Cambia temporalmente la consulta en `ActualizarSalarios.java`:
   `SELECT ... FROM emp` &rarr; `SELECT ... FROM empleados_falsa`
2. Pulsa **`Run`**.
3. **Qué decir:**
   > *"Como la consulta está protegida dentro del bloque `try-catch`, el programa captura el `SQLException`, muestra un mensaje informativo y no revienta la aplicación."*
4. Restaura la consulta a `FROM emp`.

---

## 📊 Tabla Rápida de Comprobación de Empleados

Úsala como chuleta durante la explicación:

| ID | Nombre | Oficio | Fecha Alta | Salario Inicial | Motivo de la Subida | Subida | Salario Final |
| :---: | :--- | :--- | :---: | :---: | :--- | :---: | :---: |
| **7839** | KING | PRESIDENT | 1981-11-17 | 5000,00 € | Ninguna condición aplicable | 0% | **5000,00 €** |
| **7698** | BLAKE | MANAGER | 1981-05-01 | 2850,00 € | 1ª regla: Gerente | +10% | **3135,00 €** |
| **7782** | CLARK | MANAGER | 1981-06-09 | 2450,00 € | 1ª regla: Gerente | +10% | **2695,00 €** |
| **7566** | JONES | MANAGER | 1981-04-02 | 2975,00 € | 1ª regla: Gerente | +10% | **3272,50 €** |
| **7369** | SMITH | CLERK | 1980-12-17 | 800,00 € | 2ª regla: Antigüedad (< 01/09/1981) | +15% | **920,00 €** |
| **7499** | ALLEN | SALESMAN | 1981-02-20 | 1600,00 € | 2ª regla: Antigüedad (< 01/09/1981) | +15% | **1840,00 €** |
| **7521** | WARD | SALESMAN | 1981-02-22 | 1250,00 € | 2ª regla: Antigüedad (< 01/09/1981) | +15% | **1437,50 €** |
| **7788** | SCOTT | ANALYST | 1987-07-13 | 3000,00 € | 3ª regla: Analista dpto 20 | +5% | **3150,00 €** |
| **7902** | FORD | ANALYST | 1981-12-03 | 3000,00 € | 3ª regla: Analista dpto 20 | +5% | **3150,00 €** |
| **7654** | MARTIN | SALESMAN | 1981-09-28 | 1250,00 € | 3ª regla: Vendedor | +8% | **1350,00 €** |
| **7844** | TURNER | SALESMAN | 1981-09-08 | 1500,00 € | 3ª regla: Vendedor | +8% | **1620,00 €** |
| **7876** | ADAMS | CLERK | 1987-07-13 | 1100,00 € | Ninguna condición aplicable | 0% | **1100,00 €** |
| **7900** | JAMES | CLERK | 1981-12-03 | 950,00 € | Ninguna condición aplicable | 0% | **950,00 €** |
| **7934** | MILLER | CLERK | 1982-01-23 | 1300,00 € | Ninguna condición aplicable | 0% | **1300,00 €** |

---

## 📦 Instrucciones para la Entrega en Archivo ZIP

### 1. Formato del Nombre del Archivo
El archivo debe nombrarse exactamente así:
```text
apellido1_apellido2_nombre_3J_Tarea.zip
```
* **Sin tildes** (ejemplo: `gonzalez` en vez de `gonzález`).
* **Sin letra 'ñ'** (sustitúyela por `n` o `nh`, ejemplo: `pena` en vez de `peña`).
* **Sin espacios ni caracteres raros**.

### 2. Estructura que debe tener el ZIP descomprimido
```text
├── .vscode/                         # Configuración para ejecutar con un clic
├── db/                              # Base de datos empdept.db y scripts SQL
├── docs/                            # Enunciados y guión de la defensa
├── librerias/                       # Driver SQLite y librerías auxiliares (OBLIGATORIO)
├── src/                             # Código fuente Java (OBLIGATORIO)
├── .classpath                       # Configuración de librerías en editor
├── .project                         # Descriptor del proyecto
└── README.md                        # Documentación general
```

> [!WARNING]
> **No incluyas la carpeta `bin/`** (archivos `.class` compilados). El ZIP debe entregarse limpio de archivos compilados temporales.

### 3. Comando para generar el ZIP limpio desde la terminal:
```bash
# 1. Dejar la base de datos limpia en su estado original
java -cp "bin:librerias/*" RecrearBaseDatos

# 2. Comprimir el proyecto (sustituye tus datos)
zip -r apellido1_apellido2_nombre_3J_Tarea.zip src librerias db docs .vscode .classpath .project README.md
```
