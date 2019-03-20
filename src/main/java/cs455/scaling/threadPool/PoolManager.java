package cs455.scaling.threadPool;

import cs455.scaling.utils.Task;
import java.util.concurrent.ConcurrentLinkedQueue;


public class PoolManager extends Thread {

    private final int poolSize;
    private ConcurrentLinkedQueue<Task> taskList;
    private ConcurrentLinkedQueue<Worker> workerList;
    private final int batchSize;
    private final double batchtime;


    public PoolManager(int poolSize, int size, double time){
        this.poolSize = poolSize;
        this.batchSize = size;
        this.batchtime = time;
        this.taskList = new ConcurrentLinkedQueue<>();
        this.workerList = new ConcurrentLinkedQueue<>();

    }

    public void initialize(){
        // Create worker threads and add them to workerList.
        for (int i=0; i< poolSize; ++i){
            Worker worker = new Worker(this);
            worker.setName("Worker-"+i);
            worker.start();
            workerList.add(worker);
        }
    }

    @Override
    public void run() {
        long currentTime = 0;
        while(true) {

            if (taskList.size() >= batchSize || (System.currentTimeMillis() - currentTime) >= (int)batchtime*1000) {
                currentTime = System.currentTimeMillis();
//            System.out.println("Running thread pool..."+workerList.size() + " task size: "+taskList.size());
                Worker worker = workerList.peek();
                // worker = null if the queue is empty.
                if (worker != null) {
                    // Take task from the queue and notify the worker thread of new task
                    Task task = taskList.peek();
                    if (task != null) {
//                        System.out.println("PoolManager:: Worker not null..." + " Task:: " + taskList.size());
                        worker = workerList.poll();
                        task = taskList.poll();
                        worker.addTask(task);
                        synchronized (worker) {
                            worker.notify();
                        }
                    }
                }

            }
        }
    }

    void updateWorker(Worker worker){
//        System.out.println(Thread.currentThread().getName()+" :: Adding thread back to workerList...");
        workerList.add(worker);
    }

    public void addTask(Task task){
        try {
            boolean success = taskList.add(task);
            // Main thread does the adding task...
//        System.out.println(Thread.currentThread().getName()+" :: Added task to the queue..." + task.getClass());
            if (!success) {
                System.out.println("Error adding task to the Queue..");
            }
        }catch (NullPointerException ne){
            ne.printStackTrace();
        }
    }

}
