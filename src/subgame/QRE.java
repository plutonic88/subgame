package subgame;

import games.GameUtils;
import games.MixedStrategy;

/**
 * Aux class used to store a QRE solution
 * Author: Oscar Veliz
 */
public class QRE {
    private double lambda;
    private MixedStrategy[] strategies;
    private double[] entro;

    public QRE(double lam, MixedStrategy[] s){
        lambda = lam;
        strategies = new MixedStrategy[s.length];
        for(int i = 0; i < strategies.length; i++)
            strategies[i] = new MixedStrategy(s[i].getNumActions());
        for(int i = 0; i < strategies.length;i++){
            for(int a = 1; a <= s[i].getNumActions(); a++){
                strategies[i].setProb(a,s[i].getProb(a));
            }
        }
        entro = new double[strategies.length];
        for(int i = 0; i < strategies.length;i++)
            entro[i] = GameUtils.computeEntropy(strategies[i]);
    }

    public double getLambda(){return lambda;}
    public MixedStrategy[] getStrategies(){return strategies;}
    public MixedStrategy getStrategy(int player){return strategies[player];}
    public double getEntropy(int player){return entro[player];}
    public double[] getEntropies(){return entro;}
}
