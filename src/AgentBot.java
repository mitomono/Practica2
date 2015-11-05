
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
                if(receiveBattery()<=2){
                    outbox.setReceiver(SERVIDOR);       //  Le mandamos al servidor que haga refuel
                    outbox.setContent("recarga");
                    this.send(outbox);
                    //ESPERAR RESPUESTA SERVIDOR
                }
                
                receiveRadar();
                
                if(solution){
                    sendExit(true,Scanner);
                    logout();
                }else{
                    sendExit(false,Scanner);
                    receiveScanner();
                    heuristic = botHeuristic();
                }
            
                if(heuristic == "NO"){
                    sendExit(true,Scanner);
                    logout();
                }else{
                    mandarServidorMover = heuristic;
                    exit = esperarRespuestaServidor();
                    sendExit(exit,Scanner);
                }   
            }
        }
    }
    
    @Override 
    public void finalize(){
        super.finalize();
    }
    
    
}
