import javax.swing.*;
import java.util.Scanner;

public class App {
    public static void main(String[] args) throws Exception {
        Scanner sc = new Scanner(System.in);
        System.out.println("Enter 'host' to host, or 'join' to connect:");
        String choice = sc.nextLine().trim().toLowerCase();

        boolean isHost = choice.equals("host");
        String ip = "localhost";

        if (!isHost) {
            System.out.print("Enter host IP address (e.g., 127.0.0.1): ");
            ip = sc.nextLine().trim();
        }

        int rows = 21, cols = 19, tileSize = 32;
        int boardWidth = cols * tileSize;
        int boardHeight = rows * tileSize;

        JFrame frame = new JFrame("Multiplayer Pac-Man (Distributed Systems)");
        frame.setSize(boardWidth, boardHeight);
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        PacMan game = new PacMan(isHost, ip);
        frame.add(game);
        frame.pack();
        game.requestFocusInWindow();
        frame.setVisible(true);
    }
}
