package lcpcson;

import java.util.Random;

public class ActiviteEnParallele implements Runnable {
	  private Thread thread;
	  private int id=0;
	  public ActiviteEnParallele(int id){
	    thread = new Thread(this);
	    this.id=id;
	  }
	  public void start(){
	    thread.start();
	  }
	  public void join(){
	     try {
	      thread.join();
	    } catch (Exception e) {
	      // veut-on toujours remonter l'exception ?
	    }
	  }
	  public void run() {
	    // Exécution de l'activité souhaitée...
	    // Ce code peut prendre du temps à s'exécuter,
	    // ce que nous simulons par une mise en attente
	    try {
		  Random rd=new Random();
	      Thread.sleep(rd.nextInt(2000)+500); // Mise en sommeil 3 secondes
	      System.out.println("Thread "+id+" end");
	    } catch (InterruptedException e) {}
	  }
	}