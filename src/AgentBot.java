
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
    //  Variables para almacenar el estado interno del agente
    private ArrayList<Integer> radar;
    private ArrayList<Float> scan;
    private int coorX, coorY;
    private int battery;
    //   Variables para la comunicación
    private String key;
    private ACLMessage inbox, outbox;
    private boolean exit;
    //  Variables para la heurística
    private HashMap path;
    private int[][] worldMatrix; 
    private String heuristic;
    private boolean solution;
        
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
    
    public String botHeuristic(){
        return "hola";
    }
    
    public boolean resultServer(){
        try {
            inbox = this.receiveACLMessage();
        } catch (InterruptedException ex) {
        }
        
        JsonObject resultSon = Json.parse(inbox.getContent()).asObject();
        String result = resultSon.get("result").asString();
        
        if(result.equals("CRASHED") || result.contains("BAD_"))
            return true;        //  Devuelve true porque exit = resultServer()
        else return false;      //  Por lo que exit = true cuando haya error.
    }
    
    public void sendOrder(String send){
        outbox.setReceiver(new AgentID("Denebola"));
        JsonObject order = Json.object();
        order.add("command", send);
        order.add("key", key);
        outbox.setContent(order.toString());
        this.send(outbox);
    }
    
    public void receiveGPS(){
        outbox.setReceiver(new AgentID("GPS"));
        outbox.setContent("Send");
        this.send(outbox);
        
        try {
            inbox = this.receiveACLMessage();
        } catch (InterruptedException ex) {
        }
        JsonObject aux = Json.parse(inbox.getContent()).asObject();
        coorX = aux.get("x").asInt();       //  Tal vez haya que coger gps primero??? y luego x??
        coorY = aux.get("y").asInt();               
    
    }
    
    public void receiveScanner(){
        outbox.setReceiver(new AgentID("Scanner"));
        outbox.setContent("Send");
        this.send(outbox);
        
        try {
            inbox = this.receiveACLMessage();
        } catch (InterruptedException ex) {
        }
        String auxContent = inbox.getContent();
        JsonArray auxArray = Json.parse(auxContent).asArray();
        
        scan.clear();
        for(int i=0;i<25;i++)
            scan.add(auxArray.get(i).asFloat());
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
        connect.add("radar","Bot");
        connect.add("gps","GPS");
        connect.add("scanner","Scanner");
        
        String connectS = connect.toString();
        outbox.setReceiver(new AgentID("Denebola"));
        outbox.setContent(connectS);

        this.send(outbox);
        
        try {
            inbox = this.receiveACLMessage();
        } catch (InterruptedException ex) {
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
            //sendExit(false,new AgentID("GPS"));         // Mandamos señal de continuación al GPS
            //sendExit(false,new AgentID("Scanner"));     // Mandamos señal de continuación al Scanner
            
            while(!exit){
                
                try {
                    inbox = this.receiveACLMessage();
                } catch (InterruptedException ex) {
                }
            
                processData(Json.parse(inbox.getContent()).asObject());
                
                if(battery<=4){
                    sendOrder("refuel");    //  Le mandamos al servidor que haga refuel
                }else{                                          //  Hablando con el profesor nos ha explicado que si se hace
                                                                //  refuel, el servidor vuelve a mandar los datos, por lo que
                    if(radar.get(12) == 2) solution = true;     //  considero oportuno que empiece de 0 el bucle.                    
                    else solution = false;

                    if(solution){
                        sendExit(true,new AgentID("GPS"));
                        sendExit(true,new AgentID("Scanner"));
                        exit = true;
                    }else{
                        //sendExit(false,new AgentID("GPS"));
                        receiveGPS();
                        //sendExit(false,new AgentID("Scanner"));
                        receiveScanner();
                        heuristic = botHeuristic();

                        if(heuristic == "NO"){
                            sendExit(true,new AgentID("GPS"));
                            sendExit(true,new AgentID("Scanner"));
                            exit = true;
                        }else{
                            sendOrder(heuristic);                   // Mandar orden servidor
                            exit = resultServer();                  // Procesar respuesta servidor
                            if(exit){                               // Cerrar agentes si da error
                                sendExit(exit,new AgentID("GPS"));
                                sendExit(exit,new AgentID("Scanner"));
                            }
                        }
                    }
                }
            }
        }
    }
    
    @Override 
    public void finalize(){
        sendOrder("logout");            // Desconectar del servidor
        super.finalize();
    }
    
    
}
