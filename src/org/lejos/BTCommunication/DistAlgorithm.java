/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lejos.BTCommunication;

import lejos.util.Matrix;
/**
 *
 * @author Unnati
 */
public class DistAlgorithm {
    private volatile int[]  timeStep;
    private double[] neighborVal;;
    private volatile int currentStep = 1;
    private boolean allDataReceived = false;
    private double [][] W;    
    private volatile double PG;
    

    public DistAlgorithm(){
        
    }
    
    public void initialize(int n){
        //initialize some variables based on the parameters
        W = new double[n][n];
        neighborVal = new double[n];
        timeStep = new int [n];
        for (int i=0;i<n;i++){
            neighborVal[i] = 0;
            timeStep[i] = 0;
        }
    }
    
    private Matrix  getLaplacian(double [][] A){
        double x;
        double [][] B = new double[Node.numNodes][Node.numNodes];
        for(int i=0;i<Node.numNodes;i++){
            double sum = 0;
            for(int j=0;j<Node.numNodes;j++){
                x = Math.abs(A[i][j]);
                sum += x;
                B[i][j] = Math.abs(A[i][j])*(-1);
            }
            B[i][i] = sum;
        }
        Matrix L = new Matrix(B);
        return L;      
    }
    
    public void createWeightMatrix(double [][] Aorig, double epsilon){
        double [][] A = new double[Node.numNodes][Node.numNodes];
        for (int cc = 0;cc<Node.numNodes;cc++){
            System.arraycopy(Aorig[cc], 0, A[cc], 0, Node.numNodes);
        }        
        Matrix L = getLaplacian(A);
        Matrix PP;
        //create identity Matrix
        Matrix identityM = Matrix.identity(Node.numNodes,Node.numNodes);
        L.timesEquals(epsilon);
        //L.times(epsilon);
        PP = identityM.minus(L);   
        for(int i=0;i<Node.numNodes;i++){
            for(int j=0;j<Node.numNodes;j++){
                W[i][j] = PP.get(i, j);
            }
        }
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
            for (int i=0;i<Node.numNodes;i++){
                nodeVal += W[Node.nodeID][i]*neighborVal[i];
            }
            neighborVal[Node.nodeID] = nodeVal;
            //update the iteration number
            updateCurrentStep();
            clearAllDataReceived();
            Node.delay(10);
            
        }
    }
    
    /*
     * Leader follower ICC
     */
    public void LFICC(double epsilon, double [] beta, double [] gamma){
        double PD = 850;
        double nodeVal;        
        double deltaP;
        double [] P_Gi = new double[Node.numNodes];
        
        //Node variable commcount is set as the iteration termination condition
        while (currentStep <= Node.COMMCOUNT){
            //wait until the Node has received this step's data from all channels
            while(!isAllDataReceived()){
                Node.delay(5);
            }
            
            //update the Node's value - calculations go here
            nodeVal = 0;
            for (int i=0;i<Node.numNodes;i++){
                nodeVal += W[Node.nodeID][i]*neighborVal[i];
            }
            //this is the only difference.
            int nnodes;
            if (Node.isRootNode){ 
                PG = 0;
                for (nnodes=0;nnodes<Node.numNodes;nnodes++){                   
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

    public double getW(int i, int j) {
        return W[i][j];
    }   
    
    public double getPG() {
        return PG;
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
