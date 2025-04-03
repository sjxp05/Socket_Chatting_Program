package tests;

import java.io.*;
import java.net.*;
import java.awt.*;
import javax.swing.*;

public class ReaderTest extends JFrame {
    JPanel msgPanel = new JPanel();
    JScrollPane scroll = new JScrollPane(msgPanel);

    private static Socket socket = null;
    private static BufferedReader reader;

    private int nextMsgLocation = 10;

    public ReaderTest() {
        setTitle("New Chat");
        setSize(400, 600);
        setResizable(false);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(null);

        Container pane = getContentPane();

        msgPanel.setBounds(0, 0, 350, 480);
        msgPanel.setLayout(null);

        scroll.setBounds(10, 10, 368, 480);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        pane.add(scroll);

        setVisible(true);
    }

    void readFromServer(String input) {
        String userName = input.substring(0, input.indexOf(':'));
        String sendTxt = input.substring(input.indexOf(':') + 1);

        JLabel nameLb = new JLabel(userName);
        JLabel msgLb = new JLabel(sendTxt);

        nameLb.setHorizontalAlignment(JLabel.LEFT);
        nameLb.setBounds(10, nextMsgLocation, 340, 20);
        nameLb.setFont(new Font("Sans Serif", Font.PLAIN, 15));
        nameLb.setForeground(Color.GRAY);
        msgPanel.add(nameLb);
        nextMsgLocation += 20;

        msgLb.setHorizontalAlignment(JLabel.LEFT);
        msgLb.setBounds(10, nextMsgLocation, 340, 20);
        msgLb.setFont(new Font("Sans Serif", Font.PLAIN, 15));
        msgPanel.add(msgLb);
        nextMsgLocation += 30;

        if (nextMsgLocation >= 450) {
            // 패널 크기 갱신
            msgPanel.setPreferredSize(new Dimension(350, nextMsgLocation));
            msgPanel.revalidate();
        }

        repaint(); // 이거 꼭 넣어라 제발ㄹㄹㄹ

        SwingUtilities.invokeLater(() -> {
            scroll.getVerticalScrollBar().setValue(scroll.getVerticalScrollBar().getMaximum());
        });
    }

    public static void main(String[] args) {
        try {
            socket = new Socket("localhost", 12345);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            ReaderTest testWindow = new ReaderTest();

            String input;
            while ((input = reader.readLine()) != null) {
                testWindow.readFromServer(input.strip());
            }
        } catch (Exception e) {
            System.out.println("오류 발생: " + e.getMessage());
        }
    }
}
