package com.bluemini.liveload;

import java.io.File;

import com.bluemini.websockets.server.Server;

public class LiveLoad {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		// get the location of the test CSS files
		File testCss = new File("./www");
		
		// We're going to use the LiveLoadHandler to monitor CSS at our test location
		Server s = new Server(new LiveLoadHandler(testCss.getAbsolutePath()));
		
		// we'll accept connections from all domains
		s.setHost("*");
		
		// start the WebSocket server
		new Thread(s, "WebSocketServer").start();
	}

}
