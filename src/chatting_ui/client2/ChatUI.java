package chatting_ui.client2;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.*;

public class ChatUI extends JFrame {
    JLabel roomName = new JLabel("새로운 채팅방");
    JButton sendBt = new JButton("전송");
    JButton exitBt = new JButton("나가기");

    JPanel msgPanel = new JPanel();
    JScrollPane scroll = new JScrollPane(msgPanel);
    JTextArea textInput = new JTextArea();
    JScrollPane textScroll = new JScrollPane(textInput);

    JButton membersBt = new JButton("참여자");
    JPanel membersPanel = new JPanel();
    JScrollPane memScroll = new JScrollPane(membersPanel);
    JButton nickChangeBt = new JButton("이름 변경");

    private SyncOnUpdate sync = new SyncOnUpdate();

    private volatile boolean shiftPressed = false;
    private volatile boolean viewMode = false;
    private volatile int nextMsgLocation = 10;

    ChatUI() {
        setTitle("New Chat");
        setSize(400, 600);
        setResizable(false);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        Container pane = getContentPane();
        pane.setLayout(null);

        roomName.setFont(new Font("Sans Serif", Font.BOLD, 15));
        roomName.setHorizontalAlignment(JLabel.CENTER);
        roomName.setBounds(92, 5, 200, 40);
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

        membersBt.setFont(new Font("Sans Serif", Font.BOLD, 12));
        membersBt.setBounds(307, 5, 70, 40);
        membersBt.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sync.start("view");
            }
        });
        pane.add(membersBt);

        membersPanel.setBounds(0, 0, 400, 435);
        membersPanel.setBackground(Color.LIGHT_GRAY);
        membersPanel.setLayout(null);

        memScroll.setBounds(10, 50, 368, 435);
        memScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        memScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        pane.add(memScroll);
        memScroll.setVisible(false);

        nickChangeBt.setFont(new Font("Sans Serif", Font.BOLD, 10));
        nickChangeBt.setBounds(260, 15, 80, 30);
        nickChangeBt.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sync.start("change");
            }
        });

        msgPanel.setBounds(0, 0, 350, 435);
        msgPanel.setLayout(null);

        scroll.setBounds(10, 50, 368, 435);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        pane.add(scroll);

        textInput.setFont(new Font("Sans Serif", Font.PLAIN, 15));
        textInput.setBounds(0, 0, 290, 50);
        textInput.setLineWrap(true);
        textInput.requestFocus();
        textInput.addKeyListener(new PressEnter());

        textScroll.setBounds(10, 500, 290, 50);
        textScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        textScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        pane.add(textScroll);

        sendBt.setFont(new Font("Sans Serif", Font.BOLD, 15));
        sendBt.setBounds(307, 500, 70, 50);
        sendBt.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sync.start("send");
            }
        });
        pane.add(sendBt);

        setVisible(true);
    }

    class SyncOnUpdate implements Runnable {
        private boolean flag = false;
        String option = "";

        SyncOnUpdate() {
            Thread th = new Thread(this);
            th.start();
        }

        synchronized void start(String optionReceived) {
            flag = true;
            option = optionReceived;
            this.notify();
        }

        synchronized void waitForStart() {
            if (!flag) {
                try {
                    this.wait();
                } catch (Exception e) {
                    System.exit(1);
                }
            }
        }

        @Override
        public void run() {
            while (true) {
                waitForStart();
                try {
                    if (option.equals("send")) {
                        writeMessage();
                    } else if (option.equals("change")) {
                        Main.changeNickname();
                    } else if (option.equals("view")) {
                        viewMembers();
                    }
                    flag = false;
                } catch (Exception e) {
                    System.exit(1);
                }
            }
        }
    }

    class PressEnter implements KeyListener {
        @Override
        public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_SHIFT) {
                shiftPressed = true;
            }

            if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                if (shiftPressed) {
                    StringBuffer currentTxt = new StringBuffer(textInput.getText());

                    SwingUtilities.invokeLater(() -> {
                        textInput.setText(currentTxt + "\n");
                        textInput.requestFocus();
                    });
                } else {
                    sync.start("send");
                }
            }
        }

        @Override
        public void keyTyped(KeyEvent e) {
        }

        @Override
        public void keyReleased(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_SHIFT) {
                shiftPressed = false;
            }
        }
    }

    void viewMembers() {
        if (viewMode) {
            viewMode = false;
            membersBt.setText("참여자");
            membersPanel.removeAll();
            memScroll.setVisible(false);
            return;

        } else {
            viewMode = true;
            membersBt.setText("채팅");
            Main.viewRequest();

            int nextMemberLocation = 10;

            JLabel myLb = new JLabel("      " + Main.userName);
            myLb.setFont(new Font("Sans Serif", Font.BOLD, 15));
            myLb.setOpaque(true);
            myLb.setBackground(new Color(245, 240, 190));
            myLb.setBounds(0, nextMemberLocation, 400, 40);

            membersPanel.add(myLb);
            nextMemberLocation += 50;

            membersPanel.add(nickChangeBt);
            membersPanel.setComponentZOrder(nickChangeBt, 0);

            for (String name : Main.nameList) {
                JLabel nameLb = new JLabel("      " + name);
                nameLb.setFont(new Font("Sans Serif", Font.PLAIN, 15));
                nameLb.setOpaque(true);
                nameLb.setBackground(Color.WHITE);
                nameLb.setBounds(0, nextMemberLocation, 400, 40);

                membersPanel.add(nameLb);
                nextMemberLocation += 50;
            }

            if (nextMemberLocation >= 435) {
                membersPanel.setPreferredSize(new Dimension(400, nextMemberLocation));
                membersPanel.revalidate();
                repaint();
            }

            memScroll.setVisible(true);
            return;
        }
    }

    void refresh() {
        if (viewMode) {
            viewMembers();
            sync.start("view");
        }
    }

    void writeMessage() {
        String msg = textInput.getText().strip();
        SwingUtilities.invokeLater(() -> {
            textInput.setText("");
            textInput.requestFocus();
        });

        Main.sendMessage(msg);
    }

    void showNotices(String noticeMsg) {
        JLabel noticeLb = new JLabel(noticeMsg);
        noticeLb.setHorizontalAlignment(JLabel.CENTER);
        noticeLb.setBounds(10, nextMsgLocation, 340, 20);
        noticeLb.setFont(new Font("Sans Serif", Font.PLAIN, 13));
        noticeLb.setForeground(Color.GRAY);
        msgPanel.add(noticeLb);

        Main.lastSpeakerID = -1;
        nextMsgLocation += 25;

        SwingUtilities.invokeLater(() -> {
            if (nextMsgLocation >= 435) {
                msgPanel.setPreferredSize(new Dimension(350, nextMsgLocation));
                msgPanel.revalidate();
            }
            msgPanel.repaint();

            SwingUtilities.invokeLater(() -> {
                scroll.getVerticalScrollBar().setValue(scroll.getVerticalScrollBar().getMaximum());
            });
            return;
        });

        refresh();
    }

    void showMessage(String sendName, String sendMsg, int sendID, int height) {
        if (viewMode) {
            viewMembers();
        }

        JLabel nameLb = new JLabel(sendName);
        JLabel msgLb = new JLabel(sendMsg);

        if (sendID == Main.userID) {
            nameLb.setHorizontalAlignment(JLabel.RIGHT);
            msgLb.setHorizontalAlignment(JLabel.RIGHT);
        } else {
            nameLb.setHorizontalAlignment(JLabel.LEFT);
            msgLb.setHorizontalAlignment(JLabel.LEFT);
        }

        if (sendID == Main.lastSpeakerID) {
            nextMsgLocation -= 10;
        } else {
            nameLb.setBounds(10, nextMsgLocation, 330, 20);
            nameLb.setFont(new Font("Sans Serif", Font.PLAIN, 14));
            nameLb.setForeground(Color.GRAY);
            msgPanel.add(nameLb);

            Main.lastSpeakerID = sendID;
            nextMsgLocation += 22;
        }

        msgLb.setBounds(10, nextMsgLocation, 330, height);
        msgLb.setFont(new Font("Sans Serif", Font.PLAIN, 15));
        msgPanel.add(msgLb);
        nextMsgLocation += (15 + height);

        SwingUtilities.invokeLater(() -> {
            if (nextMsgLocation >= 435) {
                msgPanel.setPreferredSize(new Dimension(350, nextMsgLocation));
                msgPanel.revalidate();
            }
            msgPanel.repaint();

            SwingUtilities.invokeLater(() -> {
                scroll.getVerticalScrollBar().setValue(scroll.getVerticalScrollBar().getMaximum());
            });
            return;
        });
    }
}
