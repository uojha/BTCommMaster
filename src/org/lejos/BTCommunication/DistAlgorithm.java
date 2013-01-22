/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lejos.BTCommunication;

/**
 *
 * @author Unnati
 */
public class DistAlgorithm {
    
    final static double ONE3RD   = 0.333333333333333333333;
    final static double TWO3RD   = 0.666666666666666666666;
    final static double ONE3RD_P = 0.333333333333333333334;
    private volatile int[]  timeStep = {0,0,0,0,0};
    private double[] neighborVal = {0,0,0,0,0};
    private volatile int currentStep = 1;
    private boolean allDataReceived = false;
//    private double [][] P = {   {ONE3RD, ONE3RD_P,  ONE3RD,  0           },
//                                {ONE3RD_P, TWO3RD,  0,          0           },
//                                {ONE3RD, 0,          ONE3RD,  ONE3RD_P   },
//                                {0,         0,          ONE3RD_P,  TWO3RD   }};
    
//    private double [][] P = {   {0.4,   0.3,  0.3,  0   },
//                                {0.3,   0.7,  0,    0   },
//                                {0.3,   0,    0.4,  0.3 },
//                                {0,     0,    0.3,  0.7 }};
//    
    private double [][] P = {   { 0.6,	0.2,	0.2,	0,	0},
                                { 0.2,	0.8,	0,	0,	0},
                                { 0.2,	0,	0.4,	0.2,	0.2},
                                { 0,	0,	0.2,	0.8,	0},
                                { 0,	0,	0.2,	0,	0.8 }};
    
    
    private volatile double PG;
    private double PD = 850;

    public DistAlgorithm(){
        
    }
    
    public void averageConsensus(){
        double nodeVal;
        //Node variable commcount is set as the iteration termination condition
        while (currentStep <= Node.COMMCOUNT){
            //wait until the Node has received this step's data from all channels
            while(!isAllDataReceived()){
                Node.delay(5);
            }
            
            //update the Node's value - calculations go here
            nodeVal = 0;
            for (int i=0;i<Node.NUMNODES;i++){
                nodeVal += P[Node.nodeID][i]*neighborVal[i];
            }
            neighborVal[Node.nodeID] = nodeVal;
            //update the iteration number
            updateCurrentStep();
            clearAllDataReceived();
            Node.delay(10);
            
        }
    }
    
    /*
     * Leader followe ICC
     */
    public void LFICC(){
        double nodeVal;
        double epsilon = 0.001;  //used same value from paper
        double deltaP;
        //using same values as paper
        //double [] alpha = {561, 310,78,561,78};
        double [] beta = {7.92, 7.85,7.8,7.92,7.8};
        double [] gamma = {0.001562,0.00194,0.00482,0.001562,0.00482};
        double [] P_Gi = new double[Node.NUMNODES];
        
        
        
        //Node variable commcount is set as the iteration termination condition
        while (currentStep <= Node.COMMCOUNT){
            //wait until the Node has received this step's data from all channels
            while(!isAllDataReceived()){
                Node.delay(5);
            }
            
            //update the Node's value - calculations go here
            nodeVal = 0;
            for (int i=0;i<Node.NUMNODES;i++){
                nodeVal += P[Node.nodeID][i]*neighborVal[i];
            }
            //this is the only difference.
            int nnodes;
            if (Node.isRootNode){ 
                PG = 0;
                for (nnodes=0;nnodes<Node.NUMNODES;nnodes++){                   
                   P_Gi[nnodes] = (nodeVal - beta[nnodes])/(2.0*gamma[nnodes]);
                   //Node.debugMessage(Double.toString(nodeVal));
                   PG = PG + P_Gi[nnodes];//total power generated
                } 
                if (currentStep > 50){
                    PD = 950;
                }
                deltaP = PD - PG;
                nodeVal = nodeVal + epsilon*deltaP;
            }
            neighborVal[Node.nodeID] = nodeVal;
            //update the iteration number
            updateCurrentStep();
            clearAllDataReceived();
            Node.delay(10);
            
        }
    }
    
    private void clearAllDataReceived(){
        allDataReceived = false;
    }
    
    private boolean isAllDataReceived(){        
        allDataReceived = true;
        for(int i=0;i<Node.NUMCHANNELS;i++){
            if (Node.activeNeighbor[i]){
                if (timeStep[i] != currentStep){
                    allDataReceived = false;
                }
            }            
        }
        return allDataReceived;
    }

    public double getPG() {
        return PG;
    }

    public void setPG(double PG) {
        this.PG = PG;
    }

    public double getPD() {
        return PD;
    }

    public void setPD(double PD) {
        this.PD = PD;
    }

    
    
    public double getNodeVal() {
        return neighborVal[Node.nodeID];
    }

    public void setNodeVal(double nodeVal) {
        neighborVal[Node.nodeID] = nodeVal;
    }   
        
    public int getTimeStep(int i) {
        return timeStep[i];
    }

    public void setTimeStep(int ts, int id) {
        this.timeStep[id] = ts;
    }
    
    public void setNeighborVal(int n_id, double n) {
        this.neighborVal[n_id] = n;
    }

    
    public int getCurrentStep() {
        return currentStep;
    }

    private void updateCurrentStep() {
        this.currentStep = this.currentStep+1;        
    }
    
    
    
    
}
