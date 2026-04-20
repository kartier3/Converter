package com.example;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ImageToPdfGui extends JFrame {

    private final DefaultListModel<String> fileListModel;
    private final JList<String> fileList;
    private final JLabel statusLabel;
    private final JTextField outputField;
    private final List<File> selectedFiles = new ArrayList<>();

    public ImageToPdfGui() {
        setTitle("Image to PDF Converter");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 450);
        setLocationRelativeTo(null);

        // Main panel with padding
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Top: Title
        JLabel titleLabel = new JLabel("Image to PDF Converter", JLabel.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        mainPanel.add(titleLabel, BorderLayout.NORTH);

        // Center: File list with drag & drop
        JPanel centerPanel = new JPanel(new BorderLayout(5, 5));

        fileListModel = new DefaultListModel<>();
        fileList = new JList<>(fileListModel);
        fileList.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        JScrollPane scrollPane = new JScrollPane(fileList);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Images to Convert (drag & drop files here)"));

        // Enable drag & drop
        new DropTarget(fileList, new FileDropTargetListener());

        centerPanel.add(scrollPane, BorderLayout.CENTER);

        // Buttons for file operations
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        JButton addButton = new JButton("Add Files");
        JButton removeButton = new JButton("Remove Selected");
        JButton clearButton = new JButton("Clear All");

        addButton.addActionListener(e -> addFiles());
        removeButton.addActionListener(e -> removeSelected());
        clearButton.addActionListener(e -> clearAll());

        buttonPanel.add(addButton);
        buttonPanel.add(removeButton);
        buttonPanel.add(clearButton);
        centerPanel.add(buttonPanel, BorderLayout.SOUTH);

        mainPanel.add(centerPanel, BorderLayout.CENTER);

        // Bottom: Output file and Convert
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 10));

        // Output file selection
        JPanel outputPanel = new JPanel(new BorderLayout(5, 0));
        outputField = new JTextField(30);
        outputField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        JButton browseButton = new JButton("Browse...");
        browseButton.addActionListener(e -> browseOutput());

        outputPanel.add(new JLabel("Output PDF: "), BorderLayout.WEST);
        outputPanel.add(outputField, BorderLayout.CENTER);
        outputPanel.add(browseButton, BorderLayout.EAST);

        bottomPanel.add(outputPanel, BorderLayout.NORTH);

        // Convert button and status
        JPanel convertPanel = new JPanel(new BorderLayout(10, 5));
        JButton convertButton = new JButton("Convert to PDF");
        convertButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        convertButton.setBackground(new Color(0, 120, 212));
        convertButton.setForeground(Color.WHITE);
        convertButton.setFocusPainted(false);
        convertButton.setPreferredSize(new Dimension(150, 40));
        convertButton.addActionListener(e -> convert());

        statusLabel = new JLabel("Ready - drag images or click Add Files");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusLabel.setForeground(Color.GRAY);

        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusPanel.add(statusLabel);

        convertPanel.add(convertButton, BorderLayout.EAST);
        convertPanel.add(statusPanel, BorderLayout.CENTER);

        bottomPanel.add(convertPanel, BorderLayout.SOUTH);

        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    private void addFiles() {
        JFileChooser chooser = new JFileChooser();
        chooser.setMultiSelectionEnabled(true);
        chooser.setFileFilter(new FileNameExtensionFilter(
            "Images (JPG, PNG, GIF, BMP, TIFF, WEBP)",
            "jpg", "jpeg", "png", "gif", "bmp", "tiff", "tif", "webp"
        ));

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            for (File file : chooser.getSelectedFiles()) {
                addFile(file);
            }
        }
    }

    private void addFile(File file) {
        if (!selectedFiles.contains(file)) {
            selectedFiles.add(file);
            fileListModel.addElement(file.getName());
            updateStatus();
        }
    }

    private void removeSelected() {
        int[] indices = fileList.getSelectedIndices();
        for (int i = indices.length - 1; i >= 0; i--) {
            selectedFiles.remove(indices[i]);
            fileListModel.remove(indices[i]);
        }
        updateStatus();
    }

    private void clearAll() {
        selectedFiles.clear();
        fileListModel.clear();
        updateStatus();
    }

    private void browseOutput() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("PDF Files", "pdf"));
        chooser.setSelectedFile(new File("output.pdf"));

        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            String path = chooser.getSelectedFile().getAbsolutePath();
            if (!path.toLowerCase().endsWith(".pdf")) {
                path += ".pdf";
            }
            outputField.setText(path);
        }
    }

    private void updateStatus() {
        int count = selectedFiles.size();
        statusLabel.setText(count + " file" + (count == 1 ? "" : "s") + " ready");
        statusLabel.setForeground(count > 0 ? new Color(0, 100, 0) : Color.GRAY);
    }

    private void convert() {
        if (selectedFiles.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Please add at least one image file.",
                "No Images", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String outputPath = outputField.getText().trim();
        if (outputPath.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Please specify an output PDF file.",
                "No Output", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Disable UI during conversion
        setEnabled(false);
        statusLabel.setText("Converting...");
        statusLabel.setForeground(Color.BLUE);

        // Run conversion in background thread
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                List<String> paths = new ArrayList<>();
                for (File f : selectedFiles) {
                    paths.add(f.getAbsolutePath());
                }
                JpgToPdfConverter.convertImagesToPdf(paths, outputPath);
                return null;
            }

            @Override
            protected void done() {
                setEnabled(true);
                try {
                    get();
                    statusLabel.setText("Conversion complete!");
                    statusLabel.setForeground(new Color(0, 150, 0));
                    JOptionPane.showMessageDialog(ImageToPdfGui.this,
                        "PDF created successfully:\n" + outputPath,
                        "Success", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception e) {
                    statusLabel.setText("Error: " + e.getCause().getMessage());
                    statusLabel.setForeground(Color.RED);
                    JOptionPane.showMessageDialog(ImageToPdfGui.this,
                        "Conversion failed:\n" + e.getCause().getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    // Drag & Drop listener
    private class FileDropTargetListener extends DropTargetAdapter {
        @Override
        public void drop(DropTargetDropEvent event) {
            event.acceptDrop(DnDConstants.ACTION_COPY);
            Transferable transferable = event.getTransferable();

            try {
                if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    @SuppressWarnings("unchecked")
                    List<File> files = (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);
                    for (File file : files) {
                        if (file.isFile()) {
                            addFile(file);
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
                            addFile(f);
                        }
                    }
                }
            }
        }
    }

    public static void main(String[] args) {
        // Use system look & feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            new ImageToPdfGui().setVisible(true);
        });
    }
}
