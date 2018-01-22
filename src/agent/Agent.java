package agent;

import java.util.ArrayList;

import games.MixedStrategy;
import solvers.Param;
import subgame.Parameters;

/**
 * Container for agent information: path, name, abstraction, etc.
 * Author: Oscar
 * Version: 2014-05-14
 */
public class Agent {
    protected String path;
    protected String name;
    protected int level;
    protected Abstraction abstraction;
    protected ArrayList<AgentsThread> p1Threads;
    protected ArrayList<AgentsThread> p2Threads;
    protected DecisionMode decisionMode;
    protected Param p;

    public enum DecisionMode { // BR:  play a best response to the the QRE distribution
    RAW, BR // RAW:  play the QRE as given
    }

    public enum Abstraction {
        CONTROL, BUCKETING, RANDOM, TOPN, KMEANS
    }

    public Agent (String newName){//no abstraction
        this(newName,Abstraction.CONTROL,0);
    }
    public Agent(String newName, Abstraction newAbstraction, int newLevel){
        name = newName;
        path = Parameters.GAME_FILES_PATH+name+".jar";
        level = newLevel;
        abstraction = newAbstraction;
        p1Threads = new ArrayList<AgentsThread>();
        p2Threads = new ArrayList<AgentsThread>();
    }

    public String getPath(){return path;}
    public String getName(){return name;}
    public int getLevel(){return level;}
    public Abstraction getAbstraction(){return abstraction;}
    public void setAbstraction(Abstraction newAbstraction){abstraction = newAbstraction;}
    public void setLevel(int newLevel){level = newLevel;}

    public String getAbstractionName(){
        switch(abstraction){
            case BUCKETING:
                return "BUCKETING";
            case RANDOM:
                return "RANDOM";
            case TOPN:
                return "TOPN";
            case KMEANS:
                return "KMEANS";
            default:
                return "CONTROL";
        }
    }
    public String toString(){
        return name + " " + getAbstractionName() + " " + level;
    }

    public String getFullName(){
        return toString();
    }

    public void setParam(Param param){
        p = param;
    }
    public Param getParam(){
        return p;
    }


    public void setDecisionMode(DecisionMode dm){
        decisionMode = dm;
    }

    public void addThreads(AgentsThread t, int player){
        if(player == 0)//player 1
            p1Threads.add(t);
        else
            p2Threads.add(t);
    }

    public MixedStrategy getStrategy(int gameNumber, int player){
        if(player == 0)
            return p1Threads.get(gameNumber).getStrategy();
        else
            return p2Threads.get(gameNumber).getStrategy();
    }

    public String getShortName(){
        String s = name;
        if(s.contains("[0.0_0.0_false]_RAW"))
            s = "UR";
        else if(s.contains("[0.0_0.0_false]_BR"))
            s = "BRUR";
        else if(s.contains("[100.0_0.0_false]_RAW"))
            s = "MSNE";
        else if(s.contains("[100.0_0.0_false]_BR"))
            s = "BRMSNE";
        else if(s.startsWith("QRE")){
            if(p.getParam1() > 0)
                s = "QRE_" + p.getParam1() + "_";
            else
                s = "QRE_" + (int)(p.getParam2()*100) +"%_";
            if(decisionMode == DecisionMode.RAW)
                s = s + "RAW";
            else
                s = s + "BR";
        }
        return s;
    }

}
