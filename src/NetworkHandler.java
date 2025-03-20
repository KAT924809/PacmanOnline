import java.io.*;
import java.net.*;

public class NetworkHandler {
    private ServerSocket serverSocket;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    public NetworkHandler(boolean isHost, String hostAddress) throws IOException {
        if (isHost) {
            serverSocket = new ServerSocket(9999);
            System.out.println("Waiting for connection...");
            socket = serverSocket.accept();
            System.out.println("Connected!");
        } else {
            socket = new Socket(hostAddress, 9999);
            System.out.println("Connected to host!");
        }
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);
    }

    public void sendMessage(String msg) {
        out.println(msg);
    }

    public String receiveMessage() throws IOException {
        return in.readLine();
    }
}
