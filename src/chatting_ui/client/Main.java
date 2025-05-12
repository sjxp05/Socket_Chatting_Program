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

    static final int[] capitalWidth = new int[] { // 알파벳 대문자별 가로 길이
            5, 4, 5, 5, 4, 4, 5, 5, 2, 3, 4, 3, 6, 5, 5, 4, 5, 4, 4, 3, 5, 4, 7, 4, 4, 4
    };
    static final int[] smallWidth = new int[] { // 알파벳 소문자별 가로 길이
            4, 5, 4, 5, 4, 2, 4, 4, 2, 2, 4, 2, 6, 4, 4, 4, 4, 3, 3, 3, 4, 4, 5, 4, 4, 4
    };

    private Main() {
        if (!saveFolder.exists()) { // 폴더가 존재하지 않을 때
            try {
                saveFolder.mkdir();
            } catch (Exception e) {
                System.exit(1);
            }
        }

        if (saveFile.exists()) { // 파일이 존재할 경우 (사용 기록이 있어 닉네임이 이미 존재함)
            try { // 파일에서 원래 아이디 및 닉네임 읽어오기
                BufferedReader nameReader = new BufferedReader(new FileReader(saveFile));
                userID = Integer.parseInt(nameReader.readLine());
                userName = nameReader.readLine();
                nameReader.close();
            } catch (Exception e) {
                System.exit(1);
            }
        }

        setNickname();
    }

    // 처음 시작할 때 닉네임 설정하기
    void setNickname() {
        // 기존 정보가 있을 경우
        if (userName.strip().length() > 0) {
            try {
                out.println("REJOIN@" + userID + "@" + userName);
                String confirmMsg = in.readLine();
                if (confirmMsg.indexOf("CONFIRM@") == 0) {
                    return;
                }
            } catch (Exception e) {
                System.exit(1);
            }
        }

        // 새로운 닉네임이 필요할 경우
        String nickNameMsg = "채팅방에서 사용할 닉네임을 입력해 주세요.\n ";
        int messageType = 3; // 안내 메시지의 아이콘 종류 결정 (3: 입력, 2: 경고)

        while (true) {
            try {
                userName = JOptionPane.showInputDialog(ui, nickNameMsg, "닉네임 설정", messageType).strip();
            } catch (Exception e) {
                System.exit(1);
            }

            if (userName.length() == 0 || userName.length() > 15) { // 길이제한
                nickNameMsg = "닉네임은 1~15자이어야 합니다.\n ";
                messageType = 2; // 메시지 아이콘을 '경고'로 바꾸기
                continue;
            }

            if (userName.indexOf('@') >= 0) { // 기호제한
                nickNameMsg = "닉네임에는 '@' 기호를 사용할 수 없습니다.";
                messageType = 2;
                continue;
            }

            try {
                // 서버에 이름 추가 요청 (중복이어도 고유 id가 있으므로 상관없음)
                out.println("NEW@" + userName);
                String confirmMsg = in.readLine();

                if (confirmMsg.indexOf("CONFIRM@") == 0) { // 서버에서 확인했을 시 id 받기
                    userID = Integer.parseInt(confirmMsg.substring(confirmMsg.indexOf("@") + 1));

                    // 파일에 새 닉네임 및 부여받은 아이디 기록
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

        String nickNameMsg = "변경할 닉네임을 입력해 주세요.\n ";
        int messageType = 3;

        while (true) {
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
                nickNameMsg = "닉네임은 1~15자이어야 합니다.\n ";
                messageType = 2;
                continue;
            }

            if (newName.indexOf('@') >= 0) { // 기호제한
                nickNameMsg = "닉네임에는 '@' 기호를 사용할 수 없습니다.";
                messageType = 2;
                continue;
            }

            try {
                // 서버 리스트에 존재하는 이름 변경 요청
                out.println("CHANGE@" + newName);
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
        out.println("VIEWNICKNAME@");

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
        StringBuffer htmlText = new StringBuffer("");
        int wordCount = 0; // 한 줄에 추가된 글자 수 (개행문자 삽입 기준이 됨)

        PutInHTML: for (int i = 0; i < msg.length(); i++) {
            // 영어 알파벳/숫자 등 가로길이별로 다르게 넣기
            if (msg.charAt(i) >= 'A' && msg.charAt(i) <= 'Z') {
                wordCount += capitalWidth[msg.charAt(i) - 'A'];
            } else if (msg.charAt(i) >= 'a' && msg.charAt(i) <= 'z') {
                wordCount += smallWidth[msg.charAt(i) - 'a'];
            } else if (msg.charAt(i) >= '0' && msg.charAt(i) <= '9') {
                wordCount += 4;
            } else if (msg.charAt(i) == '.' || msg.charAt(i) == ',') {
                wordCount++;
            } else if (msg.charAt(i) == ' ' || msg.charAt(i) == '!' || msg.charAt(i) == '(' || msg.charAt(i) == ')') {
                wordCount += 2;
            } else if (msg.charAt(i) == '?' || msg.charAt(i) == '/') {
                wordCount += 3;
            } else {
                if ((int) msg.charAt(i) < 128) { // 기본 기호일 경우
                    wordCount += 4;
                } else { // 한글 등 가로 길이가 긴 경우
                    wordCount += 8;
                }
            }

            // html에 들어갈 수 있는 문자로 변환
            switch (msg.charAt(i)) {
                case '<':
                    htmlText.append("&lt;");
                    break;

                case '>':
                    htmlText.append("&gt;");
                    break;

                case '\"':
                    htmlText.append("&quot;");
                    break;

                case '\'':
                    htmlText.append("&#39;");
                    break;

                case '&': // 여기까지는 이스케이프 필수로 처리해줘야 함
                    htmlText.append("&amp;");
                    break;

                case '@': // 프로토콜 구분기호로 사용되므로 얘도 꼭 처리해줘야됨!
                    htmlText.append("&#64;");
                    break;

                case '\n': // 줄바꿈
                    htmlText.append("<br>");
                    wordCount = 0;
                    continue PutInHTML;

                case ' ': // 연속 띄어쓰기 대비
                    htmlText.append("&nbsp;");
                    break;

                default: // 나머지 문자는 그냥 넣어도 무방
                    htmlText.append(msg.charAt(i));
                    break;
            }

            if (wordCount >= 140) {
                if (i < msg.length() - 1 && msg.charAt(i + 1) != '\n') { // 마지막 줄이 아닐 경우에만 줄바꿈
                    htmlText.append("<br>");
                }
                wordCount = 0;
            }
        }

        out.println("MSG@" + userID + "@" + userName + "@" + htmlText); // 서버로 전송
    }

    // 실시간으로 서버로부터 메시지 읽기
    synchronized void readMessage(String input) {
        String[] tokens = input.split("@"); // 프로토콜에 따라 받은 메시지를 토큰으로 나누기

        switch (tokens[0]) {
            case "VIEW": // 사용자 목록 받기
                nameList.add(tokens[1]);
                break;

            case "VIEWEND": // 사용자 목록을 다 확인했을 경우
                added = true; // viewRequest() 안의 무한루프를 멈춰줌
                break;

            case "HASCHANGED": // 본인 포함 유저의 아이디가 바뀐 경우
                if (Integer.parseInt(tokens[1]) == lastSpeakerID) {
                    // 다음 메시지부터는 바뀐 닉네임으로 뜨도록 수정
                    lastSpeakerID = -1;
                }

                ui.refresh(); // 만약 viewmode 켜진 상태이면 목록 새로고침
                break;

            case "NOTICE": // 사용자 입,퇴장 메시지일 경우
                ui.showNotices(tokens[1]); // 채팅창 가운데로 안내 메시지 표시
                break;

            case "MSG": { // 일반 사용자 메시지의 경우

                int height = 21; // 메시지의 줄 개수를 세서 라벨의 세로 길이 설정

                for (int i = 0; i < tokens[3].length(); i++) {
                    if (i == tokens[3].indexOf("<br>", i)) {
                        height += 19;
                        i += 3;
                    }
                }

                // 채팅창에 사용자이름+메시지 띄우기
                ui.showMessage(Integer.parseInt(tokens[1]), tokens[2], tokens[3], height);
                break;
            }

            default:
                break;
        }
    }

    public static void main(String[] args) {
        try {
            // 소켓 연결
            socket = new Socket("192.168.195.136", 12345);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            Main main = new Main();

            while (in != null) { // 계속 스트림 메시지 읽어들이기
                main.readMessage(in.readLine());
            }
        } catch (Exception e) { // 서버 연결이 끊어졌을 경우
            JOptionPane.showMessageDialog(null, "서버 연결에 실패했습니다 ㅠㅠ\n황지인에게 서버를 열어달라고 요청해보세요!");
            System.exit(1);
        }
    }
}