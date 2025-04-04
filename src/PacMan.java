import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.HashSet;
import javax.swing.*;

public class PacMan extends JPanel implements ActionListener, KeyListener {

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
            if (direction == 'U') { velocityX = 0; velocityY = -speed; }
            if (direction == 'D') { velocityX = 0; velocityY = speed; }
            if (direction == 'L') { velocityX = -speed; velocityY = 0; }
            if (direction == 'R') { velocityX = speed; velocityY = 0; }
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
    private HashSet<Block> walls = new HashSet<>();
    private final Object lock = new Object();
    private final HashSet<String> occupiedTiles = new HashSet<>();

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

        loadImages();
        initializeMap();

        // Initialize shared tile state
        occupiedTiles.add(pacman.x + "," + pacman.y);
        occupiedTiles.add(player2.x + "," + player2.y);

        network = new NetworkHandler(isHost, hostAddress);
        timer = new Timer(40, this);
        timer.start();

        startListeningForPlayer2();
    }

    private void loadImages() {
        pacmanRight = new ImageIcon(getClass().getResource("./pacmanRight.png")).getImage();
        pacmanLeft = new ImageIcon(getClass().getResource("./pacmanLeft.png")).getImage();
        pacmanUp = new ImageIcon(getClass().getResource("./pacmanUp.png")).getImage();
        pacmanDown = new ImageIcon(getClass().getResource("./pacmanDown.png")).getImage();

        pacman2Right = pacmanRight;
        pacman2Left = pacmanLeft;
        pacman2Up = pacmanUp;
        pacman2Down = pacmanDown;
    }

    private void initializeMap() {
        for (int r = 0; r < map.length; r++) {
            for (int c = 0; c < map[r].length(); c++) {
                char ch = map[r].charAt(c);
                int x = c * tileSize;
                int y = r * tileSize;

                if (ch == 'X') {
                    walls.add(new Block(null, x, y, tileSize, tileSize));
                } else if (ch == 'P') {
                    pacman = new Block(pacmanRight, x, y, tileSize, tileSize);
                } else if (ch == 'Q') {
                    player2 = new Block(pacman2Right, x, y, tileSize, tileSize);
                }
            }
        }
    }

    private void startListeningForPlayer2() {
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
        movePlayer(pacman);
        movePlayer(player2);
        repaint();
    }

    private void movePlayer(Block player) {
        int nextX = player.x + player.velocityX;
        int nextY = player.y + player.velocityY;
        String nextTile = nextX + "," + nextY;
        String currentTile = player.x + "," + player.y;

        synchronized (lock) {
            if (isWallCollision(nextX, nextY)) return;
            if (occupiedTiles.contains(nextTile)) return;

            // Update tile occupancy
            occupiedTiles.remove(currentTile);
            player.x = nextX;
            player.y = nextY;
            occupiedTiles.add(nextTile);
        }
    }

    private boolean isWallCollision(int x, int y) {
        for (Block wall : walls) {
            if (wall.x == x && wall.y == y) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        g.setColor(Color.BLUE);
        for (Block wall : walls) {
            g.fillRect(wall.x, wall.y, tileSize, tileSize);
        }

        g.drawImage(pacman.image, pacman.x, pacman.y, pacman.width, pacman.height, null);
        g.drawImage(player2.image, player2.x, player2.y, player2.width, player2.height, null);
    }

    @Override public void keyTyped(KeyEvent e) {}
    @Override public void keyPressed(KeyEvent e) {}

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

