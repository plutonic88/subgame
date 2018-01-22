package Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;

import subgame.Parameters;

public class Logger {
	
	public static boolean LOG_ON = false;
	public static boolean LOGIT_ON = false;
	
	
	
	public static void log(String logstring, boolean newline)
	{

		if(Logger.LOG_ON==true)
		{
		try{
			PrintWriter pw = new PrintWriter(new FileOutputStream(new File(Parameters.GAME_FILES_PATH+"logfile"+".log"),true));

			


				pw.append(logstring);
				
				if(newline)
				{
					pw.append("\n");
				}
				else
				{
					pw.append(" ");
				}

			
			
			pw.close();


		}
		catch(Exception e)
		{

		}
		}
	}
	
	
	public static void logit(String logstring)
	{

		if(Logger.LOGIT_ON==true)
		{
		try{
			PrintWriter pw = new PrintWriter(new FileOutputStream(new File(Parameters.GAME_FILES_PATH+"logfile"+".log"),true));

			


				pw.append(logstring);
				
				
				
				{
					pw.append(" ");
				}

			
			
			pw.close();


		}
		catch(Exception e)
		{

		}
		}
	}
	
	public static void regretLog(String logstring)
	{

		/*//if(Logger.LOGIT_ON==true)
		{
		try
		{
			PrintWriter pw = new PrintWriter(new FileOutputStream(new File(Parameters.GAME_FILES_PATH+"logfile"+".log"),true));
			pw.append(logstring);
			{
					pw.append(" ");
			}
			pw.close();
		}
		catch(Exception e)
		{

		}*/
		//}
	}
	
	
	
	
	

}
