package cs455.scaling.utils;

import cs455.scaling.nodes.Client;

import java.text.SimpleDateFormat;
import java.util.Date;

public class ClientStat extends Thread {
    private final Client client;

    public ClientStat(Client client){
        this.client = client;
    }

    public void run() {
//        synchronized (client) { // As the process are guarded by lock no need for this.
        while (true) {
            System.out.printf("[%s] Total Sent Count: %d, Total Received Count: %d\n",
                    new SimpleDateFormat("HH:mm:ss").format(new Date()),
                    client.getPacketSent(),
                    client.getPacketReceived());
            try {
                Thread.sleep(20000); // Wait 20sec
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }
        }
    }
}
