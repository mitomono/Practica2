
import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import es.upv.dsic.gti_ia.core.ACLMessage;
import es.upv.dsic.gti_ia.core.AgentID;
import es.upv.dsic.gti_ia.core.SingleAgent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    private String key;
    private int battery;
    private ArrayList<Integer> radar;
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
    
    public void processData(JsonObject data){
        radar.clear();
        battery = data.get("battery").asInt();
        JsonArray radarAux = data.get("radar").asArray();
        for(int i=0;i<25;i++)
            radar.add(radarAux.get(i).asInt());
    }
    
    public boolean connectServer(){
        JsonObject connect = Json.object();
        connect.add("command","login");
        connect.add("world","map1");
        connect.add("radar","bot");
        connect.add("gps","gps");
        connect.add("scanner","scanner");
        
        String connectS = connect.toString();
        outbox.setReceiver(new AgentID("Denebola"));
        outbox.setContent(connectS);

        this.send(outbox);
        
        try {
            inbox = this.receiveACLMessage();
        } catch (InterruptedException ex) {
            Logger.getLogger(AgentBot.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        JsonObject answer = Json.parse(inbox.getContent()).asObject();
        key = answer.get("result").asString();
        
        if(key.contains("BAD_"))
            return false;
        else return true;
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
        
        if(!connectServer()){
            sendExit(true,new AgentID("GPS"));         //  Mandamos señal de finalización al GPS
            sendExit(true,new AgentID("Scanner"));     //  Mandamos señal de finalización al Scanner 
        }else{
            sendExit(false,new AgentID("GPS"));         // Mandamos señal de continuación al GPS
            sendExit(false,new AgentID("Scanner"));     // Mandamos señal de continuación al Scanner
            
            
            
            while(!exit){
                
                try {
                    inbox = this.receiveACLMessage();
                } catch (InterruptedException ex) {
                    Logger.getLogger(AgentBot.class.getName()).log(Level.SEVERE, null, ex);
                }
            
                processData(Json.parse(inbox.getContent()).asObject());
           
            
            
                if(battery<=5){
                    outbox.setReceiver(new AgentID("Denebola"));       //  Le mandamos al servidor que haga refuel
                    // JSON PARA SETCONTENT
                    outbox.setContent("refuel");
                    this.send(outbox);
                    //ESPERAR RESPUESTA SERVIDOR
                }
                
                if(radar.get(12) == 2) solution = true;
                else solution = false;
                
                if(solution){
                    sendExit(true,new AgentID("GPS"));
                    sendExit(true,new AgentID("Scanner"));
                    exit = true;
                }else{
                    sendExit(false,new AgentID("GPS"));
                    receiveGPS();
                    sendExit(false,new AgentID("Scanner"));
                    
                    receiveScanner();
                    heuristic = botHeuristic();
            
                    if(heuristic == "NO"){
                        sendExit(true,new AgentID("GPS"));
                        sendExit(true,new AgentID("Scanner"));
                        exit = true;
                    }else{
                        mandarServidorMover = heuristic;        // COMUNICACIÓN SERVIDOR
                        exit = esperarRespuestaServidor();      // ESPERAR Y PROCESAR RESPUESTA SERVIDOR
                        sendExit(exit,new AgentID("GPS"));
                        sendExit(exit,new AgentID("Scanner"));
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
