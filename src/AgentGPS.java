
import es.upv.dsic.gti_ia.core.AgentID;
import es.upv.dsic.gti_ia.core.SingleAgent;


public class AgentGPS extends SingleAgent{
    
    private boolean exit;
    
    public AgentGPS(AgentID aid) throws Exception {
        super(aid);
    }
    
    @Override 
    public void init(){
        exit = false;
    }
    
    @Override 
    public void execute(){
        while(!exit){
        
        }
    }
    
    @Override 
    public void finalize(){
        super.finalize();
    }
}
