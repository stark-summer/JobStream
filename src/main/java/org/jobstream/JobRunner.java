package org.jobstream;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;


public class JobRunner extends Thread

{
//	public volatile static int scnt=0;

	JobInfo jobinfo;
	ArrayList<JobInfo> jobqueue;
	Map<String,String> stautsmap;
	String project_en;
	int crontab_id;
	String scheduler_seq;
	Map<String,JobInfo> jobinfomap;//job基本信息
	Map<String,String> project_param;
	Map<String,String> runningmap;

	public JobRunner(JobInfo jobinfo,ArrayList<JobInfo> jobqueue,Map<String,String> stautsmap,String project_en,int crontab_id,String scheduler_seq,Map<String,JobInfo> jobinfomap,Map<String,String> project_param,	Map<String,String> runningmap)
	{
		this.jobinfo=jobinfo;
		this.jobqueue=jobqueue;
		this.stautsmap=stautsmap;
		this.project_en=project_en;
		this.crontab_id=crontab_id;
		this.scheduler_seq=scheduler_seq;
		this.jobinfomap=jobinfomap;
		this.project_param=project_param;
		this.runningmap=runningmap;
	}
	/*
	public static synchronized void opScnt(String type)
	{
		if (type.equals("inc"))
		{
			scnt++;
		}
		else if(type.equals("reduce"))
		{
			scnt--;
		}
		
	}
	*/
	/*
 public static synchronized void incrementScnt()
 {
	 scnt++;
 }
 
 public static synchronized void reduceScnt()
 {
	 scnt--;
 }
 */
 

 public  void run()

 {  
	// opScnt("inc");
	 if (!runningmap.containsKey(jobinfo.getJob_en()))
	 {
	 runningmap.put(jobinfo.getJob_en(), "");
	 }
   execScript();
  // opScnt("reduce");
   if (runningmap.containsKey(jobinfo.getJob_en()))
	 {
	 runningmap.remove(jobinfo.getJob_en());
	 }
   if (!stautsmap.get(jobinfo.getJob_en()).equals("C"))
   {
	   int retry_count=PropHelper.getIntegerValue("retry.count");
	   for (int i=1;i<=retry_count;i++)
	   {
		//   opScnt("inc");
		   if (!runningmap.containsKey(jobinfo.getJob_en()))
			 {
			 runningmap.put(jobinfo.getJob_en(), "");
			 }
		   execScript();
		//   opScnt("reduce");
		   if (runningmap.containsKey(jobinfo.getJob_en()))
			 {
			 runningmap.remove(jobinfo.getJob_en());
			 }
		   if (stautsmap.get(jobinfo.getJob_en()).equals("C"))
		   {
			   break;
		   }
		   
	   }
	   
   }
 }
 
 public int getLog_id() {
	return log_id;
}
public void setLog_id(int log_id) {
	this.log_id = log_id;
}

private int log_id=0;
 
 public void init_log()
 {
		PropertyConfigurator.configure("conf/log4j.properties");
	Logger logger = Logger.getLogger(JobRunner.class.getName());
		Connection con = null;
		Statement sql = null;
		ResultSet rs=null;

		try {
			con = DbCoonect.getConnectionMySql();
			if (con == null) {
				logger.error("connect is null");
				System.exit(0);
			}
			sql = con.createStatement();

            String start_date=new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date( ));
            String datekey=new java.text.SimpleDateFormat("yyyyMMdd").format(new java.util.Date( ));
			String strSql = " insert into proj_log(project_en,proj_crontab_id,proj_scheduler_seq,job_en,job_cn,start_date,datekey,program_status,path) values('"+project_en+"',"+
					crontab_id+",'"+scheduler_seq+"','"+jobinfo.getJob_en()+"','"+jobinfo.getJob_cn()+"','"+start_date+"','"+datekey+"','"+"S"+"','"+jobinfo.getPath()+"')";
			// System.out.println("3333333333:" + strSql);
		 sql.executeUpdate(strSql);
		rs= sql.executeQuery("select last_insert_id()");
		if (rs.next())
		{
			setLog_id(rs.getInt(1));
		}
		
		}
		catch (Exception e) {
			logger.error(e.getMessage());
			//e.printStackTrace();
			

		} finally {
			try {
				con.close();
			} catch (SQLException e) {
				logger.error(e.getMessage());
			}
		}
	 
 }
		
 public void finish_log(String status,String loginfo)
 {
	 PropertyConfigurator.configure("conf/log4j.properties");
	Logger logger = Logger.getLogger(JobRunner.class.getName());
		
		Connection con = null;
		Statement sql = null;
		logger.info("job_en:"+jobinfo.getJob_en()+" status:"+status+" finish_log db connecting ");

		try {
			con = DbCoonect.getConnectionMySql();
			if (con == null) {
				logger.error("connect is null");
				System.exit(0);
			}
			logger.info("job_en:"+jobinfo.getJob_en()+" status:"+status+" finish_log db connected ");
			sql = con.createStatement();

            String end_date=new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date( ));
     
            int log_id=getLog_id();
            String strSql="update proj_log set end_date='"+end_date+"',program_status='"+status+"',loginfo='"+loginfo+"' where id ="+log_id;
            logger.info("job_en:"+jobinfo.getJob_en()+" status:"+status+" finish_log db executeUpdating ");
            sql.executeUpdate(strSql);
            logger.info("job_en:"+jobinfo.getJob_en()+" status:"+status+" finish_log db executeUpdated ");
		}
		catch (Exception e) {
			//e.printStackTrace();
			logger.error(e.getMessage());
			

		} finally {
			try {
				con.close();
			} catch (SQLException e) {
				logger.error(e.getMessage());
			}
		}
 }
 
 public String getLocalIp()
	{
		InetAddress myIPaddress=null;
		try { 
			myIPaddress=InetAddress.getLocalHost();
			}
		catch (UnknownHostException e) {}
	 return myIPaddress.getHostAddress();
	 
	}
	public String getUserName()
	{
		return System.getProperty("user.name");
	}
 public void execScript()
 {
	 PropertyConfigurator.configure("conf/log4j.properties");
		Logger logger = Logger.getLogger(JobRunner.class.getName());
		logger.info(jobinfo.getJob_en()+" begin exec");
	 stautsmap.put(jobinfo.getJob_en(), "S");
	// String command = "ssh -p"+jobinfo.getPort()+" "+jobinfo.getUser()+"@"+jobinfo.getIp()+ " \""+jobinfo.getPath()+"\"";
	 //logger.info(command);
 //   String command=jobinfo.getPath();
	 String[] cmds=null;
	 String cmd="";
	 if (getLocalIp().equals(jobinfo.getIp()))
	 {
		 
	// cmds = new String[2];
	 String prestr="";
	 if (!getUserName().equals(jobinfo.getUser()))
	 {
		 prestr="sudo -u "+jobinfo.getUser()+" ";
	 }
	 if (jobinfo.getJob_type().equals("shell"))
	 {
		cmd =prestr+"sh";
	 }
	 else if (jobinfo.getJob_type().equals("python"))
	 {
		 cmd =prestr+"python";
	 }
	 else if (jobinfo.getJob_type().equals("java"))
	 {
		 cmd =prestr+"java -cp";
	 }
	 else if (jobinfo.getJob_type().equals("mapreduce"))
	 {
		 cmd =prestr+"hadoop jar";
	 }
	 else
	 {
		 cmd =prestr+"sh";
	 }
	 
	 cmd=cmd+" "+jobinfo.getPath();
	 }
	 else
	 {
	cmds = new String[5];
	 if (jobinfo.getJob_type().equals("shell"))
	 {
		cmds[0] = PropHelper.getStringValue("sshmodule");
	 }
	 else if (jobinfo.getJob_type().equals("python"))
	 {
		 cmds[0] = PropHelper.getStringValue("pythonmodule");
	 }
	 else if (jobinfo.getJob_type().equals("java"))
	 {
		 cmds[0] = PropHelper.getStringValue("javamodule");
	 }
	 else if (jobinfo.getJob_type().equals("mapreduce"))
	 {
		 cmds[0] = PropHelper.getStringValue("mapreducemodule");
	 }
	 else
	 {
		 cmds[0] = PropHelper.getStringValue("sshmodule");
	 }
	 
	    cmds[1]=String.valueOf(jobinfo.getPort());
	    cmds[2]=jobinfo.getUser();
	    cmds[3]=jobinfo.getIp();
	    cmds[4]=jobinfo.getPath();
	 
	 }
	    if (jobinfo.getParam()!=null)
		{
	    	if (!jobinfo.getParam().equals(""))
	    	{	
			String[] s=jobinfo.getParam().split(";");
			Pattern pat = Pattern.compile("\\$\\{[a-zA-z_0-9]+\\}"); 
			Pattern pat1 = Pattern.compile("expr_date\\(([date|hour].*),([a-zA-z0-9-/ ]+)\\)"); 
			//多个参数
			for (int i=0;i<s.length;i++)
			{
				Matcher mat = pat.matcher(s[i]);
				Matcher mat1 = pat1.matcher(s[i]);
				//一个参数内有多个变量 ${thisdate}
				while(mat.find())
				{
					String item=mat.group();
					if (project_param.containsKey(item))
					s[i]=s[i].replace(item, project_param.get(item));
					else
						logger.info("job_en system param:"+item+" not in project_en:"+project_en);
				}
				//expr_date(day-1,yyyyMMdd)
				
				while(mat1.find())
				{
					String item1=mat1.group();
					s[i]=s[i].replace(item1, CommonUtil.expr_date(item1));
				
				}
				if(getLocalIp().equals(jobinfo.getIp()))
					{
					cmd=cmd+" "+s[i];
					}
				else
				{
					cmds[4]=cmds[4]+" "+s[i];
				}
			}
	    	}
		}
	
	//    logger.info(Arrays.toString(cmds));
	    
	 
	 final StringBuilder errbuilder=new StringBuilder();
	 String status="";
	 init_log();
	 Process ps=null;
	 
	    try {
	    	if(getLocalIp().equals(jobinfo.getIp()))
	    	{
	    		logger.info("cmd:"+cmd);
	    		ps = Runtime.getRuntime().exec(cmd);
	    	}
	    	else
	    	{
	    		logger.info("cmds:"+Arrays.toString(cmds));
	         ps = Runtime.getRuntime().exec(cmds);
	    	}
	      //  InputStream in = ps.getInputStream();
	        /*
	        int c;
	        while ((c = in.read()) != -1) {
	            System.out.print(c);// 如果你不需要看输出，这行可以注销掉
	        }
	        */
	    	final InputStream input1=ps.getInputStream();
	    	final InputStream input2=ps.getErrorStream();
	      
	      new Thread()
	      {
	    	  public void run()
	    	  {
	    		  try
	    		  {
	    			BufferedReader br2 = new BufferedReader(new InputStreamReader(input2));
	    	        String line2;
	    	  	  while ((line2=br2.readLine())!=null)
	    	        {
	    	  		if(errbuilder.length()>1000)
	        		{
	        		System.out.println(jobinfo.getJob_en()+":"+line2); 
	        		continue;
	        		}
	        	System.out.println(jobinfo.getJob_en()+":"+line2);
	        	errbuilder.append(line2.replace("'", "")).append(";");
	    	        	
	    	        }
	    		  }
	    		  catch (IOException e) {   
	                  e.printStackTrace();  
	            }   
	           finally{  
	              try {  
	            	  input2.close();  
	              } catch (IOException e) {  
	                  e.printStackTrace();  
	              }  
	            } 
	    	  }
	      }.start();
	      
	      new Thread()
	      {
	    	  public void run()
	    	  {
	    		  try
	    		  {
	    			BufferedReader br1 = new BufferedReader(new InputStreamReader(input1));
	    	        String line1;
	    	  	  while ((line1=br1.readLine())!=null)
	    	        {
	    	        	System.out.println(line1);
	    	        	
	    	        }
	    		  }
	    		  catch (IOException e) {   
	                  e.printStackTrace();  
	            }   
	           finally{  
	              try {  
	            	  input1.close();  
	              } catch (IOException e) {  
	                  e.printStackTrace();  
	              }  
	            } 
	    	  }
	      }.start();
	      
	      /*
	  	BufferedReader br2 = new BufferedReader(new InputStreamReader(ps.getErrorStream()));
        String line2;
  	  while ((line2=br2.readLine())!=null)
        {
        	if(errbuilder.length()>1000)
        		{
        		System.out.println(jobinfo.getJob_en()+":"+line2); 
        		continue;
        		}
        	System.out.println(jobinfo.getJob_en()+":"+line2);
        	errbuilder.append(line2.replace("'", "")).append(";");
        	
        }
		
	  	BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(ps.getInputStream())); 
	  	String s;
	  	  while((s=bufferedReader.readLine()) != null)  
	     {
	  		System.out.println(jobinfo.getJob_en()+":"+s);   
	     }
	  	  
	  	  */
	        
	   //     br2.close();
	   //     bufferedReader.close();
	        
	    
	     int exitstatus= ps.waitFor();
	        if (exitstatus==0)
	        	{
	        	status="C";
	        	 stautsmap.put(jobinfo.getJob_en(), "C");
	        	 logger.info(jobinfo.getJob_en()+" updated status C in memory");
	        	 checkAndJoinQue();
	        	 logger.info(jobinfo.getJob_en()+" updating status C in db");
	        	 finish_log(status,"");
	        	 logger.info(jobinfo.getJob_en()+" updated status C in db");
	        	 logger.info(jobinfo.getJob_en()+": success exec");
	        	 
	        	}
	        else 
	        	{
	        	status="F";
	        	 stautsmap.put(jobinfo.getJob_en(), "F");
	        	 logger.info(jobinfo.getJob_en()+" updated status F in memory");
	        	 finish_log(status,errbuilder.toString());
	        	 logger.info(jobinfo.getJob_en()+" updated status F in db");
	        	 logger.info(jobinfo.getJob_en()+": fail exec "+errbuilder.toString());
	        	 CommonUtil.sendmail(new StringBuilder("job_en:").append(jobinfo.getJob_en()).append("\n").append("path:").append(jobinfo.getPath()).append("\n")
	        			 .append("error message:").append(errbuilder.toString()).toString(), jobinfo.getPath());
	        	 
	        	}
	           
	        

	    } catch (IOException ioe) {
	    	logger.error(jobinfo.getJob_en()+": "+ioe.getMessage());
	    	status="F";
       	 stautsmap.put(jobinfo.getJob_en(), "F");
       	finish_log(status,ioe.getMessage()); 
        CommonUtil.sendmail(new StringBuilder("job_en:").append(jobinfo.getJob_en()).append("\n").append("path:").append(jobinfo.getPath()).append("\n")
   			 .append("error message:").append(ioe.getMessage()).toString(), jobinfo.getPath());
       	
	    } catch (InterruptedException e) {
	        // TODO Auto-generated catch block
	        logger.error(jobinfo.getJob_en()+": "+e.getMessage());
	       	status="F";
	       	 stautsmap.put(jobinfo.getJob_en(), "F");
	       	finish_log(status,e.getMessage()); 
	       	CommonUtil.sendmail(new StringBuilder("job_en:").append(jobinfo.getJob_en()).append("\n").append("path:").append(jobinfo.getPath()).append("\n")
	      			 .append("error message:").append(e.getMessage()).toString(), jobinfo.getPath());
	    }
	   
	 
 }
 
 public void checkAndJoinQue()
 {
	 PropertyConfigurator.configure("conf/log4j.properties");
		Logger logger = Logger.getLogger(JobRunner.class.getName());
		String job_en=jobinfo.getJob_en();
	 logger.info("job_en:"+job_en+" checkAndJoinQue running");
	 //被哪些作业依赖，再分别检查这些作业是否可以加入队列
	 HashSet<String> hs=jobinfo.getRefedjobs();
	 Map<String,String> currentstautsmap=null;
	 if (hs.size()>0)
		 currentstautsmap= new ConcurrentHashMap<String,String>(stautsmap);
	 for(Iterator it=hs.iterator();it.hasNext();)
	  {
		 String job_en_item=(String) it.next();
		 JobInfo jobinfo=jobinfomap.get(job_en_item);
		 HashSet<String> refitem=jobinfo.getRefjobs();
		 int totalsize=refitem.size();
		 int success_size=0;
		 for(Iterator it1=refitem.iterator();it1.hasNext();)
		  {
			 String job_en_item1=(String) it1.next();
			 if (currentstautsmap.containsKey(job_en_item1))
			 {
			 if (currentstautsmap.get(job_en_item1).equals("C"))
			 {
				 success_size++;
		      }
			 }
			 
			 //if (totalsize==success_size && !stautsmap.contains(jobinfo.getJob_en()))
			 if (totalsize==success_size)
			 {
				 jobinfo.setStarttime(new Date().getTime());
				 
				 synchronized(jobqueue)
				 {
					 logger.info("job_en:"+job_en+  " refed job_en:"+jobinfo.getJob_en()+" adding jobqueue");
			    jobqueue.add(jobinfo);
			    logger.info("job_en:"+job_en+  " refed job_en:"+jobinfo.getJob_en()+" added jobqueue");
				 resortJobqueue();
				  logger.info("job_en:"+job_en+  " refed job_en:"+jobinfo.getJob_en()+" resort jobqueue");
				 jobqueue.notifyAll();
				 logger.info("job_en:"+job_en+  " refed job_en:"+jobinfo.getJob_en()+" ended jobqueue");
				 }
			 }
		
		  
	  }

	  }
	 logger.info("job_en:"+job_en+" checkAndJoinQue end");
		
	
 }
 
 public void resortJobqueue()
 {
	 Comparator<JobInfo> comp = new Comparator<JobInfo>() {
	      public int compare(JobInfo o1, JobInfo o2) {
	        int res = o1.getPriority()-o2.getPriority();
	        if(res == 0) {
	          if(o1.getStarttime() < o2.getStarttime())
	            res = 1;
	          
	          else
	            res = (o1.getStarttime()==o2.getStarttime() ? 0 : -1);
	        }
	          
	        return -res;
	      }
	    };
	    
	    synchronized (jobqueue) {
	      Collections.sort(jobqueue, comp);
	    }
 }
}

