package entities;

import threads.*;
import events.ClientConnectedEvent;
import events.ClientDisconnectedEvent;
import events.ClientNicknamedEvent;
import events.MessageReceivedEvent;
import events.ServerEventsListener;
import java.io.DataOutputStream;
import java.io.IOException;
//import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Server implements ServerEventsListener{
    private ServerSocket serverSocket;
    private Map<Integer, Client> clients;
    private int clientId;
    ConnectionsHandler connectionsHandler;
    DisconnectionsHandler disconnectionsHandler;
    
    public Server(int port) throws IOException{
        serverSocket = new ServerSocket(port);
        clients = new HashMap<>();
        clientId = 0;
        System.out.println("Servidor iniciado en el puerto " + port);
    }
    
    public void start() throws IOException{
        connectionsHandler = new ConnectionsHandler(serverSocket);
        connectionsHandler.addEventsListener(this);
        connectionsHandler.start();
        
        disconnectionsHandler = new DisconnectionsHandler(clients);
        disconnectionsHandler.addEventsListener(this);
        disconnectionsHandler.start();
    }
    
    private synchronized void sendBroadcast(String data) throws IOException{
        DataOutputStream out;
        int originID = Integer.parseInt(getData("<origin>", data));
        String originNickname = clients.get(originID).getNickname();
        
        String message = getData("<message>", data);
        for (Client client: clients.values()) {
            Socket clientSocket = client.getSocket();
            out = new DataOutputStream(clientSocket.getOutputStream());
            out.writeUTF(originNickname+": "+message);
        }
    }
    
    private synchronized void addClient(Client client){
        clients.put(client.getId(), client);
    }
 
    @Override
    public void onUserConnected(ClientConnectedEvent evt) {
        try {
            Socket socket = evt.getSocket();
            Client client = new Client(socket);
            clientId++;
            client.setId(clientId);
            addClient(client);
            
            ClientHandler clientHandler = new ClientHandler(socket.getInputStream());
            clientHandler.addEventsListener(this);
            clientHandler.start();
            
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            out.writeUTF("<id>"+clientId+";");
                        
            System.out.println("El usuario ha sido registrado con el ID = "+clientId);
        } catch(IOException ex) {
            System.out.println(ex);
        }
    }
    
    private String getData(String type, String dataMessage){
        int i = dataMessage.indexOf(type) + type.length();
        int f = dataMessage.indexOf(";", i);
        
        return dataMessage.substring(i, f);
    }
    
    @Override
    public void onReceivedMessage(MessageReceivedEvent evt) {
        String message = evt.getMessage();
        if (isDisconnectionRequest(message)){
            int id = Integer.parseInt(getData("<origin>", message));
            ClientDisconnectedEvent disconnectionEvt = new ClientDisconnectedEvent(this, id);
            onClientDisconnected(disconnectionEvt);
        } else {
            try {
                sendBroadcast(message); 
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
    
    private boolean isDisconnectionRequest(String message){
        return getData("<message>", message).equals("/salir");
    }

    private synchronized void delClient(int id){
        clients.remove(id);
    }
    
    @Override
    public void onClientDisconnected(ClientDisconnectedEvent evt) {
        int id = evt.getId();
        synchronized(clients){
            try {
            clients.get(id).getSocket().close();
            } catch (IOException ex) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        delClient(id);
    }

    @Override
    public void onClientNicknamed(ClientNicknamedEvent evt) {
        String sessionData = evt.getSessionData();
        int id = Integer.parseInt(getData("<id>", sessionData));
        String nickname = getData("<nickname>", sessionData);
        
        Client client = clients.get(id);
        client.setNickname(nickname);
    }
}
