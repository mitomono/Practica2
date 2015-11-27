package practica2;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import es.upv.dsic.gti_ia.core.ACLMessage;
import es.upv.dsic.gti_ia.core.AgentID;
import es.upv.dsic.gti_ia.core.SingleAgent;

/**
 * <code>AgentGPS</code> es la clase encargada de gestionar cualquier
 * aspecto relacionado con el agente <code>gps</code>. 
 * Esto implica gestionar los datos recibidos del servidor y los datos a 
 * enviar al agente <code>bot</code>.
 * 
 * @author      Samuel Peralta Antequera
 * 
 */
public class AgentGPS extends SingleAgent{
    
    private boolean exit;
    private ACLMessage inbox, outbox;
    private boolean dataReceived;
    private String sendData;
  
    /**
     * Constructor de la clase <code>AgentGPS</code>. 
     * Se encarga de inicializar varios parámetros necesarios
     * para la comunicación y la gestión interna del agente.
     * 
     * @author      Samuel Peralta Antequera
     * 
     * @param aid   ID del agente.
     * 
     */
    public AgentGPS(AgentID aid) throws Exception {
        super(aid);
        outbox = new ACLMessage();
        sendData= new String();
        exit = false;
        dataReceived = false;
    }
    /**
     * Inicializador de la clase <code>AgentGPS</code>. 
     * Se encarga de inicializar varios parámetros necesarios
     * para la comunicación.
     * 
     * @author      Samuel Peralta Antequera
     * 
     */
    @Override 
    public void init(){    
        outbox.setSender(this.getAid());
        outbox.setReceiver(new AgentID("bot"));    
    }
    
    /**
     * Procesa los datos recibidos del servidor. 
     * Guarda los datos recibidos por el servidor
     * en la memoria interna del agente.
     * 
     * @author      Samuel Peralta Antequera
     * 
     * @param data  Datos a almacenar internamente.
     * 
     */
    public void processData(JsonObject data){
        JsonValue coordenadas = data.get("gps");
        sendData = coordenadas.toString();
    }
    
    /**
     * Función encargada de gestionar la comunicación. 
     * Cuerpo principal del agente <code>gps</code>.
     * Analiza las comunicaciones y realiza las acciones 
     * pertinentes, ya sea procesar o enviar datos.
     * 
     * @author      Samuel Peralta Antequera
     * 
     */
    @Override 
    public void execute(){
        try{
            while(!exit){
                inbox = this.receiveACLMessage();
                //System.out.println(inbox.getContent());
                
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
        } catch (InterruptedException ex) {}
    }

    /**
     * Termina la ejecución del agente <code>gps</code>. 
     * Llama al finalizador de la super clase.
     * 
     * @author      Samuel Peralta Antequera
     * 
     */
    @Override 
    public void finalize(){
        super.finalize();
    }
}