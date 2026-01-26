package skyscrapper;

import java.awt.Color;
import java.awt.Graphics2D;


public class Block {

    double x;
    double y;
    double width;
    double height;

   
    double vx = 0;     
    double vy = 0;     
    boolean falling = false;

    private final Color bodyColor;

    
    private static final Color TRIM_COLOR   = new Color(245, 245, 245);
    private static final Color DOOR_COLOR   = new Color(80, 45, 25);
    private static final Color KNOB_COLOR   = new Color(230, 200, 150);
    private static final Color WINDOW_COLOR = new Color(80, 190, 255);

  

    public Block(double x, double y, double width, double height, Color bodyColor) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.bodyColor = bodyColor;
    }


    public void update(double dt) {

        if (falling) {
            vy += 900 * dt;  
            y  += vy * dt;
        }

        x += vx * dt;         
    }


    public void render(Graphics2D g) {

        int drawX = (int) x;
        int drawY = (int) y;
        int drawW = (int) width;
        int drawH = (int) height;

        int cornerRadius = 8;

       
        g.setColor(bodyColor);
        g.fillRoundRect(drawX, drawY, drawW, drawH, cornerRadius, cornerRadius);

      
        int trimHeight = Math.max(7, Math.min(12, drawH / 6));
        g.setColor(TRIM_COLOR);
        g.fillRoundRect(drawX,drawY + drawH - trimHeight,drawW, trimHeight,cornerRadius, cornerRadius);

        int centerX = drawX + drawW / 2;
        int centerY = drawY + drawH / 2;

        if (drawH > 50) {

            
            int doorW = drawW / 4;
            int doorH = drawH / 2;

            g.setColor(DOOR_COLOR);
            g.fillRoundRect(centerX - doorW / 2,drawY + drawH - doorH - 4,doorW,doorH,6,6);

          
            g.setColor(KNOB_COLOR);
            g.fillOval(centerX + doorW / 2 - 10,drawY + drawH - doorH / 2,4,4);

        } else {

           
            int windowW = drawW / 3;
            int windowH = drawH / 2;

            g.setColor(WINDOW_COLOR);
            g.fillRoundRect(centerX - windowW / 2,centerY - windowH / 2,windowW,windowH,4,4);

           
            g.setColor(Color.WHITE);
            g.drawRoundRect(centerX - windowW / 2,centerY - windowH / 2,windowW,windowH,4,4);

            g.drawLine(centerX, centerY - windowH / 2,centerX, centerY + windowH / 2);

            g.drawLine(centerX - windowW / 2, centerY,centerX + windowW / 2, centerY);
        }
    }
}
