package subgame;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

import games.MixedStrategy;

/**
 * Created by IntelliJ IDEA.
 * User: Oscar-XPS
 * Date: 10/1/13
 * Time: 6:44 PM
 * To change this template use File | Settings | File Templates.
 */
public class StrategyMap {
    private int[] strategyMap;
    private int numActions;
    private String strategyName;
    public StrategyMap(String s){
        loadStrategy(s);
    }
    public StrategyMap(){
        //intentionally left open
    }
    public StrategyMap(int[] removedActions,int numberOfActions,String name){
        Arrays.sort(removedActions);
        numActions = numberOfActions;
        strategyName = name;
        strategyMap = new int[numberOfActions-removedActions.length];

        int[] temp = new int[numberOfActions];
        for(int i = 0; i<temp.length;i++)
            temp[i]=i;
        for(int i = 0; i<removedActions.length;i++)
            temp[removedActions[i]] = -1;
        int r = 0;
        for(int i = 0; i<temp.length;i++)
            if(temp[i]!=-1){
                strategyMap[r] = temp[i];
                r++;
            }
    }

    public MixedStrategy getStrategy(MixedStrategy ms)
    {
        double[] probs = ms.getProbs();
        double[] temp = new double[numActions+1];
        temp[0] = 0.0;//for some reason has to have a zero at the beginning
        int j = 0;
        for(int i = 1;i<temp.length;i++){
            if((i-1) == strategyMap[j]){
                temp[i] = probs[j+1];
                j++;
            }
        }
        return new MixedStrategy(temp);
    }

    public void printStrategyMap()
    {
        for(int i = 0; i < strategyMap.length;i++)
            System.out.print(strategyMap[i]+" ");
        System.out.println();
    }

    public void loadStrategy(String s){
        Scanner scan = new Scanner(s);
        scan.useDelimiter(",");
        strategyName = scan.next();
        numActions = scan.nextInt();
        ArrayList<Integer> list = new ArrayList<Integer>();
        while(scan.hasNext()){
            list.add(scan.nextInt());
        }
        strategyMap = new int[list.size()];
        for(int i = 0; i< strategyMap.length;i++)
            strategyMap[i] = list.get(i).intValue();
    }

    public String toString(){
        String s = "";
        for(int i =0; i<strategyMap.length;i++)
            s = s+strategyMap[i]+",";
        return strategyName+","+numActions+","+s;
    }

    public String getStrategyName(){
        return strategyName;
    }
    public int[] getStrategyMap(){
        return strategyMap;
    }


}
