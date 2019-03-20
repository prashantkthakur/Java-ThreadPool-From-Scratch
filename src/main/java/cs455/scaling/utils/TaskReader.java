package cs455.scaling.utils;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


public class TaskReader implements Task {

    private SelectionKey key;
    private ByteBuffer buffer;

    public TaskReader(SelectionKey key, ByteBuffer data){
        this.key = key;
        this.buffer = data;
    }

    public void execute(){
//        System.out.println("Executing read task on server...");
        try {
            String hash = SHA1FromBytes(buffer.array());
            System.out.println(Thread.currentThread().getName()+" :: Hash of received data.. "+hash + " ;Length = "+hash.length());
            while (hash.length() < 40){
                hash += "0";
            }
            key.attach(hash);
//            key.interestOps(SelectionKey.OP_WRITE);


        } catch (NoSuchAlgorithmException e){
            e.printStackTrace();
        }
        if (key.isValid()) {
            key.interestOps(SelectionKey.OP_WRITE);
        }

    }
    private String SHA1FromBytes(byte[] data) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA1");
        byte[] hash = digest.digest(data);
        BigInteger hashInt = new BigInteger(1, hash);
        return hashInt.toString(16);

    }
}
