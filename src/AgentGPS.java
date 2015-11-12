
import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import es.upv.dsic.gti_ia.core.ACLMessage;
import es.upv.dsic.gti_ia.core.AgentID;
import es.upv.dsic.gti_ia.core.SingleAgent;
import java.util.logging.Level;
import java.util.logging.Logger;


public class AgentGPS extends SingleAgent{
    
    private boolean exit;
    private ACLMessage inbox, outbox;
    private boolean dataReceived;
    private String sendData;
    
    public AgentGPS(AgentID aid) throws Exception {
        super(aid);
    }
    
    public void processData(JsonObject data){
        JsonValue coordenadas = data.get("gps");
        sendData = coordenadas.toString();
    }
    
   @Override 
    public void init(){
        exit = false;
        outbox.setSender(this.getAid());
        outbox.setReceiver(new AgentID("bot"));
        dataReceived = false;
    }
    
    @Override 
    public void execute(){
        
        try{
            while(!exit){
                inbox = this.receiveACLMessage();
                System.out.println(inbox.getContent());
                if(inbox.getSender().getLocalName().equals("bot")){
                    if(inbox.getContent().equals("ERROR")){
                        exit = true;    
                    }else if (dataReceived){
                        outbox.setContent(sendData);
                        this.send(outbox);
                        dataReceived = false;
                    }else if(!dataReceived){                //  En caso de que el agente Bot pida 
                        inbox = this.receiveACLMessage();   //  datos antes de que lleguen al agente GPS. Puede bloquear
                        processData(Json.parse(inbox.getContent()).asObject());
                        outbox.setContent(sendData);
                        this.send(outbox);
                    }
                }else{
                    processData(Json.parse(inbox.getContent()).asObject());
                    dataReceived = true;
                }
            }
        } catch (InterruptedException ex) {
        }
    }
    
    @Override 
    public void finalize(){
        super.finalize();
    }
}