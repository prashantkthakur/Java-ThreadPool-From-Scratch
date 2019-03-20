import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

public class HashLength {

    private void computeData(){
        while (true) {
            long time=0;
            try {
                time = System.currentTimeMillis();
                System.out.println(time);
                Random rand = new Random();
                byte[] data = new byte[8*1024];
                rand.nextBytes(data);
                String hash = SHA1FromBytes(data);
                if (hash.length() < 40) {
                    System.out.println("##########: " + hash + " Length: " + hash.length()+ " Byte len: "+hash.getBytes().length);
                }
                while (hash.length() < 40){
                    hash += "0";
                }
                System.out.println("Hash generated: " + hash + " Length: " + hash.length()+ " Byte len: "+hash.getBytes().length);

            }catch(NoSuchAlgorithmException nsa){
                nsa.printStackTrace();
                System.out.println("Error computing the hash code for message..");
            }
            try {
                Thread.sleep(10000/1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println(System.currentTimeMillis()-time);


        }
    }

    private String SHA1FromBytes(byte[] data) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA1");
        byte[] hash = digest.digest(data);
        BigInteger hashInt = new BigInteger(1, hash);
        return hashInt.toString(16);

    }

    public static void main(String[] args){
        HashLength instance = new HashLength();
        instance.computeData();
    }
}
