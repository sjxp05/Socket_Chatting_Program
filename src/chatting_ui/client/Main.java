package chatting_ui.client;

import java.io.*;
import java.net.*;
import java.util.ArrayList;

import javax.swing.JOptionPane;

// 클라이언트 메인(로직) 클래스
public class Main {
    static ChatUI ui = new ChatUI(); // UI 객체 생성
    /*
     * UI를 Main 내부에서 객체로 불러와야 하는 이유!
     * 
     * - ui를 객체없이 new로만 생성했을 때, main에서는 ui의 일부 기능이나 필드에만 접근할 뿐
     * 전체적인 상태에는 접근할 수 없게 됨
     * ==> main에서 ui 클래스 내부에 있는 static 메소드를 호출해야 하는 상황에서
     * ui 상태 상관없이 단순 함수 호출만 하기 때문에 사용자 입장에서 이상하게 뜰 수 있음!
     * 
     * - ui를 인스턴스로 생성하여 ui.(메소드명)으로 ui메소드에 접근
     * ==> ui 내의 해당 메소드는 static이 아닌 일반으로 선언할 수 있음,
     * ui의 상태를 반영하여 기능이 작동될 수 있음
     * ==> 에러 안남
     */

    // 서버 연결을 위한 소켓, 스트림 입출력 도구 선언
    private static Socket socket = null;
    private static BufferedReader in;
    private static PrintWriter out;

    // 사용자 정보 저장할 파일 위치
    private static final File saveFolder = new File(System.getProperty("user.home") + "/chatclient/");
    private static final File saveFile = new File(saveFolder, "chat-client-info.txt");

    // 유저목록 보기 기능을 위한 사용자 닉네임 리스트
    static ArrayList<String> nameList = new ArrayList<>();

    static int userID; // id와 사용자 닉네임은 ui 클래스에서도 접근 가능해야 함
    static volatile String userName = "";
    static volatile int lastSpeakerID = -1; // 마지막으로 말한 사람의 id
    static volatile boolean added = false; // 유저목록에 모든 사용자가 추가되었는지

    /*
     * volatile로 사용해야 하는 이유:
     * - volatile은 메인메모리의 캐시영역에 변수를 저장하여 값이 바뀌어도 바로 반영되게 해줌
     * - 실시간으로 상태를 확인해야 할 때 유용함
     */

    private Main() {
        setNickname();
    }

    // 처음 시작할 때 닉네임 설정하기
    void setNickname() {
        if (!saveFolder.exists()) { // 폴더가 존재하지 않을 때
            try {
                saveFolder.mkdir();
            } catch (Exception e) {
                System.exit(1);
            }
        }

        if (saveFile.exists()) { // 파일이 존재할 경우 (사용 기록이 있어 닉네임이 이미 존재함)
            try { // 파일에서 원래 닉네임 읽어오기
                BufferedReader nameReader = new BufferedReader(new FileReader(saveFile));
                userID = Integer.parseInt(nameReader.readLine());
                userName = nameReader.readLine();
                nameReader.close();
            } catch (Exception e) {
                System.exit(1);
            }
        }

        if (userName.strip().length() > 0) { // 서버에 사용자 입장 메시지 보내기
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

        // 새로운 닉네임이 필요할 경우
        String nickNameMsg = "채팅방에서 사용할 닉네임을 입력해 주세요.";
        int messageType = 3; // 안내 메시지의 아이콘 종류 결정

        SetNickname: while (true) {
            try {
                userName = JOptionPane.showInputDialog(ui, nickNameMsg, "닉네임 설정", messageType).strip();
            } catch (Exception e) {
                System.exit(1);
            }

            if (userName.length() == 0 || userName.length() > 15) { // 길이제한
                nickNameMsg = "닉네임은 1~15자이어야 합니다.";
                messageType = 2; // 메시지 아이콘을 '경고'로 바꾸기
                continue SetNickname;
            }

            if (userName.indexOf(';') >= 0 || userName.indexOf('@') >= 0) { // 기호제한
                nickNameMsg = "닉네임에는 ';'나 '@' 기호를 사용할 수 없습니다.";
                messageType = 2;
                continue SetNickname;
            }

            try {
                // 서버에 이름 추가 요청 (중복이어도 고유 id가 있으므로 상관없음)
                out.println(userName);
                String confirmMsg = in.readLine();

                if (confirmMsg.indexOf("OK@") == 0) { // 중복 안될 시
                    userID = Integer.parseInt(confirmMsg.substring(confirmMsg.indexOf("@") + 1));

                    // 파일에 새 닉네임 기록
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

    // 닉네임 변경
    static void changeNickname() {
        String nickNameMsg = "변경할 닉네임을 입력해 주세요.";
        int messageType = 3;

        ChangeNickname: while (true) {
            String newName;
            try {
                newName = JOptionPane
                        .showInputDialog(ui, nickNameMsg, "닉네임 변경", messageType,
                                null, null, userName)
                        .toString().strip();
            } catch (Exception e) {
                return;
            }

            if (newName.equals(userName)) { // 이름이 바뀌지 않았을 때
                return;
            }

            if (newName.length() == 0 || newName.length() > 15) { // 길이제한
                nickNameMsg = "닉네임은 1~15자이어야 합니다.";
                messageType = 2;
                continue ChangeNickname;
            }

            if (newName.indexOf(';') >= 0 || newName.indexOf('@') >= 0) { // 기호제한
                nickNameMsg = "닉네임에는 ';'나 '@' 기호를 사용할 수 없습니다.";
                messageType = 2;
                continue ChangeNickname;
            }

            try {
                // 서버 리스트에 존재하는 이름 변경 요청
                out.println(newName + "@change");
                userName = newName;

                // 파일에 새로운 이름 기록
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

    // 서버에 멤버 목록 전송 요청
    static void viewRequest() {
        nameList.clear(); // 나가는 사용자를 대비해 기존 리스트 비우기

        out.println("@viewNickname@");
        while (true) {
            if (added == true) { // 모든 사용자 닉네임이 추가되면 while루프 탈출하기
                nameList.sort(null);
                added = false;
                break;
            }
        }
    }

    // 서버로 사용자의 메시지 보내주기 (줄바꿈 가능한 html 형태로)
    static void sendMessage(String msg) {
        if (msg.length() == 0) { // 공백만 보내지 않기
            return;
        }

        // html로 바꿀 때 문자를 빠르게 추가하는 용도의 스트링버퍼
        StringBuffer htmlText = new StringBuffer("<html><body>");
        int wordCount = 0; // 한 줄에 추가된 글자 수 (개행문자 삽입 기준이 됨)

        for (int i = 0; i < msg.length(); i++) {
            if ((int) msg.charAt(i) < 128) { // 영어 알파벳 또는 기본 기호일 경우
                wordCount++;
            } else { // 한글 등 가로 길이가 긴 경우
                wordCount += 2;
            }

            // html에 들어갈 수 있는 문자로 변환
            switch (msg.charAt(i)) {
                case ' ':
                    htmlText.append("&nbsp;");
                    break;

                case '\n':
                    // 메시지 마지막에 개행 문자가 있을 경우 무시
                    if (i < msg.length() - 1) { // 마지막이 아닐 경우에는 넣기
                        htmlText.append("<br>");
                    }
                    wordCount = 0;
                    break;

                case '\"':
                    htmlText.append("&quot;");
                    break;

                case '&':
                    htmlText.append("&amp;");
                    break;

                case '<':
                    htmlText.append("&lt;");
                    break;

                case '>':
                    htmlText.append("&gt;");
                    break;

                case '÷':
                    htmlText.append("&divide;");
                    break;

                case '®':
                    htmlText.append("&reg;");
                    break;

                case '·':
                    htmlText.append("&middot;");
                    break;

                case '±':
                    htmlText.append("&plusmn;");
                    break;

                case 'ⓒ':
                    htmlText.append("&copy;");
                    break;

                case '°':
                    htmlText.append("&deg;");
                    break;

                case '×':
                    htmlText.append("&times;");
                    break;

                default:
                    htmlText.append(msg.charAt(i));
                    break;
            }

            if (wordCount >= 40) {
                if (i < msg.length() - 1 && msg.charAt(i + 1) != '\n') { // 마지막 줄이 아닐 경우에만 줄바꿈
                    htmlText.append("<br>");
                }
                wordCount = 0;
            }
        }
        htmlText.append("</body></html>");

        out.println(userName + ";" + htmlText + ';' + userID); // 서버로 전송
    }

    // 실시간으로 서버로부터 메시지 읽기
    synchronized void readMessage(String input) {
        if (input.indexOf(';') == -1) { // 사용자 메시지가 아닐 경우
            if (input.equals("@viewend")) { // 사용자 목록을 다 확인했을 경우
                added = true; // viewRequest() 안의 무한루프를 멈춰줌
                return;

            } else if (input.indexOf("@view@") >= 0) { // 사용자 목록 받기
                String[] info = input.split("@");

                if (Integer.parseInt(info[2]) != userID) { // 본인이 아닌 경우 목록에 넣기
                    nameList.add(info[0]);
                }
                return;

            } else if (input.indexOf("@changed@") >= 0) { // 본인 포함 유저의 아이디가 바뀐 경우
                int changedID = Integer.parseInt(input.substring(0, input.indexOf("@")));

                if (changedID == lastSpeakerID) { // 다음 메시지부터는 바뀐 닉네임으로 뜨도록 수정
                    lastSpeakerID = -1;
                }

                ui.refresh(); // 만약 viewmode 켜진 상태이면 목록 새로고침
                return;

            } else if (input.indexOf('@') == 0) { // 사용자 입장/퇴장 메시지의 경우
                ui.showNotices(input.substring(1)); // 채팅창 가운데로 안내 메시지 표시
            }
        } else { // 사용자가 직접 보낸 메시지일 경우
            // 스트림 메시지를 항목별로 분리
            String sendName = input.substring(0, input.indexOf(';'));
            String sendMsg = input.substring(input.indexOf(';') + 1, input.lastIndexOf(';'));
            int sendID = Integer.parseInt(input.substring(input.lastIndexOf(';') + 1));

            ui.showMessage(sendName, sendMsg, sendID); // 채팅창에 사용자이름+메시지 띄우기
        }
    }

    public static void main(String[] args) {
        try {
            // 소켓 연결
            socket = new Socket("localhost", 12345);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            Main main = new Main();

            while (in != null) { // 계속 스트림 메시지 읽어들이기
                main.readMessage(in.readLine());
            }
        } catch (Exception e) { // 서버 연결이 끊어졌을 경우
            JOptionPane.showMessageDialog(null, "서버 연결에 실패했습니다 ㅠㅠ\n황지인에게 서버를 열어달라고 요청해보세요!");
        }
    }
}