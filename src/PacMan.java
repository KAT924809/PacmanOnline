import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import javax.swing.*;

public class PacMan extends JPanel implements ActionListener, KeyListener {

    // Inner Block class
    public class Block {
        public Image image;
        public int x, y;
        public int width, height;
        public int velocityX = 0, velocityY = 0;
        private final int speed = 4;

        public Block(Image image, int x, int y, int width, int height) {
            this.image = image;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        public void updateDirection(char direction) {
            if (direction == 'U') {
                velocityX = 0;
                velocityY = -speed;
            }
            if (direction == 'D') {
                velocityX = 0;
                velocityY = speed;
            }
            if (direction == 'L') {
                velocityX = -speed;
                velocityY = 0;
            }
            if (direction == 'R') {
                velocityX = speed;
                velocityY = 0;
            }
        }
    }
    private final int rowCount = 16;
    private final int columnCount = 19;
    private final int tileSize = 32;
    private final int boardWidth = columnCount * tileSize;
    private final int boardHeight = rowCount * tileSize;

    private Image pacmanRight, pacmanLeft, pacmanUp, pacmanDown;
    private Image pacman2Right, pacman2Left, pacman2Up, pacman2Down;
    private Block pacman;
    private Block player2;
    private NetworkHandler network;
    private Timer timer;

    private final String[] map = {
            "XXXXXXXXXXXXXXXXXXX",
            "X        X        X",
            "X XX XXX X XXX XX X",
            "X                 X",
            "X XX X XXXXX X XX X",
            "X    X       X    X",
            "XXXX XXXX XXXX XXXX",
            "X                 X",
            "X        P        X",
            "X                 X",
            "X        Q        X",
            "X                 X",
            "X XX XXX X XXX XX X",
            "X    X       X    X",
            "XXXXXXXXXXXXXXXXXXX"
    };

    public PacMan(boolean isHost, String hostAddress) throws IOException {
        setPreferredSize(new Dimension(boardWidth, boardHeight));
        setBackground(Color.BLACK);
        addKeyListener(this);
        setFocusable(true);

        // Load player images
        pacmanRight = new ImageIcon(getClass().getResource("./pacmanRight.png")).getImage();
        pacmanLeft = new ImageIcon(getClass().getResource("./pacmanLeft.png")).getImage();
        pacmanUp = new ImageIcon(getClass().getResource("./pacmanUp.png")).getImage();
        pacmanDown = new ImageIcon(getClass().getResource("./pacmanDown.png")).getImage();

        pacman2Right = new ImageIcon(getClass().getResource("./pacmanRight.png")).getImage();
        pacman2Left = new ImageIcon(getClass().getResource("./pacmanLeft.png")).getImage();
        pacman2Up = new ImageIcon(getClass().getResource("./pacmanUp.png")).getImage();
        pacman2Down = new ImageIcon(getClass().getResource("./pacmanDown.png")).getImage();

        // Initialize player positions from map
        for (int r = 0; r < map.length; r++) {
            for (int c = 0; c < map[r].length(); c++) {
                char ch = map[r].charAt(c);
                if (ch == 'P') {
                    pacman = new Block(pacmanRight, c * tileSize, r * tileSize, tileSize, tileSize);
                } else if (ch == 'Q') {
                    player2 = new Block(pacman2Right, c * tileSize, r * tileSize, tileSize, tileSize);
                }
            }
        }

        // Start networking
        network = new NetworkHandler(isHost, hostAddress);

        // Start game loop timer
        timer = new Timer(40, this);
        timer.start();

        // Start listening for player2 movement
        new Thread(() -> {
            while (true) {
                try {
                    String msg = network.receiveMessage();
                    if (msg != null && msg.startsWith("DIR:")) {
                        char dir = msg.charAt(4);
                        player2.updateDirection(dir);
                        updatePlayer2Image(dir);
                    }
                } catch (IOException e) {
                    break;
                }
            }
        }).start();
    }

    private void updatePlayer2Image(char direction) {
        switch (direction) {
            case 'U': player2.image = pacman2Up; break;
            case 'D': player2.image = pacman2Down; break;
            case 'L': player2.image = pacman2Left; break;
            case 'R': player2.image = pacman2Right; break;
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        pacman.x += pacman.velocityX;
        pacman.y += pacman.velocityY;
        player2.x += player2.velocityX;
        player2.y += player2.velocityY;
        repaint();
    }

    @Override

    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Draw the maze from the map array
        g.setColor(Color.BLUE);
        for (int r = 0; r < map.length; r++) {
            for (int c = 0; c < map[r].length(); c++) {
                if (map[r].charAt(c) == 'X') {
                    g.fillRect(c * tileSize, r * tileSize, tileSize, tileSize);
                }
            }
        }

        // Draw players on top
        g.drawImage(pacman.image, pacman.x, pacman.y, pacman.width, pacman.height, null);
        g.drawImage(player2.image, player2.x, player2.y, player2.width, player2.height, null);
    }

    @Override
    public void keyTyped(KeyEvent e) {}
    @Override
    public void keyPressed(KeyEvent e) {}
    @Override
    public void keyReleased(KeyEvent e) {
        char direction = ' ';
        switch (e.getKeyCode()) {
            case KeyEvent.VK_UP: direction = 'U'; pacman.image = pacmanUp; break;
            case KeyEvent.VK_DOWN: direction = 'D'; pacman.image = pacmanDown; break;
            case KeyEvent.VK_LEFT: direction = 'L'; pacman.image = pacmanLeft; break;
            case KeyEvent.VK_RIGHT: direction = 'R'; pacman.image = pacmanRight; break;
        }

        if (direction != ' ') {
            pacman.updateDirection(direction);
            network.sendMessage("DIR:" + direction);
        }
    }
}

