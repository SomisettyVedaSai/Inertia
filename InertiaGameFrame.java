import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;

public class InertiaGameFrame extends JFrame {

    private final BoardModel model;
    private final GridPanel grid;
    private final Difficulty difficulty;
    private JPanel mainPanel;
    private JLabel statusLabel;
    private JPanel scorePanel;

    public InertiaGameFrame(Difficulty d) {
        super("Inertia Game - " + d);

        this.difficulty = d;
        this.model = new BoardModel(12, 12, d);
        this.grid = new GridPanel(model);
        
        // Connect shield animation
        model.setShieldBreakListener((row, col) -> {
            grid.triggerShieldBreak(row, col);
        });

        setupUI();
        
        setMinimumSize(new Dimension(700, 700));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        grid.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (model.gameOver) return;

                Direction dir = Direction.fromClick(
                        model.humanRow,
                        model.humanCol,
                        grid.row(e.getY()),
                        grid.col(e.getX())
                );

                if (dir == null) return;

                int startRow = model.humanRow;
                int startCol = model.humanCol;
                int startShields = model.humanShields;

                model.move(true, dir);
                
                if (model.gameOver) {
                    grid.repaint();
                    updateScorePanel();
                    endGame();
                    return;
                }

                boolean posChanged = (model.humanRow != startRow || model.humanCol != startCol);
                boolean shieldUsed = (model.humanShields < startShields);

                if (!posChanged && !shieldUsed) {
                    return; 
                }

                grid.repaint();
                updateScorePanel();
                model.checkEndGame();
                if (model.gameOver) {
                    endGame();
                    return;
                }

                SwingUtilities.invokeLater(() -> {
                    if (model.gameOver) return;

                    Direction cpuDir = Greedy.choose(model, difficulty);
                    if (cpuDir != null) {
                        model.move(false, cpuDir);
                    }
                    
                    grid.repaint();
                    updateScorePanel();
                    model.checkEndGame();

                    if (model.gameOver) {
                        endGame();
                    }
                });
            }
        });

        setVisible(true);
    }

    private void setupUI() {
        mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBackground(new Color(240, 240, 245));
        mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("INERTIA", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
        titleLabel.setForeground(new Color(50, 50, 80));
        topPanel.add(titleLabel, BorderLayout.NORTH);

        statusLabel = new JLabel("Difficulty: " + difficulty, SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        statusLabel.setForeground(new Color(100, 100, 120));
        topPanel.add(statusLabel, BorderLayout.CENTER);

        scorePanel = createScorePanel();
        topPanel.add(scorePanel, BorderLayout.SOUTH);

        JPanel gridWrapper = new JPanel(new GridBagLayout());
        gridWrapper.setOpaque(false);
        gridWrapper.add(grid);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        buttonPanel.setOpaque(false);
        
        JButton restartBtn = createStyledButton("New Game", new Color(70, 130, 180));
        JButton instructionsBtn = createStyledButton("Instructions", new Color(100, 150, 100));
        JButton exitBtn = createStyledButton("Exit", new Color(180, 100, 100));

        restartBtn.addActionListener(e -> {
            dispose();
            new InertiaGameFrame(difficulty);
        });

        instructionsBtn.addActionListener(e -> showInstructions());
        exitBtn.addActionListener(e -> System.exit(0));

        buttonPanel.add(restartBtn);
        buttonPanel.add(instructionsBtn);
        buttonPanel.add(exitBtn);

        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(gridWrapper, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(mainPanel);
        pack();
    }

    private JPanel createScorePanel() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 20, 0));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(10, 50, 10, 50));

        JPanel humanPanel = createPlayerPanel("HUMAN", Color.GREEN, model.humanScore, model.humanShields);
        JPanel cpuPanel = createPlayerPanel("CPU", Color.RED, model.cpuScore, model.cpuShields);

        panel.add(humanPanel);
        panel.add(cpuPanel);

        return panel;
    }

    private JPanel createPlayerPanel(String name, Color color, int score, int shields) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(color, 2, true),
            new EmptyBorder(10, 15, 10, 15)
        ));

        JLabel nameLabel = new JLabel(name);
        nameLabel.setFont(new Font("Arial", Font.BOLD, 16));
        nameLabel.setForeground(color);
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel scoreLabel = new JLabel("Gems: " + score);
        scoreLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        scoreLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel shieldLabel = new JLabel("🛡️ Shields: " + shields);
        shieldLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        shieldLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(nameLabel);
        panel.add(Box.createRigidArea(new Dimension(0, 5)));
        panel.add(scoreLabel);
        panel.add(shieldLabel);

        return panel;
    }

    private void updateScorePanel() {
        scorePanel.removeAll();
        
        JPanel humanPanel = createPlayerPanel("HUMAN", Color.GREEN, model.humanScore, model.humanShields);
        JPanel cpuPanel = createPlayerPanel("CPU", Color.RED, model.cpuScore, model.cpuShields);

        scorePanel.add(humanPanel);
        scorePanel.add(cpuPanel);
        
        scorePanel.revalidate();
        scorePanel.repaint();
    }

    private JButton createStyledButton(String text, Color bg) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 12));
        button.setBackground(bg);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setPreferredSize(new Dimension(120, 35));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                button.setBackground(bg.brighter());
            }
            public void mouseExited(MouseEvent e) {
                button.setBackground(bg);
            }
        });
        
        return button;
    }

    private void endGame() {
        javax.swing.Timer timer = new javax.swing.Timer(500, e -> showGameOverDialog());
        timer.setRepeats(false);
        timer.start();
    }

    private void showGameOverDialog() {
        JDialog dialog = new JDialog(this, "Game Over", true);
        dialog.setLayout(new BorderLayout(15, 15));
        dialog.setBackground(new Color(240, 240, 245));

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(new Color(240, 240, 245));
        contentPanel.setBorder(new EmptyBorder(30, 40, 20, 40));

        JLabel titleLabel = new JLabel("GAME OVER");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(new Color(50, 50, 80));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel resultLabel = new JLabel(model.gameResult);
        resultLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        resultLabel.setForeground(new Color(100, 100, 120));
        resultLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel scoreLabel = new JLabel(String.format("Final Score - Human: %d | CPU: %d", 
            model.humanScore, model.cpuScore));
        scoreLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        scoreLabel.setForeground(new Color(100, 100, 120));
        scoreLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        contentPanel.add(titleLabel);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        contentPanel.add(resultLabel);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        contentPanel.add(scoreLabel);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 15));
        buttonPanel.setOpaque(false);

        JButton newGameBtn = createStyledButton("New Game", new Color(70, 130, 180));
        JButton menuBtn = createStyledButton("Main Menu", new Color(100, 150, 100));
        JButton exitBtn = createStyledButton("Exit", new Color(180, 100, 100));

        newGameBtn.addActionListener(e -> {
            dialog.dispose();
            dispose();
            new InertiaGameFrame(difficulty);
        });

        menuBtn.addActionListener(e -> {
            dialog.dispose();
            dispose();
            menu();
        });

        exitBtn.addActionListener(e -> System.exit(0));

        buttonPanel.add(newGameBtn);
        buttonPanel.add(menuBtn);
        buttonPanel.add(exitBtn);

        dialog.add(contentPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void showInstructions() {
        JDialog dialog = new JDialog(this, "Instructions", true);
        dialog.setLayout(new BorderLayout());
        
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(new Color(240, 240, 245));
        contentPanel.setBorder(new EmptyBorder(20, 30, 20, 30));

        String[] instructions = {
            "HOW TO PLAY",
            "",
            "• Click in a direction to slide until you hit a wall or stop marker (⭕)",
            "• Collect cyan gems (◆) to score points",
            "• Avoid black mines (●) - they kill you instantly!",
            "• Collect blue shields (🛡️) for protection",
            "• If you hit a mine with a shield, the shield breaks but you survive",
            "",
            "GOAL",
            "",
            "• Collect more gems than the CPU before all gems are gone",
            "• The game ends when all gems are collected or no moves remain",
            "",
            "DIFFICULTY LEVELS",
            "",
            "• Easy",
            "• Medium",
            "• Hard"
        };

        for (String line : instructions) {
            JLabel label = new JLabel(line);
            if (line.equals("HOW TO PLAY") || line.equals("GOAL") || line.equals("DIFFICULTY LEVELS")) {
                label.setFont(new Font("Arial", Font.BOLD, 16));
                label.setForeground(new Color(50, 50, 80));
            } else {
                label.setFont(new Font("Arial", Font.PLAIN, 13));
                label.setForeground(new Color(80, 80, 100));
            }
            label.setAlignmentX(Component.LEFT_ALIGNMENT);
            contentPanel.add(label);
        }

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setOpaque(false);
        JButton okBtn = createStyledButton("Got It!", new Color(70, 130, 180));
        okBtn.addActionListener(e -> dialog.dispose());
        buttonPanel.add(okBtn);

        dialog.add(contentPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    public static void menu() {
        JFrame menuFrame = new JFrame("Inertia - Main Menu");
        menuFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        menuFrame.setSize(600, 550);
        menuFrame.setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(new Color(245, 245, 250));

        // Header Panel with gradient background
        JPanel headerPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                GradientPaint gp = new GradientPaint(0, 0, new Color(80, 80, 90), 
                                                     0, getHeight(), new Color(100, 100, 110));
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        headerPanel.setPreferredSize(new Dimension(600, 180));
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setBorder(new EmptyBorder(30, 40, 30, 40));

        JLabel titleLabel = new JLabel("INERTIA");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 56));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitleLabel = new JLabel("Slide, Collect, Survive");
        subtitleLabel.setFont(new Font("Arial", Font.PLAIN, 18));
        subtitleLabel.setForeground(new Color(230, 240, 255));
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel descLabel = new JLabel("A strategic sliding puzzle game");
        descLabel.setFont(new Font("Arial", Font.ITALIC, 14));
        descLabel.setForeground(new Color(200, 220, 240));
        descLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        headerPanel.add(Box.createVerticalGlue());
        headerPanel.add(titleLabel);
        headerPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        headerPanel.add(subtitleLabel);
        headerPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        headerPanel.add(descLabel);
        headerPanel.add(Box.createVerticalGlue());

        // Center Panel with buttons
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBackground(new Color(245, 245, 250));
        centerPanel.setBorder(new EmptyBorder(30, 80, 30, 80));

        JLabel selectLabel = new JLabel("Select Difficulty:");
        selectLabel.setFont(new Font("Arial", Font.BOLD, 16));
        selectLabel.setForeground(new Color(60, 60, 80));
        selectLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        centerPanel.add(selectLabel);
        centerPanel.add(Box.createRigidArea(new Dimension(0, 15)));

        JButton easyBtn = createMenuButton("🟢 Easy", new Color(100, 200, 100));
        JButton mediumBtn = createMenuButton("🟠 Medium", new Color(255, 180, 60));
        JButton hardBtn = createMenuButton("🔴 Hard", new Color(230, 90, 90));
        
        centerPanel.add(easyBtn);
        centerPanel.add(Box.createRigidArea(new Dimension(0, 12)));
        centerPanel.add(mediumBtn);
        centerPanel.add(Box.createRigidArea(new Dimension(0, 12)));
        centerPanel.add(hardBtn);
        centerPanel.add(Box.createRigidArea(new Dimension(0, 20)));

        // Bottom buttons
        JPanel bottomButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        bottomButtonPanel.setOpaque(false);
        
        JButton instructionsBtn = createSmallMenuButton("📖 How to Play", new Color(80, 120, 180));
        JButton exitBtn = createSmallMenuButton("❌ Exit", new Color(130, 130, 140));

        bottomButtonPanel.add(instructionsBtn);
        bottomButtonPanel.add(exitBtn);
        
        centerPanel.add(bottomButtonPanel);

        easyBtn.addActionListener(e -> {
            menuFrame.dispose();
            new InertiaGameFrame(Difficulty.EASY);
        });

        mediumBtn.addActionListener(e -> {
            menuFrame.dispose();
            new InertiaGameFrame(Difficulty.MEDIUM);
        });

        hardBtn.addActionListener(e -> {
            menuFrame.dispose();
            new InertiaGameFrame(Difficulty.HARD);
        });

        instructionsBtn.addActionListener(e -> {
            InertiaGameFrame tempFrame = new InertiaGameFrame(Difficulty.MEDIUM);
            tempFrame.setVisible(false);
            tempFrame.showInstructions();
            tempFrame.dispose();
        });

        exitBtn.addActionListener(e -> System.exit(0));

        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(centerPanel, BorderLayout.CENTER);
        menuFrame.add(mainPanel);
        menuFrame.setVisible(true);
    }

    private static JButton createMenuButton(String text, Color bg) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 18));
        button.setBackground(bg);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setMaximumSize(new Dimension(350, 50));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(bg.darker(), 0),
            BorderFactory.createEmptyBorder(10, 20, 10, 20)
        ));
        
        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                button.setBackground(bg.brighter());
                button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Color.WHITE, 2),
                    BorderFactory.createEmptyBorder(10, 20, 10, 20)
                ));
            }
            public void mouseExited(MouseEvent e) {
                button.setBackground(bg);
                button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(bg.darker(), 0),
                    BorderFactory.createEmptyBorder(10, 20, 10, 20)
                ));
            }
        });
        
        return button;
    }

    private static JButton createSmallMenuButton(String text, Color bg) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.PLAIN, 13));
        button.setBackground(bg);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setPreferredSize(new Dimension(140, 35));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                button.setBackground(bg.brighter());
            }
            public void mouseExited(MouseEvent e) {
                button.setBackground(bg);
            }
        });
        
        return button;
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        SwingUtilities.invokeLater(InertiaGameFrame::menu);
    }
}