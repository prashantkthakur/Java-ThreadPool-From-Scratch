package cs455.scaling.threadPool;

import cs455.scaling.utils.Task;



public class Worker extends Thread{

    private Task task;
    private PoolManager manager;

    Worker(PoolManager manager){
        this.manager = manager;
    }

    @Override
    public void run(){
        while (true) {

            synchronized (this) {
                try {
//                    System.out.println(Thread.currentThread().getName() + " :: Waiting...");
                    this.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
//                System.out.println(Thread.currentThread().getName()+" :: Execute the task pulled from queue...");
                // Task available now. Execute the task.
                task.execute();
                // Add thread back to the worker pool after completion
                manager.updateWorker(this);
            }


        }

    }

    void addTask(Task task){
        this.task = task;
    }
}
