package chatting_ui;

import java.io.*;
import java.net.*;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.*;

public class Client extends JFrame {
    JTextField textField = new JTextField();
    JButton sendBt = new JButton("전송");
    JPanel msgPanel = new JPanel();
    JScrollPane scroll = new JScrollPane(msgPanel);

    private static Socket socket = null;
    private static BufferedReader reader;
    private static PrintWriter writer;

    private String userName = "Client1";
    private int nextMsgLocation = 10;

    public Client() {
        setTitle("New Chat");
        setSize(400, 600);
        setResizable(false);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        Container pane = getContentPane();
        pane.setLayout(null);

        textField.setFont(new Font("Sans Serif", Font.PLAIN, 15));
        textField.setBounds(10, 500, 290, 50);
        textField.requestFocus();
        textField.addKeyListener(new PressEnter());
        pane.add(textField);

        msgPanel.setBounds(0, 0, 350, 480);
        msgPanel.setLayout(null);

        scroll.setBounds(10, 10, 368, 480);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        pane.add(scroll);

        sendBt.setFont(new Font("Sans Serif", Font.PLAIN, 15));
        sendBt.setBounds(310, 500, 70, 50);
        sendBt.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updatePanel();
            }
        });
        pane.add(sendBt);

        setVisible(true);
    }

    class PressEnter implements KeyListener {
        @Override
        public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                updatePanel();
            }
        }

        @Override
        public void keyTyped(KeyEvent e) {
        }

        @Override
        public void keyReleased(KeyEvent e) {
        }
    }

    void updatePanel() {
        String sendTxt = textField.getText().strip();
        textField.setText("");

        if (sendTxt.length() == 0) {
            return;
        }

        writer.println(userName + ":" + sendTxt);

        JLabel nameLb = new JLabel(userName);
        JLabel msgLb = new JLabel(sendTxt);

        nameLb.setHorizontalAlignment(JLabel.RIGHT);
        nameLb.setBounds(0, nextMsgLocation, 340, 20);
        nameLb.setFont(new Font("Sans Serif", Font.PLAIN, 15));
        nameLb.setForeground(Color.GRAY);
        msgPanel.add(nameLb);
        nextMsgLocation += 20;

        msgLb.setHorizontalAlignment(JLabel.RIGHT);
        msgLb.setBounds(0, nextMsgLocation, 340, 20);
        msgLb.setFont(new Font("Sans Serif", Font.PLAIN, 15));
        msgPanel.add(msgLb);
        nextMsgLocation += 30;

        if (nextMsgLocation >= 450) {
            // 패널 크기 갱신
            msgPanel.setPreferredSize(new Dimension(350, nextMsgLocation));
            msgPanel.revalidate();
        }

        repaint(); // 이거 꼭 넣어라 제발ㄹㄹㄹ

        // 스크롤바 자동 내리기 (나중에 시행)
        SwingUtilities.invokeLater(() -> {
            scroll.getVerticalScrollBar().setValue(scroll.getVerticalScrollBar().getMaximum());
        });
    }

    public static void main(String[] args) {
        try {
            socket = new Socket("localhost", 12345);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);
        } catch (Exception e) {
            System.out.println("오류 발생: " + e.getMessage());
        }

        new Client();
    }
}