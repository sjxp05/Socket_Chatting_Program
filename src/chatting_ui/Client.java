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
    JLabel roomName = new JLabel("새로운 채팅방");
    JPanel msgPanel = new JPanel();
    JScrollPane scroll = new JScrollPane(msgPanel);
    JTextField textField = new JTextField();
    JButton sendBt = new JButton("전송");
    JButton exitBt = new JButton("나가기");

    private SyncOnUpdate sync = new SyncOnUpdate();

    private static Socket socket = null;
    private static BufferedReader reader;
    private static PrintWriter writer;

    String userName = "Client1";
    String lastSpeaker = "";
    private int nextMsgLocation = 10;

    public Client() {
        setTitle("New Chat");
        setSize(400, 600);
        setResizable(false);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        Container pane = getContentPane();
        pane.setLayout(null);

        roomName.setFont(new Font("Sans Serif", Font.BOLD, 15));
        roomName.setHorizontalAlignment(JLabel.CENTER);
        roomName.setBounds(100, 5, 200, 40);
        pane.add(roomName);

        exitBt.setFont(new Font("Sans Serif", Font.BOLD, 12));
        exitBt.setBounds(10, 5, 70, 40);
        exitBt.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });
        pane.add(exitBt);

        msgPanel.setBounds(0, 0, 350, 435);
        msgPanel.setLayout(null);

        scroll.setBounds(10, 50, 368, 435);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        pane.add(scroll);

        textField.setFont(new Font("Sans Serif", Font.PLAIN, 15));
        textField.setBounds(10, 500, 290, 50);
        textField.requestFocus();
        textField.addKeyListener(new PressEnter());
        pane.add(textField);

        sendBt.setFont(new Font("Sans Serif", Font.BOLD, 15));
        sendBt.setBounds(310, 500, 70, 50);
        sendBt.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sync.start();
            }
        });
        pane.add(sendBt);

        setVisible(true);
    }

    class PressEnter implements KeyListener {
        @Override
        public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                sync.start();
            }
        }

        @Override
        public void keyTyped(KeyEvent e) {
        }

        @Override
        public void keyReleased(KeyEvent e) {
        }
    }

    class SyncOnUpdate implements Runnable {
        boolean flag = false;

        SyncOnUpdate() {
            Thread thread = new Thread(this);
            thread.start();
        }

        synchronized void start() {
            flag = true;
            this.notify();
        }

        synchronized void waitForStart() {
            if (!flag) {
                try {
                    this.wait();
                } catch (Exception e) {
                    System.out.println("오류 발생: " + e.getMessage());
                }
            }
        }

        @Override
        public void run() {
            while (true) {
                waitForStart();
                try {
                    sendMessage();
                    flag = false;
                } catch (Exception e) {
                    System.out.println("오류 발생: " + e.getMessage());
                }
            }
        }
    }

    void sendMessage() {
        String msg = textField.getText().strip();
        textField.setText("");
        textField.requestFocus();

        if (msg.length() == 0) {
            return;
        }
        writer.println(userName + ":" + msg);
    }

    synchronized void readMessage() {
        String input = "";
        try {
            input = reader.readLine();
        } catch (Exception e) {
            System.out.println("오류 발생: " + e.getMessage());
        }

        if (input.indexOf(':') == -1) {
            if (input.indexOf('.') >= 0) {
                JLabel noticeLb = new JLabel(input);

                noticeLb.setHorizontalAlignment(JLabel.CENTER);
                noticeLb.setBounds(10, nextMsgLocation, 340, 20);
                noticeLb.setFont(new Font("Sans Serif", Font.PLAIN, 13));
                noticeLb.setForeground(Color.GRAY);
                msgPanel.add(noticeLb);

                lastSpeaker = "";
                nextMsgLocation += 25;

            } else {
                return;
            }
        } else {
            String sendName = input.substring(0, input.indexOf(':'));
            String sendTxt = input.substring(input.indexOf(':') + 1);

            JLabel nameLb = new JLabel(sendName);
            JLabel msgLb = new JLabel(sendTxt);

            if (sendName.equals(userName)) {
                if (sendName.equals(lastSpeaker)) {
                    nextMsgLocation -= 10;
                } else {
                    nameLb.setHorizontalAlignment(JLabel.RIGHT);
                    nameLb.setBounds(0, nextMsgLocation, 340, 20);
                    nameLb.setFont(new Font("Sans Serif", Font.PLAIN, 14));
                    nameLb.setForeground(Color.GRAY);
                    msgPanel.add(nameLb);

                    lastSpeaker = sendName;
                    nextMsgLocation += 22;
                }

                msgLb.setHorizontalAlignment(JLabel.RIGHT);
                msgLb.setBounds(0, nextMsgLocation, 340, 20);
                msgLb.setFont(new Font("Sans Serif", Font.PLAIN, 15));
                msgPanel.add(msgLb);
                nextMsgLocation += 32;

            } else {
                if (sendName.equals(lastSpeaker)) {
                    nextMsgLocation -= 10;
                } else {
                    nameLb.setHorizontalAlignment(JLabel.LEFT);
                    nameLb.setBounds(10, nextMsgLocation, 340, 20);
                    nameLb.setFont(new Font("Sans Serif", Font.PLAIN, 14));
                    nameLb.setForeground(Color.GRAY);
                    msgPanel.add(nameLb);

                    lastSpeaker = sendName;
                    nextMsgLocation += 22;
                }

                msgLb.setHorizontalAlignment(JLabel.LEFT);
                msgLb.setBounds(10, nextMsgLocation, 340, 20);
                msgLb.setFont(new Font("Sans Serif", Font.PLAIN, 15));
                msgPanel.add(msgLb);
                nextMsgLocation += 32;
            }
        }

        if (nextMsgLocation >= 405) {
            // 패널 크기 갱신
            msgPanel.setPreferredSize(new Dimension(350, nextMsgLocation));
            msgPanel.revalidate();
        }
        repaint(); // 변경사항을 창에 반영

        SwingUtilities.invokeLater(() -> { // 스크롤바 내리기: 나중에 반영
            scroll.getVerticalScrollBar().setValue(scroll.getVerticalScrollBar().getMaximum());
        });
    }

    public static void main(String[] args) {
        try {
            socket = new Socket("localhost", 12345);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);

            Client client = new Client();
            writer.println(client.userName);

            while (reader != null) {
                client.readMessage();
            }
        } catch (Exception e) {
            System.out.println("오류 발생: " + e.getMessage());
        }
    }
}