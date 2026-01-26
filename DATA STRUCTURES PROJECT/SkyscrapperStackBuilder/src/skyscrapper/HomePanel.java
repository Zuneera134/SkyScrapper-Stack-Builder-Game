package skyscrapper;

import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class HomePanel extends JPanel {

    private final Runnable onPlay;

    private final BufferedImage homeImage;
    private final BufferedImage playImage;

    private final Rectangle playBounds = new Rectangle();
    private boolean hoveringPlay = false;

    // leaderboard
    private final Leaderboard leaderboard = new Leaderboard();

    // sounds
    private final Clip sClick;

    public HomePanel(Runnable onPlay) {
        this.onPlay = onPlay;

        setPreferredSize(new Dimension(360, 640));

        homeImage = load("/skyscrapper/home.png");
        playImage = load("/skyscrapper/play.png");

        sClick = loadClip("/skyscrapper/click.wav");

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                boolean nowHover = playBounds.contains(e.getPoint());
                if (nowHover != hoveringPlay) {
                    hoveringPlay = nowHover;
                    repaint();
                }
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (playBounds.contains(e.getPoint())) {
                    play(sClick);
                    if (onPlay != null) onPlay.run();
                }
            }
        });
    }

    private BufferedImage load(String path) {
        try {
            var url = HomePanel.class.getResource(path);
            if (url == null) {
                System.out.println("RESOURCE NOT FOUND: " + path);
                return null;
            }
            return ImageIO.read(url);
        } catch (IOException ex) {
            System.out.println("FAILED TO LOAD: " + path + " = " + ex.getMessage());
            return null;
        }
    }

    private Clip loadClip(String path) {
        try {
            InputStream in = HomePanel.class.getResourceAsStream(path);
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

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // Background image
        if (homeImage != null) {
            g.drawImage(homeImage, 0, 0, w, h, null);
        } else {
            g.setColor(new Color(30, 30, 30));
            g.fillRect(0, 0, w, h);
            g.setColor(Color.WHITE);
            g.drawString("home.png not found", 20, 20);
        }

        // Play button bounds
        int btnW = (int) (w * 0.62);
        int btnH = (int) (btnW * 0.28);
        int btnX = (w - btnW) / 2;
        int btnY = (int) (h * 0.80);
        playBounds.setBounds(btnX, btnY, btnW, btnH);

        // Leaderboard card above play button
        int boardW = (int) (w * 0.82);
        int boardH = 150;
        int boardX = (w - boardW) / 2;
        int boardY = btnY - boardH - 18; // above play button with spacing
        drawLeaderboardCard(g, boardX, boardY, boardW, boardH);

        // Play button hover effect
        int pop = hoveringPlay ? 4 : 0;
        int drawX = btnX - pop;
        int drawY = btnY - pop;
        int drawW = btnW + pop * 2;
        int drawH = btnH + pop * 2;

        if (playImage != null) {
            Object old = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

            g.drawImage(playImage, drawX, drawY, drawW, drawH, null);

            if (old != null) g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, old);
        } else {
            g.setColor(new Color(255, 190, 60));
            g.fillRoundRect(drawX, drawY, drawW, drawH, 18, 18);

            g.setColor(Color.WHITE);
            int tri = drawH / 3;
            int cx = drawX + drawW / 2;
            int cy = drawY + drawH / 2;
            int[] xs = { cx - tri / 2, cx - tri / 2, cx + tri / 2 };
            int[] ys = { cy - tri / 2, cy + tri / 2, cy };
            g.fillPolygon(xs, ys, 3);
        }
    }

    
    private void drawLeaderboardCard(Graphics2D g, int x, int y, int w, int h) {
       
        leaderboard.load();
        List<Leaderboard.Entry> top = leaderboard.top3();

        
        g.setColor(new Color(30, 60, 90, 200)); 
        g.fillRoundRect(x, y, w, h, 22, 22);

       
        g.setColor(new Color(255, 255, 255, 90));
        g.drawRoundRect(x, y, w, h, 22, 22);

       
        g.setFont(getFont().deriveFont(Font.BOLD, 18f));
        g.setColor(Color.WHITE);
        String title = "Leaderboard (Top 3)";
        FontMetrics fmT = g.getFontMetrics();
        g.drawString(title, x + (w - fmT.stringWidth(title)) / 2, y + 28);

        
        g.setFont(getFont().deriveFont(Font.PLAIN, 14f));
        int rowY = y + 58;

        for (int i = 0; i < 3; i++) {
            String name = "---";
            String right = "---";

            if (i < top.size()) {
                Leaderboard.Entry e = top.get(i);
                name = trimName(e.name, 12);
                right = e.score + " pts  •  " + formatSeconds(e.seconds);
            }

           
            g.setColor(new Color(255, 255, 255, 220));
            g.drawString((i + 1) + ".", x + 18, rowY);

           
            g.setColor(Color.WHITE);
            g.drawString(name, x + 45, rowY);

            
            g.setColor(new Color(255, 255, 255, 210));
            FontMetrics fm = g.getFontMetrics();
            g.drawString(right, x + w - 18 - fm.stringWidth(right), rowY);

          
            if (i < 2) {
                g.setColor(new Color(255, 255, 255, 45));
                g.drawLine(x + 16, rowY + 10, x + w - 16, rowY + 10);
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
}
