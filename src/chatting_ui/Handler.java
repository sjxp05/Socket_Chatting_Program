package chatting_ui;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class Handler extends Thread {
    // 서버에 연결된 모든 소켓의 outputStream 모아놓은 리스트
    static ArrayList<PrintWriter> writerList = new ArrayList<>();

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    private int userID = 0; // 서버에서 준 고유 아이디
    private String userName; // 현재 클라이언트의 닉네임과 같음

    public Handler(Socket clientSocket, int newUserID) {
        try {
            // 소켓 받아서 i/o 스트림 지정
            socket = clientSocket;
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            writerList.add(out);
            userID = newUserID; // 일단 다음 신규유저에게 쓸 아이디로 할당해주기

        } catch (Exception e) {
            System.out.println("오류 발생: " + e.getMessage());
        }
    }

    // 모든 사용자의 닉네임 목록을 해당 클라이언트의 스트림으로만 보냄 (모든 클라이언트에게 보내는거 x)
    void nicknameInfo() {

        ConcurrentHashMap<Integer, String> list = Server.viewNickname();

        for (int id : list.keySet()) {
            // '목록 전송 전용'이라는 뜻의 메시지 + 사용자의 고유id + 각 사용자 닉네임
            if (id != userID) {
                out.println("VIEW@" + list.get(id));
            }
        }
        out.println("VIEWEND@");
    }

    // 모든 사용자의 스트림으로 메시지나 안내문을 전송
    private void sendAll(String msg) {
        for (PrintWriter writer : writerList) {
            writer.println(msg);
        }
    }

    @Override
    public void run() {
        try {
            // 첫 시작 시 닉네임 지정 요청받기
            userName = in.readLine();
            String[] info = userName.split("@"); // 토큰 나누기

            if (info[0].equals("REJOIN")) { // 기존 멤버의 참여 요청 시
                userID = Integer.parseInt(info[1]);
                userName = info[2];
                Server.existingNickname(userID, userName);

            } else if (info[0].equals("NEW")) { // 새로운 멤버 요청 시 아이디 부여하기
                userName = info[1];
                Server.addNickname(userName);
            }

            out.println("CONFIRM@" + userID);

            System.out.println("[" + userName + " 연결됨]");
            sendAll("NOTICE@" + userName + " 님이 참여했습니다.");

            while (in != null) {
                String inputMsg = in.readLine();
                String[] tokens = inputMsg.split("@");

                switch (tokens[0]) {
                    case "VIEWNICKNAME" -> { // 유저목록 보기 요청을 받았을 경우
                        nicknameInfo();
                        break;
                    }

                    case "CHANGE" -> { // 닉네임 변경 요청 받았을 경우
                        Server.existingNickname(userID, tokens[1]);
                        sendAll("HASCHANGED@" + userID);
                        break;
                    }

                    case "NOTICE", "MSG" -> { // 기타 메시지(입/퇴장, 일반 사용자 메시지)는 모두에게 전송
                        sendAll(inputMsg);
                        break;
                    }

                    default -> {
                        break;
                    }
                }
            }
        } catch (IOException e) { // 소켓의 i/o 스트림과 연결이 끊겼을 경우. 즉 해당 클라이언트가 방을 나갔을 때
            System.out.println("[" + userName + " 접속 끊김]");

        } finally { // 접속 끊긴 후 마무리 작업
            sendAll("NOTICE@" + userName + " 님이 나갔습니다.");
            writerList.remove(out); // 목록에서 해당 사용자의 아웃풋 스트림 없애기
            Server.removeNickname(userID); // 서버의 닉네임 리스트에서 사용자 닉네임 지우기

            try {
                socket.close(); // 꼭 닫아줘야됨!!!
            } catch (IOException ex) {
                System.out.println("오류 발생: " + ex.getMessage());
            }
        }
    }
}
