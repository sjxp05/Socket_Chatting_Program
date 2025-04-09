package chatting_ui;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
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

    JButton membersBt = new JButton("참가자");
    JPanel membersPanel = new JPanel();
    JScrollPane memScroll = new JScrollPane(membersPanel);
    JButton nickChangeBt = new JButton("이름 변경");

    private SyncOnUpdate sync = new SyncOnUpdate();

    private static Socket socket = null;
    private static BufferedReader in;
    private static PrintWriter out;

    enum Status {
        WAITING, TRUE, FALSE
    };

    private Status changed = Status.WAITING;

    private String userName = "";
    private String lastSpeaker = "";
    private int nextMsgLocation = 10;
    private boolean viewMode = false;

    ArrayList<String> nameList = new ArrayList<>();
    private boolean added = false;

    private Client() {
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
                sync.start("members");
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
                sync.start("send");
            }
        });
        pane.add(sendBt);

        setVisible(true);
        setNickname();
    }

    class SyncOnUpdate implements Runnable {
        boolean flag = false;
        String option;

        SyncOnUpdate() {
            Thread thread = new Thread(this);
            thread.start();
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
                    System.out.println("오류 발생: " + e.getMessage());
                }
            }
        }

        @Override
        public void run() {
            while (true) {
                waitForStart();
                try {
                    if (option.equals("send")) {
                        sendMessage();
                    } else if (option.equals("change")) {
                        changeNickname();
                    } else if (option.equals("members")) {
                        viewMembers();
                    }
                    flag = false;
                } catch (Exception e) {
                    System.out.println("오류 발생: " + e.getMessage());
                }
            }
        }
    }

    class PressEnter implements KeyListener {
        @Override
        public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                sync.start("send");
            }
        }

        @Override
        public void keyTyped(KeyEvent e) {
        }

        @Override
        public void keyReleased(KeyEvent e) {
        }
    }

    void setNickname() {
        try {
            BufferedReader nameReader = new BufferedReader(new FileReader("src/chatting_ui/clientName.txt"));
            userName = nameReader.readLine();
            nameReader.close();
        } catch (Exception e) {
            System.out.println("오류 발생: " + e.getMessage());
        }

        if (userName == null || userName.strip().length() == 0) {
            String nickNameMsg = "채팅방에서 사용할 닉네임을 입력해 주세요.";

            SetNickname: while (true) {
                userName = JOptionPane.showInputDialog(nickNameMsg).strip();

                if (userName.length() > 0) {
                    if (userName.indexOf(';') >= 0 || userName.indexOf('@') >= 0) {
                        nickNameMsg = "닉네임에는 ';'나 '@' 기호를 사용할 수 없습니다.";
                        continue SetNickname;
                    }

                    // 중복체크
                    try {
                        while (true) {
                            out.println(userName);
                            String isMessage = in.readLine();

                            if (isMessage.equals("OK@nick")) {
                                PrintWriter nameWriter = new PrintWriter(
                                        new BufferedWriter(
                                                new FileWriter(new File("src/chatting_ui/clientName.txt"))));
                                nameWriter.println(userName);
                                nameWriter.close();

                                return;

                            } else if (isMessage.equals("EXIST@nick")) {
                                nickNameMsg = "이미 사용 중인 닉네임입니다.";
                                continue SetNickname;
                            }
                        }
                    } catch (Exception e) {
                        System.out.println("오류 발생: " + e.getMessage());
                    }
                } else {
                    nickNameMsg = "올바르지 않은 닉네임입니다.";
                    continue SetNickname;
                }
            }
        } else {
            out.println(userName);
            try {
                if (in.readLine() != null) {
                    return;
                }
            } catch (Exception e) {
                System.out.println("오류 발생: " + e.getMessage());
            }
        }
    }

    void changeNickname() {
        String nickNameMsg = "변경할 닉네임을 입력해 주세요.";

        ChangeNickname: while (true) {
            String newName = JOptionPane.showInputDialog(nickNameMsg, userName).strip();

            if (newName.length() > 0) {
                if (newName.equals(userName)) {
                    return;
                }

                if (newName.indexOf(';') >= 0 || newName.indexOf('@') >= 0) {
                    nickNameMsg = "닉네임에는 ';'나 '@' 기호를 사용할 수 없습니다.";
                    continue ChangeNickname;
                }

                // 중복체크
                try {
                    out.println(newName + "@change");
                    while (true) {
                        if (changed != Status.WAITING) {
                            break;
                        }
                    }

                    if (changed == Status.TRUE) {
                        userName = newName;

                        PrintWriter nameWriter = new PrintWriter(
                                new BufferedWriter(
                                        new FileWriter(new File("src/chatting_ui/clientName.txt"))));
                        nameWriter.println(userName);
                        nameWriter.close();

                        changed = Status.WAITING;
                        return;

                    } else if (changed == Status.FALSE) {
                        nickNameMsg = "이미 사용 중인 닉네임입니다.";
                        changed = Status.WAITING;
                        continue ChangeNickname;
                    }
                } catch (Exception e) {
                    System.out.println("오류 발생: " + e.getMessage());
                }
            } else {
                return;
            }
        }
    }

    void viewMembers() {
        if (viewMode) {
            membersPanel.removeAll();
            memScroll.setVisible(false);
            viewMode = false;
        } else {
            int nextMemberLocation = 10;
            nameList.clear();

            JLabel myLb = new JLabel("      " + userName);
            myLb.setFont(new Font("Sans Serif", Font.BOLD, 15));
            myLb.setOpaque(true);
            myLb.setBackground(new Color(239, 232, 180));
            myLb.setBounds(0, nextMemberLocation, 400, 40);

            membersPanel.add(myLb);
            nextMemberLocation += 50;

            membersPanel.add(nickChangeBt);
            membersPanel.setComponentZOrder(nickChangeBt, 0);

            out.println("@viewNickname");
            while (true) {
                System.out.println("waiting...");
                if (added == true) {
                    break;
                }
            }

            for (String name : nameList) {
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
            added = false;
            viewMode = true;
        }
    }

    void sendMessage() {
        String msg = textField.getText().strip();
        textField.setText("");
        textField.requestFocus();

        if (msg.length() == 0) {
            return;
        }
        out.println(userName + ";" + msg);
    }

    synchronized void readMessage() {
        String input = "";
        try {
            input = in.readLine();
        } catch (Exception e) {
            System.out.println("오류 발생: " + e.getMessage());
        }

        if (input.equals("OK@nick")) {
            changed = Status.TRUE;
            return;
        } else if (input.equals("EXIST@nick")) {
            changed = Status.FALSE;
            return;
        } else if (input.equals("@viewend@" + userName)) {
            added = true;
            return;
        } else if (input.indexOf("@view@" + userName) >= 0) {
            String name = input.substring(0, input.indexOf('@'));
            if (!name.equals(userName)) {
                nameList.add(name);
            }
            return;
        }

        if (input.indexOf(';') == -1) {
            if (input.indexOf('@') == 0) {
                JLabel noticeLb = new JLabel(input.substring(1));

                noticeLb.setHorizontalAlignment(JLabel.CENTER);
                noticeLb.setBounds(10, nextMsgLocation, 340, 20);
                noticeLb.setFont(new Font("Sans Serif", Font.PLAIN, 13));
                noticeLb.setForeground(Color.GRAY);
                msgPanel.add(noticeLb);

                lastSpeaker = "";
                nextMsgLocation += 25;
            }
        } else {
            String sendName = input.substring(0, input.indexOf(';'));
            String sendTxt = input.substring(input.indexOf(';') + 1);

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

        if (nextMsgLocation >= 435) {
            // 패널 크기 갱신
            msgPanel.setPreferredSize(new Dimension(350, nextMsgLocation));
            msgPanel.revalidate();
        }
        repaint(); // 변경사항을 창에 반영

        SwingUtilities.invokeLater(() -> { // 스크롤바 내리기: 나중에 반영
            scroll.getVerticalScrollBar().setValue(scroll.getVerticalScrollBar().getMaximum());
        });
        return;
    }

    public static void main(String[] args) {
        try {
            socket = new Socket("localhost", 12345);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            Client client = new Client();

            while (in != null) {
                client.readMessage();
            }
        } catch (Exception e) {
            System.out.println("오류 발생: " + e.getMessage());
        }
    }
}