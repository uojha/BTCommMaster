/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lejos.BTCommunication;

/**
 *
 * @author Unnati
 */
public class Clock {
    public final static boolean LOGSYNCDATA = false;


    public boolean isSyncing = false;
    
    private boolean syncStatus = Node.SYNCSTATUS_NOTSYNCED;   
    private long [][] sentTime = new long[Node.NUMCHANNELS][Node.SYNCWINDOW];
    private long [][] receivedTime = new long[Node.NUMCHANNELS][Node.SYNCWINDOW];
    private long [][] replyReceivedTime = new long[Node.NUMCHANNELS][Node.SYNCWINDOW];
    private long [] delay = new long[Node.NUMCHANNELS];
    private long [] drift = new long[Node.NUMCHANNELS];
    private long propDelay;
    private long driftRoot = 0; 
    

    public Clock(){
    }
    
        /*
     * Synchronizes the network clock with the root node's clock
     */
    public void synchronizeNetwork() {
        boolean sensorNetSynced = false;
        if (Node.SYNCHRONIZE) {
            boolean temp = false;
            if (getSyncStatus() == Node.SYNCSTATUS_NOTSYNCED) {
                waitForSync(Node.commChannels[Node.SLAVECHANNEL]);
            }
            for (int i = Node.MASTERCHANNEL1; i < Node.NUMCHANNELS; i++) {
                if (Node.activeNeighbor[i]) {
                    if (Node.commChannels[i].neighborID != Node.rootNode){
                        temp = initiateSync(Node.commChannels[i], i);
                    }
                }
            }
            //wait for this node's subnet to sync
            long tempSyncCode;
            while (!sensorNetSynced) {
                sensorNetSynced = true;
                Node.debugMessage("Syncing...");
                for (int i = Node.MASTERCHANNEL1; i < Node.NUMCHANNELS; i++) {
                    if (Node.activeNeighbor[i]) {
                        //we should not do this if the neighbor is the root. Root is already synced
                        if (Node.commChannels[i].neighborID != Node.rootNode){
                            tempSyncCode = Node.commChannels[i].readLongData(LOGSYNCDATA);
                            if (tempSyncCode != Node.SUBNET_SYNCED_CODE) {
                                sensorNetSynced = false;
                            }
                        }
                    }
                    Node.delay(100);
                }
            }
            Node.debugMessage("Synced", 0, 1);
            Node.debugMessage(Long.toString(getDriftRoot()), 4000);
            if (Node.activeNeighbor[Node.SLAVECHANNEL] && !(Node.isRootNode)) {
                Node.commChannels[Node.SLAVECHANNEL].writeLongData(Node.SUBNET_SYNCED_CODE, LOGSYNCDATA);
            }
        }
    }
    
    /**
     * Method that initiates Synchronization to slave channel ch
     * Sends sync request to the slave and waits for reply
     * If slave is synced already, returns true
     * else performs synchronization using one of the methods available
     * returns true upon successful synchronization
     */
    public boolean initiateSync(CommChannel ch, int channelID){
        boolean s;        
        ch.writeLongData(Node.SYNCREQ, LOGSYNCDATA);
        s = ch.readBoolData(LOGSYNCDATA);
        if (s){
            return s;
        }
        //communicate and record sent and received time
        for (int i=0;i<Node.SYNCWINDOW;i++){
            sentTime[channelID][i] = System.currentTimeMillis();
            ch.writeLongData(12345, LOGSYNCDATA);
            receivedTime[channelID][i] = ch.readLongData(LOGSYNCDATA);
            replyReceivedTime[channelID][i] = System.currentTimeMillis();            
        }
        SyncTPSN(channelID);
        ch.writeLongData(Node.SYNCPARAMS, LOGSYNCDATA);
        ch.writeLongData(delay[channelID],LOGSYNCDATA);
        ch.writeLongData(drift[channelID], LOGSYNCDATA);   
        ch.writeLongData(Node.STARTTIME, LOGSYNCDATA);  
        return s;
        
    }    
    /**
     * Method that waits for a master's synchronization request.
     * Upon receiving the request, sends its status. If it is already synced
     * sends true else sends false. No further action is required if the slave
     * is already synced. If not, the synchronization is performed
     * The delay and drift are saved and sync status is updated upon successful
     * completion of this method
     */
    public void waitForSync(CommChannel ch){
        long n;
        while (syncStatus == false){
            n = ch.readLongData(LOGSYNCDATA);            
            if(n == Node.SYNCREQ){
                //send reply
                ch.writeBoolData(syncStatus, LOGSYNCDATA);                
                //wait for synchronization routine
                for (int i=0;i<Node.SYNCWINDOW;i++){
                    ch.readLongData(LOGSYNCDATA);
                    ch.writeLongData(System.currentTimeMillis(), LOGSYNCDATA);                    
                }   
            //once synchronization is completed, the master sends the drift  
            }else if (n==Node.SYNCPARAMS){
                //store the synchronization parameters
                propDelay = ch.readLongData(LOGSYNCDATA);
                driftRoot = ch.readLongData(LOGSYNCDATA);  
                Node.STARTTIME = ch.readLongData(LOGSYNCDATA); 
                ch.logData(driftRoot);
                setSyncStatus(Node.SYNCSTATUS_SYNCED);
            }        
            ch.delay(5);
        }
    }
    
    
    
 //SYNCHRONIZATION METHODS
    /**
     * Proposed by Ganeriwal et al.
     * Time-Sync protocol for Sensor Networks
     * Has two phases - "level discovery phase" and
     * "Synchronization phase". Only Synchronization phase
     * is implemented in this function. Assumes that network
     * delay is constant. When this method is running other threads
     * should not run. Basic formula
     * drift = ((T2-T1)-T4-T3))/2 where 
     * drift is the relative clock drift     
     * T1 is the time when master sends the message,
     * T2 is the time when slave receives the the message, 
     * T3 is the time when slave sends the reply and
     * T4 is the time when master receives the reply
     * delay = ((T2-T1)+(T4-T3))/2 where
     * delay is the propagation delay
     * T2 = T1 + delta + delay
     */
    private void SyncTPSN(int channelID){
        long T1, T2, T3, T4;
        double tempdelay = 0;
        double tempdrift = 0;
        for (int i=0;i<Node.SYNCWINDOW;i++){
            T1 = sentTime[channelID][i];
            T2 = receivedTime[channelID][i];
            T3 = receivedTime[channelID][i];
            T4 = replyReceivedTime[channelID][i];
            tempdelay = tempdelay + (((T2-T1)+(T4-T3))/2);
            tempdrift = tempdrift + (((T2-T1)-(T4-T3))/2);
        }
        delay[channelID] = (long)(tempdelay/Node.SYNCWINDOW);
        drift[channelID] = driftRoot + (long)(tempdrift/Node.SYNCWINDOW);        
    }
    
    //GETTERS AND SETTERS
    
     public boolean getSyncStatus() {
        return syncStatus;
    }
    public void setSyncStatus(boolean syncStatus) {
        this.syncStatus = syncStatus;
    }    
    public long getPropDelay() {
        return propDelay;
    }
    public long getDriftRoot() {
        return driftRoot;
    }    
    public boolean getSyncing(){
        return isSyncing;
    }    
    public void setDriftRoot(long driftRoot) {
        this.driftRoot = driftRoot;
    }    
    public void setSyncing(boolean s){
        isSyncing = s;
    }    
    public void setPropDelay(long propDelayRoot) {
        this.propDelay = propDelayRoot;
    }
    
}
