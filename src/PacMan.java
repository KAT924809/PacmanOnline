import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.Timer;

public class PacMan extends JPanel implements ActionListener, KeyListener {

    class Block {
        public Image image;
        public int x, y, width, height;
        public int velocityX = 0, velocityY = 0;
        final int speed = 4;
        int startX, startY;

        Block(Image image, int x, int y, int w, int h) {
            this.image = image;
            this.x = x;
            this.y = y;
            this.width = w;
            this.height = h;
            this.startX = x;
            this.startY = y;
        }

        void updateDirection(char d) {
            switch (d) {
                case 'U' -> { velocityX = 0; velocityY = -speed; }
                case 'D' -> { velocityX = 0; velocityY = speed; }
                case 'L' -> { velocityX = -speed; velocityY = 0; }
                case 'R' -> { velocityX = speed; velocityY = 0; }
            }
        }

        Rectangle getBounds() {
            return new Rectangle(x, y, width, height);
        }

        void reset() {
            x = startX;
            y = startY;
            velocityX = velocityY = 0;
        }
    }

    private final int tileSize = 32, rows = 21, cols = 19;
    private final int boardWidth = cols * tileSize, boardHeight = rows * tileSize;

    private Image wallImg, foodImg;
    private Image pacR, pacL, pacU, pacD;
    private Image ghostB, ghostR, ghostP, ghostO;

    private Block pacman, player2;
    private final Set<Block> walls = new HashSet<>();
    private final Set<Block> foods = new HashSet<>();
    private final Set<Block> ghosts = new HashSet<>();

    private final Object lock = new Object();
    private final Set<String> occupiedTiles = new HashSet<>();
    private Timer timer;
    private NetworkHandler network;

    private boolean hasToken = false;
    private boolean requestedToken = false;
    private boolean isHost;
    private final Queue<String> requestQueue = new LinkedList<>();

    private boolean isMounted = false;
    private final String mountPath = "./nfs/mounted_data/";

    private int score = 0;

    private final String[] map = {
            "XXXXXXXXXXXXXXXXXXX",
            "X        X        X",
            "X XX XXX X XXX XX X",
            "X                 X",
            "X XX X XXXXX X XX X",
            "X    X       X    X",
            "XXXX XXXX XXXX XXXX",
            "OOOX X       X XOOO",
            "XXXX X XXrXX X XXXX",
            "O       bpo       O",
            "XXXX X XXXXX X XXXX",
            "OOOX X       X XOOO",
            "XXXX X XXXXX X XXXX",
            "X        X        X",
            "X XX XXX X XXX XX X",
            "X  X     P     X  X",
            "XX X X XXXXX X X XX",
            "X    X   X   X    X",
            "X XXXXXX X XXXXXX X",
            "X                 X",
            "XXXXXXXXXXXXXXXXXXX"
    };


    public PacMan(boolean isHost, String hostAddress) throws IOException {
        this.isHost = isHost;
        this.hasToken = isHost;

        setPreferredSize(new Dimension(boardWidth, boardHeight));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);

        loadImages();
        loadMap();

        network = new NetworkHandler(isHost, hostAddress);
        timer = new Timer(40, this);
        timer.start();

        startListeningThread();
        startTokenManagerThread();
        startGhostTaskThread();
    }

    private void loadImages() {
        wallImg = new ImageIcon(getClass().getResource("./powerFood.png")).getImage();
        foodImg = new ImageIcon(getClass().getResource("./powerFood.png")).getImage();

        pacR = new ImageIcon(getClass().getResource("./pacmanRight.png")).getImage();
        pacL = new ImageIcon(getClass().getResource("./pacmanLeft.png")).getImage();
        pacU = new ImageIcon(getClass().getResource("./pacmanUp.png")).getImage();
        pacD = new ImageIcon(getClass().getResource("./pacmanDown.png")).getImage();

        ghostB = new ImageIcon(getClass().getResource("./blueGhost.png")).getImage();
        ghostR = new ImageIcon(getClass().getResource("./redGhost.png")).getImage();
        ghostP = new ImageIcon(getClass().getResource("./pinkGhost.png")).getImage();
        ghostO = new ImageIcon(getClass().getResource("./orangeGhost.png")).getImage();
    }

    private void loadMap() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                char ch = map[r].charAt(c);
                int x = c * tileSize;
                int y = r * tileSize;

                switch (ch) {
                    case 'X' -> walls.add(new Block(wallImg, x, y, tileSize, tileSize));
                    case 'P' -> pacman = new Block(pacR, x, y, tileSize, tileSize);
                    case 'Q' -> player2 = new Block(pacR, x, y, tileSize, tileSize);
                    case 'b' -> ghosts.add(new Block(ghostB, x, y, tileSize, tileSize));
                    case 'r' -> ghosts.add(new Block(ghostR, x, y, tileSize, tileSize));
                    case 'p' -> ghosts.add(new Block(ghostP, x, y, tileSize, tileSize));
                    case 'o' -> ghosts.add(new Block(ghostO, x, y, tileSize, tileSize));
                    case ' ' -> foods.add(new Block(null, x + 14, y + 14, 4, 4));
                }
            }
        }

        // Track occupied tiles
        occupiedTiles.add(pacman.x + "," + pacman.y);
        occupiedTiles.add(player2.x + "," + player2.y);
    }
    private void startListeningThread() {
        new Thread(() -> {
            while (true) {
                try {
                    String msg = network.receiveMessage();
                    if (msg == null) continue;

                    if (msg.startsWith("DIR:")) {
                        char dir = msg.charAt(4);
                        player2.updateDirection(dir);
                    } else if (msg.equals("REQUEST_TOKEN")) {
                        if (hasToken) {
                            network.sendMessage("TOKEN");
                            hasToken = false;
                        } else {
                            requestQueue.add("client");
                        }
                    } else if (msg.equals("TOKEN")) {
                        hasToken = true;
                        requestedToken = false;
                    }
                } catch (IOException e) {
                    break;
                }
            }
        }).start();
    }

    private void startTokenManagerThread() {
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(1000);
                    if (isHost && !requestQueue.isEmpty()) {
                        String req = requestQueue.poll();
                        if (req.equals("client")) {
                            network.sendMessage("TOKEN");
                            hasToken = false;
                        }
                    }
                } catch (InterruptedException ignored) {}
            }
        }).start();
    }

    private void startGhostTaskThread() {
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(4000);
                    System.out.println("[Ghost Thread] Ghost logic running...");
                } catch (InterruptedException ignored) {}
            }
        }).start();
    }

    private boolean isWallCollision(int x, int y) {
        Rectangle next = new Rectangle(x, y, tileSize, tileSize);
        for (Block wall : walls) {
            if (next.intersects(wall.getBounds())) return true;
        }
        return false;
    }

    private void move(Block player, boolean isPacman) {
        int nextX = player.x + player.velocityX;
        int nextY = player.y + player.velocityY;
        Rectangle nextBounds = new Rectangle(nextX, nextY, player.width, player.height);

        synchronized (lock) {
            if (isWallCollision(nextX, nextY)) return;

            String pos = nextX + "," + nextY;
            if (occupiedTiles.contains(pos)) return;

            String cur = player.x + "," + player.y;
            occupiedTiles.remove(cur);
            player.x = nextX;
            player.y = nextY;
            occupiedTiles.add(pos);

            if (isPacman) {
                Block eaten = null;
                for (Block food : foods) {
                    if (player.getBounds().intersects(food.getBounds())) {
                        eaten = food;
                        score += 10;
                        break;
                    }
                }
                if (eaten != null) foods.remove(eaten);
            }
        }
    }

    private void requestToken() {
        if (!hasToken && !requestedToken) {
            requestedToken = true;
            network.sendMessage("REQUEST_TOKEN");
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        move(pacman, true);
        move(player2, false);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        for (Block wall : walls) g.drawImage(wall.image, wall.x, wall.y, wall.width, wall.height, null);
        for (Block food : foods) {
            g.setColor(Color.WHITE);
            g.fillRect(food.x, food.y, food.width, food.height);
        }
        for (Block ghost : ghosts) g.drawImage(ghost.image, ghost.x, ghost.y, ghost.width, ghost.height, null);
        g.drawImage(pacman.image, pacman.x, pacman.y, pacman.width, pacman.height, null);
        g.drawImage(player2.image, player2.x, player2.y, player2.width, player2.height, null);

        g.setColor(Color.YELLOW);
        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.drawString("Score: " + score, 10, 20);
    }

    @Override public void keyTyped(KeyEvent e) {}
    @Override public void keyReleased(KeyEvent e) {}

    @Override
    public void keyPressed(KeyEvent e) {
        char dir = ' ';
        int code = e.getKeyCode();

        // Player 1 (Arrow keys)
        switch (code) {
            case KeyEvent.VK_UP -> { dir = 'U'; pacman.image = pacU; pacman.updateDirection(dir); }
            case KeyEvent.VK_DOWN -> { dir = 'D'; pacman.image = pacD; pacman.updateDirection(dir); }
            case KeyEvent.VK_LEFT -> { dir = 'L'; pacman.image = pacL; pacman.updateDirection(dir); }
            case KeyEvent.VK_RIGHT -> { dir = 'R'; pacman.image = pacR; pacman.updateDirection(dir); }
        }

        // Send player 1 movement to network
        if (code == KeyEvent.VK_UP || code == KeyEvent.VK_DOWN ||
                code == KeyEvent.VK_LEFT || code == KeyEvent.VK_RIGHT) {
            network.sendMessage("DIR:" + dir);
        }

        // Player 2 (WASD)
        switch (code) {
            case KeyEvent.VK_W -> { player2.image = pacU; player2.updateDirection('U'); }
            case KeyEvent.VK_S -> { player2.image = pacD; player2.updateDirection('D'); }
            case KeyEvent.VK_A -> { player2.image = pacL; player2.updateDirection('L'); }
            case KeyEvent.VK_D -> { player2.image = pacR; player2.updateDirection('R'); }
        }

        // NFS Mount/Unmount
        if (code == KeyEvent.VK_M) {
            if (!isMounted) mountNFS(); else unmountNFS();
        }
    }


    private void mountNFS() {
        File file = new File(mountPath + "highscore.txt");
        if (file.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line = br.readLine();
                System.out.println("[NFS] Mounted! High Score: " + line);
                isMounted = true;
            } catch (IOException e) {
                System.out.println("[NFS] Mount failed.");
            }
        }
    }

    private void unmountNFS() {
        isMounted = false;
        System.out.println("[NFS] Unmounted.");
    }
}
