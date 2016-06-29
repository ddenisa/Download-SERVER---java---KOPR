package socket;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;


public class Server {
	// a unique ID for each connection
	private static int uniqueId;
	// an ArrayList to keep the list of the Client
	private ArrayList<ClientThread> al;
	// to display time
	private SimpleDateFormat sdf;
	// the port number to listen for connection
	private int port;
	// the boolean that will be turned of to stop the server
	private boolean keepGoing;
	

	
	public Server(int port) {
		
		this.port = port;
		
		sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
		
		al = new ArrayList<ClientThread>();
	}

	public void start() {
		keepGoing = true;
		/* create server socket and wait for connection requests */
		try 
		{
			
			ServerSocket serverSocket = new ServerSocket(port);

			
			while(keepGoing) 
			{
				
				display("Server waiting for Clients on IP "+serverSocket.getInetAddress()+", port " + serverSocket.getLocalPort() + ".");
				
				Socket socket = serverSocket.accept();  
				
				// if I was asked to stop
				if(!keepGoing)
					break;
				
				ClientThread t = new ClientThread(socket);  
				al.add(t);									
				t.start();
			}
			// I was asked to stop
			try {
				serverSocket.close();
				for(int i = 0; i < al.size(); ++i) {
					ClientThread tc = al.get(i);
					try {
					tc.sInput.close();
					tc.socket.close();
					}
					catch(IOException ioE) {
						
					}
				}
			}
			catch(Exception e) {
				display("Exception closing the server and clients: " + e);
			}
		}
		
		catch (IOException e) {
            String msg = sdf.format(new Date()) + " Exception on new ServerSocket: " + e + "\n";
			display(msg);
		}
	}		
    /*
     * For the GUI to stop the server
     */
	@SuppressWarnings("resource")
	protected void stop() {
		keepGoing = false;
		// connect to myself as Client to exit statement 
		// Socket socket = serverSocket.accept();
		try {
			new Socket("localhost", port);
		}
		catch(Exception e) {
			
		}
	}
	
	
	private void display(String msg) {
		String log = sdf.format(new Date()) + " " + msg;
		System.out.println(log);
	}
	
	// for a client who logs off using the LOGOUT message
	synchronized void remove(int id) {		
		for(int i = 0; i < al.size(); ++i) {
			ClientThread ct = al.get(i);
			if(ct.id == id) {
				al.remove(i);
				return;
			}
		}
	}
	

	/** One instance of this thread will run for each client */
	class ClientThread extends Thread {
		int loginId = -1;
		
		// the socket where to listen/talk
		Socket socket;
		ObjectInputStream sInput;
		// my unique id (easier for disconnection)
		int id;
		// the User name of the Client
		String username;
		// the date I connect
		String date;

		// Constructor
		ClientThread(Socket socket) {
			id = ++uniqueId;
			this.socket = socket;
			/* Creating both Data Stream */
			System.out.println("Thread trying to create Object Input/Output Streams");
			username = socket.getInetAddress().toString();
			try
			{
				sInput  = new ObjectInputStream(socket.getInputStream());
			}
			catch (IOException e) {
				display("Exception creating new Input/output Streams: " + e);
				return;
			}

            date = new Date().toString() + "\n";
            display(username + " just connected.");
		}

		
		public void run() {
			boolean keepGoing = true;
			while(keepGoing) {
				// read a String (which is an object)
				Object o = null;
				try {
					o = sInput.readObject();
					
					if (o instanceof DownloadRequestMessage) {
						DownloadRequestMessage m = (DownloadRequestMessage)o;
						sendFilePartition(m);
						keepGoing = false;
					}
				}
				catch (EOFException e) {break;}
				catch (IOException e) {
					display(username + " Exception reading Streams: " + e);
					break;				
				}
				catch(ClassNotFoundException e) {
					display(username + " Exception reading Streams: " + e);
					break;
				}
				display(username + ": " + o);
				
			}
			remove(id);
			close();
		}
		
		 public void sendFilePartition(DownloadRequestMessage m) {
			 BufferedInputStream bis = null;
		        try {
		            //handle file read
		            File file = new File(m.fileName);
		            long offset = file.length()/m.partitionsCount*m.partitionIndex + m.offset;
		            long bytesToSend;
		            if (m.partitionIndex == m.partitionsCount-1)
		            	bytesToSend = file.length() - offset;
		            else
		            	bytesToSend = file.length()/m.partitionsCount - m.offset;
		            
		            byte[] buffer = new byte[Globals.batchSize];

		            FileInputStream fis = new FileInputStream(file);
		            bis = new BufferedInputStream(fis);
		            bis.skip(offset);

		            //handle file send over socket
		            OutputStream os = socket.getOutputStream();

		            //Sending file name and file size to the server
		            DataOutputStream dos = new DataOutputStream(os);
		            dos.writeLong(bytesToSend);
		            while (bytesToSend > 0) {
		            	long bytesRead = bis.read(buffer, 0, (int)Math.min(bytesToSend, buffer.length));
		            	dos.write(buffer, 0, buffer.length);
		            	bytesToSend -= bytesRead;
		            }
		            dos.close();
		            System.out.println("Request " + m + " successfully responded.");
		        } catch (Exception e) {
		            System.err.println("File does not exist!");
		        } 
		        finally {
		        	if (bis != null)
						try {
							bis.close();
						} catch (IOException e) {}
		        }
		}
		 
		
		
		private void close() {
			display("disconnecting "+username);
			try {
				if(sInput != null) sInput.close();
			}
			catch(Exception e) {};
			try {
				if(socket != null) socket.close();
			}
			catch (Exception e) {}
		}
	}
}