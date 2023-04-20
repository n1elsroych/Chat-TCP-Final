package threads;

import entities.Client;
import events.ClientDisconnectedEvent;
import events.ServerEventsListener;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DisconnectionsHandler extends Thread{
    Map<Integer, Client> clients;
    
    private ArrayList<ServerEventsListener> listeners;
    
    public DisconnectionsHandler(Map<Integer, Client> clients){
        this.clients = clients;
        listeners = new ArrayList<>();
    }
    
    @Override
    public void run() {
        while(true) {
            synchronized (clients) {
                for (Map.Entry<Integer, Client> entry : clients.entrySet()){
                    Client client = entry.getValue();
                    System.out.println(client.getNickname());
                    Socket clientSocket = client.getSocket();
                    InetAddress inetAddress = clientSocket.getInetAddress();
//                    try {
//                        if (!inetAddress.isReachable(1000)){
//                       
//                            clientSocket.close();                         
//                            System.out.println("El cliente "+client.getNickname()+" con ID = "+client.getId()+" ya no esta conectado");
//                            triggerClientDisconnectedEvent(client.getId());
//                        }
//                    } catch (IOException ex) {
//                        Logger.getLogger(DisconnectionsHandler.class.getName()).log(Level.SEVERE, null, ex);
//                    }
                     try { //Esto no esta funcionando
                        if (!inetAddress.isReachable(1000)){//isReacheable !clientSocket.isConnected()
                            System.out.println("Entro");
                            clientSocket.close();
                            System.out.println("El cliente "+client.getNickname()+" con ID = "+client.getId()+" ya no esta conectado");
                            triggerClientDisconnectedEvent(client.getId());
                        }    
                    } catch (IOException ex) {
                        System.out.println(ex);
                        ex.printStackTrace();
                    }
                }
                
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    Logger.getLogger(DisconnectionsHandler.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

        }
    }
    
    public void addEventsListener(ServerEventsListener listener){
        listeners.add(listener);
    }
    
    public void removeMiEventoListener(ServerEventsListener listener) {
        listeners.remove(listener);
    }
    
    public void triggerClientDisconnectedEvent(int id) {
        ClientDisconnectedEvent evt = new ClientDisconnectedEvent(this, id);
        for (ServerEventsListener listener : listeners) {
            listener.onClientDisconnected(evt);
        }
    }
    
}
