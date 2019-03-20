package cs455.scaling.utils;

import cs455.scaling.nodes.Server;

import java.text.SimpleDateFormat;
import java.util.Date;

public class ServerStat implements Runnable {
    private Server server;
    private boolean done;

    public ServerStat(Server svr) {
        this.server = svr;
        this.done = false;

    }

    public void stopThread() {
        done = true;
    }

    public void run() {
        while (!done) {
            int numClients = server.getNumClients();
            int throughput = server.getPacketSent();
            double mean = server.getMean();
            double std = server.getStd();
            String timeStamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
//            int clients = server.getClients();
//            System.out.printf("[%s] Server Throughput: %d messages/s, Active Client Connections: %d-%d," +
//                            " Mean Per-Client Throughput: %.2f messages/s, Std. Dev. Of Per-client Throughput: %.2f messages/s\n",
//                    timeStamp,throughput, numClients,clients, mean, std);
            System.out.printf("[%s] Server Throughput: %d messages/s, Active Client Connections: %d," +
                            " Mean Per-Client Throughput: %.3f messages/s, Std. Dev. Of Per-client Throughput: %.3f " +
                            "messages/s\n",
                    timeStamp,throughput/20, numClients,mean, std);
            try{
                Thread.sleep(20000);
            }catch (InterruptedException ie){
                ie.printStackTrace();
            }

        }
    }
}
