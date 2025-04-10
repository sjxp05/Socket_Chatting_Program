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
    JLabel roomName = new JLabel("새로운 채팅방"); // 채팅방 이름
    JPanel msgPanel = new JPanel(); // 메시지 띄우는 창
    JScrollPane scroll = new JScrollPane(msgPanel); // 메시지 넣은 스크롤페인
    JTextField textField = new JTextField(); // 보낼 메시지 입력하는 곳
    JButton sendBt = new JButton("전송"); // 전송 버튼
    JButton exitBt = new JButton("나가기"); // 방 나가기 버튼

    JButton membersBt = new JButton("참여자"); // 모든 사용자 목록 확인
    JPanel membersPanel = new JPanel(); // 사용자 목록 띄우는 창
    JScrollPane memScroll = new JScrollPane(membersPanel); // 사용자 목록 넣은 스크롤페인
    JButton nickChangeBt = new JButton("이름 변경"); // 닉변 버튼

    private SyncOnUpdate sync = new SyncOnUpdate(); // 버튼 스레드 동기화시키는 객체
    private ArrayList<String> nameList = new ArrayList<>(); // 사용자 목록 저장할 리스트

    private static Socket socket = null;
    private static BufferedReader in;
    private static PrintWriter out;

    // 닉네임 변경 시 신호 확인을 위한 enum 변수
    enum Status {
        WAITING, TRUE, FALSE
    };

    private Status changed = Status.WAITING;

    private String userName = ""; // 사용자 이름
    private String lastSpeaker = ""; // 마지막으로 메시지 보낸 사람의 이름
    private int nextMsgLocation = 10; // 패널에서 다음 메시지 라벨을 어디 띄울지 결정하기 위함
    private boolean viewMode = false; // 사용자 목록을 띄워놨는지 여부
    private boolean added = false; // 목록에 모든 사용자를 추가했는지 여부

    private Client() {
        setTitle("New Chat");
        setSize(400, 600);
        setResizable(false);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        Container pane = getContentPane();
        pane.setLayout(null);

        // 채팅방 이름
        roomName.setFont(new Font("Sans Serif", Font.BOLD, 15));
        roomName.setHorizontalAlignment(JLabel.CENTER);
        roomName.setBounds(92, 5, 200, 40);
        pane.add(roomName);

        // 방 나가기 버튼
        exitBt.setFont(new Font("Sans Serif", Font.BOLD, 12));
        exitBt.setBounds(10, 5, 70, 40);
        exitBt.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });
        pane.add(exitBt);

        // 참여자 목록 보기 버튼
        membersBt.setFont(new Font("Sans Serif", Font.BOLD, 12));
        membersBt.setBounds(307, 5, 70, 40);
        membersBt.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sync.start("members");
            }
        });
        pane.add(membersBt);

        // 참여자 목록 패널
        membersPanel.setBounds(0, 0, 400, 435);
        membersPanel.setBackground(Color.LIGHT_GRAY);
        membersPanel.setLayout(null);

        // 참여자 목록 스크롤페인
        memScroll.setBounds(10, 50, 368, 435);
        memScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        memScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        pane.add(memScroll);
        memScroll.setVisible(false);

        // 닉변버튼
        nickChangeBt.setFont(new Font("Sans Serif", Font.BOLD, 10));
        nickChangeBt.setBounds(260, 15, 80, 30);
        nickChangeBt.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sync.start("change");
            }
        });

        // 메시지 띄우는 패널
        msgPanel.setBounds(0, 0, 350, 435);
        msgPanel.setLayout(null);

        // 메시지 스크롤페인
        scroll.setBounds(10, 50, 368, 435);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        pane.add(scroll);

        // 보낼 메시지 적는 칸
        textField.setFont(new Font("Sans Serif", Font.PLAIN, 15));
        textField.setBounds(10, 500, 290, 50);
        textField.requestFocus();
        textField.addKeyListener(new PressEnter()); // 텍스트필드에서 엔터 키를 누를 시 바로 전송됨
        pane.add(textField);

        // 전송버튼
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
        setNickname(); // 처음 접속 시 닉네임 설정
    }

    // 버튼 스레드 동기화시키는 클래스
    class SyncOnUpdate implements Runnable {
        boolean flag = false;
        String option; // 버튼별로 기능을 다르게

        SyncOnUpdate() {
            Thread thread = new Thread(this);
            thread.start();
        }

        // 버튼을 누르면 이 함수가 작동되어 정해진 기능을 함
        synchronized void start(String optionReceived) {
            flag = true;
            option = optionReceived;
            this.notify(); // 스레드를 run 시켜줌
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

        // notify되었을 때 돌아감
        @Override
        public void run() {
            while (true) {
                waitForStart();
                try {
                    if (option.equals("send")) { // 일반 메시지 보내기
                        sendMessage();
                    } else if (option.equals("change")) { // 닉변 버튼 눌렀을때
                        changeNickname();
                    } else if (option.equals("members")) { // 사용자목록 보기 눌렀을때
                        viewMembers();
                    }
                    flag = false;
                } catch (Exception e) {
                    System.out.println("오류 발생: " + e.getMessage());
                }
            }
        }
    }

    // 엔터키 눌렀을 때 전송(전송버튼과 같은 기능)
    class PressEnter implements KeyListener {
        @Override
        public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                sync.start("send"); // 전송버튼과 똑같이 함수 호출
            }
        }

        @Override
        public void keyTyped(KeyEvent e) {
        }

        @Override
        public void keyReleased(KeyEvent e) {
        }
    }

    // 처음 닉네임 정하기
    void setNickname() {
        try {
            BufferedReader nameReader = new BufferedReader(new FileReader("src/chatting_ui/clientName.txt"));
            userName = nameReader.readLine();
            nameReader.close();
        } catch (FileNotFoundException e) { // 파일이 없을 경우 넘어가기
            System.out.print("");
        } catch (IOException e) { // 기타 오류의 경우 프로그램 강제 종료
            System.out.println("오류 발생: " + e.getMessage());
            System.exit(1);
        } finally {
            userName = "";
        }

        // 이미 쓰던 닉네임이 있는 경우
        if (userName != null && userName.strip().length() > 0) {
            try {
                out.println(userName);
                String isMessage = in.readLine();

                if (isMessage.equals("OK@nick")) {
                    return;
                }
                /*
                 * 겹치지 않으면 그냥 쓰기 (return)
                 * 방에 있는 다른 사람과 겹치면 다음 단계로 넘어가기 (새로운 닉네임 받기)
                 */
            } catch (Exception e) {
                System.out.println("오류 발생: " + e.getMessage());
            }
        }

        String nickNameMsg = "채팅방에서 사용할 닉네임을 입력해 주세요.";

        // 조건 충족시킬 때까지 계속해서 닉네임 받기
        SetNickname: while (true) {
            // 새로운 닉네임 받기
            userName = JOptionPane.showInputDialog(nickNameMsg).strip();

            // 길이제한
            if (userName.length() == 0 || userName.length() > 15) {
                nickNameMsg = "닉네임은 1~15자이어야 합니다.";
                continue SetNickname;
            }

            // 기호제한
            if (userName.indexOf(';') >= 0 || userName.indexOf('@') >= 0) {
                nickNameMsg = "닉네임에는 ';'나 '@' 기호를 사용할 수 없습니다.";
                continue SetNickname;
            }

            // 중복체크
            try {
                while (true) {
                    out.println(userName); // 스트림을 통해 핸들러 객체에서 읽어야 서버에 있는지 확인 가능
                    String isMessage = in.readLine(); // 핸들러 객체기 스트림으로 보낸 신호 읽기

                    if (isMessage.equals("OK@nick")) { // 등록에 성공한 경우
                        // 새 닉네임을 파일에 저장
                        PrintWriter nameWriter = new PrintWriter(
                                new BufferedWriter(
                                        new FileWriter(new File("src/chatting_ui/clientName.txt"))));
                        nameWriter.println(userName);
                        nameWriter.close();

                        return;

                    } else if (isMessage.equals("EXIST@nick")) { // 중복된 경우
                        nickNameMsg = "이미 사용 중인 닉네임입니다.";
                        continue SetNickname;
                    }
                }
            } catch (Exception e) {
                System.out.println("오류 발생: " + e.getMessage());
            }
        }
    }

    // 닉네임 바꾸기 함수
    void changeNickname() {
        String nickNameMsg = "변경할 닉네임을 입력해 주세요.";

        ChangeNickname: while (true) {
            // 변경할 닉네임 입력받기 (현재 이름이 기본으로 입력되어 있음)
            String newName = JOptionPane.showInputDialog(nickNameMsg, userName).strip();

            // 이름이 바뀌지 않았을 경우
            if (newName.equals(userName)) {
                return;
            }

            // 길이제한
            if (newName.length() == 0 || newName.length() > 15) {
                nickNameMsg = "닉네임은 1~15자이어야 합니다.";
                continue ChangeNickname;
            }

            // 기호제한
            if (newName.indexOf(';') >= 0 || newName.indexOf('@') >= 0) {
                nickNameMsg = "닉네임에는 ';'나 '@' 기호를 사용할 수 없습니다.";
                continue ChangeNickname;
            }

            // 중복체크
            try {
                out.println(newName + "@change"); // 스트림을 통해 핸들러에 중복 여부 확인 요청
                /*
                 * 신호 받을 때까지 기다리기!
                 * - 첫 등록 시에는 대화에 참여중이 아니었기에 처음에 오는 신호만 받으면 되지만
                 * - 대화 참여 중에 닉변을 시도했을 때는 뭐가 닉변 관련 메시지이고 뭐가 일반메시지인지 구별해서 받아야 함!!
                 * - 닉변관련 메시지를 받았는지 확인하기 위해 changed 변수를 활용함
                 */
                while (true) {
                    if (changed != Status.WAITING) {
                        break;
                    }
                }

                if (changed == Status.TRUE) { // 등록 성공 시
                    userName = newName; // 사용자 이름을 바뀐 이름으로 저장

                    // 파일에 바뀐 닉네임을 저장
                    PrintWriter nameWriter = new PrintWriter(
                            new BufferedWriter(
                                    new FileWriter(new File("src/chatting_ui/clientName.txt"))));
                    nameWriter.println(userName);
                    nameWriter.close();

                    changed = Status.WAITING; // changed 변수 초기화
                    return;

                } else if (changed == Status.FALSE) { // 중복될 경우
                    nickNameMsg = "이미 사용 중인 닉네임입니다.";
                    changed = Status.WAITING; // changed 변수 초기화
                    continue ChangeNickname;
                }
            } catch (Exception e) {
                System.out.println("오류 발생: " + e.getMessage());
            }
        }
    }

    // 참여자 목록 보기 함수
    void viewMembers() {
        if (viewMode) { // 목록이 띄워져 있는 상태에서 버튼을 누르면 숨길 수 있음
            membersBt.setText("참여자");
            membersPanel.removeAll();
            memScroll.setVisible(false);
            viewMode = false;
        } else {
            membersBt.setText("채팅");
            int nextMemberLocation = 10; // 다음 참여자 라벨을 띄울 위치
            nameList.clear(); // 혹시 모를 인원수 변화를 대비해 리스트 초기화

            // 본인 정보 포함된 라벨 표시
            JLabel myLb = new JLabel("      " + userName);
            myLb.setFont(new Font("Sans Serif", Font.BOLD, 15));
            myLb.setOpaque(true);
            myLb.setBackground(new Color(239, 232, 180));
            myLb.setBounds(0, nextMemberLocation, 400, 40);

            membersPanel.add(myLb);
            nextMemberLocation += 50; // 다음 라벨의 위치 조정

            // 닉변 버튼 보이게 하기
            membersPanel.add(nickChangeBt);
            membersPanel.setComponentZOrder(nickChangeBt, 0); // 맨 앞으로 이동시키기!

            // 스트림을 통해 핸들러에 사용자목록 전송 요청
            out.println("@viewNickname");
            // 모든 사용자를 목록에 추가 완료할 때까지 기다리기
            while (true) {
                System.out.print("");
                if (added == true) {
                    break;
                }
            }

            // 각 사용자 이름을 넣은 라벨 추가
            for (String name : nameList) {
                JLabel nameLb = new JLabel("      " + name);
                nameLb.setFont(new Font("Sans Serif", Font.PLAIN, 15));
                nameLb.setOpaque(true);
                nameLb.setBackground(Color.WHITE);
                nameLb.setBounds(0, nextMemberLocation, 400, 40);

                membersPanel.add(nameLb);
                nextMemberLocation += 50;
            }

            // 멤버 수가 많을 경우 창 키우기 (스크롤바 자동 생성)
            if (nextMemberLocation >= 435) {
                membersPanel.setPreferredSize(new Dimension(400, nextMemberLocation));
                membersPanel.revalidate();
                repaint();
            }

            // 목록 패널 보이게 하기
            memScroll.setVisible(true);
            added = false; // 목록 추가 완료됐는지 여부 초기화
            viewMode = true;
        }
    }

    // 일반 메시지 전송 함수
    void sendMessage() {
        String msg = textField.getText().strip();
        textField.setText(""); // 한 번 보낸 내용은 지우기
        textField.requestFocus(); // 커서를 텍스트필드로 다시 옮기기

        if (msg.length() == 0) { // 공백일 경우 안 보내기
            return;
        }
        out.println(userName + ";" + msg); // 스트림으로 사용자명 및 내용 보내기
    }

    // 스트림으로 받은 내용 읽어서 표시하거나 여러 기능을 하는 함수
    synchronized void readMessage() {
        String input = "";
        try {
            input = in.readLine();
        } catch (Exception e) {
            System.out.println("오류 발생: " + e.getMessage());
        }

        if (input.equals("OK@nick")) { // 닉변 요청 후 성공했을 때
            changed = Status.TRUE;
            return;
        } else if (input.equals("EXIST@nick")) { // 닉변 요청 후 실패했을 때
            changed = Status.FALSE;
            return;
        } else if (input.equals("@viewend")) { // 목록보기 요청 후 모든 목록을 읽어왔을 때
            added = true; // 목록에 모든 사용자를 추가했다는 의미로 변수 값을 바꿔주기
            return;
        } else if (input.indexOf("@view") >= 0) {
            String name = input.substring(0, input.indexOf('@'));
            // 사용자 목록에 이름 추가 (단 본인 이름은 추가하지 않고 맨 앞에 띄움!)
            if (!name.equals(userName)) {
                nameList.add(name);
            }
            return;
        }

        if (input.indexOf(';') == -1) {
            if (input.indexOf('@') == 0) { // 입장, 퇴장 안내 메시지의 경우
                // 참여자 목록 숨기고 새로 온 메시지를 보여주기
                if (viewMode) {
                    viewMembers();
                }

                JLabel noticeLb = new JLabel(input.substring(1));
                noticeLb.setHorizontalAlignment(JLabel.CENTER);
                noticeLb.setBounds(10, nextMsgLocation, 340, 20);
                noticeLb.setFont(new Font("Sans Serif", Font.PLAIN, 13));
                noticeLb.setForeground(Color.GRAY);
                msgPanel.add(noticeLb);

                lastSpeaker = ""; // 앞으로 어떤 참가자가 대화를 시작하든 앞에 닉네임이 표시됨 (누가 말하는지 혼동 방지)
                nextMsgLocation += 25;
            }
        } else { // 사용자가 보낸 일반 메시지일 경우
            if (viewMode) {
                viewMembers();
            }

            String sendName = input.substring(0, input.indexOf(';'));
            String sendTxt = input.substring(input.indexOf(';') + 1);

            JLabel nameLb = new JLabel(sendName);
            JLabel msgLb = new JLabel(sendTxt);

            if (sendName.equals(userName)) { // 본인이 보낸 메시지는 오른쪽
                if (sendName.equals(lastSpeaker)) { // 연속해서 보낸 메시지는 닉네임을 위에 표시하지 않음
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

            } else { // 다른 사람이 보낸 메시지는 왼쪽
                if (sendName.equals(lastSpeaker)) { // 연속으로 보낸 메시지는 닉네임을 위에 표시하지 않음
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

        // 필요한 경우 패널 크기 갱신
        if (nextMsgLocation >= 435) {
            msgPanel.setPreferredSize(new Dimension(350, nextMsgLocation));
            msgPanel.revalidate();
        }
        repaint(); // 변경사항을 창에 반영

        SwingUtilities.invokeLater(() -> { // 스크롤바 내리기: 모든 변경사항이 반영된 뒤 나중에 반영
            scroll.getVerticalScrollBar().setValue(scroll.getVerticalScrollBar().getMaximum());
        });
        return;
    }

    public static void main(String[] args) {
        try {
            // 서버에 연결 및 i/o 스트림 지정
            socket = new Socket("localhost", 12345);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            Client client = new Client(); // 새 클라이언트 객체 생성 및 닉네임 정하기

            while (in != null) {
                client.readMessage(); // 닉네임 설정된 후로는 계속 새로 오는 메시지 받기
            }
        } catch (Exception e) {
            System.out.println("오류 발생: " + e.getMessage());
        }
    }
}