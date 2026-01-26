package skyscrapper;

import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import javax.swing.Timer;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.*;

public class GamePanel extends JPanel {

    private static final int WIDTH  = 340;
    private static final int HEIGHT = 605;

    private final Leaderboard leaderboard = new Leaderboard();
    private long runStartNano = 0;
    private boolean savedThisGame = false; // ask name only once per run

    private double difficulty = 0.0;
    private double minOverlapRequired = 0.5;
    private double craneSpeed = 100.0;

    private int W() { return (getWidth()  > 0) ? getWidth()  : WIDTH; }
    private int H() { return (getHeight() > 0) ? getHeight() : HEIGHT; }

    private BufferedImage bgImage;
    private BufferedImage starImg;
    private boolean bgImageLoaded = false;
    private boolean starImageLoaded = false;

    // sounds
    private Clip sDrop, sPerfect, sGameOver, sClick;

    private static class BlockStack {
        private Block[] data = new Block[64];
        private int size = 0;

        void clear() { size = 0; }
        int size() { return size; }
        boolean isEmpty() { return size == 0; }

        void push(Block b) {
            if (size >= data.length) {
                Block[] next = new Block[data.length * 2];
                System.arraycopy(data, 0, next, 0, data.length);
                data = next;
            }
            data[size++] = b;
        }

        Block pop() {
            if (size <= 0) return null;
            Block b = data[--size];
            data[size] = null;
            return b;
        }

        Block peek() {
            if (size <= 0) return null;
            return data[size - 1];
        }

        Block get(int i) {
            if (i < 0 || i >= size) return null;
            return data[i];
        }
    }

    private final BlockStack tower = new BlockStack();
    private final java.util.List<Block> fallingBlocks = new ArrayList<>();

    private Block currentBlock;
    private Block baseBlock;
    private double baseY;

    private final Timer timer;
    private long lastTime;

    private final Random rand = new Random();

    private int score = 0;
    private boolean paused = false;

    private enum GameState { MENU, RUNNING, GAMEOVER }
    private GameState state = GameState.MENU;

    private final Rectangle pauseButton = new Rectangle();
    private double cameraOffsetY = 0;

    private String feedbackText = "";
    private double feedbackTimer = 0.0;

    private boolean shakeActive = false;
    private double shakeTimer = 0.0;
    private static final double SHAKE_DURATION = 0.35;

    private boolean craneFrozen = false;
    private int frozenTopY = 0;
    private int frozenRopeBottom = 0;

    public GamePanel() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);

        baseY = HEIGHT - 140;

        loadAssets();
        createBaseBlock();

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();

                int mx = e.getX();
                int my = e.getY();

                if (pauseButton.contains(mx, my) && state == GameState.RUNNING) {
                    paused = !paused;
                    play(sClick);
                    return;
                }

                if (state == GameState.MENU) {
                    beginGame();
                    play(sClick);
                    return;
                }

                if (state == GameState.GAMEOVER) {
                    restartGame();
                    state = GameState.RUNNING;
                    play(sClick);
                    return;
                }

                if (state == GameState.RUNNING && !paused) {
                    if (currentBlock != null && !currentBlock.falling) {
                        int topY = (int) (currentBlock.y - 60);
                        craneFrozen = true;
                        frozenTopY = topY;
                        frozenRopeBottom = topY + 20;

                        play(sDrop);

                        currentBlock.falling = true;
                        currentBlock.vy = 0;
                        currentBlock.vx = 0;
                    }
                }
            }
        });

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
              
//                if(e.getKeyCode() == KeyEvent.VK_DOWN){
//                    if(state == GameState.RUNNING && !paused){
//                        if(currentBlock != null && !currentBlock.falling){
//                            int topY =(int) (currentBlock.y-60);
//                            craneFrozen= true;
//                            frozenTopY = topY;
//                            frozenRopeBottom= topY+20;
//                            
//                            play(sDrop);
//                            
//                            currentBlock.falling= true;
//                            currentBlock.vy=0;
//                            currentBlock.vx=0;
//                        }
//                        
//                    }
//                }
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) System.exit(0);
                if (e.getKeyCode() == KeyEvent.VK_P) { paused = !paused; play(sClick); }
                if (e.getKeyCode() == KeyEvent.VK_R) {
                    restartGame();
                    state = GameState.RUNNING;
                    play(sClick);
                }
            }
        });

        timer = new Timer(1000 / 60, ev -> gameLoop());
        lastTime = System.nanoTime();
    }

    public void start() { timer.start(); }

    public void beginGame() {
        paused = false;
        state = GameState.RUNNING;
        runStartNano = System.nanoTime();
        savedThisGame = false;
    }

    private void loadAssets() {
        bgImage = tryLoadImage("/skyscrapper/background.png");
        bgImageLoaded = (bgImage != null);

        BufferedImage rawStar = tryLoadImage("/skyscrapper/star.png");
        if (rawStar != null) {
            starImg = cropTransparent(rawStar);
            starImageLoaded = true;
        } else {
            starImg = null;
            starImageLoaded = false;
        }

        sDrop     = loadClip("/skyscrapper/drop.wav");
        sPerfect  = loadClip("/skyscrapper/perfect.wav");
        sGameOver = loadClip("/skyscrapper/gameover.wav");
        sClick    = loadClip("/skyscrapper/click.wav");
    }

    private BufferedImage tryLoadImage(String path) {
        try {
            URL url = GamePanel.class.getResource(path);
            if (url == null) {
                System.out.println("IMAGE NOT FOUND: " + path);
                return null;
            }
            return ImageIO.read(url);
        } catch (Exception ex) {
            System.out.println("FAILED TO LOAD IMAGE: " + path);
            ex.printStackTrace();
            return null;
        }
    }

    private Clip loadClip(String path) {
        try {
            InputStream in = GamePanel.class.getResourceAsStream(path);
            if (in == null) {
                System.out.println("SOUND NOT FOUND: " + path);
                return null;
            }
            AudioInputStream ais = AudioSystem.getAudioInputStream(new BufferedInputStream(in));
            Clip clip = AudioSystem.getClip();
            clip.open(ais);
            return clip;
        } catch (Exception e) {
            System.out.println("FAILED TO LOAD SOUND: " + path);
            e.printStackTrace();
            return null;
        }
    }

    private void play(Clip c) {
        if (c == null) return;
        if (c.isRunning()) c.stop();
        c.setFramePosition(0);
        c.start();
    }

    private BufferedImage cropTransparent(BufferedImage src) {
        if (src == null) return null;

        try {
            int w = src.getWidth();
            int h = src.getHeight();

            int minX = w, minY = h, maxX = -1, maxY = -1;

            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int a = (src.getRGB(x, y) >>> 24) & 0xFF;
                    if (a > 5) {
                        if (x < minX) minX = x;
                        if (y < minY) minY = y;
                        if (x > maxX) maxX = x;
                        if (y > maxY) maxY = y;
                    }
                }
            }

            if (maxX < minX || maxY < minY) return src;
            return src.getSubimage(minX, minY, (maxX - minX + 1), (maxY - minY + 1));
        } catch (Exception e) {
            System.out.println("cropTransparent failed");
            e.printStackTrace();
            return src;
        }
    }

    private void gameLoop() {
        long now = System.nanoTime();
        double dt = (now - lastTime) / 1_000_000_000.0;
        lastTime = now;
        if (dt > 0.05) dt = 0.05;

        if (state == GameState.RUNNING && !paused) {
            updateDifficulty(dt);
            update(dt);
        }

        repaint();
    }

    private String askNameLeaderboard() {
        Color oldOPBg = UIManager.getColor("OptionPane.background");
        Color oldPanelBg = UIManager.getColor("Panel.background");

        try {
            UIManager.put("OptionPane.background", Color.YELLOW);
            UIManager.put("Panel.background", Color.YELLOW);

            return JOptionPane.showInputDialog(
                    this,
                    "Score > 100 waaaoww!\nEnter your name:",
                    "Leaderboard!",
                    JOptionPane.PLAIN_MESSAGE
            );

        } finally {
            UIManager.put("OptionPane.background", oldOPBg);
            UIManager.put("Panel.background", oldPanelBg);
        }
    }

    private void createBaseBlock() {
        tower.clear();
        fallingBlocks.clear();
        score = 0;

        currentBlock = null;

        feedbackText = "";
        feedbackTimer = 0;

        shakeActive = false;
        shakeTimer = 0;

        craneFrozen = false;

        double baseWidth  = 100;
        double baseHeight = 60;
        double baseX = W() / 2.0 - baseWidth / 2.0;
        double baseBlockY = baseY - baseHeight;

        baseBlock = new Block(baseX, baseBlockY, baseWidth, baseHeight, new Color(210, 90, 40));
        baseBlock.falling = false;
        baseBlock.vx = 0;
        baseBlock.vy = 0;

        tower.push(baseBlock);

        spawnNextBlock();
    }

    private void restartGame() {
        baseY = HEIGHT - 140;
        cameraOffsetY = 0;
        craneFrozen = false;
        difficulty = 0.0;
        minOverlapRequired = 0.5;
        craneSpeed = 100.0;

        createBaseBlock();
        paused = false;
        state = GameState.RUNNING;
        runStartNano = System.nanoTime();
        savedThisGame = false;
    }

    private void updateDifficulty(double dt) {
        difficulty += dt * 0.015;
        difficulty += score * 0.0005;
        if (difficulty > 1.0) difficulty = 1.0;

        minOverlapRequired = 0.5 + difficulty * 0.2;
        craneSpeed = 100 + difficulty * 120;
    }

    private void update(double dt) {
        if (currentBlock != null && !currentBlock.falling) {
            currentBlock.x += currentBlock.vx * dt;

            if (currentBlock.x <= 30) {
                currentBlock.x = 30;
                currentBlock.vx = Math.abs(currentBlock.vx);
            } else if (currentBlock.x + currentBlock.width >= W() - 30) {
                currentBlock.x = W() - 30 - currentBlock.width;
                currentBlock.vx = -Math.abs(currentBlock.vx);
            }
        }

        if (feedbackTimer > 0) {
            feedbackTimer -= dt;
            if (feedbackTimer < 0) {
                feedbackTimer = 0;
                feedbackText = "";
            }
        }

        if (shakeActive) {
            shakeTimer -= dt;
            if (shakeTimer <= 0) {
                shakeTimer = 0;
                shakeActive = false;
            }
        }

        java.util.List<Block> toRemove = new ArrayList<>();
        for (int i = 0; i < fallingBlocks.size(); i++) {
            Block b = fallingBlocks.get(i);
            if (b == null) continue;
            b.update(dt);
            if (b.y > H() + 200) toRemove.add(b);
        }
        for (int i = 0; i < toRemove.size(); i++) {
            fallingBlocks.remove(toRemove.get(i));
        }

        if (currentBlock != null && currentBlock.falling) {
            currentBlock.update(dt);

            Block top = tower.peek();
            if (top != null) {
                double targetY = top.y - currentBlock.height;
                if (currentBlock.y >= targetY) {
                    currentBlock.y = targetY;
                    handlePlacement(top, currentBlock);
                }
            }
        }

        updateCamera(dt);
    }

    private void updateCamera(double dt) {
        double marginTop = 160;

        double topY = Double.MAX_VALUE;
        for (int i = 0; i < tower.size(); i++) {
            Block b = tower.get(i);
            if (b != null && b.y < topY) topY = b.y;
        }

        if (currentBlock != null && currentBlock.y < topY) topY = currentBlock.y;
        if (topY == Double.MAX_VALUE) return;

        double topScreenY = topY + cameraOffsetY;

        double targetOffsetY = cameraOffsetY;
        if (topScreenY < marginTop) {
            double neededDelta = marginTop - topScreenY;
            targetOffsetY = cameraOffsetY + neededDelta;
        }

        double speed = 5.0;
        double t = Math.min(1.0, speed * dt);
        cameraOffsetY = cameraOffsetY + (targetOffsetY - cameraOffsetY) * t;
    }

    private void setFeedback(String text) {
        feedbackText = text;
        feedbackTimer = 1.0;
    }

    private void triggerShake() {
        shakeActive = true;
        shakeTimer = SHAKE_DURATION;
    }

    // de-duplicated game over path (same logic, just one place)
    private void failPlacement(Block placed, String feedback) {
        placed.falling = true;
        if (placed.vy <= 0) placed.vy = 200;
        fallingBlocks.add(placed);
        currentBlock = null;
        state = GameState.GAMEOVER;

        onGameOverMaybeSave();

        play(sGameOver);
        setFeedback(feedback);
        triggerShake();
    }

    private void handlePlacement(Block below, Block placed) {
        double left  = Math.max(below.x, placed.x);
        double right = Math.min(below.x + below.width, placed.x + placed.width);
        double overlap = right - left;

        if (overlap <= 0) {
            failPlacement(placed, "Oops");
            return;
        }

        double overlapRatio = overlap / placed.width;

        if (overlapRatio < minOverlapRequired) {
            failPlacement(placed, "Oopps");
            return;
        }

        placed.vx = 0;
        placed.vy = 0;
        placed.falling = false;

        if (overlapRatio > 0.97) {
            setFeedback("Perfect!!");
            play(sPerfect);
        } else if (overlapRatio > 0.7) {
            setFeedback("Nice");
        }

        addScoreForPlacement(overlapRatio);

        tower.push(placed);
        currentBlock = null;

        spawnNextBlock();
    }

    private void addScoreForPlacement(double overlapRatio) {
        if (overlapRatio > 1.0) overlapRatio = 1.0;
        if (overlapRatio < 0.5) overlapRatio = 0.5;

        double t = (overlapRatio - 0.5) / 0.5;
        int points = 1 + (int) Math.round(t * 9);
        score += points;
    }

    private void onGameOverMaybeSave() {
        if (savedThisGame) return;
        savedThisGame = true;

        if (score <= 100) return;

        double seconds = (System.nanoTime() - runStartNano) / 1_000_000_000.0;

        String name = askNameLeaderboard();
        if (name == null) return;

        name = name.trim();
        if (name.isEmpty()) name = "Player";

        leaderboard.add(name, score, seconds);
        leaderboard.save();

        // ensure the panel shows latest results immediately
        leaderboard.load();
    }

    private void spawnNextBlock() {
        Block top = tower.peek();
        if (top == null) return;

        double rawWidth = top.width + (rand.nextInt(21) - 10);
        double maxWidth = W() - 80;
        double width    = Math.max(50, Math.min(maxWidth, rawWidth));

        double height = 50;
        double startY = top.y - 140;

        boolean fromRight = rand.nextBoolean();
        double x = fromRight ? (W() - 40 - width) : 40;
        double vx = fromRight ? -craneSpeed : craneSpeed;

        currentBlock = new Block(x, startY, width, height, new Color(220, 80, 40));
        currentBlock.vx = vx;
        currentBlock.falling = false;

        craneFrozen = false;
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawBackground(g);

        Graphics2D world = (Graphics2D) g.create();
        world.translate(0, cameraOffsetY);

        if (shakeActive) {
            int maxOffset = 6;
            int sx = (int) ((rand.nextDouble() * 2 - 1) * maxOffset);
            int sy = (int) ((rand.nextDouble() * 2 - 1) * maxOffset);
            world.translate(sx, sy);
        }

        drawGroundAndFence(world);
        drawTowerAndBlocks(world);
        drawCrane(world);
        world.dispose();

        drawHUD(g);
        drawFeedback(g);
        drawMenuOrGameOver(g);
        drawScreenFlash(g);
    }

    private void drawBackground(Graphics2D g) {
        if (bgImageLoaded && bgImage != null) {
            g.drawImage(bgImage, 0, 0, W(), H(), null);
        } else {
            g.setColor(new Color(255, 150, 80));
            g.fillRect(0, 0, W(), H());
        }

        g.setColor(new Color(0, 0, 0, 12));
        g.fillRect(0, 0, W(), H());
    }

    private void drawGroundAndFence(Graphics2D g) {
        int groundTop = (int) baseY + 40;

        g.setColor(new Color(120, 70, 40));
        g.fillRect(0, groundTop, W(), H() - groundTop);

        g.setColor(new Color(210, 200, 190));
        g.fillRect(0, groundTop - 30, W(), 30);

        g.setColor(new Color(190, 180, 170));
        int fenceY = groundTop - 40;
        g.fillRect(0, fenceY + 10, W(), 6);

        g.setColor(new Color(150, 110, 80));
        for (int x = 0; x < W(); x += 20) {
            g.drawLine(x, fenceY + 10, x + 10, fenceY);
            g.drawLine(x + 10, fenceY, x + 20, fenceY + 10);
        }
    }

    private void drawTowerAndBlocks(Graphics2D g) {
        for (int i = 0; i < tower.size(); i++) {
            Block b = tower.get(i);
            if (b != null) b.render(g);
        }

        for (int i = 0; i < fallingBlocks.size(); i++) {
            Block fb = fallingBlocks.get(i);
            if (fb != null) fb.render(g);
        }

        if (currentBlock != null) currentBlock.render(g);
    }

    private void drawCrane(Graphics2D g) {
        if (currentBlock == null) return;

        int cx = (int) Math.round(currentBlock.x + currentBlock.width / 2.0);

        int topY, ropeBottom;
        if (craneFrozen) {
            topY = frozenTopY;
            ropeBottom = frozenRopeBottom;
        } else {
            topY = (int) (currentBlock.y - 60);
            ropeBottom = topY + 20;
        }

        int ropeTop = (int) Math.round(-cameraOffsetY);
        if (ropeTop > ropeBottom) ropeTop = ropeBottom;

        g.setColor(new Color(120, 80, 40));
        g.fillRect(cx - 2, ropeTop, 4, ropeBottom - ropeTop);

        g.setColor(new Color(255, 210, 120));
        g.fillRoundRect(cx - 25, topY, 50, 30, 8, 8);
        g.setColor(new Color(170, 120, 60));
        g.drawRoundRect(cx - 25, topY, 50, 30, 8, 8);

        g.setStroke(new BasicStroke(4f));
        int armEndY = craneFrozen ? (ropeBottom - 4) : ((int) currentBlock.y - 4);
        g.drawLine(cx - 10, topY + 26, cx - 20, armEndY);
        g.drawLine(cx + 10, topY + 26, cx + 20, armEndY);
        g.setStroke(new BasicStroke(1f));
    }

    private void drawHUD(Graphics2D g) {
        int pad = 10;
        int starX = pad;
        int starY = pad;

        int box = 34;
        int gap = 6;

        if (starImageLoaded && starImg != null) {
            int imgW = starImg.getWidth();
            int imgH = starImg.getHeight();

            double s = Math.min((double) box / imgW, (double) box / imgH);
            int drawW = (int) Math.round(imgW * s);
            int drawH = (int) Math.round(imgH * s);

            int dx = starX + (box - drawW) / 2;
            int dy = starY + (box - drawH) / 2;

            Object oldInterp = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(starImg, dx, dy, drawW, drawH, null);
            if (oldInterp != null) g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, oldInterp);
        } else {
            drawStar(g, starX + box / 2, starY + box / 2, box / 2 - 2, new Color(255, 240, 100));
        }

        float fontSize = box * 0.80f;
        g.setFont(g.getFont().deriveFont(Font.BOLD, fontSize));
        g.setColor(Color.WHITE);

        FontMetrics fm = g.getFontMetrics();
        int textX = starX + box + gap;
        int textY = starY + (box + fm.getAscent() - fm.getDescent()) / 2;

        g.drawString(String.valueOf(score), textX, textY);

        int size = 32;
        int px = W() - size - 12;
        int py = 10;
        pauseButton.setBounds(px, py, size, size);

        g.setColor(new Color(255, 200, 120));
        g.fillRoundRect(px, py, size, size, 8, 8);
        g.setColor(new Color(220, 140, 60));
        g.drawRoundRect(px, py, size, size, 8, 8);

        g.setColor(Color.WHITE);
        int barW = 6;
        int barGap = 6;
        g.fillRoundRect(px + 8, py + 7, barW, size - 14, 3, 3);
        g.fillRoundRect(px + 8 + barW + barGap, py + 7, barW, size - 14, 3, 3);
    }

    private void drawMenuOrGameOver(Graphics2D g) {
        if (state == GameState.RUNNING && paused) {
            drawCenterCard(g, "Paused", "Press P or tap pause to resume", H() / 2 - 60);
            return;
        }

        if (state == GameState.RUNNING) return;

        if (state == GameState.MENU) {
            drawCenterCard(g, "Tap to Start", "Click anywhere to begin", H() / 2 - 60);
            return;
        }

        // GAMEOVER
        drawCenterCard(g, "Game Over", "Click to restart", H() / 2 - 90);
        drawTop3(g);
    }

    private void drawCenterCard(Graphics2D g, String title, String sub, int topY) {
        int cardW = 240;
        int cardH = 110;
        int x = W() / 2 - cardW / 2;
        int y = topY;

        g.setColor(new Color(0, 0, 0, 150));
        g.fillRoundRect(x, y, cardW, cardH, 22, 22);

        g.setColor(new Color(255, 255, 255, 90));
        g.drawRoundRect(x, y, cardW, cardH, 22, 22);

        g.setColor(Color.WHITE);
        g.setFont(g.getFont().deriveFont(Font.BOLD, 22f));
        FontMetrics fmT = g.getFontMetrics();
        g.drawString(title, W() / 2 - fmT.stringWidth(title) / 2, y + 42);

        g.setFont(g.getFont().deriveFont(Font.PLAIN, 13f));
        FontMetrics fmS = g.getFontMetrics();
        g.setColor(new Color(255, 255, 255, 230));
        g.drawString(sub, W() / 2 - fmS.stringWidth(sub) / 2, y + 70);
    }

    private void drawTop3(Graphics2D g) {
        java.util.List<Leaderboard.Entry> top = leaderboard.top3();

        int cardW = 290;
        int cardH = 140;
        int x = W() / 2 - cardW / 2;
        int y = H() / 2 + 35;

        g.setColor(new Color(0, 0, 0, 155));
        g.fillRoundRect(x, y, cardW, cardH, 22, 22);
        g.setColor(new Color(255, 255, 255, 85));
        g.drawRoundRect(x, y, cardW, cardH, 22, 22);

        g.setFont(g.getFont().deriveFont(Font.BOLD, 16f));
        g.setColor(Color.WHITE);
        String t = "Leaderboard (Top 3)";
        FontMetrics fm = g.getFontMetrics();
        g.drawString(t, x + (cardW - fm.stringWidth(t)) / 2, y + 28);

        g.setFont(g.getFont().deriveFont(Font.PLAIN, 13.5f));
        int rowY = y + 55;

        for (int i = 0; i < 3; i++) {
            String name = "---";
            String s = "---";
            if (i < top.size()) {
                Leaderboard.Entry e = top.get(i);
                name = trimName(e.name, 10);
                s = e.score + " pts  •  " + formatSeconds(e.seconds);
            }

            g.setColor(new Color(255, 255, 255, 230));
            g.drawString((i + 1) + ".", x + 18, rowY);

            g.setColor(Color.WHITE);
            g.drawString(name, x + 45, rowY);

            g.setColor(new Color(255, 255, 255, 210));
            int rightPad = 18;
            FontMetrics fmRow = g.getFontMetrics();
            g.drawString(s, x + cardW - rightPad - fmRow.stringWidth(s), rowY);

            if (i < 2) {
                g.setColor(new Color(255, 255, 255, 40));
                g.drawLine(x + 16, rowY + 10, x + cardW - 16, rowY + 10);
            }

            rowY += 28;
        }
    }

    private String trimName(String s, int max) {
        if (s == null) return "Player";
        s = s.trim();
        if (s.isEmpty()) return "Player";
        if (s.length() <= max) return s;
        return s.substring(0, Math.max(1, max - 1)) + "…";
    }

    private String formatSeconds(double s) {
        int total = (int) Math.round(s);
        return (total / 60) + ":" + String.format("%02d", total % 60);
    }

    private void drawFeedback(Graphics2D g) {
        if (feedbackTimer <= 0 || feedbackText.isEmpty()) return;

        float a = (float) Math.max(0, Math.min(1, feedbackTimer));

        g.setFont(g.getFont().deriveFont(Font.BOLD, 22f));
        FontMetrics fm = g.getFontMetrics();

        int textWidth = fm.stringWidth(feedbackText);
        int x = W() / 2 - textWidth / 2;
        int y = H() / 2 - 120;

        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, a * 0.55f));
        g.setColor(new Color(0, 0, 0));
        g.drawString(feedbackText, x + 2, y - 2);

        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, a));
        g.setColor(Color.WHITE);
        g.drawString(feedbackText, x, y - 4);

        g.setComposite(AlphaComposite.SrcOver);
    }

    private void drawScreenFlash(Graphics2D g) {
        if (!shakeActive) return;

        double t = shakeTimer / SHAKE_DURATION;
        t = Math.max(0, Math.min(1, t));

        int alpha = (int) (t * 140);
        g.setColor(new Color(255, 60, 60, alpha));
        g.fillRect(0, 0, W(), H());
    }

    private void drawStar(Graphics2D g, int cx, int cy, int r, Color color) {
        int points = 10;
        int[] xs = new int[points];
        int[] ys = new int[points];

        double angle = -Math.PI / 2;
        double step = Math.PI / 5;

        for (int i = 0; i < points; i++) {
            double rad = (i % 2 == 0) ? r : r / 2.5;
            xs[i] = (int) (cx + Math.cos(angle) * rad);
            ys[i] = (int) (cy + Math.sin(angle) * rad);
            angle += step;
        }

        g.setColor(color);
        g.fillPolygon(xs, ys, points);
        g.setColor(new Color(230, 180, 80));
        g.drawPolygon(xs, ys, points);
    }
}
