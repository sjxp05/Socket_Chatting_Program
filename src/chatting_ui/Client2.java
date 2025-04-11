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

public class Client2 extends JFrame {
    JLabel roomName = new JLabel("새로운 채팅방");
    JPanel msgPanel = new JPanel();
    JScrollPane scroll = new JScrollPane(msgPanel);
    JTextField textField = new JTextField();
    JButton sendBt = new JButton("전송");
    JButton exitBt = new JButton("나가기");

    JButton membersBt = new JButton("참여자");
    JPanel membersPanel = new JPanel();
    JScrollPane memScroll = new JScrollPane(membersPanel);
    JButton nickChangeBt = new JButton("이름 변경");

    private SyncOnUpdate sync = new SyncOnUpdate();
    private ArrayList<String> nameList = new ArrayList<>();

    private static Socket socket = null;
    private static BufferedReader in;
    private static PrintWriter out;

    private String userHome = System.getProperty("user.home");
    private File saveFile = new File(userHome, "chat-client2-info.txt");

    private int userID;
    private String userName = "";
    private int lastSpeakerID = -1;
    private int nextMsgLocation = 10;
    private boolean viewMode = false;
    private boolean added = false;

    private Client2() {
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
                        sendMessage();
                    } else if (option.equals("change")) {
                        changeNickname();
                    } else if (option.equals("members")) {
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
        if (saveFile.exists()) {
            try {
                BufferedReader nameReader = new BufferedReader(new FileReader(saveFile));
                userID = Integer.parseInt(nameReader.readLine());
                userName = nameReader.readLine();
                nameReader.close();
            } catch (Exception e) {
                System.exit(1);
            }
        }

        if (userName.strip().length() > 0) {
            try {
                out.println("" + userID + "@" + userName);
                String confirmMsg = in.readLine();
                if (confirmMsg.indexOf("OK@") == 0) {
                    return;
                }
            } catch (Exception e) {
                System.exit(1);
            }
        }

        String nickNameMsg = "채팅방에서 사용할 닉네임을 입력해 주세요.";

        SetNickname: while (true) {
            try {
                userName = JOptionPane.showInputDialog(nickNameMsg).strip();
            } catch (Exception e) {
                System.exit(1);
            }

            if (userName.length() == 0 || userName.length() > 15) {
                nickNameMsg = "닉네임은 1~15자이어야 합니다.";
                continue SetNickname;
            }

            if (userName.indexOf(';') >= 0 || userName.indexOf('@') >= 0) {
                nickNameMsg = "닉네임에는 ';'나 '@' 기호를 사용할 수 없습니다.";
                continue SetNickname;
            }

            try {
                out.println(userName);
                String confirmMsg = in.readLine();

                if (confirmMsg.indexOf("OK@") == 0) {
                    userID = Integer.parseInt(confirmMsg.substring(confirmMsg.indexOf("@") + 1));

                    PrintWriter nameWriter = new PrintWriter(new BufferedWriter(new FileWriter(saveFile)));
                    nameWriter.println(userID);
                    nameWriter.println(userName);
                    nameWriter.close();

                    return;
                }

            } catch (Exception e) {
                System.exit(1);
            }
        }
    }

    void changeNickname() {
        String nickNameMsg = "변경할 닉네임을 입력해 주세요.";

        ChangeNickname: while (true) {
            String newName;
            try {
                newName = JOptionPane.showInputDialog(nickNameMsg, userName).strip();
            } catch (Exception e) {
                return;
            }

            if (newName.equals(userName)) {
                return;
            }

            if (newName.length() == 0 || newName.length() > 15) {
                nickNameMsg = "닉네임은 1~15자이어야 합니다.";
                continue ChangeNickname;
            }

            if (newName.indexOf(';') >= 0 || newName.indexOf('@') >= 0) {
                nickNameMsg = "닉네임에는 ';'나 '@' 기호를 사용할 수 없습니다.";
                continue ChangeNickname;
            }

            try {
                out.println(newName + "@change");
                userName = newName;

                PrintWriter nameWriter = new PrintWriter(new BufferedWriter(new FileWriter(saveFile)));
                nameWriter.println(userID);
                nameWriter.println(userName);
                nameWriter.close();
                return;

            } catch (Exception e) {
                System.exit(1);
            }
        }

    }

    void viewMembers() {
        if (viewMode) {
            membersBt.setText("참여자");
            membersPanel.removeAll();
            memScroll.setVisible(false);
            viewMode = false;
        } else {
            membersBt.setText("채팅");
            int nextMemberLocation = 10;
            nameList.clear();

            JLabel myLb = new JLabel("      " + userName);
            myLb.setFont(new Font("Sans Serif", Font.BOLD, 15));
            myLb.setOpaque(true);
            myLb.setBackground(new Color(245, 240, 190));
            myLb.setBounds(0, nextMemberLocation, 400, 40);

            membersPanel.add(myLb);
            nextMemberLocation += 50;

            membersPanel.add(nickChangeBt);
            membersPanel.setComponentZOrder(nickChangeBt, 0);

            out.println("@viewNickname@");
            while (true) {
                System.out.print("");
                if (added == true) {
                    nameList.sort(null);
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
        out.println(userName + ";" + msg + ';' + userID);
    }

    synchronized void readMessage() {
        String input = "";
        try {
            input = in.readLine();
        } catch (Exception e) {
            System.exit(1);
        }

        if (input.equals("@viewend")) {
            added = true;
            return;
        } else if (input.indexOf("@view@") >= 0) {
            String[] info = input.split("@");

            if (Integer.parseInt(info[2]) != userID) {
                nameList.add(info[0]);
            }
            return;
        }

        if (input.indexOf(';') == -1) {
            if (input.indexOf('@') == 0) {
                if (viewMode) {
                    viewMembers();
                }

                JLabel noticeLb = new JLabel(input.substring(1));
                noticeLb.setHorizontalAlignment(JLabel.CENTER);
                noticeLb.setBounds(10, nextMsgLocation, 340, 20);
                noticeLb.setFont(new Font("Sans Serif", Font.PLAIN, 13));
                noticeLb.setForeground(Color.GRAY);
                msgPanel.add(noticeLb);

                lastSpeakerID = -1;
                nextMsgLocation += 25;
            }
        } else {
            if (viewMode) {
                viewMembers();
            }

            String[] receivedMsg = input.split(";");

            JLabel nameLb = new JLabel(receivedMsg[0]);
            JLabel msgLb = new JLabel(receivedMsg[1]);
            int sendID = Integer.parseInt(receivedMsg[2]);

            if (sendID == userID) {
                if (sendID == lastSpeakerID) {
                    nextMsgLocation -= 10;
                } else {
                    nameLb.setHorizontalAlignment(JLabel.RIGHT);
                    nameLb.setBounds(0, nextMsgLocation, 340, 20);
                    nameLb.setFont(new Font("Sans Serif", Font.PLAIN, 14));
                    nameLb.setForeground(Color.GRAY);
                    msgPanel.add(nameLb);

                    lastSpeakerID = sendID;
                    nextMsgLocation += 22;
                }

                msgLb.setHorizontalAlignment(JLabel.RIGHT);
                msgLb.setBounds(0, nextMsgLocation, 340, 20);
                msgLb.setFont(new Font("Sans Serif", Font.PLAIN, 15));
                msgPanel.add(msgLb);
                nextMsgLocation += 32;

            } else {
                if (sendID == lastSpeakerID) {
                    nextMsgLocation -= 10;
                } else {
                    nameLb.setHorizontalAlignment(JLabel.LEFT);
                    nameLb.setBounds(10, nextMsgLocation, 340, 20);
                    nameLb.setFont(new Font("Sans Serif", Font.PLAIN, 14));
                    nameLb.setForeground(Color.GRAY);
                    msgPanel.add(nameLb);

                    lastSpeakerID = sendID;
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
            msgPanel.setPreferredSize(new Dimension(350, nextMsgLocation));
            msgPanel.revalidate();
        }
        repaint();

        SwingUtilities.invokeLater(() -> {
            scroll.getVerticalScrollBar().setValue(scroll.getVerticalScrollBar().getMaximum());
        });
        return;
    }

    public static void main(String[] args) {
        try {
            socket = new Socket("localhost", 12345);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            Client2 client2 = new Client2();

            while (in != null) {
                client2.readMessage();
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "서버 연결에 실패했습니다 ㅠㅠ\n황지인에게 서버를 열어달라고 요청해보세요!");
        }
    }
}