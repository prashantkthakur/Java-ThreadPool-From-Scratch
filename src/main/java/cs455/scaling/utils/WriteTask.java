package cs455.scaling.utils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;



public class WriteTask implements Task {

    private SelectionKey key;
//    private Server server;
    private String data;

//    public WriteTask(Server server, SelectionKey key,String data){
    public WriteTask(SelectionKey key,String data){
        this.key = key;
        this.data = data;
//        this.server = server;
    }

    public void execute() {

        try {
            SocketChannel channel = (SocketChannel) key.channel();
            ByteBuffer buffer = ByteBuffer.wrap(data.getBytes());
            int bytes = channel.write(buffer); // Byte read information
//            System.out.println(Thread.currentThread().getName() + " :: Write task: " + data + " ; Bytes written: " + bytes);
//            server.updateSentCounter();
            key.interestOps(SelectionKey.OP_READ);


        }catch (ClosedChannelException cce){
            System.out.println("Closed Channel Exception received.");
            key.cancel();
        }catch (IOException e){
            e.printStackTrace();
        }

    }

}
