
import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import es.upv.dsic.gti_ia.core.ACLMessage;
import es.upv.dsic.gti_ia.core.AgentID;
import es.upv.dsic.gti_ia.core.SingleAgent;
import java.util.logging.Level;
import java.util.logging.Logger;


public class AgentScanner extends SingleAgent{

    private boolean exit;
    private ACLMessage inbox, outbox;
    private boolean dataReceived;
    private String sendData;
    
    public AgentScanner(AgentID aid) throws Exception {
        super(aid);
    }
    
    public void processData(JsonObject data){
        JsonArray aux = data.get("Scanner").asArray();
        sendData = aux.toString();
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
                
                if(inbox.getSender().getLocalName().equals("bot")){
                    if(inbox.getContent().equals("ERROR")){
                        exit = true;    
                    }else if (dataReceived){
                        outbox.setContent(sendData);
                        this.send(outbox);
                        dataReceived = false;
                    }else if(!dataReceived){                //  En caso de que el agente Bot pida 
                        inbox = this.receiveACLMessage();   //  datos antes de que lleguen al agente Scanner. Puede bloquear
                        processData(Json.parse(inbox.getContent()).asObject());
                        outbox.setContent(sendData);
                        this.send(outbox);
                        dataReceived = false;
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
