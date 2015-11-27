package practica2;

import es.upv.dsic.gti_ia.core.AgentID;
import es.upv.dsic.gti_ia.core.AgentsConnection;
/**
 * <code>Practica2</code> es la clase encargada de ejecutar
 * el proyecto. Esto implica conectarse al servidor privado del grupo 4
 * e inicializar y asignar nombre a los agentes.
 * @author      Samuel Peralta Antequera
 * @author      Adrián Lara Roldán
 * @author      Raúl Alberto Calderón López
 * @author      Raúl López Arévalo
 */
public class Practica2 {
/**
 * Función engargada de empezar la ejecución del proyecto.
 * @author      Samuel Peralta Antequera
 * @author      Adrián Lara Roldán
 * @author      Raúl Alberto Calderón López
 * @author      Raúl López Arévalo
 * 
 * @param args  Argumentos base.
 */
    public static void main(String[] args) throws Exception {

        //  Declaración de los agentes
        AgentBot bot;
        AgentScanner scanner;
        AgentGPS gps;

        //  Conexión con el servidor
        AgentsConnection.connect("isg2.ugr.es", 6000, "Denebola", "Leon", "Russo", false);

        //  Inicialización de los agentes
        bot = new AgentBot(new AgentID("bot"));
        scanner = new AgentScanner(new AgentID("scanner"));
        gps = new AgentGPS(new AgentID("gps"));

        //  Comienzo de la ejecucion de los agentes
        scanner.start();
        gps.start();
        bot.start();
    }

}
