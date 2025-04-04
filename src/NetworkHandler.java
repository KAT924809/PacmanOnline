import java.io.*;
import java.net.*;

public class NetworkHandler {
    private ServerSocket serverSocket;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private final int TIMEOUT = 1000; // 1-second timeout for deadlock version.

    public NetworkHandler(boolean isHost, String hostAddress) throws IOException {
        if (isHost) {
            serverSocket = new ServerSocket(9999);
            System.out.println("Waiting for connection...");
            connectHost();
        } else {
            connectClient(hostAddress);
        }
    }

    private void connectHost() throws IOException {
        while (true) {
            try {
                socket = serverSocket.accept();
                System.out.println("Connected!");
                setupStreams();
                break;
            } catch (IOException e) {
                System.out.println("Connection lost, waiting for reconnection...");
            }
        }
    }

    private void connectClient(String hostAddress) {
        while (true) {
            try {
                socket = new Socket(hostAddress, 9999);
                System.out.println("Connected to host!");
                setupStreams();
                break;
            } catch (IOException e) {
                System.out.println("Host not available, retrying...");
                try { Thread.sleep(2000); } catch (InterruptedException ignored) {} // Retry every 2 seconds
            }
        }
    }

    private void setupStreams() throws IOException {
        socket.setSoTimeout(TIMEOUT);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);
    }

    public void sendMessage(String msg) {
        if (socket != null && !socket.isClosed()) {
            out.println(msg);
        }
    }

    public String receiveMessage() throws IOException {
        try {
            return in.readLine();
        } catch (SocketTimeoutException e) {
            return null;
        } catch (IOException e) {
            System.out.println("Connection lost, waiting for reconnection...");
            return null;
        }
    }
}
