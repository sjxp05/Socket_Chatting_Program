package chatting_ui.client;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.*;

// UI 표시/변경만 담당하는 클래스
public class ChatUI extends JFrame {
    JLabel roomName = new JLabel("새로운 채팅방"); // 방 이름 라벨
    JButton sendBt = new JButton("전송"); // 전송버튼
    JButton exitBt = new JButton("나가기"); // 나가기 버튼

    JPanel msgPanel = new JPanel(); // 메시지 표시 창
    JScrollPane scroll = new JScrollPane(msgPanel); // 메시지창을 넣은 스크롤페인
    JTextArea textInput = new JTextArea(); // 메시지 입력 칸
    JScrollPane textScroll = new JScrollPane(textInput); // 메시지 입력 칸을 넣은 스크롤페인

    JButton membersBt = new JButton("참여자"); // 유저 목록 보기/채팅창 돌아가기 버튼
    JPanel membersPanel = new JPanel(); // 유저 목록 띄우는 창
    JScrollPane memScroll = new JScrollPane(membersPanel); // 유저목록 창을 넣은 스크롤페인
    JButton nickChangeBt = new JButton("이름 변경"); // 닉변버튼

    private SyncOnUpdate sync = new SyncOnUpdate(); // 버튼 누를 때 동기화해주는 Runnable 객체

    private volatile boolean shiftPressed = false; // 쉬프트키 눌렀는지 여부
    private volatile boolean viewMode = false; // 유저 목록 보기가 켜져 있는지
    private volatile int nextMsgLocation = 10; // 채팅창에서 다음 메시지가 표시될 위치
    /*
     * volatile 필드는 모두 M.M 내부 캐시에 저장되어 실시간 접근이 필요한 변수들임!
     */

    ChatUI() {
        setTitle("New Chat");
        setSize(400, 600);
        setResizable(false);
        setLocationRelativeTo(null); // 화면 가운데에 띄우기
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        Container pane = getContentPane();
        pane.setLayout(null);

        roomName.setFont(new Font("Sans Serif", Font.BOLD, 15));
        roomName.setHorizontalAlignment(JLabel.CENTER);
        roomName.setBounds(92, 5, 200, 40);
        pane.add(roomName);

        exitBt.setFont(new Font("Sans Serif", Font.BOLD, 12));
        exitBt.setBounds(10, 5, 70, 40);
        exitBt.addActionListener(new ActionListener() { // 나가기 버튼 누르면 바로 종료, 서버와 연결 끊어짐
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });
        pane.add(exitBt);

        membersBt.setFont(new Font("Sans Serif", Font.BOLD, 12));
        membersBt.setBounds(307, 5, 70, 40);
        membersBt.addActionListener(new ActionListener() { // 참여자 보기 버튼 누르면 view에 해당하는 함수 실행
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
        nickChangeBt.addActionListener(new ActionListener() { // 닉변 버튼 누르면 change에 해당하는 함수 실행
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
        textInput.addKeyListener(new PressEnter()); // 텍스트창에서 shift 또는 enter 키를 누를 시 작동함

        textScroll.setBounds(10, 500, 290, 50);
        textScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        textScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        pane.add(textScroll);

        sendBt.setFont(new Font("Sans Serif", Font.BOLD, 15));
        sendBt.setBounds(307, 500, 70, 50);
        sendBt.addActionListener(new ActionListener() { // 전송버튼 누를시 send에 해당하는 함수 호출
            @Override
            public void actionPerformed(ActionEvent e) {
                sync.start("send");
            }
        });
        pane.add(sendBt);

        setVisible(true);
    }

    // 버튼 눌렀을 때 스레드 관리를 위한 Runnable 클래스
    class SyncOnUpdate implements Runnable {
        private boolean flag = false;
        String option = ""; // 호출할 함수의 종류

        SyncOnUpdate() {
            Thread th = new Thread(this);
            th.start();
        }

        synchronized void start(String optionReceived) {
            flag = true;
            option = optionReceived;
            this.notify(); // 버튼이 눌렸을 때 스레드 깨우기
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
                        writeMessage(); // UI 클래스 내부 메소드
                    } else if (option.equals("change")) {
                        Main.changeNickname(); // Main 클래스에 있는 static 메소드
                    } else if (option.equals("view")) {
                        viewMembers(); // UI 클래스 내부 메소드
                    }
                    flag = false;
                } catch (Exception e) {
                    System.exit(1);
                }
            }
        }
    }

    // 텍스트창에서 쉬프트/엔터 키 입력받았을 때의 동작
    class PressEnter implements KeyListener {
        @Override
        public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_SHIFT) { // 쉬프트키가 눌린 상태일 때
                shiftPressed = true;
            }

            if (e.getKeyCode() == KeyEvent.VK_ENTER) { // 쉬프트와 엔터키(and/or) 눌린 상태일때
                if (shiftPressed) { // 쉬프트키와 동시에 눌렸을 때: 줄바꾸기
                    StringBuffer currentTxt = new StringBuffer(textInput.getText());
                    currentTxt.append("\n");

                    SwingUtilities.invokeLater(() -> {
                        textInput.setText(currentTxt.toString());
                        textInput.requestFocus();
                    });
                } else { // 쉬프트키 없이 단독: 전송
                    sync.start("send");
                }
            }
        }

        @Override
        public void keyTyped(KeyEvent e) {
        }

        @Override
        public void keyReleased(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_SHIFT) { // 쉬프트키를 떼었을 때
                shiftPressed = false;
            }
        }
    }

    void viewMembers() {
        if (viewMode) { // 이미 표시되어 있을 때 끄기
            viewMode = false;
            membersBt.setText("참여자");
            membersPanel.removeAll(); // 패널 안의 모든 요소 지우기
            memScroll.setVisible(false);
            return;

        } else { // 표시 안된 상태일 때 표시하기
            viewMode = true;
            membersBt.setText("채팅");
            Main.viewRequest(); // Main클래스의 static 메소드 호출로 서버에 목록 전송 요청

            int nextMemberLocation = 10; // 패널 안에서 다음 라벨이 올 위치

            // 사용자 본인 정보 (맨 위에!)
            JLabel myLb = new JLabel("      " + Main.userName);
            myLb.setFont(new Font("Sans Serif", Font.BOLD, 15));
            myLb.setOpaque(true);
            myLb.setBackground(new Color(245, 240, 190));
            myLb.setBounds(0, nextMemberLocation, 400, 40);

            membersPanel.add(myLb);
            nextMemberLocation += 50;

            membersPanel.add(nickChangeBt); // 닉변버튼 표시
            membersPanel.setComponentZOrder(nickChangeBt, 0); // 라벨 위에 표시되도록 맨 위로 올려주기

            // 다른 참여자 목록 표시, 라벨로 모두 추가
            for (String name : Main.nameList) {
                JLabel nameLb = new JLabel("      " + name);
                nameLb.setFont(new Font("Sans Serif", Font.PLAIN, 15));
                nameLb.setOpaque(true);
                nameLb.setBackground(Color.WHITE);
                nameLb.setBounds(0, nextMemberLocation, 400, 40);

                membersPanel.add(nameLb);
                nextMemberLocation += 50;
            }

            // 사람이 많을 시 패널 길이 조정 (스크롤바 자동 추가됨)
            if (nextMemberLocation >= 435) {
                membersPanel.setPreferredSize(new Dimension(400, nextMemberLocation));
                membersPanel.revalidate(); // 패널 크기 갱신
                repaint(); // 사용자 화면 갱신. 꼭 해줘야됨!
            }

            memScroll.setVisible(true);
            return;
        }
    }

    // 유저 목록 표시되어 있을 경우 새로고침
    void refresh() {
        if (viewMode) {
            viewMembers(); // 스레드관리 필요없는 창닫기 함수는 그냥 호출
            sync.start("view");
        }
    }

    // 텍스트창에 입력된 메시지를 메인함수로 보내주기
    void writeMessage() {
        String msg = textInput.getText().strip();
        SwingUtilities.invokeLater(() -> { // 텍스트 입력창 비우기
            textInput.setText("");
            textInput.requestFocus();
        });

        Main.sendMessage(msg);
    }

    // 사용자 입장/퇴장 안내 표시
    void showNotices(String noticeMsg) {
        JLabel noticeLb = new JLabel(noticeMsg);
        noticeLb.setHorizontalAlignment(JLabel.CENTER); // 가운데에 표시
        noticeLb.setBounds(10, nextMsgLocation, 340, 20);
        noticeLb.setFont(new Font("Sans Serif", Font.PLAIN, 13));
        noticeLb.setForeground(Color.GRAY);
        msgPanel.add(noticeLb);

        Main.lastSpeakerID = -1;
        nextMsgLocation += 25;

        // 길이가 길어질 경우 패널 길이 조정, 스크롤바 내리기
        SwingUtilities.invokeLater(() -> {
            if (nextMsgLocation >= 435) {
                msgPanel.setPreferredSize(new Dimension(350, nextMsgLocation));
                msgPanel.revalidate();
            }
            msgPanel.repaint(); // 이거 해줘야 사용자 화면에 반영됨!!

            SwingUtilities.invokeLater(() -> {
                scroll.getVerticalScrollBar().setValue(scroll.getVerticalScrollBar().getMaximum());
            });
            return;
        });

        // 유저 목록 표시되어 있을 경우 새로고침
        refresh();
    }

    // 사용자가 보낸 일반 메시지 표시
    void showMessage(String sendName, String sendMsg, int sendID, int height) {
        if (viewMode) { // 채팅창 표시하기
            viewMembers();
        }

        JLabel nameLb = new JLabel(sendName); // 메시지 보낸 사용자 이름 라벨
        JTextPane msgLb = new JTextPane(); // 전송된 메시지 표시 라벨
        msgLb.setContentType("text/html");
        msgLb.setEditable(false);
        msgLb.setOpaque(false);

        if (sendID == Main.userID) { // 사용자 본인의 메시지: 오른쪽에 표시
            nameLb.setHorizontalAlignment(JLabel.RIGHT);
            msgLb.setText(
                    "<html><body style='font-family: Segoe UI Emoji; font-size: 12px; text-align: right;'>" + sendMsg +
                            "</body></html>");
        } else { // 본인 외 다른 상대방의 메시지: 왼쪽에 표시
            nameLb.setHorizontalAlignment(JLabel.LEFT);
            msgLb.setText(
                    "<html><body style='font-family: Segoe UI Emoji; font-size: 12px; text-align: left;'>" + sendMsg +
                            "</body></html>");
        }

        if (sendID == Main.lastSpeakerID) { // 직전에 말한 사람과 같을 경우 이름 표시하지 않음
            nextMsgLocation -= 10;
        } else { // 이름 표시 라벨 배치
            nameLb.setBounds(10, nextMsgLocation, 330, 20);
            nameLb.setFont(new Font("Sans Serif", Font.PLAIN, 14));
            nameLb.setForeground(Color.GRAY);
            msgPanel.add(nameLb);

            Main.lastSpeakerID = sendID;
            nextMsgLocation += 22;
        }

        // 메시지 라벨 배치
        msgLb.setBounds(10, nextMsgLocation, 330, height);
        // msgLb.setFont(new Font("Sans Serif", Font.PLAIN, 15));
        msgPanel.add(msgLb);
        nextMsgLocation += (15 + height);

        // 내용이 길어지면 패널 길이 수정 및 스크롤바 내리기
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
