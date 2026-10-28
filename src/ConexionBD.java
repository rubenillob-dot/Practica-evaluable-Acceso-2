import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.util.Properties;
import java.util.logging.Logger;

// Clase de utilidad para conectarnos a la base de datos SQLite y restaurar los datos
public class ConexionBD {

    private static final String ARCHIVO_BD = "empdept.db";
    private static final String SCRIPT_SQL = "emp_dept_salgrade_SQLITE.sql";

    private static boolean driverListo = false;

    // Envoltorio para poder registrar el driver en DriverManager si se carga desde el JAR
    private static class DriverShim implements Driver {
        private final Driver driver;
        DriverShim(Driver d) { this.driver = d; }
        @Override public boolean acceptsURL(String u) throws SQLException { return this.driver.acceptsURL(u); }
        @Override public Connection connect(String u, Properties p) throws SQLException { return this.driver.connect(u, p); }
        @Override public int getMajorVersion() { return this.driver.getMajorVersion(); }
        @Override public int getMinorVersion() { return this.driver.getMinorVersion(); }
        @Override public DriverPropertyInfo[] getPropertyInfo(String u, Properties p) throws SQLException { return this.driver.getPropertyInfo(u, p); }
        @Override public boolean jdbcCompliant() { return this.driver.jdbcCompliant(); }
        @Override public Logger getParentLogger() throws SQLFeatureNotSupportedException { return this.driver.getParentLogger(); }
    }

    // Busca un fichero tanto si estamos en la raíz del proyecto como si estamos en otra carpeta
    public static File buscarArchivo(String nombre) {
        String[] posiblesRutas = {
            "db/" + nombre,
            nombre,
            "sql/" + nombre,
            "../db/" + nombre,
            "../" + nombre,
            "../sql/" + nombre
        };
        for (String ruta : posiblesRutas) {
            File f = new File(ruta);
            if (f.exists()) return f;
        }

        File dirDb = new File("db");
        if (dirDb.exists() && dirDb.isDirectory()) {
            return new File("db", nombre);
        }

        return new File(nombre);
    }

    // Busca el JAR del driver SQLite en la carpeta librerias o lib
    private static File buscarJarSQLite() {
        String[] posiblesCarpetas = {"librerias", "../librerias", "lib", "../lib", "."};
        for (String carpeta : posiblesCarpetas) {
            File dir = new File(carpeta);
            if (dir.exists() && dir.isDirectory()) {
                File[] archivos = dir.listFiles();
                if (archivos != null) {
                    for (File f : archivos) {
                        if (f.getName().startsWith("sqlite-jdbc") && f.getName().endsWith(".jar")) {
                            return f;
                        }
                    }
                }
            }
        }
        return null;
    }

    // Comprueba que el driver de SQLite esté disponible.
    // Si el editor no lo tiene en el classpath, lo cargamos desde la carpeta librerias.
    private static synchronized void asegurarDriver() throws SQLException {
        if (driverListo) {
            return;
        }

        try {
            // Intento normal: si ya está en el classpath
            Class.forName("org.sqlite.JDBC");
            driverListo = true;
        } catch (ClassNotFoundException e) {
            // Si no está, lo buscamos en librerias/ y lo registramos a mano
            File jar = buscarJarSQLite();
            if (jar != null && jar.exists()) {
                try {
                    URLClassLoader loader = new URLClassLoader(new URL[]{ jar.toURI().toURL() }, ConexionBD.class.getClassLoader());
                    Driver d = (Driver) Class.forName("org.sqlite.JDBC", true, loader).getDeclaredConstructor().newInstance();
                    DriverManager.registerDriver(new DriverShim(d));
                    driverListo = true;
                } catch (Exception ex) {
                    throw new SQLException("Error al cargar el driver desde " + jar.getName() + ": " + ex.getMessage(), ex);
                }
            } else {
                throw new SQLException("No se encontró el driver de SQLite en la carpeta librerias.", e);
            }
        }
    }

    // Abre y devuelve una conexión con la base de datos SQLite
    public static Connection getConnection() throws SQLException {
        asegurarDriver();
        File dbFile = buscarArchivo(ARCHIVO_BD);
        String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
        return DriverManager.getConnection(url);
    }

    // Lee el script SQL y vuelve a crear las tablas con los datos originales
    public static void reiniciarBaseDatos() {
        File scriptFile = buscarArchivo(SCRIPT_SQL);
        if (!scriptFile.exists()) {
            System.err.println("No se ha encontrado el archivo SQL: " + SCRIPT_SQL);
            return;
        }

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             BufferedReader br = new BufferedReader(new FileReader(scriptFile))) {

            StringBuilder sql = new StringBuilder();
            String linea;
            while ((linea = br.readLine()) != null) {
                linea = linea.trim();
                // Saltamos comentarios del script y líneas en blanco
                if (linea.startsWith("--") || linea.isEmpty()) {
                    continue;
                }
                sql.append(linea).append(" ");
                // Al llegar al punto y coma ejecutamos la sentencia
                if (linea.endsWith(";")) {
                    stmt.execute(sql.toString());
                    sql.setLength(0);
                }
            }
            System.out.println("Base de datos reiniciada correctamente con los datos originales.");

        } catch (SQLException | IOException e) {
            System.err.println("Error al reiniciar la base de datos: " + e.getMessage());
        }
    }
}
