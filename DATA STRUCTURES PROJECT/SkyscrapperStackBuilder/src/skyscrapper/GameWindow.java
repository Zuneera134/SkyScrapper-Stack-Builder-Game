package skyscrapper;

import javax.swing.*;
import java.awt.*;

public class GameWindow extends JFrame {

    private final CardLayout cards = new CardLayout();
    private final JPanel root = new JPanel(cards);

    private final GamePanel gamePanel = new GamePanel();
    private final HomePanel homePanel = new HomePanel(() -> {
        cards.show(root, "GAME");
        gamePanel.beginGame();     
        gamePanel.requestFocusInWindow();
    });

    public GameWindow() {
        super("Skyscrapper");

        root.add(homePanel, "HOME");
        root.add(gamePanel, "GAME");

        setContentPane(root);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        pack();
        setLocationRelativeTo(null);

       
        gamePanel.start();

        cards.show(root, "HOME");
        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(GameWindow::new);
    }
}
