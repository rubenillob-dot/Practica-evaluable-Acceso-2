// Programa para restaurar la base de datos a sus valores iniciales.
// Nos sirve para hacer las pruebas en la corrección oral tantas veces como haga falta.
public class RecrearBaseDatos {

    public static void main(String[] args) {
        System.out.println("Restaurando los datos iniciales de la base de datos...");
        ConexionBD.reiniciarBaseDatos();
        System.out.println("Listo. Ya puedes ejecutar ActualizarSalarios.");
    }
}
