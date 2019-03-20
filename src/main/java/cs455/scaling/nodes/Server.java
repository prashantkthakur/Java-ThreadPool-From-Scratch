package cs455.scaling.nodes;

import cs455.scaling.threadPool.PoolManager;
import cs455.scaling.utils.ServerStat;
import cs455.scaling.utils.Task;
import cs455.scaling.utils.TaskReader;
import cs455.scaling.utils.WriteTask;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;


public class Server {
    private final int portNum;
    private final int poolSize;
    private final int bufferSize;
    private final int batchSize;
    private final double batchTime;
    private PoolManager threadPool;
    private Selector selector;
    private ServerSocketChannel serverSocketChannel;
    private int packetSent;
    private int packetReceived;
    private ServerStat stat; // Created here so that it can be killed if there is any problem.
    private int numClients;
    private ConcurrentHashMap<SelectionKey, Integer> clientCounter = new ConcurrentHashMap<>();
    private ReentrantLock readlock = new ReentrantLock();
    private ReentrantLock writelock = new ReentrantLock();
    private ReentrantLock connectionlock = new ReentrantLock();



    private Server(int portNum, int poolSize, int batchSize, double batchTime){
        this.poolSize = poolSize;
        this.portNum = portNum;
        this.batchSize = batchSize;
        this.batchTime = batchTime;
        this.bufferSize = 8*1024;
        this.packetSent = 0;
        this.packetReceived = 0;
        this.numClients = 0;
    }

    public int getClients(){
        try{
            connectionlock.lock();
            return this.numClients;
        }finally {
            connectionlock.unlock();
        }

    }

    public int getNumClients(){
        try{
            readlock.lock();
//            connectionlock.lock();
//            return this.numClients;
            return this.clientCounter.size();
        }finally {
//            connectionlock.unlock();
            readlock.unlock();
        }

    }

    public int getPacketSent(){
        // Reset the counter once the stat has been sent.
        try{
            writelock.lock();
            int retVal = this.packetSent;
            this.packetSent = 0;
            return retVal;
        }finally {
            writelock.unlock();
        }

    }

    private void resetCounters(){
        try {
            readlock.lock();
            for (SelectionKey key : clientCounter.keySet()) {
                if (key.isValid()) {
                    clientCounter.put(key, 0);
                } else {
                    clientCounter.remove(key);
                }
            }
        }finally {
            readlock.unlock();
        }
    }

    public double getMean(){
//        System.out.println("Client = "+numClients+" ; counter_client = "+clientCounter.size());
        if (numClients == 0 || clientCounter.size() == 0){
            return 0;
        }
        try{
            readlock.lock();
//            int tmp = this.packetReceived;
            int sum = 0;
            for(SelectionKey key: clientCounter.keySet()){
                sum += clientCounter.get(key);
            }
            this.packetReceived = 0;
//            return tmp/numClients;
            return sum/clientCounter.size()/20;

        }finally {
            readlock.unlock();
        }

    }

    public double getStd(){
        double std = 0.0;
        try{
            readlock.lock();
            double mean = getMean();
            for (Integer val: clientCounter.values()){
                    std += Math.pow(val - mean, 2);
            }
        }finally {
            readlock.unlock();
        }
        resetCounters();
        if (clientCounter.size() == 0){
            return 0.0;
        }else {
            return Math.sqrt(std / clientCounter.size())/20;
        }
    }

    private void initialize() throws IOException{
        // Create poolManager
        threadPool = new PoolManager(poolSize,batchSize,batchTime);
        threadPool.initialize();
        threadPool.setName("Thread-Pool");
        threadPool.start();

        // Open selector
        selector = Selector.open();
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.socket().bind(new InetSocketAddress(portNum));
        serverSocketChannel.configureBlocking(false); // Non-Blocking

        // Register channel to the selector to use it.
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        System.out.println("Started Server " + InetAddress.getLocalHost().getHostName() +" to listen on port "+ portNum );

        // Create thread to print out server information
        stat = new ServerStat(this);
        Thread statThread = new Thread(stat);
        statThread.start();

        // Start Server operations like read, send, accept
        startProcess();

    }

    private void startProcess() throws IOException{

        while(true){

            selector.select();
            Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();
            while (keyIterator.hasNext()){
                SelectionKey key = keyIterator.next();
                keyIterator.remove();
                if(key.isAcceptable()) {
//                    System.out.println("New connection received...");
                    // ServerSocketChannel accepted connections.
                    connectionHandler(key);

//                } else if (key.isConnectable()) {
                    // a connection was established with a remote server.


                } else if (key.isValid() && key.isReadable()) {
                    // Channel ready to read
//                    System.out.println("Readable..."+key.isValid() + key.attachment());
                    readHandler(key);

                } else if (key.isValid() && key.isWritable()) {
                    // Channel ready to write
                    writeHandler(key);
                }

            }


        }
    }

    private void connectionHandler(SelectionKey key) throws IOException{
        ServerSocketChannel socket = (ServerSocketChannel) key.channel();
        SocketChannel channel = socket.accept();
        channel.configureBlocking(false);
        // Prepare channel for read
        channel.register(selector, SelectionKey.OP_READ);
        try{
            connectionlock.lock();
            numClients++;
        }finally {
            connectionlock.unlock();
        }

    }

    private void removeClient(SelectionKey key){
        clientCounter.remove(key);
        if (numClients > 0) {
            try {
                connectionlock.lock();
                numClients--;

            } finally {
                connectionlock.unlock();
            }
        }else {
            System.out.println("****Number of Client below 0.*****");
        }
    }

    private void readHandler(SelectionKey key) {
        // Create ReadTask for handling this operation
        SocketChannel channel = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
        int data = 0; // Byte read information
        try {
            while (buffer.hasRemaining() && data != -1) {
                data = channel.read(buffer);
                if (data == 0) {
                    break;
                }
//                ByteBuffer tmp = ByteBuffer.allocate(bufferSize);
//                data = channel.read(tmp);
                if (data == -1){
//                    System.out.println("Server :: "+Thread.currentThread().getName()+ " ::  Removing broken connection from server.");
                    // Connection closed by the client.
                    removeClient(key);
                    key.cancel();
                    channel.close();
                    return;
                }
            }

        }catch (IOException e){
            System.out.println("IOException on server; Client disconnected during read operation. Client may be dead." +
                    " Call 911. :-)");
            removeClient(key);
            key.cancel();
            try {
                channel.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            return;
        }
        Task readTask = new TaskReader(key, buffer);
        threadPool.addTask(readTask);
        if(key.isValid()){
            try {
                readlock.lock();
                packetReceived++;
                if(clientCounter.containsKey(key)){
                    clientCounter.put(key, clientCounter.get(key) + 1);
                }else{
                    clientCounter.put(key,1);
                }
            } finally {
                readlock.unlock();
            }
        }else {
            removeClient(key);
        }

    }

    private  void writeHandler(SelectionKey key){

        if (key.attachment() != null) {
            String hashValue = (String)key.attachment();
//            System.out.println(Thread.currentThread().getName()+" :: Writing from server..." +hashValue);
        key.attach(null);
        if (hashValue == null){
            key.interestOps(SelectionKey.OP_READ);
            return;
        }

//            Task writeTask = new WriteTask(this, key, hashValue); // Test (not helpful) - Support for Increment packetSent from writeTask..
            Task writeTask = new WriteTask(key, hashValue);
            threadPool.addTask(writeTask);
            try{
                writelock.lock();
                packetSent++;
            }finally {
                writelock.unlock();
            }

        }
        key.interestOps(SelectionKey.OP_READ);
    }

    public static void main(String[] args){
        if (args.length != 4){
            System.out.println("java cs455.scaling.server.Server <portnum> <thread-pool-size> <batch-size> <batch-time>");
            System.exit(1);
        }
        int port = Integer.parseInt(args[0]);
        int poolSize = Integer.parseInt(args[1]);
        int batchSize = Integer.parseInt(args[2]);
        double batchTime = Double.parseDouble(args[3]);
        try {
            Server server = new Server(port, poolSize, batchSize, batchTime);
            server.initialize();
        }catch (IOException e){
            e.printStackTrace();
            System.out.println("Error initializing server..");
        }

    }

}
