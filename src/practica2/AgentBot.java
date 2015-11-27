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
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import sun.java2d.loops.FillRect;

/**
 * <code>AgentBot</code> es la clase encargada de gestionar la interacción
 * final con el servidor y usuario. Esto implica recibir datos, enviar órdenes,
 * procesar información, generar resultados visibles,etc.
 *
 * @author      Samuel Peralta Antequera
 * @author      Adrián Lara Roldán
 * @author      Francisco Jesús Forte Jiménez
 * @author      Raúl Martín Pineda
 * @author      Raúl Alberto Calderón López
 * @author      Raúl López Arévalo
 *
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
    private String map;
    //  Variables para la heurística
    private HashMap<Pair<Integer, Integer>, Integer> path;
    private Integer[][] worldMatrix;
    private String heuristic;
    private boolean solution;
    // variables para mensages
    AgentID IDscanner;
    AgentID IDgps;
    AgentID IDdenebola;

    //Para crear la imagen
    private File jpgfile;
    private BufferedImage bimage;
    private int counter;

    /**
     * Constructor de la clase <code>AgentBot</code>.
     * Se encarga de inicializar varios parámetros necesarios
     * para la comunicación y la gestión interna del agente.
     *
     * @author      Samuel Peralta Antequera
     * @author      Adrián Lara Roldán
     *
     * @param aid   ID del agente.
     *
     */
    public AgentBot(AgentID aid) throws Exception {
        super(aid);
        outbox = new ACLMessage();
        radar = new ArrayList<>();
        scan = new ArrayList<>();
        path = new HashMap<>();
        map = "map1";
        IDscanner = new AgentID("scanner");
        IDgps = new AgentID("gps");
        IDdenebola = new AgentID("Denebola");
        worldMatrix = new Integer[510][510];
        for(int i=0;i<510;i++){
            for(int j=0;j<510;j++){
                worldMatrix[i][j]=-1;
            }
        }
        exit = false;
        solution = true;
    }

    /**
     * Inicializador de la clase <code>AgentBot</code>.
     * Se encarga de inicializar varios parámetros necesarios
     * para la comunicación.
     *
     * @author      Samuel Peralta Antequera
     *
     */
    @Override
    public void init() {
        outbox.setSender(this.getAid());
    }

    /**
     * Conecta el agente con el servidor.
     * Se encarga de conectarse al servidor con
     * el mapa deseado y con las opciones necesarias
     * para poder resolver los distintos mapas.
     *
     * @author      Samuel Peralta Antequera
     * @author      Adrián Lara Roldán
     */
    public boolean connectServer() {
        JsonObject connect = Json.object();
        connect.add("command", "login");
        connect.add("world", map);
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
            System.out.println("Conexion correcta");
            return true;
        }
    }

    /**
     * Función encargada de gestionar la resolución del problema.
     * Cuerpo principal del agente <code>bot</code>. Recibe y analiza
     * todos los datos disponibles y realiza la acción más óptima para
     * lograr llegar al objetivo.
     *
     * @author      Samuel Peralta Antequera
     * @author      Adrián Lara Roldán
     * @author      Francisco Jesús Forte Jiménez
     * @author      Raúl Martín Pineda
     *
     */
    @Override
    public void execute() {

        if (!connectServer()) {
            sendExit(true, IDgps);         //  Mandamos señal de finalización al GPS
            sendExit(true, IDscanner);     //  Mandamos señal de finalización al Scanner
        } else {
            while (!exit) {
                counter++;

                try {
                    inbox = this.receiveACLMessage();
                } catch (InterruptedException ex) {
                }

                processData(Json.parse(inbox.getContent()).asObject());

                if (battery <= 4) {
                    sendOrder("refuel");    //  Le mandamos al servidor que haga refuel
                    try {

                        inbox = this.receiveACLMessage();
                        System.out.println(" repostaje = " + inbox.getContent());
                    } catch (InterruptedException ex) {
                        Logger.getLogger(AgentBot.class.getName()).log(Level.SEVERE, null, ex);
                    }
                } else {

                    if (radar.get(12) == 2) {
                        solution = true;
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

                        updateMatrix();
                        generateImage();

                        heuristic = botHeuristic();

                        if (heuristic.equals("NO")) {
                            System.out.println("NO HAY SOLUCIÓN");
                            sendExit(true, IDgps);         //  Mandamos señal de finalización al GPS
                            sendExit(true, IDscanner);
                            exit = true;
                        } else {
                            System.out.println("muevete al " + heuristic);
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

    /**
     * Función encargada de recibir la información del agente <code>gps</code>.
     * Procesa los datos recibidos y los almacena internamente para su uso
     * posterior.
     *
     * @author      Samuel Peralta Antequera
     * @author      Adrián Lara Roldán
     * @author      Raúl Alberto Calderón
     *
     */
    public void receiveGPS() {

        outbox.setReceiver(IDgps);
        outbox.setContent("Send");
        this.send(outbox);

        try {
            inbox = this.receiveACLMessage();
        } catch (InterruptedException ex) {
        }
        JsonObject aux = Json.parse(inbox.getContent()).asObject();
        coorX = aux.get("x").asInt() + 2;
        coorY = aux.get("y").asInt() + 2;
    }

    /**
     * Función encargada de recibir la información del agente <code>scanner</code>.
     * Procesa los datos recibidos y los almacena internamente para su uso
     * posterior.
     *
     * @author      Samuel Peralta Antequera
     * @author      Adrián Lara Roldán
     * @author      Raúl Alberto Calderón
     *
     */
    public void receiveScanner() {
        outbox.setReceiver(IDscanner);
        outbox.setContent("Send");
        this.send(outbox);

        try {
            inbox = this.receiveACLMessage();
        } catch (InterruptedException ex) {
        }

        String auxContent = inbox.getContent();
        JsonArray auxArray = Json.parse(auxContent).asArray();

        scan.clear();
        for (int i = 0; i < 25; i++) {
            scan.add(auxArray.get(i).asFloat());
        }
    }

    /**
     * Función encargada de mandar órdenes al servidor.
     * Envía la orden deseada al servidor.
     *
     * @author      Samuel Peralta Antequera
     *
     * @param send  Orden a enviar al servidor.
     */
    public void sendOrder(String send) {

        outbox.setReceiver(IDdenebola);
        JsonObject order = Json.object();
        order.add("command", send);
        order.add("key", key);
        outbox.setContent(order.toString());
        this.send(outbox);
    }

    /**
     * Función encargada de procesar el resultado envíado
     * por el servidor.
     * Recolecta la información que envía el servidor relacionada
     * con el movimiento del agente.
     *
     * @author      Samuel Peralta Antequera
     *
     */
    public boolean resultServer() {
        try {
            inbox = this.receiveACLMessage();
        } catch (InterruptedException ex) {
        }

        JsonObject resultSon = Json.parse(inbox.getContent()).asObject();
        String result = resultSon.get("result").asString();

        if (result.equals("CRASHED") || result.contains("BAD_")) {
            if (result.equals("CRASHED")) {
                System.out.println("TE ESTRELLASTEEEEEE");
            }
            return true;        //  Devuelve true porque exit = resultServer()
        } else {
            return false;      //  Por lo que exit = true cuando haya error.
        }
    }

     /**
     * Calcula la casilla más cercana al objetivo que no
     * no sea un muro.
     *
     * @author      Adrián Lara Roldán
     *
     * @param rad  Casillas adyacentes al <code>bot</code>.
     * @param scan   Casillas adyacentes al <code>bot</code> con el gradiente.
     *
     */
    public int min(ArrayList<Float> scan, ArrayList<Integer> rad) {
        Float min = (Float) Collections.max(scan);
        int pos = -1;
        for (int i = 0; i < scan.size(); i++) {
            if (scan.get(i) < min && rad.get(i) != 1) {
                pos = i;
                min = scan.get(i);
            }
        }

        return pos;

    }

     /**
     * Decidimos el movimiento siguiente del bot, siempre conocemos mediante el agente
     * gps en que posición del mapa estamos y con el radar evitamos chocarnos.
     * utilizando el agente scanner elegimos la posición mas prometedora, teniendo en cuenta
     * que hayamos pasado pocas veces por ella, y almacenamos la ruta que vamos
     * siguiendo en la variable path, de tipo Hash, una vez hecho esto
     * cada vez que pasamos por una casilla aumentamos en uno el campo "valor" del Hash
     * y asi llevamos una cuenta siempre de cuantas veces hemos pasado por un sitio,
     * en caso de pasar mas de 10 veces por un sitio asumimos que no existe solución.
     *
     * @author      Raúl Martín Pineda
     * @author      Francisco Jesús Forte
     * @author      Adrián Lara
     *
     * @return  siguiente movimiento.
     *
     */
    public String botHeuristic() {
        int pos;
        String R = "NO";
        ArrayList<Float> auxS = new ArrayList<>();
        ArrayList<Integer> auxR = new ArrayList<>();
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

        pos = this.min(auxS, auxR);

        boolean salir = false;
        while (!salir) {
            if(this.path.containsValue(10)){
               salir = true;
            }
            switch (pos) {
                case 0:
                    //R="moveNW";
                    if (!this.path.containsKey(new Pair(coorX - 1, coorY - 1))) {
                        R = "moveNW";
                        salir = true;
                    } else {
                        auxS.set(0, (Float) Collections.max(this.scan));
                        pos = this.min(auxS, auxR);
                    }
                    break;
                case 1:
                    //R="moveN";
                    if (!this.path.containsKey(new Pair(coorX, coorY - 1))) {
                        R = "moveN";
                        salir = true;
                    } else {
                        auxS.set(1, (Float) Collections.max(this.scan));
                        pos = this.min(auxS, auxR);
                    }
                    break;
                case 2:
                    //R="moveNE";
                    if (!this.path.containsKey(new Pair(coorX + 1, coorY - 1))) {
                        R = "moveNE";
                        salir = true;
                    } else {
                        auxS.set(2, (Float) Collections.max(this.scan));
                        pos = this.min(auxS, auxR);
                    }
                    break;
                case 3:
                    //R="moveW";
                    if (!this.path.containsKey(new Pair(coorX - 1, coorY))) {
                        R = "moveW";
                        salir = true;
                    } else {
                        auxS.set(3, (Float) Collections.max(this.scan));
                        pos = this.min(auxS, auxR);
                    }
                    break;
                case 4:
                    //R="moveE";
                    if (!this.path.containsKey(new Pair(coorX + 1, coorY))) {
                        R = "moveE";
                        salir = true;
                    } else {
                        auxS.set(4, (Float) Collections.max(this.scan));
                        pos = this.min(auxS, auxR);
                    }
                    break;
                case 5:
                    //R="moveSW";
                    if (!this.path.containsKey(new Pair(coorX - 1, coorY + 1))) {
                        R = "moveSW";
                        salir = true;
                    } else {
                        auxS.set(5, (Float) Collections.max(this.scan));
                        pos = this.min(auxS, auxR);
                    }
                    break;
                case 6:
                    //R="moveS";
                    if (!this.path.containsKey(new Pair(coorX, coorY + 1))) {
                        R = "moveS";
                        salir = true;
                    } else {
                        auxS.set(6, (Float) Collections.max(this.scan));
                        pos = this.min(auxS, auxR);
                    }
                    break;
                case 7:
                    //R="moveSE";
                    if (!this.path.containsKey(new Pair(coorX + 1, coorY - 1))) {
                        R = "moveSE";
                        salir = true;
                    } else {
                        auxS.set(7, (Float) Collections.max(this.scan));
                        pos = this.min(auxS, auxR);
                    }
                    break;
                case -1:
                    R = botHeuristic2();

                    salir = true;
            }
        }
        //return "moveSW";
        //System.out.println(R);
        return R;
    }

     /**
     * En casos donde la heurística anterior pudiera generar un error lógico
     * que nos dejara sin poder mover el bot, asumimos que pueda alejarse del
     * objetivo que marca el scanner para poder salir de algunos obstáculos.
     *
     * @author      Raúl Martín Pineda
     * @author      Francisco Jesús Forte
     * @author      Adrián Lara
     *
     * @return      Siguiente movimiento.
     *
     */
    public String botHeuristic2() {
        String R = new String();
        R = "NO";
        Pair<Integer, Integer> N = new Pair(coorX, coorY - 1);
        Pair<Integer, Integer> S = new Pair(coorX, coorY + 1);
        Pair<Integer, Integer> E = new Pair(coorX + 1, coorY);
        Pair<Integer, Integer> W = new Pair(coorX - 1, coorY);
        Pair<Integer, Integer> NE = new Pair(coorX + 1, coorY - 1);
        Pair<Integer, Integer> NW = new Pair(coorX - 1, coorY - 1);
        Pair<Integer, Integer> SE = new Pair(coorX + 1, coorY - 1);
        Pair<Integer, Integer> SW = new Pair(coorX - 1, coorY + 1);

        ArrayList<Float> auxS = new ArrayList<>();
        auxS.add(scan.get(7));//N
        auxS.add(scan.get(17));//S
        auxS.add(scan.get(13));//E
        auxS.add(scan.get(11));//W
        auxS.add(scan.get(8));//NE
        auxS.add(scan.get(6));//NW
        auxS.add(scan.get(18));//SE
        auxS.add(scan.get(16));//SW

        ArrayList<Pair<Integer, Integer>> aux = new ArrayList<>();
        aux.add(N);
        aux.add(S);
        aux.add(E);
        aux.add(W);
        aux.add(NE);
        aux.add(NW);
        aux.add(SE);
        aux.add(SW);

        int min = 999999999;
        int dir = -1;

        Pair n;
        int M;
        for (int i = 0; i < aux.size(); i++) {
            n = aux.get(i);
            if (path.containsKey(n)) {
                M = path.get(n);
                if (M *auxS.get(i)< min && M < 3) {
                    min = M;
                    dir = i;
                }
            }
        }

        if (dir >= 0) {
            System.out.println("SEGUNDA HEURISTICA");
            switch (dir) {
                case 0:
                    R = "moveN";
                    break;
                case 1:
                    R = "moveS";
                    break;
                case 2:
                    R = "moveE";
                    break;
                case 3:
                    R = "moveW";
                    break;
                case 4:
                    R = "moveNE";
                    break;
                case 5:
                    R = "moveNW";
                    break;
                case 6:
                    R = "moveSE";
                    break;
                case 7:
                    R = "moveSW";
                    break;
                default:
                    R = "NO";

            }
        }

        return R;
    }

    /**
     * Función encargada de procesar los datos recibidos
     * de los sensores del servidor.
     * Almacena internamente los datos recibidos del
     * servidor.
     *
     * @author      Samuel Peralta Antequera
     * @author      Adrián Lara Roldán
     *
     * @param data
     *
     */
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

            battery = (int) data.get("battery").asDouble();

        } catch (InterruptedException ex) {
            Logger.getLogger(AgentBot.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Función encargada de sincronizar la finalización de los
     * otros agente.
     * Si se produce algún error, se les envía un mensade de
     * desconexión al resto de agentes. En caso contrario, se
     * les indica que prosigan con su ejecución.
     *
     * @author      Samuel Peralta Antequera
     *
     * @param e         <code>true</code> si debe desconectarse el agente
     *                  receptor. <code>false</code> en caso de que deba
     *                  seguir su ejecución.
     * @param receiver  Identificador del agente que debe recibir el
     *                  mensaje.
     *
     */
    public void sendExit(boolean e, AgentID receiver) {
        outbox.setReceiver(receiver);
        if (e) {
            outbox.setContent("ERROR");
        } else {
            outbox.setContent("OK");
        }
        this.send(outbox);
    }

    /**
     * Termina la ejecución del agente <code>bot</code>.
     * Llama al finalizador de la super clase y realiza
     * la desconexión del servidor.
     *
     * @author      Samuel Peralta Antequera
     *
     */
    @Override
    public void finalize() {
        sendOrder("logout");            // Desconectar del servidor

        super.finalize();
    }

    /**
     * Muestra por pantalla la información recibida del
     * agente <code>radar</code>.
     *
     * @author      Adrián Lara Roldán
     *
     */
    public void printRadar() {
        System.out.println("Radar: ");
        for (int i = 0; i < 5; i++) {
            for (int k = 0; k < 5; k++) {
                System.out.print(radar.get(i * 5 + k) + " ");
            }
            System.out.print("\n");
        }
        System.out.print("\n");
    }

    /**
     * Muestra por pantalla la información recibida del
     * agente <code>scanner</code>.
     *
     * @author      Adrián Lara Roldán
     *
     */
    public void printScanner() {
        System.out.println("Scanner:");
        for (int i = 0; i < 5; i++) {
            for (int k = 0; k < 5; k++) {
                System.out.print(scan.get(i * 5 + k) + " ");
            }
            System.out.print("\n");
        }
        System.out.print("\n");
    }

    /**
     * Muestra por pantalla la información del mapa
     * del mundo interno.
     *
     * @author      Adrián Lara Roldán
     *
     */
    public void printMap() {
        System.out.println("Mapa:");

        for (int i = 0; i < 510; i++) {
            for (int j = 0; j < 510; j++) {
                System.out.print(worldMatrix[i][j]);
            }
            System.out.print("\n");
        }

    }

    /**
     * Muestra por pantalla la información recibida del
     * agente <code>gps</code>.
     *
     * @author      Adrián Lara Roldán
     *
     */
    public void printGPS() {
        System.out.println("GPS:");
        System.out.println("coordenada X: " + coorX + " ,coordenada Y: " + coorY);
    }

    /**
     * Actualiza la matriz del mundo.
     * Sobreescribe la información del mundo
     * conforme se recibe del radar.
     *
     * @author      Francisco Jesús Forte Jiménez
     *
     */
    public void updateMatrix(){
        Pair<Integer, Integer> k = new Pair(this.coorX, this.coorY);

        if (path.containsKey(k)) {
            path.replace(k, path.get(k), path.get(k) + 1);
        } else {
            this.path.put(new Pair(this.coorX, this.coorY), 0);
            worldMatrix[coorY][coorX] = 3;
        }

        if (worldMatrix[coorY-2][coorX-2] != 3)
            worldMatrix[coorY-2][coorX-2] = radar.get(0);

        if (worldMatrix[coorY-2][coorX-1] != 3)
            worldMatrix[coorY-2][coorX-1] = radar.get(1);

        if (worldMatrix[coorY-2][coorX] != 3)
            worldMatrix[coorY-2][coorX] = radar.get(2);

        if (worldMatrix[coorY-2][coorX+1] != 3)
            worldMatrix[coorY-2][coorX+1] = radar.get(3);

        if (worldMatrix[coorY-2][coorX+2] != 3)
            worldMatrix[coorY-2][coorX+2] = radar.get(4);

        if (worldMatrix[coorY-1][coorX-2] != 3)
            worldMatrix[coorY-1][coorX-2] = radar.get(5);

        if (worldMatrix[coorY-1][coorX-1] != 3)
            worldMatrix[coorY-1][coorX-1] = radar.get(6);

        if (worldMatrix[coorY-1][coorX] != 3)
            worldMatrix[coorY-1][coorX] = radar.get(7);

        if (worldMatrix[coorY-1][coorX+1] != 3)
            worldMatrix[coorY-1][coorX+1] = radar.get(8);

        if (worldMatrix[coorY-1][coorX+2] != 3)
            worldMatrix[coorY-1][coorX+2] = radar.get(9);

        if (worldMatrix[coorY][coorX-2] != 3)
            worldMatrix[coorY][coorX-2] = radar.get(10);

        if (worldMatrix[coorY][coorX-1] != 3)
            worldMatrix[coorY][coorX-1] = radar.get(11);

        if (worldMatrix[coorY][coorX+1] != 3)
            worldMatrix[coorY][coorX+1] = radar.get(13);

        if (worldMatrix[coorY][coorX+2] != 3)
            worldMatrix[coorY][coorX+2] = radar.get(14);

        if (worldMatrix[coorY+1][coorX-2] != 3)
            worldMatrix[coorY+1][coorX-2] = radar.get(15);

        if (worldMatrix[coorY+1][coorX-1] != 3)
            worldMatrix[coorY+1][coorX-1] = radar.get(16);

        if (worldMatrix[coorY+1][coorX] != 3)
            worldMatrix[coorY+1][coorX] = radar.get(17);

        if (worldMatrix[coorY+1][coorX+1] != 3)
            worldMatrix[coorY+1][coorX+1] = radar.get(18);

        if (worldMatrix[coorY+1][coorX+2] != 3)
            worldMatrix[coorY+1][coorX+2] = radar.get(19);

        if (worldMatrix[coorY+2][coorX-2] != 3)
            worldMatrix[coorY+2][coorX-2] = radar.get(20);

        if (worldMatrix[coorY+2][coorX-1] != 3)
            worldMatrix[coorY+2][coorX-1] = radar.get(21);

        if (worldMatrix[coorY+2][coorX] != 3)
            worldMatrix[coorY+2][coorX] = radar.get(22);

        if (worldMatrix[coorY+2][coorX+1] != 3)
            worldMatrix[coorY+2][coorX+1] = radar.get(23);

        if (worldMatrix[coorY+2][coorX+2] != 3)
            worldMatrix[coorY+2][coorX+2] = radar.get(24);

    }

    /**
     * Genera la imagen de la ejecución actual.
     * Crea una nueva imagen a partir de los datos
     * que tenemos del mundo actual.
     *
     * @author  Francisco Jesús Forte Jiménez
     *
     */
    public void generateImage(){
        jpgfile = new File(map+"_"+ Integer.toString(counter)+".jpg");
        bimage = new BufferedImage(1020, 1020,BufferedImage.TYPE_INT_RGB);
        Graphics g = bimage.getGraphics();

        try {

            for(int i=0;i<510;i++){
                for(int j=0;j<510;j++){
                    if (worldMatrix[j][i] == 0){
                        g.setColor(Color.white);
                        g.fillRect(i*2, j*2, i*2, j*2);
                    }else if (worldMatrix[j][i] == 1){
                        g.setColor(Color.black);
                        g.fillRect(i*2, j*2, i*2, j*2);
                    }else if (worldMatrix[j][i] == 3){
                        g.setColor(Color.green);
                        g.fillRect(i*2, j*2, i*2, j*2);
                    }else if (worldMatrix[j][i] == 2){
                        g.setColor(Color.red);
                        g.fillRect(i*2, j*2, i*2, j*2);
                    }else if (worldMatrix[j][i] == -1){
                        g.setColor(Color.BLUE);
                        g.fillRect(i*2, j*2, i*2, j*2);
                    }
                }
            }

            g.setColor(Color.white);
            g.setFont(new Font(g.getFont().getFontName(), Font.PLAIN, 20));
            g.drawString("GPS: ("+ Integer.toString(coorX) +"," + Integer.toString(coorY)+")", 850, 50);


            ImageIO.write(bimage, "jpg", jpgfile);
        } catch (IOException e) {
            System.out.println("Error al crear imagen");
        }
    }

}
