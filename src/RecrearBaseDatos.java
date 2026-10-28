/**
 * Clase auxiliar para restaurar rápidamente la base de datos con sus datos iniciales.
 * Facilita repetir las pruebas tantas veces como sea necesario durante la corrección oral.
 */
public class RecrearBaseDatos {

    public static void main(String[] args) {
        System.out.println("Restaurando base de datos a su estado original...");
        ConexionBD.reiniciarBaseDatos();
        System.out.println("Completado. Ahora puedes ejecutar ActualizarSalarios.");
    }
}
