
import es.upv.dsic.gti_ia.core.AgentID;
import es.upv.dsic.gti_ia.core.AgentsConnection;

public class main {

    public static void main(String[] args) throws Exception {
        
        //  Declaración de los agentes
        AgentBot bot;
        AgentScanner scanner;
        AgentGPS gps;
        
        //  Conexión con el servidor
        AgentsConnection.connect("isg2.ugr.es", 6000, "Denebola", "Leon", "Russo", false);
        
        //  Inicialización de los agentes
        bot = new AgentBot(new AgentID("bot"));
        scanner=new AgentScanner(new AgentID("scanner"));
        gps = new AgentGPS(new AgentID("gps"));
        
        //  Comienzo de la ejecucion de los agentes
        scanner.start();
        gps.start();
        bot.start();
    }
    
}
