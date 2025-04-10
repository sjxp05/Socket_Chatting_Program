package chatting_ui;

import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    // 닉네임 저장할때 필요한 동기화 자동으로 되는 해시세트
    private static final Set<String> nicknames = ConcurrentHashMap.newKeySet();

    // 닉네임 추가하기 및 성공 여부 반환
    public static boolean addNickname(String userName) {
        return nicknames.add(userName);
    }

    // 닉네임 삭제하기
    public static void removeNickname(String userName) {
        nicknames.remove(userName);
    }

    // 현재 모든 닉네임 목록 불러오기
    public static Set<String> viewNickname() {
        return nicknames;
    }

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(12345)) {
            System.out.println("서버 가동 중...");

            // 서버가 가동되는 동안 클라이언트 계속해서 받기
            while (true) {
                Socket clientSocket = serverSocket.accept();
                Handler handler = new Handler(clientSocket); // 받은 소켓 하나당 스레드(핸들러 객체) 하나 추가하기
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
