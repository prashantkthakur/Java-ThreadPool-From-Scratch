package cs455.scaling.utils;

import cs455.scaling.nodes.Client;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

public class SendPayload extends Thread {
    private Client client;
    private SelectionKey key;
    private int rounds;

    public SendPayload(Client client, SelectionKey key, int rounds) {
        this.client = client;
        this.key = key;
        this.rounds = rounds;
    }

    public void run() {
        while (true) {
            try {
                Random rand = new Random();
                byte[] data = new byte[client.getBufferSize()];
                rand.nextBytes(data);
                ByteBuffer buffer = ByteBuffer.wrap(data);
                String hash = SHA1FromBytes(data);
                SocketChannel channel = (SocketChannel) key.channel();
                channel.write(buffer);
                while (hash.length() < 40){
                    hash += "0";
                }
                client.updateCounters(hash);
                key.interestOps(SelectionKey.OP_READ);
//                System.out.println("Client: sent data with hashcode "+hash + " ;Length="+hash.length());
//                client.incrementPacketSent();
            } catch (IOException io) {
//                io.printStackTrace();
                System.out.println("IOException on Client SendPayload; Error sending payload from client..");
            }catch(NoSuchAlgorithmException nsa){
//                nsa.printStackTrace();
                System.out.println("NoSuchAlgorithmException on Client SendPayload; Error computing the hash code for message..");
            }
            try {
                Thread.sleep(1000/rounds);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }

    private String SHA1FromBytes(byte[] data) throws NoSuchAlgorithmException{
        MessageDigest digest = MessageDigest.getInstance("SHA1");
        byte[] hash = digest.digest(data);
        BigInteger hashInt = new BigInteger(1, hash);
        return hashInt.toString(16);

    }
}