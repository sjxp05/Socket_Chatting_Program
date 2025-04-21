package chatting_ui.server;

import java.io.*;
import java.net.*;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    // 닉네임 저장할때 필요한 동기화 자동으로 되는 해시맵
    private static final ConcurrentHashMap<Integer, String> userInfo = new ConcurrentHashMap<>();
    private static int newUserID = 0;

    private static String serverHome = System.getProperty("user.home");
    private static File serverSaveFile = new File(serverHome, "last_id.txt");

    // 닉네임 추가하기
    public static synchronized void addNickname(String userName) {
        userInfo.putIfAbsent(newUserID, userName);
        newUserID++;

        try {
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

    public static synchronized void removeNickname(int userID) {
        userInfo.remove(userID);
    }

    // 현재 모든 닉네임 목록 불러오기
    public static synchronized ConcurrentHashMap<Integer, String> viewNickname() {
        return userInfo;
    }

    public static void main(String[] args) {
        try {
            if (serverSaveFile.exists()) {
                BufferedReader reader = new BufferedReader(new FileReader(serverSaveFile));
                newUserID = Integer.parseInt(reader.readLine());
                reader.close();
            } else {
                PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(serverSaveFile)));
                writer.println("0");
                writer.close();
            }
        } catch (Exception e) {
            System.exit(1);
        }

        try (ServerSocket serverSocket = new ServerSocket(12345)) {
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
