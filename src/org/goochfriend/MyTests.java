package org.goochfriend;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;

import org.junit.Test;

import static org.junit.Assert.*;

public class MyTests {
	@Test
	public void runTest() throws Exception {
		SimpleHTTPServer srv = new SimpleHTTPServer(48200);
		srv.start();
		URL u = new URL("http://localhost:48200/test.html");
		{
			BufferedReader br = new BufferedReader(new InputStreamReader(u.openStream()));
			String inLine = null;
		    while (((inLine = br.readLine()) != null) && (!(inLine.equals("")))) {
		    	System.out.println(inLine);
		    }
		}
	    System.out.println("Done with GET");
	    System.out.println();
	    
	    URLConnection connection = u.openConnection();
	    connection.setDoOutput(true); // Triggers POST.
	    connection.setRequestProperty("Accept-Charset", "UTF-8");
	    connection.setRequestProperty("Content-Type", "plain/xml;charset=UTF-8");
	    try (OutputStream output = connection.getOutputStream()) {
	        output.write("TestRequest".getBytes("UTF-8"));
	    }

		{
			BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			String inLine = null;
		    while (((inLine = br.readLine()) != null) && (!(inLine.equals("")))) {
		    	System.out.println(inLine);
		    }
		}
	    System.out.println("Done with POST");
	    System.out.println();

	    
	    srv.stop();
	    assertEquals(2,srv.getNumRequests());
	    assertNotNull(srv.getLastRequest());
	    assertNotNull(srv.getRequest(0));
	    assertNotNull(srv.getRequest(1));
	    assertEquals("GET /test.html HTTP/1.1",srv.getRequest(0).getCommand());
	    assertEquals("POST /test.html HTTP/1.1",srv.getRequest(1).getCommand());
	    assertEquals("", srv.getRequest(0).getBody());
	    assertEquals("TestRequest", srv.getRequest(1).getBody());
	    // We don't want this to fail
	    srv.join();
	    {
	    	ServerSocket srvSock=new ServerSocket(48200,1);
	    	assertNotNull(srvSock);
	    	srvSock.close();
	    }
	}
	
}
