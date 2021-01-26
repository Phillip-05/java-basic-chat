package Server;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;

public class ThreadHandleClient implements Runnable {
    private Thread threadHandleClients;
    private final String threadName;
    private Server server; // The server socket
    private Socket socket; // the client socket
    private boolean running = true; // are we running or should we end?
    private int myClientId;

    public ThreadHandleClient(String threadName, Server server, Socket s, int myClientId) {
        this.threadName = threadName;
        this.server = server;
        this.socket = s;
        this.myClientId = myClientId;
    }

    @Override
    public void run() {
        System.out.println("Thread running " + threadName);

        getMessageFromClient(socket);

        System.out.println("Thread ended " + threadName);
    }

    private void getMessageFromClient(Socket s) {
        while(running) { // While we're running
            try {
                DataInputStream dIn = new DataInputStream(s.getInputStream()); // Create a new dataInputStream

                switch (dIn.readByte()) { // Check what type of message the client sends us
                    case 0: // the client is disconnecting
                        server.clientIsDisconnecting(myClientId); // get the ID of the client and disconnect him
                        running = false; // Stop this thread
                        break;

                    case 1: // Normal message
                        server.sendMessageFromClientToClients(myClientId, dIn.readUTF()); // read text from the client and send it to the server and to all the other clients
                        break;

                    case 2:
                        boolean isOp = dIn.readBoolean();
                        String command = dIn.readUTF();
                        int argAmount = dIn.readInt();
                        String[] args = new String[argAmount];
                        for (int i = 0; i < argAmount; i++) {
                            args[i] = dIn.readUTF();
                        }

                        if(!server.handleCommands(isOp, command, args)) {
                            server.sendMessage("Unknown command!", s, myClientId);
                        }
                        break;

                    default:
                        String message = dIn.readUTF();
                        if(message.equals("")) {
                            message = "null";
                        }
                        System.out.println("Got undefined Message from client in thread " + threadName + "! Message: " + message);
                        server.sendMessage("Undefined Message Type!",socket,myClientId);
                        break;
                }
            } catch (IOException e) {
                System.out.println("Can't get message from client in thread " + threadName);
            }
        }
    }

    public void start() {
        System.out.println("THREAD " + threadName + " STARTED");
        if (threadHandleClients == null) {
            threadHandleClients = new Thread(this, threadName);
            threadHandleClients.start();
        }
    }
}