package solvers;

/**
 * Parameter class for agents
 */
public class Param {
    private double param1;
    private double param2;
    private boolean param3;

    public Param(double p1, double p2, boolean p3){
        param1 = p1;
        param2 = p2;
        param3 = p3;
    }

    public double getParam1(){
        return param1;
    }

    public double getParam2(){
        return param2;
    }

    public boolean getParam3(){
        return param3;
    }

    public String toString(){
        return "["+param1+"_"+param2+"_"+param3+"]";
    }


}
