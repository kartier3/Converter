package com.example;

import com.formdev.flatlaf.FlatIntelliJLaf;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;

public class ImageToPdfGui extends JFrame {

    private final JPanel imagesPanel;
    private final List<ImageItem> imageItems = new ArrayList<>();
    private final JLabel statusLabel;
    private final JTextField outputField;
    private final JLabel dropOverlayLabel;
    private final Timer animationTimer;
    private float dropOverlayAlpha = 0f;
    private boolean isDropActive = false;

    private JDialog hiddenDialog;
    private boolean isHidden = false;
    private final Map<File, ImageIcon> thumbnailCache = new HashMap<>();

    public ImageToPdfGui() {
        setTitle("Image to PDF Converter");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(700, 550);
        setLocationRelativeTo(null);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (!isHidden) hideToCorner();
                else System.exit(0);
            }
        });

        // Animation timer for smooth transitions
        animationTimer = new Timer(16, e -> updateAnimations());
        animationTimer.start();

        // Main panel
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        mainPanel.setOpaque(false);

        // Top with hide button
        JLabel titleLabel = new JLabel("Image to PDF Converter", JLabel.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        titleLabel.setForeground(new Color(88, 28, 135));

        JButton hideButton = createStyledButton("− Hide", new Color(147, 51, 234));
        hideButton.setPreferredSize(new Dimension(70, 28));
        hideButton.addActionListener(e -> hideToCorner());

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        topPanel.add(titleLabel, BorderLayout.CENTER);
        topPanel.add(hideButton, BorderLayout.EAST);

        // Images panel with thumbnails - using FlowLayout for grid effect
        imagesPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        imagesPanel.setBackground(new Color(250, 245, 255, 200));
        imagesPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(168, 85, 247, 100), 2),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        JScrollPane scrollPane = new JScrollPane(imagesPanel);
        scrollPane.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(147, 51, 234)),
            "Drop images here",
            javax.swing.border.TitledBorder.CENTER,
            javax.swing.border.TitledBorder.TOP,
            new Font("Segoe UI", Font.BOLD, 12),
            new Color(147, 51, 234)
        ));
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);

        // Drop overlay
        dropOverlayLabel = new JLabel("📁 DROP FILES HERE", JLabel.CENTER);
        dropOverlayLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        dropOverlayLabel.setForeground(new Color(147, 51, 234));
        dropOverlayLabel.setBackground(new Color(250, 245, 255, 220));
        dropOverlayLabel.setOpaque(true);
        dropOverlayLabel.setBorder(BorderFactory.createDashedBorder(new Color(147, 51, 234), 3, 5));
        dropOverlayLabel.setVisible(false);

        // Layered pane for drop overlay
        JLayeredPane layeredPane = new JLayeredPane();
        layeredPane.setPreferredSize(new Dimension(600, 300));
        scrollPane.setBounds(0, 0, 600, 300);
        dropOverlayLabel.setBounds(50, 50, 500, 200);
        layeredPane.add(scrollPane, JLayeredPane.DEFAULT_LAYER);
        layeredPane.add(dropOverlayLabel, JLayeredPane.PALETTE_LAYER);

        // Enable drag & drop on both
        new DropTarget(scrollPane, new FileDropTargetListener());
        new DropTarget(imagesPanel, new FileDropTargetListener());
        new DropTarget(dropOverlayLabel, new FileDropTargetListener());

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        buttonPanel.setOpaque(false);
        JButton addButton = createStyledButton("+ Add Files", new Color(59, 130, 246));
        JButton removeButton = createStyledButton("− Remove", new Color(239, 68, 68));
        JButton clearButton = createStyledButton("× Clear All", new Color(107, 114, 128));

        addButton.addActionListener(e -> addFiles());
        removeButton.addActionListener(e -> removeSelected());
        clearButton.addActionListener(e -> clearAll());

        buttonPanel.add(addButton);
        buttonPanel.add(removeButton);
        buttonPanel.add(clearButton);

        JPanel centerPanel = new JPanel(new BorderLayout(5, 5));
        centerPanel.setOpaque(false);
        centerPanel.add(layeredPane, BorderLayout.CENTER);
        centerPanel.add(buttonPanel, BorderLayout.SOUTH);

        // Bottom panel
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 10));
        bottomPanel.setOpaque(false);

        outputField = new JTextField(30);
        outputField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        outputField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(147, 51, 234, 100)),
            BorderFactory.createEmptyBorder(5, 8, 5, 8)
        ));

        JButton browseButton = createStyledButton("Browse...", new Color(107, 114, 128));
        browseButton.addActionListener(e -> browseOutput());

        JButton clearOutputButton = createStyledButton("Clear", new Color(239, 68, 68));
        clearOutputButton.setPreferredSize(new Dimension(60, 28));
        clearOutputButton.addActionListener(e -> {
            outputField.setText("");
            showToast("Output path cleared", new Color(107, 114, 128));
        });

        JPanel outputButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        outputButtonPanel.setOpaque(false);
        outputButtonPanel.add(browseButton);
        outputButtonPanel.add(clearOutputButton);

        JPanel outputPanel = new JPanel(new BorderLayout(5, 0));
        outputPanel.setOpaque(false);
        outputPanel.add(new JLabel("Output: "), BorderLayout.WEST);
        outputPanel.add(outputField, BorderLayout.CENTER);
        outputPanel.add(outputButtonPanel, BorderLayout.EAST);

        statusLabel = new JLabel("👋 Welcome! Drag & drop images here or click 'Add Files' to begin");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusLabel.setForeground(new Color(107, 114, 128));

        JButton convertButton = createStyledButton("⚡ Convert to PDF", new Color(147, 51, 234));
        convertButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        convertButton.setPreferredSize(new Dimension(180, 42));
        convertButton.addActionListener(e -> convert());

        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusPanel.setOpaque(false);
        statusPanel.add(statusLabel);

        JPanel convertPanel = new JPanel(new BorderLayout(10, 5));
        convertPanel.setOpaque(false);
        convertPanel.add(convertButton, BorderLayout.EAST);
        convertPanel.add(statusPanel, BorderLayout.CENTER);

        bottomPanel.add(outputPanel, BorderLayout.NORTH);
        bottomPanel.add(convertPanel, BorderLayout.SOUTH);

        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(centerPanel, BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        // Background with gradient
        JPanel bgPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Purple gradient background
                GradientPaint gp = new GradientPaint(0, 0, new Color(253, 244, 255), 0, getHeight(), new Color(243, 232, 255));
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, getWidth(), getHeight());
                // Subtle purple glow border
                g2d.setColor(new Color(147, 51, 234, 30));
                g2d.setStroke(new BasicStroke(2));
                g2d.drawRoundRect(5, 5, getWidth()-10, getHeight()-10, 20, 20);
                g2d.dispose();
            }
        };
        bgPanel.setOpaque(false);
        bgPanel.add(mainPanel, BorderLayout.CENTER);

        add(bgPanel);

        // Keyboard shortcuts
        setupKeyboardShortcuts();
    }

    private void setupKeyboardShortcuts() {
        InputMap inputMap = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getRootPane().getActionMap();

        // Ctrl+O: Add files
        inputMap.put(KeyStroke.getKeyStroke("control O"), "addFiles");
        actionMap.put("addFiles", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addFiles();
            }
        });

        // Delete: Remove selected
        inputMap.put(KeyStroke.getKeyStroke("DELETE"), "removeSelected");
        actionMap.put("removeSelected", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                removeSelected();
            }
        });

        // Ctrl+Delete: Clear all
        inputMap.put(KeyStroke.getKeyStroke("control DELETE"), "clearAll");
        actionMap.put("clearAll", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clearAll();
            }
        });

        // Ctrl+B: Browse output
        inputMap.put(KeyStroke.getKeyStroke("control B"), "browseOutput");
        actionMap.put("browseOutput", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                browseOutput();
            }
        });

        // Ctrl+Enter: Convert
        inputMap.put(KeyStroke.getKeyStroke("control ENTER"), "convert");
        actionMap.put("convert", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                convert();
            }
        });

        // Escape: Hide to corner
        inputMap.put(KeyStroke.getKeyStroke("ESCAPE"), "hideToCorner");
        actionMap.put("hideToCorner", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                hideToCorner();
            }
        });
    }

    private JButton createStyledButton(String text, Color baseColor) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setForeground(Color.WHITE);
        btn.setBackground(baseColor);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(text.length() > 10 ? 140 : 100, 32));
        
        // Hover effect
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(baseColor.brighter());
            }
            @Override
            public void mouseExited(MouseEvent e) {
                btn.setBackground(baseColor);
            }
        });
        return btn;
    }

    private void updateAnimations() {
        // Smooth fade for drop overlay
        if (isDropActive && dropOverlayAlpha < 1f) {
            dropOverlayAlpha += 0.1f;
            if (dropOverlayAlpha > 1f) dropOverlayAlpha = 1f;
            dropOverlayLabel.setBackground(new Color(250, 245, 255, (int)(220 * dropOverlayAlpha)));
        } else if (!isDropActive && dropOverlayAlpha > 0f) {
            dropOverlayAlpha -= 0.1f;
            if (dropOverlayAlpha < 0f) {
                dropOverlayAlpha = 0f;
                dropOverlayLabel.setVisible(false);
            } else {
                dropOverlayLabel.setBackground(new Color(250, 245, 255, (int)(220 * dropOverlayAlpha)));
            }
        }
    }

    private void showDropOverlay() {
        isDropActive = true;
        dropOverlayLabel.setVisible(true);
    }

    private void hideDropOverlay() {
        isDropActive = false;
    }

    private void hideToCorner() {
        if (hiddenDialog == null) {
            hiddenDialog = new JDialog();
            hiddenDialog.setUndecorated(true);
            hiddenDialog.setSize(70, 70);
            hiddenDialog.setShape(new RoundRectangle2D.Double(0, 0, 70, 70, 20, 20));

            JPanel orbPanel = new JPanel(new BorderLayout());
            orbPanel.setBackground(new Color(147, 51, 234));
            orbPanel.setBorder(BorderFactory.createLineBorder(new Color(168, 85, 247), 3));

            JLabel icon = new JLabel("⚡", JLabel.CENTER);
            icon.setFont(new Font("Segoe UI", Font.BOLD, 28));
            icon.setForeground(Color.WHITE);

            orbPanel.add(icon, BorderLayout.CENTER);
            hiddenDialog.add(orbPanel);

            // Pulse animation
            Timer pulseTimer = new Timer(1000, new AbstractAction() {
                int scale = 0;
                @Override
                public void actionPerformed(ActionEvent e) {
                    scale = (scale + 1) % 2;
                    orbPanel.setBorder(BorderFactory.createLineBorder(
                        scale == 0 ? new Color(168, 85, 247) : new Color(200, 150, 255), 3));
                }
            });
            pulseTimer.start();

            hiddenDialog.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) { restoreWindow(); }
            });
        }

        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        hiddenDialog.setLocation(screen.width - 100, screen.height - 120);

        // Animate out
        Timer fadeOut = new Timer(16, null);
        final float[] alpha = {1f};
        fadeOut.addActionListener(e -> {
            alpha[0] -= 0.1f;
            if (alpha[0] <= 0) {
                fadeOut.stop();
                isHidden = true;
                setVisible(false);
                hiddenDialog.setVisible(true);
            }
        });
        fadeOut.start();
    }

    private void restoreWindow() {
        isHidden = false;
        hiddenDialog.setVisible(false);
        setVisible(true);
        toFront();

        // Animate in
        Timer fadeIn = new Timer(16, null);
        final float[] alpha = {0f};
        fadeIn.addActionListener(e -> {
            alpha[0] += 0.1f;
            if (alpha[0] >= 1) fadeIn.stop();
        });
        fadeIn.start();
    }

    private void addFiles() {
        ModernFileDialog dialog = new ModernFileDialog(this, false, null);
        dialog.setMultiSelect(true);
        dialog.setFileFilter("Images (JPG, PNG, GIF, BMP, TIFF, WEBP)", 
            new String[]{"jpg", "jpeg", "png", "gif", "bmp", "tiff", "tif", "webp"});
        dialog.setVisible(true);
        
        File[] files = dialog.getSelectedFiles();
        if (files != null) {
            for (File file : files) {
                addFileAnimated(file);
            }
        }
    }

    private void addFileAnimated(File file) {
        if (imageItems.stream().anyMatch(item -> item.file.equals(file))) return;

        ImageItem item = new ImageItem(file);
        imageItems.add(item);
        imagesPanel.add(item.panel);

        // Animate item appearing
        item.panel.setScale(0);
        Timer scaleTimer = new Timer(16, null);
        final float[] scale = {0f};
        scaleTimer.addActionListener(e -> {
            scale[0] += 0.1f;
            if (scale[0] >= 1f) {
                scale[0] = 1f;
                scaleTimer.stop();
            }
            item.panel.setScale(scale[0]);
            imagesPanel.revalidate();
            imagesPanel.repaint();
        });
        scaleTimer.start();

        updateStatus();
    }

    private void removeSelected() {
        imageItems.removeIf(item -> {
            if (item.isSelected) {
                // Animate out
                Timer fadeOut = new Timer(16, null);
                final float[] alpha = {1f};
                fadeOut.addActionListener(e -> {
                    alpha[0] -= 0.15f;
                    if (alpha[0] <= 0) {
                        alpha[0] = 0;
                        fadeOut.stop();
                        imagesPanel.remove(item.panel);
                        imagesPanel.revalidate();
                        imagesPanel.repaint();
                    }
                    item.panel.setAlpha(alpha[0]);
                });
                fadeOut.start();
                return true;
            }
            return false;
        });
        updateStatus();
    }

    private void clearAll() {
        // Animate all out
        Timer fadeOut = new Timer(16, null);
        final float[] alpha = {1f};
        fadeOut.addActionListener(e -> {
            alpha[0] -= 0.1f;
            if (alpha[0] <= 0) {
                alpha[0] = 0;
                fadeOut.stop();
                imageItems.clear();
                imagesPanel.removeAll();
                imagesPanel.revalidate();
                imagesPanel.repaint();
                updateStatus();
            }
            for (ImageItem item : imageItems) {
                item.panel.setAlpha(alpha[0]);
            }
        });
        fadeOut.start();
    }

    private void browseOutput() {
        ModernFileDialog dialog = new ModernFileDialog(this, true, "output.pdf");
        dialog.setVisible(true);
        String path = dialog.getSelectedPath();
        if (path != null && !path.isEmpty()) {
            if (!path.toLowerCase().endsWith(".pdf")) path += ".pdf";
            outputField.setText(path);
        }
    }

    private void updateStatus() {
        int count = imageItems.size();
        if (count == 0) {
            statusLabel.setText("👋 Welcome! Drag & drop images here or click 'Add Files' to begin");
            statusLabel.setForeground(new Color(107, 114, 128));
        } else {
            statusLabel.setText("📷 " + count + " image" + (count == 1 ? "" : "s") + " ready • Choose output location and click Convert");
            statusLabel.setForeground(new Color(147, 51, 234));
        }
    }

    private void convert() {
        if (imageItems.isEmpty()) {
            showToast("Please add images first", Color.ORANGE);
            return;
        }

        String outputPath = outputField.getText().trim();
        if (outputPath.isEmpty()) {
            showToast("Please specify output PDF", Color.ORANGE);
            return;
        }

        setEnabled(false);
        statusLabel.setText("⏳ Converting " + imageItems.size() + " image" + (imageItems.size() == 1 ? "" : "s") + " to PDF...");

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                List<String> paths = new ArrayList<>();
                for (ImageItem item : imageItems) {
                    paths.add(item.file.getAbsolutePath());
                }
                JpgToPdfConverter.convertImagesToPdf(paths, outputPath);
                return null;
            }

            @Override
            protected void done() {
                setEnabled(true);
                try {
                    get();
                    statusLabel.setText("✅ Success! PDF created at: " + new File(outputPath).getName());
                    statusLabel.setForeground(new Color(34, 197, 94));
                    showToast("PDF created successfully!", new Color(34, 197, 94));
                } catch (Exception e) {
                    statusLabel.setText("❌ Error: " + e.getCause().getMessage());
                    statusLabel.setForeground(Color.RED);
                    showToast("Conversion failed: " + e.getCause().getMessage(), Color.RED);
                }
            }
        };
        worker.execute();
    }

    private void showToast(String message, Color color) {
        JDialog toast = new JDialog(this, false);
        toast.setUndecorated(true);
        toast.setSize(300, 50);

        JLabel label = new JLabel(message, JLabel.CENTER);
        label.setFont(new Font("Segoe UI", Font.BOLD, 14));
        label.setForeground(Color.WHITE);
        label.setBackground(color);
        label.setOpaque(true);
        label.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        toast.add(label);
        toast.setLocationRelativeTo(this);
        toast.setLocation(toast.getX(), toast.getY() + 100);

        // Animate in and out
        Timer showTimer = new Timer(2000, e -> {
            Timer fadeOut = new Timer(50, null);
            final float[] alpha = {1f};
            fadeOut.addActionListener(e2 -> {
                alpha[0] -= 0.1f;
                if (alpha[0] <= 0) {
                    fadeOut.stop();
                    toast.dispose();
                }
                toast.setOpacity(alpha[0]);
            });
            fadeOut.start();
        });
        showTimer.setRepeats(false);
        showTimer.start();

        toast.setOpacity(0f);
        toast.setVisible(true);

        Timer fadeIn = new Timer(16, null);
        final float[] alpha = {0f};
        fadeIn.addActionListener(e -> {
            alpha[0] += 0.1f;
            if (alpha[0] >= 1) fadeIn.stop();
            toast.setOpacity(alpha[0]);
        });
        fadeIn.start();
    }

    // Custom image item with thumbnail
    private class ImageItem {
        final File file;
        final ScalablePanel panel;
        boolean isSelected = false;

        ImageItem(File file) {
            this.file = file;
            this.panel = new ScalablePanel();
            panel.setLayout(new BorderLayout());
            panel.setPreferredSize(new Dimension(100, 120));
            panel.setBackground(new Color(255, 255, 255, 200));
            panel.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 1));

            // Load thumbnail async
            SwingWorker<ImageIcon, Void> worker = new SwingWorker<>() {
                @Override
                protected ImageIcon doInBackground() throws Exception {
                    if (thumbnailCache.containsKey(file)) {
                        return thumbnailCache.get(file);
                    }
                    BufferedImage img = ImageIO.read(file);
                    if (img != null) {
                        Image scaled = img.getScaledInstance(90, 90, Image.SCALE_SMOOTH);
                        ImageIcon icon = new ImageIcon(scaled);
                        thumbnailCache.put(file, icon);
                        return icon;
                    }
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        ImageIcon icon = get();
                        if (icon != null) {
                            JLabel imgLabel = new JLabel(icon);
                            imgLabel.setHorizontalAlignment(JLabel.CENTER);
                            panel.add(imgLabel, BorderLayout.CENTER);
                        } else {
                            panel.add(new JLabel("📷", JLabel.CENTER), BorderLayout.CENTER);
                        }
                    } catch (Exception e) {
                        panel.add(new JLabel("📷", JLabel.CENTER), BorderLayout.CENTER);
                    }
                    panel.revalidate();
                    panel.repaint();
                }
            };
            worker.execute();

            // Filename label
            String name = file.getName();
            if (name.length() > 15) name = name.substring(0, 12) + "...";
            JLabel nameLabel = new JLabel(name, JLabel.CENTER);
            nameLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            panel.add(nameLabel, BorderLayout.SOUTH);

            // Selection click
            panel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    isSelected = !isSelected;
                    panel.setBackground(isSelected ? new Color(168, 85, 247, 100) : new Color(255, 255, 255, 200));
                    panel.setBorder(BorderFactory.createLineBorder(
                        isSelected ? new Color(147, 51, 234) : new Color(200, 200, 200), 2));
                }
            });

            panel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        }
    }

    // Panel that supports scaling and alpha for animations
    private static class ScalablePanel extends JPanel {
        private float scale = 1f;
        private float alpha = 1f;

        void setScale(float scale) {
            this.scale = scale;
        }

        void setAlpha(float alpha) {
            this.alpha = alpha;
            setOpaque(alpha >= 0.9f);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            
            if (scale != 1f) {
                int w = getWidth();
                int h = getHeight();
                int newW = (int)(w * scale);
                int newH = (int)(h * scale);
                int x = (w - newW) / 2;
                int y = (h - newH) / 2;
                g2d.translate(x, y);
                g2d.scale(scale, scale);
            }
            
            super.paintComponent(g2d);
            g2d.dispose();
        }
    }

    // Drag & Drop listener with overlay effects
    private class FileDropTargetListener extends DropTargetAdapter {
        @Override
        public void dragEnter(DropTargetDragEvent event) {
            event.acceptDrag(DnDConstants.ACTION_COPY);
            showDropOverlay();
        }

        @Override
        public void dragExit(DropTargetEvent event) {
            hideDropOverlay();
        }

        @Override
        public void drop(DropTargetDropEvent event) {
            hideDropOverlay();
            event.acceptDrop(DnDConstants.ACTION_COPY);
            Transferable transferable = event.getTransferable();

            try {
                if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    @SuppressWarnings("unchecked")
                    List<File> files = (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);
                    for (File file : files) {
                        if (file.isFile()) {
                            addFileAnimated(file);
                        } else if (file.isDirectory()) {
                            addFilesFromDirectory(file);
                        }
                    }
                }
                event.dropComplete(true);
            } catch (Exception e) {
                event.dropComplete(false);
            }
        }

        private void addFilesFromDirectory(File directory) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isFile()) {
                        String name = f.getName().toLowerCase();
                        if (name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") ||
                            name.endsWith(".gif") || name.endsWith(".bmp") || name.endsWith(".tiff") ||
                            name.endsWith(".tif") || name.endsWith(".webp")) {
                            addFileAnimated(f);
                        }
                    }
                }
            }
        }
    }

    // Modern styled file dialog
    private class ModernFileDialog extends JDialog {
        private final boolean saveMode;
        private String selectedPath = null;
        private File[] selectedFiles = null;
        private final JTextField filenameField;
        private final JList<FileItem> fileList;
        private final DefaultListModel<FileItem> fileListModel;
        private final JTree folderTree;
        private DefaultMutableTreeNode treeRoot;
        private File currentDir;
        private boolean multiSelect = false;
        private String filterName = "All Files";
        private String[] extensions = null;
        private JLabel pathLabel;
        private boolean isBuildingTree = false;

        ModernFileDialog(JFrame parent, boolean saveMode, String defaultName) {
            super(parent, saveMode ? "Save PDF" : "Select Images", true);
            this.saveMode = saveMode;
            this.currentDir = new File(System.getProperty("user.home"));
            
            setSize(750, 500);
            setLocationRelativeTo(parent);
            
            JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
            mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
            mainPanel.setBackground(new Color(253, 244, 255));

            // Header with path
            JPanel headerPanel = new JPanel(new BorderLayout(5, 0));
            headerPanel.setOpaque(false);
            
            pathLabel = new JLabel("Location: " + currentDir.getAbsolutePath());
            pathLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            pathLabel.setForeground(new Color(107, 114, 128));
            
            JButton upButton = createStyledButton("↑ Up", new Color(107, 114, 128));
            upButton.setPreferredSize(new Dimension(60, 28));
            upButton.addActionListener(e -> goUp());
            
            headerPanel.add(pathLabel, BorderLayout.CENTER);
            headerPanel.add(upButton, BorderLayout.EAST);
            
            // Split pane: tree on left, file list on right
            JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
            splitPane.setDividerLocation(200);
            splitPane.setBorder(null);
            splitPane.setOpaque(false);
            
            // Folder tree
            treeRoot = new DefaultMutableTreeNode("Computer");
            folderTree = new JTree(treeRoot);
            folderTree.setRootVisible(true);
            folderTree.setShowsRootHandles(true);
            folderTree.setCellRenderer(new FolderTreeRenderer());
            folderTree.setBackground(new Color(250, 250, 255));
            folderTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
            
            JScrollPane treeScroll = new JScrollPane(folderTree);
            treeScroll.setBorder(BorderFactory.createLineBorder(new Color(168, 85, 247, 100)));
            treeScroll.setOpaque(false);
            treeScroll.getViewport().setOpaque(false);
            
            // File list
            fileListModel = new DefaultListModel<>();
            fileList = new JList<>(fileListModel);
            fileList.setCellRenderer(new FileCellRenderer());
            fileList.setBackground(new Color(255, 255, 255, 220));
            fileList.setSelectionMode(multiSelect ? ListSelectionModel.MULTIPLE_INTERVAL_SELECTION : ListSelectionModel.SINGLE_SELECTION);
            
            JScrollPane fileScroll = new JScrollPane(fileList);
            fileScroll.setBorder(BorderFactory.createLineBorder(new Color(168, 85, 247, 100)));
            fileScroll.setOpaque(false);
            fileScroll.getViewport().setOpaque(false);
            
            splitPane.setLeftComponent(treeScroll);
            splitPane.setRightComponent(fileScroll);
            
            // Tree navigation
            folderTree.addTreeSelectionListener(e -> {
                if (isBuildingTree) return;
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) folderTree.getLastSelectedPathComponent();
                if (node != null && node.getUserObject() instanceof FileNode) {
                    FileNode fileNode = (FileNode) node.getUserObject();
                    if (fileNode.file.isDirectory()) {
                        currentDir = fileNode.file;
                        loadFileList(fileNode.file);
                    }
                }
            });
            
            // Double-click file selection or folder navigation
            fileList.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        FileItem item = fileList.getSelectedValue();
                        if (item == null) return;
                        
                        if (item.file != null && item.file.isDirectory()) {
                            // Navigate into folder
                            loadDirectory(item.file);
                        } else if (item.file != null && !item.file.isDirectory() && !saveMode) {
                            // Select file
                            selectAndClose();
                        }
                    }
                }
            });

            // Bottom controls
            JPanel bottomPanel = new JPanel(new BorderLayout(5, 10));
            bottomPanel.setOpaque(false);
            
            filenameField = new JTextField(defaultName != null ? defaultName : "");
            filenameField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            filenameField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(168, 85, 247, 150)),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
            ));
            filenameField.setBackground(new Color(255, 255, 255, 240));
            
            JLabel filenameLabel = new JLabel(saveMode ? "Filename: " : "Selected: ");
            filenameLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            
            JPanel filenamePanel = new JPanel(new BorderLayout(5, 0));
            filenamePanel.setOpaque(false);
            filenamePanel.add(filenameLabel, BorderLayout.WEST);
            filenamePanel.add(filenameField, BorderLayout.CENTER);
            
            // Buttons
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
            buttonPanel.setOpaque(false);
            
            JButton cancelButton = createStyledButton("Cancel", new Color(107, 114, 128));
            cancelButton.addActionListener(e -> dispose());
            
            JButton selectButton = createStyledButton(saveMode ? "Save" : "Select", new Color(147, 51, 234));
            selectButton.setPreferredSize(new Dimension(100, 32));
            selectButton.addActionListener(e -> selectAndClose());
            
            buttonPanel.add(cancelButton);
            buttonPanel.add(selectButton);
            
            bottomPanel.add(filenamePanel, BorderLayout.NORTH);
            bottomPanel.add(buttonPanel, BorderLayout.SOUTH);
            
            mainPanel.add(headerPanel, BorderLayout.NORTH);
            mainPanel.add(splitPane, BorderLayout.CENTER);
            mainPanel.add(bottomPanel, BorderLayout.SOUTH);
            
            add(mainPanel);
            loadDirectory(currentDir);
            
            getRootPane().setDefaultButton(selectButton);
        }

        void setMultiSelect(boolean multi) {
            this.multiSelect = multi;
            fileList.setSelectionMode(multi ? ListSelectionModel.MULTIPLE_INTERVAL_SELECTION : ListSelectionModel.SINGLE_SELECTION);
        }

        void setFileFilter(String name, String[] exts) {
            this.filterName = name;
            this.extensions = exts;
            loadDirectory(currentDir);
        }

        private void loadDirectory(File dir) {
            currentDir = dir;
            buildTree();
            loadFileList(dir);
        }
        
        private void buildTree() {
            isBuildingTree = true;
            treeRoot.removeAllChildren();
            
            // Add drives on Windows, or root on Unix
            File[] roots = File.listRoots();
            if (roots != null && roots.length > 1) {
                for (File root : roots) {
                    String label = root.getAbsolutePath();
                    long free = root.getFreeSpace();
                    String size = " " + (free / 1024 / 1024 / 1024) + " GB free";
                    DefaultMutableTreeNode node = new DefaultMutableTreeNode(new FileNode(root, label + size));
                    addSubdirectories(node, root, 1);
                    treeRoot.add(node);
                }
            } else {
                // Single root (Unix) or no drives found
                File home = new File(System.getProperty("user.home"));
                DefaultMutableTreeNode node = new DefaultMutableTreeNode(new FileNode(home.getParentFile() != null ? home.getParentFile() : home, "Home"));
                addSubdirectories(node, home.getParentFile() != null ? home.getParentFile() : home, 2);
                treeRoot.add(node);
            }
            
            // Expand and select current path
            folderTree.expandPath(new javax.swing.tree.TreePath(treeRoot.getPath()));
            selectNodeInTree(treeRoot, currentDir);
            folderTree.updateUI();
            isBuildingTree = false;
        }
        
        private void loadFileList(File dir) {
            fileListModel.clear();
            if (pathLabel != null) {
                pathLabel.setText("Location: " + dir.getAbsolutePath());
            }
            
            File[] files = dir.listFiles();
            if (files != null) {
                List<File> dirs = new ArrayList<>();
                List<File> fileList = new ArrayList<>();
                
                for (File f : files) {
                    if (f.isDirectory()) dirs.add(f);
                    else if (extensions == null || matchesFilter(f.getName())) fileList.add(f);
                }
                
                dirs.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
                fileList.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
                
                for (File d : dirs) {
                    fileListModel.addElement(new FileItem(d, d.getName(), "📁"));
                }
                for (File f : fileList) {
                    String icon = f.getName().toLowerCase().endsWith(".pdf") ? "📄" : "🖼️";
                    fileListModel.addElement(new FileItem(f, f.getName(), icon));
                }
            }
        }
        
        private void addSubdirectories(DefaultMutableTreeNode parent, File dir, int depth) {
            if (depth <= 0) return;
            File[] files = dir.listFiles();
            if (files != null) {
                List<File> dirs = new ArrayList<>();
                for (File f : files) {
                    if (f.isDirectory() && !f.isHidden()) dirs.add(f);
                }
                dirs.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
                for (File d : dirs) {
                    DefaultMutableTreeNode node = new DefaultMutableTreeNode(new FileNode(d, d.getName()));
                    parent.add(node);
                    if (isAncestor(d, currentDir)) {
                        addSubdirectories(node, d, depth - 1);
                        folderTree.expandPath(new javax.swing.tree.TreePath(node.getPath()));
                    }
                }
            }
        }
        
        private boolean isAncestor(File potentialAncestor, File file) {
            File parent = file.getParentFile();
            while (parent != null) {
                if (parent.equals(potentialAncestor)) return true;
                parent = parent.getParentFile();
            }
            return false;
        }
        
        private void selectNodeInTree(DefaultMutableTreeNode root, File target) {
            for (int i = 0; i < root.getChildCount(); i++) {
                DefaultMutableTreeNode child = (DefaultMutableTreeNode) root.getChildAt(i);
                if (child.getUserObject() instanceof FileNode) {
                    FileNode node = (FileNode) child.getUserObject();
                    if (node.file.equals(target) || isParentOf(node.file, target)) {
                        folderTree.setSelectionPath(new javax.swing.tree.TreePath(child.getPath()));
                        if (node.file.equals(target)) return;
                        selectNodeInTree(child, target);
                        return;
                    }
                }
            }
        }
        
        private boolean isParentOf(File parent, File child) {
            File p = child.getParentFile();
            while (p != null) {
                if (p.equals(parent)) return true;
                p = p.getParentFile();
            }
            return false;
        }

        private boolean matchesFilter(String name) {
            if (extensions == null) return true;
            String lower = name.toLowerCase();
            for (String ext : extensions) {
                if (lower.endsWith("." + ext.toLowerCase())) return true;
            }
            return false;
        }

        private void goUp() {
            if (currentDir.getParentFile() != null) {
                loadDirectory(currentDir.getParentFile());
            }
        }

        private void selectAndClose() {
            if (saveMode) {
                String name = filenameField.getText().trim();
                if (!name.isEmpty()) {
                    selectedPath = new File(currentDir, name).getAbsolutePath();
                    dispose();
                }
            } else {
                List<File> selected = new ArrayList<>();
                for (FileItem item : fileList.getSelectedValuesList()) {
                    if (!item.file.isDirectory()) selected.add(item.file);
                }
                
                if (!selected.isEmpty()) {
                    selectedFiles = selected.toArray(new File[0]);
                    selectedPath = selected.get(0).getAbsolutePath();
                    dispose();
                }
            }
        }

        String getSelectedPath() { return selectedPath; }
        File[] getSelectedFiles() { return selectedFiles; }

        private class FileItem {
            final File file;
            final String display;
            final String icon;
            
            FileItem(File file, String display, String icon) {
                this.file = file;
                this.display = display;
                this.icon = icon;
            }
            
            @Override
            public String toString() {
                return icon + " " + display;
            }
        }

        private class FileCellRenderer implements ListCellRenderer<FileItem> {
            private final JPanel panel = new JPanel(new BorderLayout(8, 0));
            private final JLabel iconLabel = new JLabel();
            private final JLabel textLabel = new JLabel();
            
            FileCellRenderer() {
                panel.setOpaque(true);
                panel.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
                iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
                textLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                panel.add(iconLabel, BorderLayout.WEST);
                panel.add(textLabel, BorderLayout.CENTER);
            }
            
            @Override
            public Component getListCellRendererComponent(JList<? extends FileItem> list, FileItem value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                iconLabel.setText(value.icon);
                textLabel.setText(value.display);
                
                if (isSelected) {
                    panel.setBackground(new Color(168, 85, 247, 180));
                    textLabel.setForeground(Color.WHITE);
                    iconLabel.setForeground(Color.WHITE);
                } else {
                    panel.setBackground(index % 2 == 0 ? new Color(255, 255, 255, 200) : new Color(250, 250, 255, 200));
                    textLabel.setForeground(new Color(88, 28, 135));
                    iconLabel.setForeground(new Color(147, 51, 234));
                }
                
                return panel;
            }
        }

        // Tree node wrapper
        private class FileNode {
            final File file;
            final String display;
            
            FileNode(File file, String display) {
                this.file = file;
                this.display = display;
            }
            
            @Override
            public String toString() {
                return display;
            }
        }

        // Tree cell renderer
        private class FolderTreeRenderer extends DefaultTreeCellRenderer {
            @Override
            public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel,
                    boolean expanded, boolean leaf, int row, boolean hasFocus) {
                super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
                
                if (value instanceof DefaultMutableTreeNode) {
                    Object userObj = ((DefaultMutableTreeNode) value).getUserObject();
                    if (userObj instanceof FileNode) {
                        FileNode node = (FileNode) userObj;
                        setText(node.display);
                        if (node.file.isDirectory()) {
                            setIcon(expanded ? openIcon : closedIcon);
                        }
                    } else if ("Computer".equals(userObj.toString())) {
                        setText("Computer");
                    }
                }
                
                setFont(new Font("Segoe UI", Font.PLAIN, 12));
                setForeground(new Color(88, 28, 135));
                if (sel) {
                    setForeground(Color.WHITE);
                }
                
                return this;
            }
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(new FlatIntelliJLaf());
            UIManager.put("Button.arc", 12);
            UIManager.put("Component.arc", 12);
            UIManager.put("TextComponent.arc", 10);
        } catch (Exception ex) {
            System.err.println("Failed to initialize FlatLaf: " + ex.getMessage());
        }

        SwingUtilities.invokeLater(() -> new ImageToPdfGui().setVisible(true));
    }
}
