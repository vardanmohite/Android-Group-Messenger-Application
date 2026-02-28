package edu.buffalo.cse.cse486586.groupmessenger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class GroupMessengerActivity extends Activity {
	
	private int emulator_instance;
	final String localHost="10.0.2.2";
	boolean test2=false;
	final int sequencerNumber = 5554;
	static int globalClock,localClock,sequenceNumberLocalApp;
	static int avd0LocalClock,avd1LocalClock,avd2LocalClock;
	Map<Long,String> holdbackQueue;
	Map<Long,Integer> sequenceQueue;
	Map<String,String> sequenceOrderingQueue;
	ContentValues cv_pushToContentProvider;
	private ContentResolver mContentResolver;
    private Uri mUri;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);
        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
    	String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
    	emulator_instance=Integer.parseInt(portStr);
    	mUri = buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger.provider");
    	mContentResolver=getContentResolver();
    	createServer();
    	holdbackQueue = new HashMap<Long, String>();
    	sequenceQueue = new HashMap<Long, Integer>();
    	if(emulator_instance == sequencerNumber)
    	{
    		sequenceOrderingQueue = new HashMap<String,String>();	
    	}
        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));
            }
    private Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }
    private void createServer() 
	{
		new Thread(new Runnable()
		{
			public void run()
			{
				try 
				{
					ServerSocket s = new ServerSocket(10000);
					while(true)
					{
						Socket ss=s.accept();
						String msg_ServerSide=null;
						BufferedReader br=new BufferedReader(new InputStreamReader(ss.getInputStream()));
						msg_ServerSide=br.readLine();
						//System.out.println(msg_ServerSide);
						String[] message=msg_ServerSide.split(",");
						//System.out.println(message.length);
						if(message.length > 3)
						{
							testCaseMultiCastMessage();//testCase2
						}
						if(message[0].equals("sequenceMessage"))
						{
							if(localClock+1 == Integer.parseInt(message[2]))
							{
								if(sequenceQueue.containsKey(Long.parseLong(message[1])))
								{
									String upMesg=holdbackQueue.get(Long.parseLong(message[1]));
									new displayMessage().execute(upMesg);
									localClock++;
									holdbackQueue.remove(Long.parseLong(message[1]));
									sequenceQueue.remove(Long.parseLong(message[1]));
									checkSQ((localClock+1));
								}
								else if(holdbackQueue.containsKey(Long.parseLong(message[1])))
								{
									String upMesg=holdbackQueue.get(Long.parseLong(message[1]));
									new displayMessage().execute(upMesg);
									localClock++;
									checkSQ(localClock+1);
									holdbackQueue.remove(Long.parseLong(message[1]));
								}
								else if(!sequenceQueue.containsKey(Long.parseLong(message[1])))
								{
								sequenceQueue.put(Long.parseLong(message[1]), Integer.parseInt(message[2]));
								}
							}
							else
							{
								if(!sequenceQueue.containsKey(Long.parseLong(message[1])))
								{
								sequenceQueue.put(Long.parseLong(message[1]), Integer.parseInt(message[2]));
								}
							}
						}
						else
						{								
							if(emulator_instance == sequencerNumber)
							{
								//System.out.println(message[0]);
								String [] locArr=message[0].split(":");
								String localClockCheck=locArr[0];
								int localMessageClock=Integer.parseInt(locArr[1]);
								if(localClockCheck.equals("avd0"))
								{
									if((avd0LocalClock)==localMessageClock)
									{
										multiCastSequence(message[1]);
										avd0LocalClock++;
										orderingQueueCheck(0);
									}
									else
									{
										sequenceOrderingQueue.put(message[0], message[1]);
									}
								}
								else if(localClockCheck.equals("avd1"))
								{
									if((avd1LocalClock)==localMessageClock)
									{
										multiCastSequence(message[1]);
										avd1LocalClock++;
										orderingQueueCheck(1);
									}
									else
									{
										sequenceOrderingQueue.put(message[0], message[1]);
									}
								}
								else if(localClockCheck.equals("avd2"))
								{
									if((avd2LocalClock)==localMessageClock)
									{
										multiCastSequence(message[1]);
										avd2LocalClock++;
										orderingQueueCheck(2);
									}
									else
									{
										sequenceOrderingQueue.put(message[0], message[1]);
									}
								}
								
							}
							if(!holdbackQueue.containsKey(Long.parseLong(message[1])))
							{
							holdbackQueue.put(Long.parseLong(message[1]), message[0]);
							checkSQ(localClock+1);
							}
							//System.out.println(holdbackQueue+" "+sequenceQueue+" "+sequenceOrderingQueue);
							if(holdbackQueue!=null && sequenceQueue!=null && sequenceOrderingQueue!=null)
							{
								//System.out.println(holdbackQueue.size()+" "+sequenceQueue.size()+" "+sequenceOrderingQueue.size());
							}
							br.close();
							ss.close();
						}
					}
				} catch (IOException e) 
				{
					e.printStackTrace();
				}
			}

			private void orderingQueueCheck(int ii) {
				Set<String> checkString=sequenceOrderingQueue.keySet();
				Iterator<String> i=checkString.iterator();
				while(i.hasNext())
				{
					String shouldIMultiCastMessage=(String)i.next();
					String [] here=shouldIMultiCastMessage.split(":");
					int clockVal=Integer.parseInt(here[1]);
					if(ii == 0)
					{
						if((avd0LocalClock) == clockVal)
						{
							multiCastSequence(sequenceOrderingQueue.get(shouldIMultiCastMessage));
							avd0LocalClock++;
							sequenceOrderingQueue.remove(shouldIMultiCastMessage);
							orderingQueueCheck(0);
							break;
						}
					}
					else if(ii == 1)
					{
						if((avd1LocalClock) == clockVal)
						{
							multiCastSequence(sequenceOrderingQueue.get(shouldIMultiCastMessage));
							avd1LocalClock++;
							sequenceOrderingQueue.remove(shouldIMultiCastMessage);
							orderingQueueCheck(1);
							break;
						}						
					}
					else if(ii == 2)
					{
						if((avd2LocalClock) == clockVal)
						{
							multiCastSequence(sequenceOrderingQueue.get(shouldIMultiCastMessage));
							avd2LocalClock++;
							sequenceOrderingQueue.remove(shouldIMultiCastMessage);
							orderingQueueCheck(2);
							break;
						}						
					}
				}
			}

			private void multiCastSequence(String string) 
			{				
				globalClock++;
				String seqMessage="sequenceMessage"+","+string+","+globalClock;
				multiCastMessage(seqMessage);
				
			}

			private void checkSQ(int lc) 
			{
				if(sequenceQueue.containsValue(lc))
				{
					Set<Long> keys= sequenceQueue.keySet();
					Iterator<Long> it=keys.iterator();
					while(it.hasNext())
					{
						Long l=(Long)it.next();
						if(sequenceQueue.get(l)==lc)
						{
							String upMesg=holdbackQueue.get(l);
							new displayMessage().execute(upMesg);
							localClock++;
							holdbackQueue.remove(l);
							sequenceQueue.remove(l);
							checkSQ(localClock+1);
							break;
						}
					}
				}
				else
				{
					return;
				}
			}
		}).start();
		
	}
	class displayMessage extends AsyncTask<String, Integer, String>
	{

		@Override
		protected String doInBackground(String... params) 
		{
			return params[0];
		}
		@Override
		protected void onPostExecute(String result) 
		{
			super.onPostExecute(result);
			String [] finalMessage=result.split(":");
			cv_pushToContentProvider = new ContentValues();
			cv_pushToContentProvider.put("key",globalClock);
			
			if(finalMessage.length > 2)
			{
				TextView tv = (TextView) findViewById(R.id.textView1);
				tv.append("\n"+finalMessage[2]);
				cv_pushToContentProvider.put("value", finalMessage[2]);
				Toast.makeText(getApplicationContext(), finalMessage[2],Toast.LENGTH_SHORT).show();
			}
			else
			{
				TextView tv = (TextView) findViewById(R.id.textView1);
				tv.append("\n"+result);
				cv_pushToContentProvider.put("value", result);
				Toast.makeText(getApplicationContext(), result,Toast.LENGTH_SHORT).show();
			}
			mContentResolver.insert(mUri, cv_pushToContentProvider);
		}		
	}
	public void sendMessage(View v)
	{
		String send_ClientMessage;
		EditText ed=(EditText)findViewById(R.id.editText1);
		send_ClientMessage=ed.getText().toString();
		if(send_ClientMessage == null || send_ClientMessage == "")
			return;
		String sender=null;
		if(emulator_instance == 5554)
		{
			long random_key = UUID.randomUUID().getLeastSignificantBits();
			sender="avd0"+":"+sequenceNumberLocalApp+":"+send_ClientMessage+","+random_key;
			sequenceNumberLocalApp++;
		}
		else if(emulator_instance == 5556)
		{
			long random_key = UUID.randomUUID().getLeastSignificantBits();
			sender="avd1"+":"+sequenceNumberLocalApp+":"+send_ClientMessage+","+random_key;
			sequenceNumberLocalApp++;
		}
		else if(emulator_instance == 5558)
		{
			long random_key = UUID.randomUUID().getLeastSignificantBits();
			sender="avd2"+":"+sequenceNumberLocalApp+":"+send_ClientMessage+","+random_key;
			sequenceNumberLocalApp++;
		}
		multiCastMessage(sender);
		ed.setText(null);
	}
	public void testOne(View v)
	{
		//System.out.println("In test One");
		new Thread(new Runnable()
		{
			public void run()
			{
				for(int i=0;i<5;i++)
				{
					//System.out.println("Sending Message");
					long random_key = UUID.randomUUID().getLeastSignificantBits();
					String s=null;
					
					if(emulator_instance == 5554)
					{
						s="avd0"+":"+sequenceNumberLocalApp+","+random_key;
						sequenceNumberLocalApp++;
					}
					else if(emulator_instance == 5556)
					{
						s="avd1"+":"+sequenceNumberLocalApp+","+random_key;
						sequenceNumberLocalApp++;
					}
					else if(emulator_instance == 5558)
					{
						s="avd2"+":"+sequenceNumberLocalApp+","+random_key;
						sequenceNumberLocalApp++;
					}
					multiCastMessage(s);
					try {
						Thread.sleep(3000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}).start();
	}
	public void testTwo(View v)
	{
		test2=true;

		String s=null;
		if(emulator_instance == 5554)
		{
			long random_key = UUID.randomUUID().getLeastSignificantBits();
			s="avd0"+":"+sequenceNumberLocalApp+","+random_key+","+"test2"+","+"pass";;
			sequenceNumberLocalApp++;
		}
		else if(emulator_instance == 5556)
		{
			long random_key = UUID.randomUUID().getLeastSignificantBits();
			s="avd1"+":"+sequenceNumberLocalApp+","+random_key+","+"test2"+","+"pass";;
			sequenceNumberLocalApp++;
		}
		else if(emulator_instance == 5558)
		{
			long random_key = UUID.randomUUID().getLeastSignificantBits();
			s="avd2"+":"+sequenceNumberLocalApp+","+random_key+","+"test2"+","+"pass";;
			sequenceNumberLocalApp++;
		}
		multiCastMessage(s);
	}
	private void testCaseMultiCastMessage()
	{
		for(int i=1;i<3;i++)
		{
			long random_key = UUID.randomUUID().getLeastSignificantBits();
			String s1=null;
			if(emulator_instance == 5554)
			{
				s1="avd0"+":"+sequenceNumberLocalApp+","+random_key;
				sequenceNumberLocalApp++;
			}
			else if(emulator_instance == 5556)
			{
				s1="avd1"+":"+sequenceNumberLocalApp+","+random_key;
				sequenceNumberLocalApp++;

			}
			else if(emulator_instance == 5558)
			{
				s1="avd2"+":"+sequenceNumberLocalApp+","+random_key;
				sequenceNumberLocalApp++;
			}
			
			multiCastMessage(s1);
		}
	}
	private void multiCastMessage(final String message) 
	{
	new Thread(new Runnable()
	{
		public void run()
		{
			Socket s=null;
			try {
				s=new Socket(localHost,11112);
				sendandUpdate(s,message);
				s.close();
				s=null;
				s=new Socket(localHost,11116);
				sendandUpdate(s,message);
				s.close();
				s=null;
				s=new Socket(localHost,11108);
				sendandUpdate(s,message);
				s.close();
			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}


	}).start();	
				
	}
	private void sendandUpdate(Socket s, String message) throws IOException 
	{
		PrintWriter pw = new PrintWriter(s.getOutputStream());
		pw.println(message);
		pw.close();
		s.close();
	}


}
