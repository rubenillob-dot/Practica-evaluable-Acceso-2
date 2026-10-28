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

/**
 * Clase encargada de gestionar la conexión con la base de datos SQLite
 * y proveer utilidades como el reinicio de datos iniciales.
 *
 * Incluye carga automática de librerías para funcionar tanto si se ejecuta
 * desde el IDE (botón Run), desde terminal o con cualquier configuración de classpath.
 */
public class ConexionBD {

    private static final String DB_FILE_NAME = "empdept.db";
    private static final String SCRIPT_SQL_NAME = "emp_dept_salgrade_SQLITE.sql";

    private static boolean driverRegistrado = false;

    /**
     * Driver envoltorio para registrar un Driver cargado dinámicamente
     * en caso de que el entorno no haya incluido el classpath de librerias/.
     */
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

    /**
     * Localiza un archivo buscando en las rutas habituales del proyecto (db/, sql/, raíz o directorio superior).
     */
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

        // Si la carpeta db/ existe, devolver la ruta dentro de db/
        File dirDb = new File("db");
        if (dirDb.exists() && dirDb.isDirectory()) {
            return new File("db", nombre);
        }

        return new File(nombre);
    }

    /**
     * Busca el archivo JAR del driver SQLite en las carpetas habituales.
     */
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

    /**
     * Asegura que el driver JDBC de SQLite esté cargado y registrado.
     * Si no está en el classpath normal, lo carga automáticamente desde la carpeta librerias/.
     */
    private static synchronized void asegurarDriver() throws SQLException {
        if (driverRegistrado) {
            return;
        }

        try {
            // Intento 1: Carga normal desde el classpath del proyecto
            Class.forName("org.sqlite.JDBC");
            driverRegistrado = true;
        } catch (ClassNotFoundException e) {
            // Intento 2: Carga dinámica directa desde librerias/ o lib/
            File jar = buscarJarSQLite();
            if (jar != null && jar.exists()) {
                try {
                    URLClassLoader loader = new URLClassLoader(new URL[]{ jar.toURI().toURL() }, ConexionBD.class.getClassLoader());
                    Driver d = (Driver) Class.forName("org.sqlite.JDBC", true, loader).getDeclaredConstructor().newInstance();
                    DriverManager.registerDriver(new DriverShim(d));
                    driverRegistrado = true;
                } catch (Exception ex) {
                    throw new SQLException("Error al cargar dinámicamente el driver desde " + jar.getPath() + ": " + ex.getMessage(), ex);
                }
            } else {
                throw new SQLException("No se encontró el driver JDBC de SQLite en el classpath ni en la carpeta 'librerias/'.", e);
            }
        }
    }

    /**
     * Obtiene una conexión activa a la base de datos SQLite.
     *
     * @return Objeto Connection activo
     * @throws SQLException si ocurre un error de conexión
     */
    public static Connection getConnection() throws SQLException {
        asegurarDriver();
        File dbFile = buscarArchivo(DB_FILE_NAME);
        String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
        return DriverManager.getConnection(url);
    }

    /**
     * Recrea las tablas e inserta los datos iniciales ejecutando el script SQL.
     * Muy útil para la preparación de las pruebas en la corrección oral.
     */
    public static void reiniciarBaseDatos() {
        File scriptFile = buscarArchivo(SCRIPT_SQL_NAME);
        if (!scriptFile.exists()) {
            System.err.println("No se encontró el archivo de script: " + SCRIPT_SQL_NAME);
            return;
        }

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             BufferedReader br = new BufferedReader(new FileReader(scriptFile))) {

            StringBuilder sql = new StringBuilder();
            String linea;
            while ((linea = br.readLine()) != null) {
                linea = linea.trim();
                // Ignorar comentarios y líneas vacías
                if (linea.startsWith("--") || linea.isEmpty()) {
                    continue;
                }
                sql.append(linea).append(" ");
                // Si la sentencia termina en ';', se ejecuta
                if (linea.endsWith(";")) {
                    stmt.execute(sql.toString());
                    sql.setLength(0);
                }
            }
            File dbFile = buscarArchivo(DB_FILE_NAME);
            System.out.println("Base de datos '" + dbFile.getName() + "' reiniciada con éxito con los datos originales.");

        } catch (SQLException | IOException e) {
            System.err.println("Error al reiniciar la base de datos: " + e.getMessage());
        }
    }
}
