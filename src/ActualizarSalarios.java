import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;

/**
 * Clase principal que actualiza fila a fila el salario de los empleados
 * según las condiciones del enunciado en una única transacción.
 */
public class ActualizarSalarios {

    // Fecha límite para la condición de antigüedad: 01/09/1981
    private static final LocalDate FECHA_LIMITE_ANTIGUEDAD = LocalDate.of(1981, 9, 1);

    public static void main(String[] args) {
        System.out.println("===============================================================================");
        System.out.println("   PROYECTO JDBC: ACTUALIZACIÓN SALARIAL EN TABLA EMP (SQLITE)");
        System.out.println("===============================================================================");

        try (Connection conn = ConexionBD.getConnection()) {

            // Paso 1: Mostrar los datos originales de la tabla emp (Prueba 1)
            System.out.println("\n--- 1. ESTADO ORIGINAL DE LOS EMPLEADOS ---");
            mostrarEmpleados(conn);

            // Paso 2: Ejecutar la actualización transaccional fila a fila
            System.out.println("\n--- 2. PROCESANDO ACTUALIZACIÓN SALARIAL (FILA A FILA) ---");
            actualizarSalarios(conn);

            // Paso 3: Mostrar los datos tras la actualización (Prueba 1)
            System.out.println("\n--- 3. ESTADO FINAL DE LOS EMPLEADOS TRAS LA TRANSACCIÓN ---");
            mostrarEmpleados(conn);

        } catch (SQLException e) {
            System.err.println("Error en la conexión o gestión de la base de datos: " + e.getMessage());
        }
    }

    /**
     * Recorre fila a fila los empleados con un ResultSet y actualiza sus salarios
     * dentro de una única transacción.
     *
     * @param conn Conexión activa a la base de datos
     */
    public static void actualizarSalarios(Connection conn) {
        String sqlSelect = "SELECT empno, ename, job, hiredate, sal, deptno FROM emp";
        String sqlUpdate = "UPDATE emp SET sal = ? WHERE empno = ?";

        try {
            // DESACTIVAR AUTOCOMMIT: La actualización de todas las filas debe ser una única transacción
            conn.setAutoCommit(false);

            try (Statement selectStmt = conn.createStatement();
                 ResultSet rs = selectStmt.executeQuery(sqlSelect);
                 PreparedStatement updateStmt = conn.prepareStatement(sqlUpdate)) {

                int totalProcesados = 0;
                int totalModificados = 0;

                // Recorremos de manera repetitiva el ResultSet fila a fila
                while (rs.next()) {
                    totalProcesados++;
                    int empno = rs.getInt("empno");
                    String ename = rs.getString("ename");
                    String job = rs.getString("job");
                    String hiredateStr = rs.getString("hiredate");
                    double salActual = rs.getDouble("sal");
                    int deptno = rs.getInt("deptno");

                    LocalDate hiredate = (hiredateStr != null) ? LocalDate.parse(hiredateStr) : null;

                    // Evaluar condiciones de subida según el orden estricto indicado
                    double porcentaje = calcularPorcentajeSubida(job, hiredate, deptno);

                    if (porcentaje > 0) {
                        // Calcular nuevo salario y redondear a 2 decimales
                        double nuevoSalario = salActual * (1.0 + (porcentaje / 100.0));
                        nuevoSalario = BigDecimal.valueOf(nuevoSalario)
                                                 .setScale(2, RoundingMode.HALF_UP)
                                                 .doubleValue();

                        // Actualizar fila mediante PreparedStatement
                        updateStmt.setDouble(1, nuevoSalario);
                        updateStmt.setInt(2, empno);
                        updateStmt.executeUpdate();

                        totalModificados++;
                        System.out.printf("  [SUBIDA +%4.1f%%] Empno: %d | %-9s (%-9s) | Salario anterior: %7.2f € -> Nuevo salario: %7.2f €%n",
                                porcentaje, empno, ename, job, salActual, nuevoSalario);
                    } else {
                        System.out.printf("  [SIN CAMBIO   ] Empno: %d | %-9s (%-9s) | Salario: %7.2f €%n",
                                empno, ename, job, salActual);
                    }
                }

                // =========================================================================
                // PRUEBA 2 PARA LA CORRECCIÓN ORAL (DEMOSTRACIÓN DE ROLLBACK):
                // Descomenta la siguiente línea para abortar la ejecución antes del commit
                // y comprobar que ningún salario se modifica en la base de datos.
                // =========================================================================
                // if (true) throw new SQLException(">> SIMULACIÓN DE ERROR MANUAL: ABORTANDO ANTES DEL COMMIT <<");

                // Confirmar la transacción si todas las filas se actualizaron sin errores
                conn.commit();
                System.out.println("\n>> ÉXITO: Transacción confirmada (COMMIT).");
                System.out.println("   Empleados procesados: " + totalProcesados + " | Salarios modificados: " + totalModificados);

            } catch (SQLException e) {
                // Si ocurre cualquier error, abortamos la transacción completa
                System.err.println("\n>> ATENCIÓN: Ha ocurrido un fallo durante la actualización: " + e.getMessage());
                conn.rollback();
                System.err.println(">> ROLLBACK realizado: Se han cancelado todos los cambios de la transacción.");
            } finally {
                // Restauramos el modo autocommit por defecto
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            System.err.println("Error en la gestión transaccional de la conexión: " + e.getMessage());
        }
    }

    /**
     * Evalúa las condiciones en el orden estricto especificado en el enunciado:
     * 1. Gerentes (job = MANAGER) -> +10%
     * 2. Antigüedad anterior al 01/09/81 -> +15%
     * 3. Para el resto de los empleados:
     *    - Analistas (job = ANALYST) del departamento 20 -> +5%
     *    - Vendedores (job = SALESMAN) -> +8%
     *
     * @param job Puesto de trabajo
     * @param hiredate Fecha de contratación
     * @param deptno Número de departamento
     * @return Porcentaje de incremento salarial (0.0 si no aplica ninguna)
     */
    public static double calcularPorcentajeSubida(String job, LocalDate hiredate, int deptno) {
        if (job == null) {
            return 0.0;
        }

        // Condición 1: Los gerentes (job = MANAGER) tienen una subida del 10%
        if (job.equalsIgnoreCase("MANAGER")) {
            return 10.0;
        }

        // Condición 2: Antigüedad anterior al 01/09/81 tienen una subida del 15%
        if (hiredate != null && hiredate.isBefore(FECHA_LIMITE_ANTIGUEDAD)) {
            return 15.0;
        }

        // Condición 3: Para el resto de los empleados
        if (job.equalsIgnoreCase("ANALYST") && deptno == 20) {
            // Analistas del departamento 20 tienen una subida del 5%
            return 5.0;
        }

        if (job.equalsIgnoreCase("SALESMAN")) {
            // Vendedores tienen una subida del 8%
            return 8.0;
        }

        // Resto sin subida
        return 0.0;
    }

    /**
     * Muestra por consola la lista de empleados de forma formateada.
     *
     * @param conn Conexión activa a la base de datos
     * @throws SQLException si ocurre un error en la consulta
     */
    public static void mostrarEmpleados(Connection conn) throws SQLException {
        String sql = "SELECT empno, ename, job, hiredate, sal, deptno FROM emp ORDER BY empno";

        System.out.println("-------------------------------------------------------------------------------");
        System.out.printf("%-6s | %-10s | %-10s | %-12s | %-10s | %-6s%n",
                "EMPNO", "NOMBRE", "OFICIO", "FECHA ALTA", "SALARIO", "DEPTO");
        System.out.println("-------------------------------------------------------------------------------");

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                System.out.printf("%-6d | %-10s | %-10s | %-12s | %8.2f € | %-6d%n",
                        rs.getInt("empno"),
                        rs.getString("ename"),
                        rs.getString("job"),
                        rs.getString("hiredate"),
                        rs.getDouble("sal"),
                        rs.getInt("deptno"));
            }
        }
        System.out.println("-------------------------------------------------------------------------------");
    }
}
