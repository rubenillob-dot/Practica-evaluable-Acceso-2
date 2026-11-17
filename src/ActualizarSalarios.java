import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;

// Clase principal para actualizar los salarios de los empleados con JDBC y SQLite.

public class ActualizarSalarios {


    private static final LocalDate FECHA_CORTE = LocalDate.of(1981, 9, 1);

    //guardar el porcentaje 
    private static class DatosSubida {
        final double porcentaje;
        final String motivo;

        DatosSubida(double porcentaje, String motivo) {
            this.porcentaje = porcentaje;
            this.motivo = motivo;
        }
    }

    public static void main(String[] args) {
        System.out.println("         ACTUALIZACIÓN DE SALARIOS EN LA TABLA EMP              ");

        try (Connection conn = ConexionBD.getConnection()) {

            // ver salarios
            System.out.println("\n[1] Empleados antes de la actualización:");
            mostrarEmpleados(conn);

            System.out.println("\n[2] Procesando empleados fila a fila...");
            actualizarSalarios(conn);

            System.out.println("\n[3] Empleados tras la transacción:");
            mostrarEmpleados(conn);

        } catch (SQLException e) {
            System.err.println("No se pudo conectar con la base de datos: " + e.getMessage());
        }
    }

    public static void actualizarSalarios(Connection conn) {
        String consultaSelect = "SELECT empno, ename, job, hiredate, sal, deptno FROM emp";
        String sentenciaUpdate = "UPDATE emp SET sal = ? WHERE empno = ?";

        try {
            conn.setAutoCommit(false);

            try (Statement stmtSelect = conn.createStatement();
                 ResultSet rs = stmtSelect.executeQuery(consultaSelect);
                 PreparedStatement stmtUpdate = conn.prepareStatement(sentenciaUpdate)) {

                int subidasAplicadas = 0;
                int sinCambios = 0;

                // Recorremos los empleados 
                while (rs.next()) {
                    int empno = rs.getInt("empno");
                    String ename = rs.getString("ename");
                    String job = rs.getString("job");
                    String fechaStr = rs.getString("hiredate");
                    double salActual = rs.getDouble("sal");
                    int deptno = rs.getInt("deptno");

                    LocalDate hiredate = (fechaStr != null) ? LocalDate.parse(fechaStr) : null;

                
                    DatosSubida subida = calcularSubida(job, hiredate, deptno);

                    if (subida.porcentaje > 0) {
                        
                        double nuevoSalario = salActual * (1.0 + (subida.porcentaje / 100.0));
                        nuevoSalario = BigDecimal.valueOf(nuevoSalario)
                                                 .setScale(2, RoundingMode.HALF_UP)
                                                 .doubleValue();

                        
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

               
                // if (true) throw new SQLException("Fallo simulado para comprobar que el rollback funciona.");

                conn.commit();
                System.out.println("\n-> Transacción confirmada (commit con éxito).");
                System.out.println("   Resumen: " + subidasAplicadas + " empleados con subida y " + sinCambios + " empleados sin cambios.");

            } catch (SQLException e) {
                
                System.err.println("\n-> Ha ocurrido un error durante la actualización: " + e.getMessage());
                conn.rollback();
                System.err.println("-> Rollback ejecutado: no se ha modificado ningún dato en la base de datos.");
            } finally {
            
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            System.err.println("Error en la transacción: " + e.getMessage());
        }
    }

    // Calcula el porcentaje de subida siguiendo el orden exacto del enunciado:
  
    public static DatosSubida calcularSubida(String job, LocalDate hiredate, int deptno) {
        if (job == null) {
            return new DatosSubida(0.0, "Sin puesto asignado");
        }

        // Gerentes 
        if ("MANAGER".equalsIgnoreCase(job)) {
            return new DatosSubida(10.0, "Puesto de gerente (MANAGER)");
        }

        // Antigüedad anterior al 01/09/1981
        if (hiredate != null && hiredate.isBefore(FECHA_CORTE)) {
            return new DatosSubida(15.0, "Antigüedad anterior al 01/09/1981");
        }

        // Para el resto de empleados
        if ("ANALYST".equalsIgnoreCase(job) && deptno == 20) {
            return new DatosSubida(5.0, "Analista del departamento 20");
        }

        if ("SALESMAN".equalsIgnoreCase(job)) {
            return new DatosSubida(8.0, "Puesto de vendedor (SALESMAN)");
        }

        return new DatosSubida(0.0, "No cumple condiciones de subida");
    }

    // Muestra por pantalla una tabla ordenada y fácil de leer
    public static void mostrarEmpleados(Connection conn) throws SQLException {
        String sql = "SELECT empno, ename, job, hiredate, sal, deptno FROM emp ORDER BY empno";

        System.out.printf("   %-5s | %-9s | %-10s | %-11s | %-11s | %-6s%n",
                "ID", "Nombre", "Oficio", "Fecha Alta", "Salario", "Depto");

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
    }
}
