import java.io.*;
import java.net.*;

public class NetworkHandler {
    private ServerSocket serverSocket;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private final int PORT = 9999;
    private final int TIMEOUT = 100; // shorter timeout for smoother gameplay

    public NetworkHandler(boolean isHost, String hostAddress) throws IOException {
        if (isHost) {
            serverSocket = new ServerSocket(PORT);
            System.out.println("[Host] Waiting for client connection...");
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
                System.out.println("[Host] Connection attempt failed, retrying...");
            }
        }
    }

    private void connectAsClient(String host) {
        while (true) {
            try {
                socket = new Socket(host, PORT);
                System.out.println("[Client] Connected to host.");
                setupStreams();
                break;
            } catch (IOException e) {
                System.out.println("[Client] Could not connect to host, retrying in 2s...");
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ignored) {}
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

    public String receiveMessage() {
        try {
            return in.readLine();
        } catch (SocketTimeoutException e) {
            return null; // No data this frame
        } catch (IOException e) {
            System.out.println("[Network] Connection error: " + e.getMessage());
            return null;
        }
    }

    public void close() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) socket.close();
            if (serverSocket != null && !serverSocket.isClosed()) serverSocket.close();
        } catch (IOException e) {
            System.out.println("[Network] Error while closing: " + e.getMessage());
        }
    }
}
