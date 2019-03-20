package cs455.scaling.nodes;

import cs455.scaling.utils.ClientStat;
import cs455.scaling.utils.SendPayload;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.locks.ReentrantLock;

public class Client {
    private final String serverHost;
    private final int serverPort;
    private final int rounds;
    private final int bufferSize = 8 *1024;
    private Selector selector;
    private LinkedList<String> hashList;
    private int packetSent;
    private int packetReceived;
    private ReentrantLock readlock = new ReentrantLock();
    private ReentrantLock writelock = new ReentrantLock();


    private Client(String host, int port, int rounds){
        this.serverHost = host;
        this.serverPort = port;
        this.rounds = rounds;
        this.hashList = new LinkedList<>();
        this.packetReceived = 0;
        this.packetSent = 0;
    }

    public int getPacketSent(){
        int tmp;
        try {
            writelock.lock();
            tmp = this.packetSent;
            this.packetSent = 0;
        }finally {
            writelock.unlock();
        }

        return tmp;
    }

    public int getPacketReceived(){
        int tmp;
        try {
            readlock.lock();
            tmp = this.packetReceived;
            this.packetReceived = 0;
        }finally {
            readlock.unlock();
        }

        return tmp;
    }

    public int getBufferSize(){
        return bufferSize;
    }

    public void updateCounters(String hash){
        try{
            writelock.lock();
//            System.out.println("Generated: "+hash);
            hashList.add(hash);
            packetSent++;
        }finally {
            writelock.unlock();
        }
    }

    private void removeHash(String hash){
        try{
            System.out.println("Removing hash...."+hash);
            writelock.lock();
            hashList.remove(hash);
        }finally {
//            System.out.println("REMOVED HashList length: "+ hashList.size());
            writelock.unlock();
        }
    }

    private void initialize() throws IOException {
        selector = Selector.open();
        SocketChannel channel = SocketChannel.open();
        channel.configureBlocking(false);
        channel.register(selector, SelectionKey.OP_CONNECT);
        channel.connect(new InetSocketAddress(serverHost, serverPort));
        System.out.println("Started Client on " + InetAddress.getLocalHost().getHostName());
        startProcess();
    }

    private void startProcess() throws IOException{
        while (true){
            selector.select();
            Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();
            while (keyIterator.hasNext()){
                SelectionKey key = keyIterator.next();
                keyIterator.remove();
                if (key.isConnectable()){
                    connectHandler(key);
                }else if (key.isReadable()){
                    readHandler(key);
                }
            }

        }
    }

    private void connectHandler(SelectionKey key) throws IOException{
        SocketChannel channel = (SocketChannel) key.channel();
        channel.finishConnect();
//        System.out.println("Clinet connect Handler ######################");
        // Create a thread to print client stat.
        Thread stat = new ClientStat(this);
        stat.setName("Client-Stat");
        stat.start();

        // After connection has been setup send packets to server.
        Thread payloadSender = new SendPayload(this, key, rounds);
        payloadSender.start();
    }

    private void readHandler(SelectionKey key){
        try {
            SocketChannel channel = (SocketChannel) key.channel();
            ByteBuffer buffer = ByteBuffer.allocate(40);
            while(buffer.hasRemaining()) {
                int data = channel.read(buffer);
                if (data != 40){
//                    System.out.println("Client: Hash received is smaller in size ("+data+")");
                    return;
                }
            }
            removeHash(new String(buffer.array()));
            readlock.lock();
            packetReceived++;

        }catch (IOException e){
            e.printStackTrace();
        }finally {
            readlock.unlock();
        }

    }

    public static void main(String[] args){
        if (args.length != 3){
            System.out.println("java cs455.scaling.client.Client <server-host> <server-port> <message-rate>");
            System.exit(1);
        }
        String host = args[0];
        int port = Integer.parseInt(args[1]);
        int rounds = Integer.parseInt(args[2]);
        Client client = new Client(host, port, rounds);
        try {
            client.initialize();
//            client.startProcess();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



}
