package chatting_ui;

import java.io.*;
import java.net.*;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    // 닉네임 저장할때 필요한 동기화 자동으로 되는 해시맵
    private static final ConcurrentHashMap<Integer, String> userInfo = new ConcurrentHashMap<>();
    private static int newUserID = 0; // 다음에 들어오는 사용자에게 부여할 고유 ID

    private static String serverHome = System.getProperty("user.home"); // 사용자 폴더 위치 읽어오기
    private static File serverFolder = new File(serverHome, "chatserver/"); // 폴더 위치 지정
    private static File serverSaveFile = new File(serverFolder, "last_id.txt"); // 텍스트 파일 위치 지정

    // 닉네임 추가하기
    public static synchronized void addNickname(String userName) {
        userInfo.putIfAbsent(newUserID, userName); // 핸들러에게 요청받은 사용자의 아이디 및 닉네임을 리스트에 추가
        newUserID++; // 다음 아이디 업데이트

        try { // 다음 아이디를 파일에 쓰기
            PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(serverSaveFile)));
            writer.println(newUserID);
            writer.close();
        } catch (Exception e) {
            System.exit(1);
        }
    }

    // 닉네임 변경하기/기존회원 닉네임 추가
    public static synchronized void existingNickname(int userID, String userName) {
        userInfo.put(userID, userName);
    }

    // 방을 나간 회원을 서버 리스트에서 삭제
    public static synchronized void removeNickname(int userID) {
        userInfo.remove(userID);
    }

    // 현재 모든 닉네임 목록 불러오기
    public static synchronized ConcurrentHashMap<Integer, String> viewNickname() {
        return userInfo;
    }

    public static void main(String[] args) {
        try {
            if (!serverFolder.exists()) { // 폴더가 존재하지 않을 시 새로 만들기
                serverFolder.mkdir();
            }

            if (serverSaveFile.exists()) { // 파일이 존재할 경우
                BufferedReader reader = new BufferedReader(new FileReader(serverSaveFile));
                newUserID = Integer.parseInt(reader.readLine()); // 파일에 적혀 있는 번호가 다음 사용자 ID임
                reader.close();
            } else { // 파일이 존재하지 않을 경우(아이디를 0으로 초기화)
                PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(serverSaveFile)));
                writer.println("0");
                writer.close();
            }

        } catch (Exception e) {
            System.exit(1);
        }

        try (ServerSocket serverSocket = new ServerSocket(80)) {
            System.out.println("서버 가동 중...");

            // 서버가 가동되는 동안 클라이언트 계속해서 받기
            while (true) {
                Socket clientSocket = serverSocket.accept();
                Handler handler = new Handler(clientSocket, newUserID); // 받은 소켓 하나당 스레드(핸들러 객체) 하나 추가하기
                handler.start();
                System.out.println("클라이언트와 연결되었습니다.");
            }
        } catch (Exception e) {
            String errMsg = e.getMessage();

            if (errMsg.equals("Connection reset")) {
                System.out.println("서버를 종료합니다."); // 사실이거 run할때는 쓸데없긴함ㅋㅋ
                System.exit(0);
            } else {
                System.out.println("오류 발생: " + errMsg);
                /* 놀라운 점: sout 말고 out만 쳐도 println이 자동으로 나온다 ㄷㄷ */
            }
        }
    }
}
