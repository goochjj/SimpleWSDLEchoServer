package org.goochfriend;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.StringWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.ArrayList;

public class SimpleHTTPServer {
	private int port = 48000;
	private Runner runner = null;

	public SimpleHTTPServer(int port) {
		this.port = port;
	}

	public void start() throws IOException {
		ServerSocket srvSock = new ServerSocket(port,1);
		Runner run = new Runner(srvSock);
		runner = run;
		run.start();
	}
	public int getNumRequests() { return runner.getNumRequests(); }
	public RequestData getRequest(int idx) { return runner.getRequest(idx); }
	public RequestData getLastRequest() { return runner.getLastRequest(); }
	public void stop() {
		runner.alive = false;
		try { if (runner.srvSock!=null) runner.srvSock.close(); } catch (IOException ex) {}
		//try { runner.notify(); } catch (IllegalMonitorStateException ex) {}
		//runner.interrupt();
	}
	public void join() throws InterruptedException {
		runner.join();
	}

	private static class Runner extends Thread {
		private ServerSocket srvSock;
		private List<RequestData> requestData = new ArrayList<RequestData>();
		private volatile boolean alive = true;

		public Runner(ServerSocket sock) { this.srvSock = sock; }
		public int getNumRequests() { synchronized(requestData) { return requestData.size(); } }
		public RequestData getRequest(int idx) { synchronized(requestData) { return requestData.get(idx); } }
		public RequestData getLastRequest() { synchronized(requestData) { return requestData.get(requestData.size()-1); } }
		public void run() {
			while(alive) {
				List<String> headers = new ArrayList<String>();
				RequestData req = new RequestData();
				req.headerLines = headers;
				req.body = "";
				Socket sock = null;
				BufferedReader br = null;
				PrintStream os = null;
				try {
					sock = srvSock.accept();
					os = new PrintStream(sock.getOutputStream());
					br = new BufferedReader(new InputStreamReader(sock.getInputStream()));
					String inLine = br.readLine();
					req.command = inLine;
					boolean isPost = inLine.startsWith("POST");
					String contentTypeHdr="";
					int contentLength = 0;
					while (((inLine = br.readLine()) != null) && (!(inLine.equals("")))) {
						headers.add(inLine);
						//System.out.println(inLine);
						final String contentHeader = "Content-Length: ";
						if (isPost && inLine.startsWith(contentHeader)) {
							contentLength = Integer.parseInt(inLine.substring(contentHeader.length()));
						}
						if (isPost && inLine.startsWith("Content-Type:")) {
							contentTypeHdr = inLine;
						}
					}
					//System.out.println("");
					// This will probably choke on non-text content types
					if (isPost) {
						int c = 0;
						StringWriter ws = new StringWriter();
						for (int i = 0; i < contentLength; i++) {
							c = br.read();
							ws.append((char) c);
						}
						req.body = ws.getBuffer().toString();
					}
					//try { br.close(); br = null; } catch (IOException e) { e.printStackTrace(); }
					os.print("HTTP/1.0 200 OK\r\n");
					if (isPost && req.body.contains("http://schemas.xmlsoap.org/soap/envelope")) {
						StringWriter newBody = new StringWriter();
						newBody.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
						/*	<return>&lt;S:Envelope xmlns:S="http://schemas.xmlsoap.org/soap/envelope/" xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/"&gt;&lt;SOAP-ENV:Header/&gt;&lt;S:Body&gt;&lt;ns2:EchoRequest xmlns:ns2="http://jaxws.goochfriend.org/endpoint/"&gt;&lt;EchoHeader&gt;&lt;applicationId&gt;Hello World&lt;/applicationId&gt;&lt;/EchoHeader&gt;&lt;/ns2:EchoRequest&gt;&lt;/S:Body&gt;&lt;/S:Envelope&gt;</return></ns2:EchoRequestResponse></S:Body></S:Envelope>

						*/
						newBody.write("<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><soapenv:Header/><soapenv:Body>");
						newBody.write("<EchoRequestResponse xmlns=\"http://jaxws.goochfriend.org/endpoint/\"><return>");
						newBody.write("<![CDATA[");
						newBody.write(req.body);
						newBody.write("]]>");
						newBody.write("</return></EchoRequestResponse></soapenv:Body></soapenv:Envelope>");
						String newBodyStr = newBody.getBuffer().toString();
						os.print("Content-Type: text/xml\r\n"); 
						os.print("Content-Length: "); os.print(newBodyStr.length()); os.print("\r\n");
						os.print("\r\n");
						os.print(newBodyStr);
					} else if (isPost) {
						os.print(contentTypeHdr);
						os.print("Content-Length: "); os.print(contentLength); os.print("\r\n");
						os.print("\r\n");
						os.print(req.body);
					} else {
						os.print("Content-Type: text/plain\r\n\r\nHello World!\r\n");
					}
					os.flush();
				} catch(Exception ex) {
					req.error = ex;
				} finally {
					if (alive) {
						synchronized(requestData) {
							requestData.add(req);
						}
					}
					//try { if (br!=null) br.close(); br=null; } catch (IOException e) { e.printStackTrace(); }
					if (os!=null) { os.close(); os=null; }
					try { if (sock!=null) sock.close(); sock=null; } catch (IOException e) { e.printStackTrace(); }
				}
			}
			try {
				System.out.println("Server socket closing");
				srvSock.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public static class RequestData {
		private String command;
		private List<String> headerLines;
		private String body;
		private Throwable error;

		public String getCommand() { return command; }
		public List<String> getHeaderLines() { return headerLines; }
		public Throwable getError() { return error; }
		public String getBody() { return body; }
	}
}
