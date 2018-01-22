package agent;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import games.EmpiricalMatrixGame;
import games.MixedStrategy;
import parsers.GamutParser;
import subgame.Parameters;


public class AgentsThread implements Runnable {

	private Process process;
	private ArrayList<String> cmd;
	private String agentPath;
	private String agentName;
	private String game;
	private int player;
	private int actionChosen;
	public final int unAbstracted = 0;
    public final int bucketed = 1;
    public final int removed = 2;
    private int currentState;
	private ArrayList<String> output;
    private double fraction;
    private int instance;

    public AgentsThread (String agentPath, String agentName, String game, int player, int state){
        this(agentPath,agentName,game,player,state,0.0);
    }
	public AgentsThread(String agentPath, String agentName, String game, int player, int state,double fraction) {
		this.cmd = new ArrayList<String>();
		this.player = player;
		this.output = new ArrayList<String>();
		this.agentPath = agentPath.substring(0, agentPath.length() - (agentName.length() + 4));
		this.agentName = agentName;
		this.game = game+ Parameters.GAMUT_GAME_EXTENSION;
        currentState = state;
        this.fraction = fraction;
        this.instance = 0;//not needed in this case
	}
    public AgentsThread(String agentPath, String agentName, String game, int player, int state,double fraction,int instance) {
		this.cmd = new ArrayList<String>();
		this.player = player;
		this.output = new ArrayList<String>();
		this.agentPath = agentPath.substring(0, agentPath.length() - (agentName.length() + 4));
		this.agentName = agentName;
		this.game = game+ Parameters.GAMUT_GAME_EXTENSION;
        currentState = state;
        this.fraction = fraction;
        this.instance = instance;
	}
	public void run() {
		cmd.clear();
		cmd.add("java");
		cmd.add("-jar");
		cmd.add(agentPath+agentName+".jar");
		cmd.add("-game");
        if(currentState==unAbstracted)
		    cmd.add(Parameters.GAME_FILES_PATH + game );
        else if (currentState == bucketed)
            cmd.add(Parameters.GAME_FILES_PATH + "b-"+((int)fraction)+"-"+ game );
        else //state == removed
            cmd.add(Parameters.GAME_FILES_PATH + "r"+fraction+"-"+instance+"-" + game);
		cmd.add("-player");
		cmd.add(Integer.toString(player));
        int tries = 0;

        try {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.directory(new File(agentPath));
            process = pb.start();
            //process.waitFor();
            while (true){
                try{
                    process.exitValue();
                    break;
                }
                catch(IllegalThreadStateException we){
                    if(tries++<=10){
                        try {
                            Thread.sleep(1000);
                        } catch(InterruptedException ex) {
                            //Thread.currentThread().interrupt();
                            //throw new RuntimeException("Exception while running gambit: "+ ex.getMessage());
                        }
                    }
                    else{
                        process.destroy();
                        break;
                    }
                }
            }
            InputStream is = process.getInputStream();
            parseOutput(is);
            is.close();
            process.getOutputStream().close();
            process.getErrorStream().close();
            process.destroy();
        }
        /*catch (InterruptedException ie) {
            process.destroy();
        }*/
        catch (Exception e) {
            //throw new RuntimeException("Exception while running gambit: "+ e.getMessage());
        }
    }

	private void parseOutput(InputStream is) throws IOException {
		InputStreamReader isr = new InputStreamReader(is);
		BufferedReader br = new BufferedReader(isr);
		String line;
		while ((line = br.readLine()) != null) {
            //debug
			//System.out.println(line);
			output.add(line);
		}
		isr.close();
		br.close();
	}

	public int getAction() {
		for (int i = 0; i < output.size(); i++) {
			String[] line = output.get(i).split(",");
			if (line[0].equals("Strategy")) {
				actionChosen = Integer.parseInt(line[1]);
				break;
			}
		}
		return actionChosen;
	}

    public MixedStrategy getStrategy()
    {
        try{
        String strat = output.get(0);
        strat = strat.substring(1,strat.length()-1);
        strat = "0.0, "+ strat;//for some reason it needs to start with a zero
        //System.out.println(strat);  //debug
        Scanner scan = new Scanner(strat);
        scan.useDelimiter(", ");
        List<Double> list = new ArrayList<Double>();
        while(scan.hasNext()){
            list.add(scan.nextDouble());
        }
        double[] probs = new double[list.size()];
        for(int i = 0;i<probs.length;i++)
            probs[i]=list.get(i).doubleValue();

        return new MixedStrategy(probs);
        }
        catch(Exception e)
        {
            EmpiricalMatrixGame mg;
            if(currentState==unAbstracted)
                mg= new EmpiricalMatrixGame(GamutParser.readGamutGame(Parameters.GAME_FILES_PATH +this.game));
            else if (currentState == bucketed)
                mg= new EmpiricalMatrixGame(GamutParser.readGamutGame(Parameters.GAME_FILES_PATH + "b-"+((int)fraction)+"-"+this.game));
            else //state == removed
                mg= new EmpiricalMatrixGame(GamutParser.readGamutGame(Parameters.GAME_FILES_PATH + "r"+fraction+"-"+instance+"-" +this.game));
            //mg= new EmpiricalMatrixGame(GamutParser.readGamutGame(this.game));
            int actions = mg.getNumActions(this.player-1);
            double[] probs = new double[actions+1];
            probs[0]=0.0;
            for(int i = 1;i<probs.length;i++)
                probs[i] = -1.0;//invalid strategy
            return new MixedStrategy(probs);
        }
    }
}