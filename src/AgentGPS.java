
import es.upv.dsic.gti_ia.core.ACLMessage;
import es.upv.dsic.gti_ia.core.AgentID;
import es.upv.dsic.gti_ia.core.SingleAgent;
import java.util.logging.Level;
import java.util.logging.Logger;


public class AgentGPS extends SingleAgent{
    
    private boolean exit, datosrecibidosservidor;
    private ACLMessage inbox,outbux;
    
    public AgentGPS(AgentID aid) throws Exception {
        super(aid);
    }
    
    @Override 
    public void init(){
        exit = false;
        datosrecibidosservidor = false;
    }
    
    @Override 
    public void execute(){
        
        try{
            while(!exit){
                inbox = this.receiveACLMessage();
                
                if(inbox.getSender().getLocalName().equals("Bot")){
                    if(inbox.getContent().equals("ERROR"))
                        exit = true;    
                    else if (datosrecibidosservidor){
                        //enviar respuesta
                        datosrecibidosservidor = false;
                    }
                }else{
                    processdata();
                    datosrecibidosservidor = true;
                }
            }
        } catch (InterruptedException ex) {
            Logger.getLogger(AgentBot.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    @Override 
    public void finalize(){
        super.finalize();
    }
}
