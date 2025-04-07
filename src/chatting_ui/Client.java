package chatting_ui;

import java.io.*;
import java.net.*;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.*;

public class Client extends JFrame {
    JTextField textField = new JTextField();
    JButton sendBt = new JButton("전송");
    JPanel msgPanel = new JPanel();
    JScrollPane scroll = new JScrollPane(msgPanel);

    private SyncOnUpdate sync = new SyncOnUpdate();
    private WhenEnter whenEnter = new WhenEnter();
    private WhenExit whenExit = new WhenExit();

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
                sync.start();
            }
        });
        pane.add(sendBt);

        addWindowListener(new WindowListener() {
            @Override
            public void windowOpened(WindowEvent e) {
                whenEnter.start();
            }

            @Override
            public void windowClosing(WindowEvent e) {
                whenExit.start();
            }

            @Override
            public void windowActivated(WindowEvent e) {
            }

            @Override
            public void windowClosed(WindowEvent e) {
            }

            @Override
            public void windowDeactivated(WindowEvent e) {
            }

            @Override
            public void windowDeiconified(WindowEvent e) {
            }

            @Override
            public void windowIconified(WindowEvent e) {
            }
        });

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

    class WhenEnter implements Runnable {
        boolean flag = false;

        WhenEnter() {
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
                    sendNotice(" 님이 참여했습니다.");
                    flag = false;
                } catch (Exception e) {
                    System.out.println("오류 발생: " + e.getMessage());
                }
            }
        }
    }

    class WhenExit implements Runnable {
        boolean flag = false;

        WhenExit() {
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
                    sendNotice(" 님이 나갔습니다.");
                    flag = false;
                } catch (Exception e) {
                    System.out.println("오류 발생: " + e.getMessage());
                }
            }
        }
    }

    void sendNotice(String status) {
        writer.println(userName + status);

        SwingUtilities.invokeLater(() -> {
            JLabel noticeLb = new JLabel(userName + status);

            noticeLb.setHorizontalAlignment(JLabel.CENTER);
            noticeLb.setBounds(10, nextMsgLocation, 340, 20);
            noticeLb.setFont(new Font("Sans Serif", Font.PLAIN, 13));
            noticeLb.setForeground(Color.GRAY);
            msgPanel.add(noticeLb);
            nextMsgLocation += 20;

            if (nextMsgLocation >= 450) {
                // 패널 크기 갱신
                msgPanel.setPreferredSize(new Dimension(350, nextMsgLocation));
                msgPanel.revalidate();
            }

            repaint();

            SwingUtilities.invokeLater(() -> {
                scroll.getVerticalScrollBar().setValue(scroll.getVerticalScrollBar().getMaximum());
            });
        });
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

    synchronized void readMessage(String input) {
        if (input.indexOf(':') == -1) {
            JLabel noticeLb = new JLabel(input);

            noticeLb.setHorizontalAlignment(JLabel.CENTER);
            noticeLb.setBounds(10, nextMsgLocation, 340, 20);
            noticeLb.setFont(new Font("Sans Serif", Font.PLAIN, 13));
            noticeLb.setForeground(Color.GRAY);
            msgPanel.add(noticeLb);
            nextMsgLocation += 20;

        } else {
            String sendName = input.substring(0, input.indexOf(':'));
            String sendTxt = input.substring(input.indexOf(':') + 1);

            if (sendName.equals(userName)) {
                return;
            }

            JLabel nameLb = new JLabel(sendName);
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
        }

        if (nextMsgLocation >= 450) {
            // 패널 크기 갱신
            msgPanel.setPreferredSize(new Dimension(350, nextMsgLocation));
            msgPanel.revalidate();
        }

        repaint();

        SwingUtilities.invokeLater(() -> {
            scroll.getVerticalScrollBar().setValue(scroll.getVerticalScrollBar().getMaximum());
        });
    }

    public static void main(String[] args) {
        try {
            socket = new Socket("localhost", 12345);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);

            Client client = new Client();

            String inputFromServer;
            while ((inputFromServer = reader.readLine()) != null) {
                client.readMessage(inputFromServer);
            }
        } catch (Exception e) {
            System.out.println("오류 발생: " + e.getMessage());
        }
    }
}