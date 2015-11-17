package practica2;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import es.upv.dsic.gti_ia.core.ACLMessage;
import es.upv.dsic.gti_ia.core.AgentID;
import es.upv.dsic.gti_ia.core.SingleAgent;


public class AgentScanner extends SingleAgent{

    private boolean exit;
    private ACLMessage inbox, outbox;
    private boolean dataReceived;
    private String sendData = new String();
    
    public AgentScanner(AgentID aid) throws Exception {
        super(aid);
        outbox=new ACLMessage();
        exit = false;
        dataReceived = false;
    }
    
    @Override 
    public void init(){
        outbox.setSender(this.getAid());
        outbox.setReceiver(new AgentID("bot"));  
    }
    
    public void processData(JsonObject data){
        JsonArray aux = data.get("scanner").asArray();
        sendData = aux.toString();
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
        } catch (InterruptedException ex) {}
    }
    
    @Override 
    public void finalize(){
        super.finalize();
    }   
}
