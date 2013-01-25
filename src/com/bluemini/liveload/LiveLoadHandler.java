/**
 * Copyright (c) 2013, Nick Harvey
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 * - Neither the name of the <ORGANIZATION> nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *   
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 *   AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 *   IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 *   ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 *   LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 *   CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 *   SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 *   INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 *   CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 *   ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE.
 */
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
						message.append("location", f.getAbsolutePath());
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
