import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;

// Clase principal para actualizar los salarios de los empleados con JDBC y SQLite.
// Toda la actualización se realiza fila a fila dentro de una única transacción.
public class ActualizarSalarios {

    // Fecha de corte que marca el enunciado: 1 de septiembre de 1981
    private static final LocalDate FECHA_CORTE = LocalDate.of(1981, 9, 1);

    // Estructura sencilla para guardar el porcentaje y la explicación de cada caso
    private static class DatosSubida {
        final double porcentaje;
        final String motivo;

        DatosSubida(double porcentaje, String motivo) {
            this.porcentaje = porcentaje;
            this.motivo = motivo;
        }
    }

    public static void main(String[] args) {
        System.out.println("===============================================================================");
        System.out.println("         ACTUALIZACIÓN DE SALARIOS EN LA TABLA EMP (JDBC + SQLite)             ");
        System.out.println("===============================================================================");

        try (Connection conn = ConexionBD.getConnection()) {

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

    // Recorre todos los empleados con un ResultSet y actualiza sus salarios en una sola transacción
    public static void actualizarSalarios(Connection conn) {
        String consultaSelect = "SELECT empno, ename, job, hiredate, sal, deptno FROM emp";
        String sentenciaUpdate = "UPDATE emp SET sal = ? WHERE empno = ?";

        try {
            // Desactivamos el autocommit para que todas las filas formen una única transacción
            conn.setAutoCommit(false);

            try (Statement stmtSelect = conn.createStatement();
                 ResultSet rs = stmtSelect.executeQuery(consultaSelect);
                 PreparedStatement stmtUpdate = conn.prepareStatement(sentenciaUpdate)) {

                int subidasAplicadas = 0;
                int sinCambios = 0;

                // Recorremos los empleados uno a uno con el ResultSet
                while (rs.next()) {
                    int empno = rs.getInt("empno");
                    String ename = rs.getString("ename");
                    String job = rs.getString("job");
                    String fechaStr = rs.getString("hiredate");
                    double salActual = rs.getDouble("sal");
                    int deptno = rs.getInt("deptno");

                    LocalDate hiredate = (fechaStr != null) ? LocalDate.parse(fechaStr) : null;

                    // Evaluamos qué subida le corresponde según las reglas y su orden
                    DatosSubida subida = calcularSubida(job, hiredate, deptno);

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

    // Calcula el porcentaje de subida siguiendo el orden exacto del enunciado:
    // 1º Gerentes (MANAGER): 10%
    // 2º Antigüedad anterior al 01/09/1981: 15%
    // 3º Resto:
    //    - Analistas (ANALYST) del departamento 20: 5%
    //    - Vendedores (SALESMAN): 8%
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

    // Muestra por pantalla una tabla ordenada y fácil de leer
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
}
