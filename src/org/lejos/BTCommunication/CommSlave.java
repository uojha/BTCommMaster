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
        long n;
        threadRunning = true;
        Node.debugMessage("Starting Slave",1000);
        for(int i=0;i<Node.COMMCOUNT;i++){				
            n = readLongData(true);
            writeLongData(System.currentTimeMillis(),true);            
            delay(10);
        }
        Node.debugMessage("Slave Done",1000);
        threadRunning = false;        
    }
} 
