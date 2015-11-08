
import es.upv.dsic.gti_ia.core.ACLMessage;
import es.upv.dsic.gti_ia.core.AgentID;
import es.upv.dsic.gti_ia.core.SingleAgent;
import java.util.HashMap;

/*
*   Clase AgentBot que se encargará de procesar los datos y
*   mandar las órdenes al servidor.
*/
public class AgentBot extends SingleAgent{
    
    private HashMap path;
    private int[][] worldMatrix; 
    private boolean exit;
    private boolean solution;
    private String heuristic;
    private ACLMessage inbox, outbox; // No sé si hace falta, lo he copiado de la práctica
    
    public AgentBot(AgentID aid) throws Exception {
        super(aid);
    }
    
    public void sendExit(boolean e, AgentID receiver){
        outbox.setReceiver(receiver);
        if(e){           
            outbox.setContent("ERROR");  
        }else{
            outbox.setContent("OK");
        }
        this.send(outbox);
    }
    
    public void receiveGPS(){
    
    }
    
    public int receiveBattery(){
        return 3;
    }
    
    public void receiveRadar(){
    
    }
    
    public void receiveScanner(){
    
    }
    
    public boolean esperarRespuestaServidor(){
        return true;
    }
    
    public String botHeuristic(){
        return "hola";
    }
    
    @Override 
    public void init(){
        worldMatrix = new int[500][500];
        path = new HashMap();
        outbox.setSender(this.getAid());
        exit = false;
        solution = true;
    }
    
    @Override 
    public void execute(){
        if(error_conexion){
            sendExit(true,GPS);         //  Mandamos señal de finalización al GPS
            sendExit(true,Scanner);     //  Mandamos señal de finalización al Scanner 
        }else{
            sendExit(false,GPS);         // Mandamos señal de continuación al GPS
            sendExit(false,Scanner);     // Mandamos señal de continuación al Scanner
            
            receiveGPS();
            sendExit(true,GPS);         //  Falta en el diagrama, le diríamos al GPS que se cierre
            
            while(!exit){
                //  RECIBIR JSON SERVIDOR Y PROCESARLO
                if(batery<=10){
                    outbox.setReceiver("Denebola");       //  Le mandamos al servidor que haga refuel
                    // JSON PARA SETCONTENT
                    outbox.setContent("recarga");
                    this.send(outbox);
                    //ESPERAR RESPUESTA SERVIDOR
                }
                
                receiveRadar();
                if(array[12] == 2) solution = true;
                else solution = false;
                
                if(solution){
                    sendExit(true,Scanner);
                    exit = true;
                }else{
                    sendExit(false,Scanner);
                    receiveScanner();
                    heuristic = botHeuristic();
            
                    if(heuristic == "NO"){
                        sendExit(true,Scanner);
                        exit = true;
                    }else{
                        mandarServidorMover = heuristic;        // COMUNICACIÓN SERVIDOR
                        exit = esperarRespuestaServidor();      // ESPERAR Y PROCESAR RESPUESTA SERVIDOR
                        sendExit(exit,Scanner);                 // DECIRLE A SCANNER SI APAGARSE
                    }
                }
            }
        }
    }
    
    @Override 
    public void finalize(){
        super.finalize();
    }
    
    
}
