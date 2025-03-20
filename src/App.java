import javax.swing.*;
import java.util.Scanner;


public class App {
    public static void main(String[] args) throws Exception {
        Scanner sc = new Scanner(System.in);
        System.out.println("Enter 'host' to host or 'join' to connect:");
        String choice = sc.nextLine();
        boolean isHost = choice.equalsIgnoreCase("host");

        String hostAddress = "";
        if (!isHost) {
            System.out.print("Enter host IP address: ");
            hostAddress = sc.nextLine();
        }

        JFrame frame = new JFrame("Multiplayer PacMan");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        PacMan gamePanel = new PacMan(isHost, hostAddress);
        frame.add(gamePanel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        gamePanel.requestFocus();
    }
}

