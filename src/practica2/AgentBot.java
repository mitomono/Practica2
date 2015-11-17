package practica2;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import edu.emory.mathcs.backport.java.util.Collections;
import es.upv.dsic.gti_ia.core.ACLMessage;
import es.upv.dsic.gti_ia.core.AgentID;
import es.upv.dsic.gti_ia.core.SingleAgent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.util.Pair;

/*
*   Clase AgentBot que se encargará de procesar los datos y
*   mandar las órdenes al servidor.
 */
public class AgentBot extends SingleAgent {

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
    private HashMap<Pair<Integer,Integer>,Integer> path;
    private int[][] worldMatrix;
    private String heuristic;
    private boolean solution;
    // variables para mensages
    AgentID IDscanner;
    AgentID IDgps;
    AgentID IDdenebola;

    public AgentBot(AgentID aid) throws Exception {
        super(aid);
        outbox = new ACLMessage();
        radar = new ArrayList<>();
        scan = new ArrayList<>();
        path = new HashMap<>();
        IDscanner = new AgentID("scanner");
        IDgps = new AgentID("gps");
        IDdenebola = new AgentID("Denebola");
        worldMatrix = new int[500][500];
        exit = false;
        solution = true;
    }

    @Override
    public void init() {
        outbox.setSender(this.getAid());
    }

    public boolean connectServer() {
        JsonObject connect = Json.object();
        connect.add("command", "login");
        connect.add("world", "map10");
        connect.add("radar", "bot");
        connect.add("battery", "bot");
        connect.add("gps", "gps");
        connect.add("scanner", "scanner");
        String connectS = connect.toString();
        outbox.setReceiver(IDdenebola);
        outbox.setContent(connectS);

        this.send(outbox);

        try {
            inbox = this.receiveACLMessage();
        } catch (InterruptedException ex) {
        }

        JsonObject answer = Json.parse(inbox.getContent()).asObject();
        key = answer.get("result").asString();

        if (key.contains("BAD_")) {
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void execute() {
        if (!connectServer()) {
            sendExit(true, IDgps);         //  Mandamos señal de finalización al GPS
            sendExit(true, IDscanner);     //  Mandamos señal de finalización al Scanner 
        } else {
            while (!exit) {
                try {
                    inbox = this.receiveACLMessage();
                } catch (InterruptedException ex) {
                }

                System.out.println(inbox.getContent());
                processData(Json.parse(inbox.getContent()).asObject());

                if (battery <= 4) {
                    sendOrder("refuel");    //  Le mandamos al servidor que haga refuel
                    try {
                        inbox = this.receiveACLMessage();
                        System.out.println(" repostaje = "+inbox.getContent());
                    } catch (InterruptedException ex) {
                        Logger.getLogger(AgentBot.class.getName()).log(Level.SEVERE, null, ex);
                    }
                } else {                                          //  Hablando con el profesor nos ha explicado que si se hace
                    //  refuel, el servidor vuelve a mandar los datos, por lo que
                    if (radar.get(12) == 2) {
                        solution = true;     //  considero oportuno que empiece de 0 el bucle.   
                        System.out.println("ENCONTRADOOO");
                    } else {
                        solution = false;
                    }

                    if (solution) {
                        sendExit(solution, IDgps);         //  Mandamos señal de finalización al GPS
                        sendExit(solution, IDscanner);
                        exit = true;
                    } else {
                        receiveGPS();
                        receiveScanner();
                        heuristic = botHeuristic();

                        if (heuristic.equals("NO")) {
                            System.out.println("FALLO DE HEURISTICA");
                            sendExit(true, IDgps);         //  Mandamos señal de finalización al GPS
                            sendExit(true, IDscanner);
                            exit = true;
                        } else {
                            sendOrder(heuristic);                   // Mandar orden servidor
                            exit = resultServer();                  // Procesar respuesta servidor
                            if (exit) {                               // Cerrar agentes si da error
                                sendExit(true, IDgps);         //  Mandamos señal de finalización al GPS
                                sendExit(true, IDscanner);
                            }
                        }
                    }
                }
            }
        }
    }

    public void receiveGPS() {

        outbox.setReceiver(IDgps);
        outbox.setContent("Send");
        this.send(outbox);

        try {
            inbox = this.receiveACLMessage();
        } catch (InterruptedException ex) {
        }
        JsonObject aux = Json.parse(inbox.getContent()).asObject();
        coorX = aux.get("x").asInt();
        coorY = aux.get("y").asInt();
    }

    public void receiveScanner() {
        outbox.setReceiver(IDscanner);
        outbox.setContent("Send");
        this.send(outbox);

        try {
            inbox = this.receiveACLMessage();
        } catch (InterruptedException ex) {}
        
        String auxContent = inbox.getContent();
        JsonArray auxArray = Json.parse(auxContent).asArray();

        scan.clear();
        for (int i = 0; i < 25; i++) {
            scan.add(auxArray.get(i).asFloat());
        }
    }

    public void sendOrder(String send) {
        Pair<Integer,Integer> k = new Pair(this.coorX,this.coorY);
        this.path.putIfAbsent(k, 0);
        outbox.setReceiver(IDdenebola);
        JsonObject order = Json.object();
        order.add("command", send);
        order.add("key", key);
        outbox.setContent(order.toString());
        this.send(outbox);
    }

    public boolean resultServer() {
        try {
            inbox = this.receiveACLMessage();
        } catch (InterruptedException ex) {}

        JsonObject resultSon = Json.parse(inbox.getContent()).asObject();
        String result = resultSon.get("result").asString();

        if (result.equals("CRASHED") || result.contains("BAD_")) {
            if(result.equals("CRASHED"))
                System.out.println("TE ESTRELLASTEEEEEE");
            return true;        //  Devuelve true porque exit = resultServer()
        } else {
            return false;      //  Por lo que exit = true cuando haya error.
        }
    }
    
    public int min(ArrayList<Float>m,ArrayList<Integer>m1){
        Float min = (Float)Collections.max(m);
        int pos=-1;
        for (int i =0; i < m.size(); i++){
            if(m.get(i)< min && m1.get(i)!= 1){
                pos = i;
                min = m.get(i);
            }
        }

        return pos;

    }

    public String botHeuristic() {
        int pos;
        String R = "NO";
        ArrayList<Float> auxS=new ArrayList<>();
        ArrayList<Integer> auxR=new ArrayList<>();
        auxS.add(scan.get(6));
        auxS.add(scan.get(7));
        auxS.add(scan.get(8));
        auxS.add(scan.get(11));
        auxS.add(scan.get(13));
        auxS.add(scan.get(16));
        auxS.add(scan.get(17));
        auxS.add(scan.get(18));
        
        auxR.add(this.radar.get(6));
        auxR.add(radar.get(7));
        auxR.add(radar.get(8));
        auxR.add(radar.get(11));
        auxR.add(radar.get(13));
        auxR.add(radar.get(16));
        auxR.add(radar.get(17));
        auxR.add(radar.get(18));
        
        pos = this.min(auxS,auxR);
   /*
        Float min = (Float)Collections.max(aux);
        int pos=0;
        for (int i =0; i < aux.size(); i++){
            if(aux.get(i)< min && this.radar.get(i)!= 1){
                pos = i;
                min = aux.get(i);
            }
        }
        */
   boolean salir = false;
   while(!salir){
        switch(pos){
            case 0:
                //R="moveNW";
                if(!this.path.containsKey(new Pair(coorX-1,coorY-1))){
                    R="moveNW";
                    salir = true;
                }
                else{
                    auxS.set(0, (Float)Collections.max(this.scan));
                    pos=this.min(auxS,auxR);
                }
                break;
            case 1:
                //R="moveN";
                if(!this.path.containsKey(new Pair(coorX,coorY-1))){
                    R="moveN";
                    salir = true;
                }
                else{
                    auxS.set(1, (Float)Collections.max(this.scan));
                    pos=this.min(auxS,auxR);
                }
                break;
            case 2:
                //R="moveNE";
                 if(!this.path.containsKey(new Pair(coorX+1,coorY-1))){
                    R="moveNE";
                    salir = true;
                }
                else{
                    auxS.set(2, (Float)Collections.max(this.scan));
                    pos=this.min(auxS,auxR);
                }
                break;
            case 3:
                //R="moveW";
                 if(!this.path.containsKey(new Pair(coorX-1,coorY))){
                    R="moveW";
                    salir = true;
                }
                else{
                    auxS.set(3, (Float)Collections.max(this.scan));
                    pos=this.min(auxS,auxR);
                }
                break;
            case 4:
                //R="moveE";
                 if(!this.path.containsKey(new Pair(coorX+1,coorY))){
                    R="moveE";
                    salir = true;
                }
                else{
                    auxS.set(4, (Float)Collections.max(this.scan));
                    pos=this.min(auxS,auxR);
                }
                break;
            case 5:
                //R="moveSW";
                 if(!this.path.containsKey(new Pair(coorX-1,coorY+1))){
                    R="moveSW";
                    salir = true;
                }
                else{
                    auxS.set(5, (Float)Collections.max(this.scan));
                    pos=this.min(auxS,auxR);
                }
                break;
            case 6:
                //R="moveS";
                 if(!this.path.containsKey(new Pair(coorX,coorY+1))){
                    R="moveS";
                    salir = true;
                }
                else{
                    auxS.set(6, (Float)Collections.max(this.scan));
                    pos=this.min(auxS,auxR);
                }
                break;
            case 7:
                //R="moveSE";
                 if(!this.path.containsKey(new Pair(coorX+1,coorY-1))){
                    R="moveSE";
                    salir = true;
                }
                else{
                    auxS.set(7, (Float)Collections.max(this.scan));
                    pos=this.min(auxS,auxR);
                }
                break;
            default:
                R="NO";
                salir=true;
        }
   }
        //return "moveSW";
        System.out.println(R);
        return R;
    }

    public void processData(JsonObject data) {
        try {
            if (!radar.isEmpty()) {
                radar.clear();
            }

            JsonArray radarAux = data.get("radar").asArray();
            
            for (int i = 0; i < 25; i++) {
                radar.add(radarAux.get(i).asInt());
            }
            
            inbox = this.receiveACLMessage();
            data = Json.parse(inbox.getContent()).asObject();
            System.out.println(data.toString());
            battery = (int) data.get("battery").asDouble();
            
        } catch (InterruptedException ex) {
            Logger.getLogger(AgentBot.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void sendExit(boolean e, AgentID receiver) {
        outbox.setReceiver(receiver);
        if (e) {
            outbox.setContent("ERROR");
        } else {
            outbox.setContent("OK");
        }
        this.send(outbox);
    }

    @Override
    public void finalize() {
        sendOrder("logout");            // Desconectar del servidor
        super.finalize();
    }

}
