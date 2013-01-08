package org.lejos.BTCommunication;
import lejos.nxt.comm.Bluetooth;

/**
 *
 * @author Unnati
 */
public class CommSlave extends CommChannel{
    
    public CommSlave(){        
        
    }
    
    public CommSlave(String n){
        super(n);              
    }
    
    @Override
    public int connect(){
        super.connect();
        Node.debugMessage("Waiting...",0,0);
        while (!connected){          
            nodeConnection = Bluetooth.waitForConnection();
            delay(100);
            connected = true;
        } 
        Node.debugMessage("Connected...",0,1);
        return 1;
    }
    
    @Override
    public void run() {        
        long n, deltaT, startTime;
        threadRunning = true;
        Node.debugMessage("Starting Slave",1000);
         
        //get the info from master
        deltaT = readLongData(false);   
        startTime = readLongData(false);   
        Node.debugMessage(Long.toString(System.currentTimeMillis()));
        Node.debugMessage(Long.toString(startTime),0,1);
        while ((System.currentTimeMillis()- Node.clock.getDriftRoot()) < startTime){
            delay(5);
        }        
        for(int i=0;i<Node.COMMCOUNT;i++){
            startTime = startTime+deltaT;
            while (System.currentTimeMillis() - Node.clock.getDriftRoot() < startTime){
                delay(2);
            }
            writeLongData(System.currentTimeMillis() - Node.clock.getDriftRoot(),false);           
            //logData(readLongData(true));
            //Node.debugMessage(Integer.toString(i),0,2);
            //delay(5);            
        }
        Node.debugMessage("Slave Done",1000);
        threadRunning = false;        
    }
} 
