package games;


public class MatrixSubgame extends Game {
	
  Game parent;		/* the MatrixGame from which this subgame is derived 
   					   @TODO: change to MatrixGame after path compression is implemented */
  private int[][] actions;	/* actions[player] contains the actions available to that player */
  
  public MatrixSubgame(Game parent, int[][] actions) {
    /* @TODO: path compression when passed a MatrixSubgame */
	super(parent.getNumPlayers(), 1); /* placeholder initialization before we have numActions[] */
    for(int p=0; p<this.nPlayers; p++) {
    	assert(actions[p].length>0);
    	this.nActions[p]=actions[p].length;
    }
    this.parent=parent;
    this.actions=actions;
    this.updateGameSize();
  }
  
  public double getPayoff(int[] outcome, int player) {
    int[] originalOutcome=new int[this.nPlayers];
    for(int p=0; p<this.nPlayers; p++) {
      /* Games use one-based indexing, but this array implementation is zero-based */
      originalOutcome[p]=this.actions[p][outcome[p]-1];
    }
    return this.parent.getPayoff(originalOutcome, player);
  }
  
  public double[] getPayoffs(int[] outcome) {
    int[] originalOutcome=new int[this.nPlayers];
    for(int p=0; p<this.nPlayers; p++) {
      /* Games use one-based indexing, but this array implementation is zero-based */
      originalOutcome[p]=this.actions[p][outcome[p]-1];
    }
    return this.parent.getPayoffs(originalOutcome);
  }
}
