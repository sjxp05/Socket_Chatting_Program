package chatting_ui;

import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    private static final Set<String> nicknames = ConcurrentHashMap.newKeySet();

    public static boolean addNickname(String userName) {
        return nicknames.add(userName);
    }

    public static void removeNickname(String userName) {
        nicknames.remove(userName);
    }

    // 성공하면 지울거임
    public static void printNicknames() {
        for (String i : nicknames) {
            System.out.println(i);
        }
    }

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(12345)) {
            System.out.println("서버를 시작합니다.");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                Handler handler = new Handler(clientSocket);
                handler.start();
                System.out.println("클라이언트와 연결되었습니다.");
            }
        } catch (Exception e) {
            String errMsg = e.getMessage();

            if (errMsg.equals("Connection reset")) {
                System.out.println("서버를 종료합니다.");
                System.exit(0);
            } else {
                System.out.println("오류 발생: " + errMsg);
                /* 놀라운 점: sout 말고 out만 쳐도 println이 자동으로 나온다 ㄷㄷ */
            }
        }
    }
}
