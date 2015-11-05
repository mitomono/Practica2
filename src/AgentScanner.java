
import es.upv.dsic.gti_ia.core.AgentID;
import es.upv.dsic.gti_ia.core.SingleAgent;


public class AgentScanner extends SingleAgent{

    private boolean exit;
    
    public AgentScanner(AgentID aid) throws Exception {
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
