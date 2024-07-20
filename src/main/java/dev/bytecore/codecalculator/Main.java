package dev.bytecore.codecalculator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main {
    private JFrame frame;
    private JTextField costPerLineField;
    private JTextArea resultArea;
    private JButton browseButton;
    private JButton calculateButton;
    private JFileChooser fileChooser;
    private JLabel statusLabel;
    private JProgressBar progressBar;
    private File selectedDirectory;

    public Main() {
        frame = new JFrame("Code Line Counter");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 500);
        frame.setLayout(new BorderLayout());

        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        costPerLineField = new JTextField();
        resultArea = new JTextArea();
        resultArea.setEditable(false);
        resultArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        resultArea.setBorder(BorderFactory.createTitledBorder("Results"));

        browseButton = new JButton("Browse");
        calculateButton = new JButton("Calculate");
        statusLabel = new JLabel("Made by Boryte (ByteCore)");
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);

        gbc.gridx = 0;
        gbc.gridy = 0;
        inputPanel.add(new JLabel("Cost per Line:"), gbc);
        gbc.gridx = 1;
        gbc.gridy = 0;
        inputPanel.add(costPerLineField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        inputPanel.add(browseButton, gbc);
        gbc.gridx = 1;
        gbc.gridy = 1;
        inputPanel.add(calculateButton, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        inputPanel.add(progressBar, gbc);

        frame.add(inputPanel, BorderLayout.NORTH);
        frame.add(new JScrollPane(resultArea), BorderLayout.CENTER);
        frame.add(statusLabel, BorderLayout.SOUTH);

        fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        browseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int returnValue = fileChooser.showOpenDialog(frame);
                if (returnValue == JFileChooser.APPROVE_OPTION) {
                    selectedDirectory = fileChooser.getSelectedFile();
                }
            }
        });

        calculateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String costPerLineText = costPerLineField.getText();

                if (selectedDirectory == null || costPerLineText.isEmpty()) {
                    JOptionPane.showMessageDialog(frame, "Please select a directory and enter the cost per line.");
                    return;
                }

                try {
                    double costPerLine = Double.parseDouble(costPerLineText);
                    new LineCounterTask(selectedDirectory.toPath(), costPerLine).execute();
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(frame, "Invalid cost per line. Please enter a valid number.");
                }
            }
        });

        frame.setVisible(true);
    }

    private class LineCounterTask extends SwingWorker<Integer, Void> {
        private final Path directory;
        private final double costPerLine;

        public LineCounterTask(Path directory, double costPerLine) {
            this.directory = directory;
            this.costPerLine = costPerLine;
        }

        @Override
        protected Integer doInBackground() throws Exception {
            return countLines(directory);
        }

        @Override
        protected void done() {
            try {
                int totalLines = get();
                double totalCost = totalLines * costPerLine;
                resultArea.setText(String.format("Total Lines of Code: %d\nTotal Cost: $%.2f", totalLines, totalCost));
                progressBar.setValue(100);
            } catch (InterruptedException | ExecutionException ex) {
                JOptionPane.showMessageDialog(frame, "An error occurred while counting the lines.");
            }
        }

        private int countLines(Path directory) throws IOException {
            int totalLines = 0;

            try (Stream<Path> paths = Files.walk(directory)) {
                List<Path> javaFiles = paths.filter(Files::isRegularFile)
                        .filter(p -> p.toString().endsWith(".java"))
                        .collect(Collectors.toList());

                int fileCount = javaFiles.size();
                int currentFile = 0;

                for (Path file : javaFiles) {
                    totalLines += countLinesInFile(file);
                    currentFile++;
                    int progress = (int) ((currentFile / (double) fileCount) * 100);
                    setProgress(progress);
                    progressBar.setValue(progress);
                }
            }

            return totalLines;
        }

        private int countLinesInFile(Path file) throws IOException {
            int lineCount = 0;
            try (BufferedReader reader = Files.newBufferedReader(file)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.trim().isEmpty() && !line.trim().startsWith("import")) {
                        lineCount++;
                    }
                }
            }
            return lineCount;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::new);
    }
}