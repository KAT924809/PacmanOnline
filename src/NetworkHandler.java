import java.io.*;
import java.net.*;

public class NetworkHandler {
    private ServerSocket serverSocket;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private final int TIMEOUT = 1000;

    public NetworkHandler(boolean isHost, String hostAddress) throws IOException {
        if (isHost) {
            serverSocket = new ServerSocket(9999);
            System.out.println("[Host] Waiting for connection...");
            connectAsHost();
        } else {
            connectAsClient(hostAddress);
        }
    }

    private void connectAsHost() throws IOException {
        while (true) {
            try {
                socket = serverSocket.accept();
                System.out.println("[Host] Client connected.");
                setupStreams();
                break;
            } catch (IOException e) {
                System.out.println("[Host] Connection failed, retrying...");
            }
        }
    }

    private void connectAsClient(String host) {
        while (true) {
            try {
                socket = new Socket(host, 9999);
                System.out.println("[Client] Connected to host.");
                setupStreams();
                break;
            } catch (IOException e) {
                System.out.println("[Client] Could not connect, retrying...");
                try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
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
            System.out.println("[Network] Connection lost.");
            return null;
        }
    }
}
