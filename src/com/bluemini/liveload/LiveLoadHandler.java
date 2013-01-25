package com.bluemini.liveload;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONException;
import org.json.JSONObject;

import com.bluemini.websockets.server.SWSSHandler;
import com.bluemini.websockets.server.WSRequest;
import com.bluemini.websockets.server.WSResponse;

public class LiveLoadHandler extends SWSSHandler {
	
	Map<File, Long> watching = new HashMap<File, Long>();
	WSRequest connection = null;
	
	public LiveLoadHandler(String path)
	{
		File f = new File(path);
		if (!f.exists() || !f.isDirectory()) {
			System.out.println("Unknown location or not a directory. " + path);
			System.exit(1);
		}
		
		File[] files = f.listFiles();
		for (int fi=0; fi<files.length; fi++)
		{
			File test = files[fi];
			String name = test.getName();
			String ext = getExtension(name);
			if (ext.equalsIgnoreCase("css"))
			{
				System.out.println("Found a CSS file");
				watching.put(test, new Long(test.lastModified()));
			}
			else
			{
				System.out.println(name + "(" + ext + ") is not a CSS file");
			}
		}
		
		// create a timer to watch the directory every 5 seconds
		Timer t = new Timer("Watcher");
		t.schedule(new FileCheck(this), 2000, 5000);
		System.out.println("Stating routine checks");

	}
	
	public void doCheck()
	{
		File f;
		Set<File> keys = watching.keySet();
		Iterator<File> e = keys.iterator();
		while (e.hasNext())
		{
			f = e.next();
			Long last = f.lastModified();
			if (last > watching.get(f))
			{
				System.out.println("Send a message to the page that the CSS has been updated!");
				watching.put(f, last);
				try
				{
					if (connection != null)
					{
						JSONObject message = new JSONObject();
						message.append("location", f.getName());
						sendResponse(connection, new WSResponse(WSRequest.OPCODE_TEXT_FRAME, message.toString()));
					}
				}
				catch (UnsupportedEncodingException uee)
				{
					System.out.println("Unable to send message to page. " + uee.getMessage());
				}
				catch (JSONException je)
				{
					System.out.println("ERROR creating the JSON response. " + je.getMessage());
				}
			}
		}
	}

	private String getExtension(String filename)
	{
		int dotLoc = filename.lastIndexOf("."); 
		if (dotLoc > -1 && dotLoc < filename.length())
		{
			return filename.substring(dotLoc+1);
		}
		return "";
	}

	@Override
	public void upgrade(WSRequest request) {
		this.connection = request;
	}

	@Override
	public void response(WSRequest request) {
		try
		{
			sendResponse(request, new WSResponse(WSRequest.OPCODE_TEXT_FRAME, "Hi there!"));
		}
		catch (UnsupportedEncodingException uee)
		{
			System.out.println(uee.getMessage());
		}
	}
	
}

class FileCheck extends TimerTask
{
	
	final LiveLoadHandler callback;
	
	public FileCheck(LiveLoadHandler handler)
	{
		this.callback = handler;
	}
	
	@Override
	public void run()
	{
		callback.doCheck();
	}
}
