package chatting_ui;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Handler extends Thread {
    // 서버에 연결된 모든 소켓의 outputStream 모아놓은 리스트
    static ArrayList<PrintWriter> writerList = new ArrayList<>();

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    private int userID = 0; // 서버에서 준 기본 아이디
    private String userName; // 현재 클라이언트의 닉네임과 같음

    public Handler(Socket clientSocket, int newUserID) {
        try {
            // 소켓 받아서 i/o 스트림 지정
            socket = clientSocket;
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writerList.add(out);
            userID = newUserID;
        } catch (Exception e) {
            System.out.println("오류 발생: " + e.getMessage());
        }
    }

    // 모든 사용자의 닉네임 목록을 해당 클라이언트의 스트림으로만 보냄 (모든 사용자 x)
    void nicknameInfo() {
        ConcurrentHashMap<Integer, String> list = Server.viewNickname();

        for (int id : list.keySet()) {
            out.println(list.get(id) + "@view@" + id);
        }
        out.println("@viewend");
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
            // 첫 시작 시 닉네임 받기
            userName = in.readLine();

            if (userName.indexOf('@') >= 0) {
                String[] info = userName.split("@");
                userID = Integer.parseInt(info[0]);
                userName = info[1];

                Server.existingNickname(userID, userName);
                out.println("OK@" + userID);
            } else {
                Server.addNickname(userName);
                out.println("OK@" + userID);
            }

            System.out.println("[" + userName + " 연결됨]");
            sendAll("@" + userName + " 님이 참여했습니다.");

            while (in != null) {
                String inputMsg = in.readLine();

                if (inputMsg.equals("@viewNickname@")) {
                    // 닉네임 목록 보기 요청을 받았을 경우
                    nicknameInfo();
                } else if (inputMsg.indexOf(';') == -1 && inputMsg.indexOf("@change") >= 0) {
                    // 닉네임 변경 요청 받았을 경우
                    Server.existingNickname(userID, inputMsg.substring(0, inputMsg.indexOf('@')));
                    sendAll(userID + "@changed@");
                } else {
                    // 사용자가 보낸 메시지나 입장/퇴장 안내는 모든 사용자에게 보냄
                    sendAll(inputMsg);
                }
            }
        } catch (IOException e) { // 소켓의 i/o 스트림과 연결이 끊겼을 경우. 즉 해당 클라이언트가 방을 나갔을 때
            System.out.println("[" + userName + " 접속 끊김]");

        } finally { // 접속 끊긴 후 마무리 작업
            sendAll("@" + userName + " 님이 나갔습니다.");
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
