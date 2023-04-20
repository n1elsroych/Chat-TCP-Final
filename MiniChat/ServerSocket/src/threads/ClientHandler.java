package threads;

import events.ClientDisconnectedEvent;
import events.ClientNicknamedEvent;
import events.MessageReceivedEvent;
import events.ServerEventsListener;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientHandler extends Thread{
    private DataInputStream in;
    
    private ArrayList<ServerEventsListener> listeners;
    
    public ClientHandler(InputStream inputStream){
        in = new DataInputStream(inputStream);
        listeners = new ArrayList<>();
    }
    
    @Override
    public void run() {
        receiveNickname();
        
        boolean isConnected = true;
        while (isConnected) {
            try {
                String message = in.readUTF();
                if (isDisconnectionRequest(message)){
                    int id = Integer.parseInt(getData("<origin>", message));
                    System.out.println("El cliente con ID = "+id+" solicito desconectarse");
                    isConnected = false;
                }
                triggerMessageReceivedEvent(message);
            } catch (IOException ex) {
                ex.printStackTrace();
                isConnected = false;
            }
        }
    }
    
    private String getData(String type, String dataMessage){
        int i = dataMessage.indexOf(type) + type.length();
        int f = dataMessage.indexOf(";", i);
        
        return dataMessage.substring(i, f);
    }
    
    private boolean isDisconnectionRequest(String message){
        return getData("<message>", message).equals("/salir");
    }
    
    private void receiveNickname(){
        try {
            String sessionData = in.readUTF();
            triggerClientNicknamedEvent(sessionData);
        } catch (IOException ex) {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void addEventsListener(ServerEventsListener listener){
        listeners.add(listener);
    }
    
    public void removeMiEventoListener(ServerEventsListener listener) {
        listeners.remove(listener);
    }
    
    public void triggerMessageReceivedEvent(String message) {
        MessageReceivedEvent evt = new MessageReceivedEvent(this, message);
        for (ServerEventsListener listener : listeners) {
            listener.onReceivedMessage(evt);
        }
    }
    
    public void triggerClientNicknamedEvent(String sessionData){
        ClientNicknamedEvent evt = new ClientNicknamedEvent(this, sessionData);
        for (ServerEventsListener listener : listeners){
            listener.onClientNicknamed(evt);
        }
    }
    
    public void triggerClientDisconnectedEvent(int id) {
        ClientDisconnectedEvent evt = new ClientDisconnectedEvent(this, id);
        for (ServerEventsListener listener : listeners) {
            listener.onClientDisconnected(evt);
        }
    }
}
