import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import javax.swing.*;

public class GridPanel extends JPanel {

    private final BoardModel model;
    private final int size = 45;
    private java.util.List<ShieldAnimation> shieldAnimations = new ArrayList<>();

    public GridPanel(BoardModel m) {
        model = m;
        setPreferredSize(new Dimension(m.cols * size, m.rows * size));
        setBackground(new Color(240, 240, 245));
        
        javax.swing.Timer animTimer = new javax.swing.Timer(30, e -> {
            boolean needsRepaint = false;
            Iterator<ShieldAnimation> it = shieldAnimations.iterator();
            while (it.hasNext()) {
                ShieldAnimation anim = it.next();
                anim.update();
                if (anim.isFinished()) {
                    it.remove();
                }
                needsRepaint = true;
            }
            if (needsRepaint) {
                repaint();
            }
        });
        animTimer.start();
    }

    public void triggerShieldBreak(int row, int col) {
        shieldAnimations.add(new ShieldAnimation(col * size + size/2, row * size + size/2));
    }

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // Draw grid cells
        for (int r = 0; r < model.rows; r++) {
            for (int c = 0; c < model.cols; c++) {
                int x = c * size;
                int y = r * size;
                Cell cell = model.grid[r][c];

                // Background - simple original style
                g2.setColor(cell.wall ? Color.DARK_GRAY : Color.LIGHT_GRAY);
                g2.fillRect(x, y, size, size);

                // Draw stop markers
                if (cell.stop) {
                    g2.setColor(Color.BLACK);
                    g2.setStroke(new BasicStroke(2));
                    g2.drawOval(x + 6, y + 6, size - 12, size - 12);
                }

                // Draw mines - enhanced bomb style
                if (cell.mine) {
                    int centerX = x + size/2;
                    int centerY = y + size/2 + 1;
                    int bombSize = size - 20;
                    
                    // Shadow
                    g2.setColor(new Color(0, 0, 0, 60));
                    g2.fillOval(centerX - bombSize/2 + 2, centerY - bombSize/2 + 3, bombSize, bombSize);
                    
                    // Bomb body - dark sphere with gradient
                    RadialGradientPaint rgp = new RadialGradientPaint(
                        centerX - 4, centerY - 4, bombSize/2,
                        new float[]{0.0f, 0.7f, 1.0f},
                        new Color[]{new Color(50, 50, 55), new Color(20, 20, 25), new Color(10, 10, 15)}
                    );
                    g2.setPaint(rgp);
                    g2.fillOval(centerX - bombSize/2, centerY - bombSize/2, bombSize, bombSize);
                    
                    // Highlight
                    g2.setColor(new Color(255, 255, 255, 100));
                    g2.fillOval(centerX - bombSize/2 + 4, centerY - bombSize/2 + 3, 7, 7);
                    
                    // Fuse
                    g2.setColor(new Color(40, 40, 45));
                    g2.setStroke(new BasicStroke(2f));
                    g2.drawLine(centerX - 1, centerY - bombSize/2, centerX - 4, centerY - bombSize/2 - 7);
                    
                    // Spark on fuse
                    g2.setColor(new Color(255, 200, 50));
                    g2.fillOval(centerX - 7, centerY - bombSize/2 - 10, 5, 5);
                    g2.setColor(new Color(255, 255, 100));
                    g2.fillOval(centerX - 6, centerY - bombSize/2 - 9, 3, 3);
                }

                // Draw gems - simple original cyan style
                if (cell.gem) {
                    g2.setColor(Color.CYAN);
                    int[] xPoints = {x + size/2, x + size - 10, x + size/2, x + 10};
                    int[] yPoints = {y + 10, y + size/2, y + size - 10, y + size/2};
                    g2.fillPolygon(xPoints, yPoints, 4);
                }

                // Draw shields - simple original style
                if (cell.shield) {
                    g2.setColor(Color.BLUE);
                    g2.fillOval(x + 12, y + 12, size - 24, size - 24);
                    g2.setColor(Color.WHITE);
                    g2.setFont(new Font("SansSerif", Font.BOLD, 14));
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString("S", x + size/2 - fm.stringWidth("S")/2, y + size/2 + fm.getAscent()/2 - 2);
                }

                // Grid lines
                g2.setColor(Color.GRAY);
                g2.setStroke(new BasicStroke(1));
                g2.drawRect(x, y, size, size);
            }
        }

        // Draw shield bubbles for human - simple original style
        if (model.humanShields > 0) {
            g2.setColor(new Color(0, 191, 255, 128));
            g2.fillOval(model.humanCol * size + 5, model.humanRow * size + 5, size - 10, size - 10);
            g2.setColor(Color.BLACK);
            g2.setFont(new Font("SansSerif", Font.BOLD, 12));
            FontMetrics fm = g2.getFontMetrics();
            String text = String.valueOf(model.humanShields);
            g2.drawString(text, model.humanCol * size + size/2 - fm.stringWidth(text)/2, 
                         model.humanRow * size + size/2 + fm.getAscent()/2 - 2);
        }

        // Draw shield bubbles for CPU - simple original style
        if (model.cpuShields > 0) {
            g2.setColor(new Color(0, 191, 255, 128));
            g2.fillOval(model.cpuCol * size + 5, model.cpuRow * size + 5, size - 10, size - 10);
            g2.setColor(Color.BLACK);
            g2.setFont(new Font("SansSerif", Font.BOLD, 12));
            FontMetrics fm = g2.getFontMetrics();
            String text = String.valueOf(model.cpuShields);
            g2.drawString(text, model.cpuCol * size + size/2 - fm.stringWidth(text)/2,
                         model.cpuRow * size + size/2 + fm.getAscent()/2 - 2);
        }

        // Draw players - simple original style
        g2.setColor(Color.GREEN);
        g2.fillOval(model.humanCol * size + 10, model.humanRow * size + 10, size - 20, size - 20);

        g2.setColor(Color.RED);
        g2.fillOval(model.cpuCol * size + 10, model.cpuRow * size + 10, size - 20, size - 20);

        // Draw shield break animations
        for (ShieldAnimation anim : shieldAnimations) {
            anim.draw(g2);
        }
    }

    private void drawPlayer(Graphics2D g2, int x, int y, Color color, String label) {
        int centerX = x + size/2;
        int centerY = y + size/2;
        int playerSize = size - 16;
        
        // Shadow
        g2.setColor(new Color(0, 0, 0, 70));
        g2.fillOval(centerX - playerSize/2 + 2, centerY - playerSize/2 + 4, playerSize, playerSize);
        
        // Outer glow rings
        for (int i = 3; i > 0; i--) {
            int glowAlpha = 25 * i;
            g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), glowAlpha));
            int glowSize = playerSize + i * 6;
            g2.fillOval(centerX - glowSize/2, centerY - glowSize/2, glowSize, glowSize);
        }
        
        // Main circle with radial gradient
        RadialGradientPaint rgp = new RadialGradientPaint(
            centerX - 6, centerY - 6, playerSize/2,
            new float[]{0.0f, 0.6f, 1.0f},
            new Color[]{color.brighter().brighter(), color, color.darker()}
        );
        g2.setPaint(rgp);
        g2.fillOval(centerX - playerSize/2, centerY - playerSize/2, playerSize, playerSize);
        
        // Glossy highlight
        g2.setColor(new Color(255, 255, 255, 180));
        g2.fillOval(centerX - playerSize/2 + 5, centerY - playerSize/2 + 4, 12, 12);
        
        // Label with shadow
        g2.setFont(new Font("Arial", Font.BOLD, 15));
        FontMetrics fm = g2.getFontMetrics();
        int textX = centerX - fm.stringWidth(label)/2;
        int textY = centerY + fm.getAscent()/2 - 1;
        
        g2.setColor(new Color(0, 0, 0, 120));
        g2.drawString(label, textX + 1, textY + 1);
        g2.setColor(Color.WHITE);
        g2.drawString(label, textX, textY);
    }

    private void drawShieldBubble(Graphics2D g2, int x, int y, Color color, int count) {
        int centerX = x + size/2;
        int centerY = y + size/2;
        int bubbleSize = size - 8;
        
        // Outer glow
        for (int i = 4; i > 0; i--) {
            int alpha = 15 * i;
            g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha));
            int glowSize = bubbleSize + i * 5;
            g2.fillOval(centerX - glowSize/2, centerY - glowSize/2, glowSize, glowSize);
        }
        
        // Main bubble with radial gradient
        RadialGradientPaint rgp = new RadialGradientPaint(
            centerX - 8, centerY - 8, bubbleSize/2,
            new float[]{0.0f, 0.7f, 1.0f},
            new Color[]{
                new Color(150, 220, 255, 100),
                new Color(100, 200, 255, 80),
                new Color(50, 150, 255, 60)
            }
        );
        g2.setPaint(rgp);
        g2.fillOval(centerX - bubbleSize/2, centerY - bubbleSize/2, bubbleSize, bubbleSize);
        
        // Border
        g2.setColor(new Color(100, 200, 255, 150));
        g2.setStroke(new BasicStroke(2.5f));
        g2.drawOval(centerX - bubbleSize/2, centerY - bubbleSize/2, bubbleSize, bubbleSize);
        
        // Glossy shine
        g2.setColor(new Color(255, 255, 255, 150));
        g2.fillArc(centerX - bubbleSize/2 + 6, centerY - bubbleSize/2 + 6, 
                   bubbleSize - 20, bubbleSize - 20, 45, 120);
        
        // Shield count
        g2.setFont(new Font("Arial", Font.BOLD, 17));
        FontMetrics fm = g2.getFontMetrics();
        String text = String.valueOf(count);
        int textX = centerX - fm.stringWidth(text)/2;
        int textY = centerY + fm.getAscent()/2 - 1;
        
        g2.setColor(new Color(0, 0, 0, 150));
        g2.drawString(text, textX + 1, textY + 1);
        g2.setColor(new Color(255, 255, 255));
        g2.drawString(text, textX, textY);
    }

    public int row(int y) { return y / size; }
    public int col(int x) { return x / size; }

    // Shield break animation class
    private class ShieldAnimation {
        private int x, y;
        private int frame = 0;
        private final int maxFrames = 30;
        private java.util.List<Particle> particles = new ArrayList<>();
        private java.util.List<ExplosionRing> rings = new ArrayList<>();

        public ShieldAnimation(int x, int y) {
            this.x = x;
            this.y = y;
            
            // Create explosive particles in all directions
            for (int i = 0; i < 20; i++) {
                double angle = (Math.PI * 2 * i) / 20;
                particles.add(new Particle(x, y, angle, 1));
            }
            
            // Add some random particles
            for (int i = 0; i < 15; i++) {
                double angle = Math.random() * Math.PI * 2;
                particles.add(new Particle(x, y, angle, 0.7 + Math.random() * 0.6));
            }
            
            // Create expanding rings
            for (int i = 0; i < 3; i++) {
                rings.add(new ExplosionRing(x, y, i * 5));
            }
        }

        public void update() {
            frame++;
            for (Particle p : particles) {
                p.update();
            }
            for (ExplosionRing r : rings) {
                r.update();
            }
        }

        public boolean isFinished() {
            return frame >= maxFrames;
        }

        public void draw(Graphics2D g2) {
            // Draw rings first (behind particles)
            for (ExplosionRing r : rings) {
                r.draw(g2);
            }
            
            // Draw center flash
            if (frame < 8) {
                int flashAlpha = (int)(200 * (1 - frame/8.0));
                int flashSize = 20 + frame * 4;
                
                g2.setColor(new Color(255, 200, 100, flashAlpha));
                g2.fillOval(x - flashSize/2, y - flashSize/2, flashSize, flashSize);
                
                g2.setColor(new Color(255, 255, 200, flashAlpha/2));
                g2.fillOval(x - flashSize/4, y - flashSize/4, flashSize/2, flashSize/2);
            }
            
            // Draw particles
            for (Particle p : particles) {
                p.draw(g2);
            }
        }

        private class Particle {
            double x, y, vx, vy;
            int size;
            Color color;
            double speedMult;

            public Particle(int startX, int startY, double angle, double speedMult) {
                this.x = startX;
                this.y = startY;
                this.speedMult = speedMult;
                double speed = (3 + Math.random() * 3) * speedMult;
                this.vx = Math.cos(angle) * speed;
                this.vy = Math.sin(angle) * speed;
                this.size = 3 + (int)(Math.random() * 5);
                
                // Vary colors for explosion effect
                double colorRand = Math.random();
                if (colorRand < 0.4) {
                    this.color = new Color(255, 150, 50); // Orange
                } else if (colorRand < 0.7) {
                    this.color = new Color(255, 200, 100); // Yellow
                } else {
                    this.color = new Color(100, 200, 255); // Blue (shield color)
                }
            }

            public void update() {
                x += vx;
                y += vy;
                vx *= 0.92;
                vy *= 0.92;
                vy += 0.15; // Gravity
            }

            public void draw(Graphics2D g2) {
                int alpha = (int)(255 * (1 - (double)frame / maxFrames));
                alpha = Math.max(0, Math.min(255, alpha));
                
                // Particle glow
                int glowSize = size + 4;
                g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha/3));
                g2.fillOval((int)x - glowSize/2, (int)y - glowSize/2, glowSize, glowSize);
                
                // Particle core
                g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha));
                g2.fillOval((int)x - size/2, (int)y - size/2, size, size);
                
                // Bright center
                g2.setColor(new Color(255, 255, 255, alpha/2));
                g2.fillOval((int)x - size/4, (int)y - size/4, size/2, size/2);
            }
        }
        
        private class ExplosionRing {
            int x, y;
            int radius;
            int delay;
            
            public ExplosionRing(int x, int y, int delay) {
                this.x = x;
                this.y = y;
                this.radius = 5;
                this.delay = delay;
            }
            
            public void update() {
                if (frame > delay) {
                    radius += 3;
                }
            }
            
            public void draw(Graphics2D g2) {
                if (frame <= delay) return;
                
                int effectiveFrame = frame - delay;
                int alpha = (int)(180 * (1 - (double)effectiveFrame / (maxFrames - delay)));
                alpha = Math.max(0, Math.min(180, alpha));
                
                if (alpha > 0) {
                    g2.setColor(new Color(255, 200, 100, alpha/2));
                    g2.setStroke(new BasicStroke(4));
                    g2.drawOval(x - radius, y - radius, radius * 2, radius * 2);
                    
                    g2.setColor(new Color(255, 255, 200, alpha/3));
                    g2.setStroke(new BasicStroke(2));
                    g2.drawOval(x - radius + 2, y - radius + 2, radius * 2 - 4, radius * 2 - 4);
                }
            }
        }
    }
}