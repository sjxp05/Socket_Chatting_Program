package chatting_ui.client2;

import java.io.*;
import java.net.*;
import java.util.ArrayList;

import javax.swing.JOptionPane;

public class Main {
    static ChatUI ui = new ChatUI();

    private static Socket socket = null;
    private static BufferedReader in;
    private static PrintWriter out;

    private static final File saveFolder = new File(System.getProperty("user.home") + "/chatclient2/");
    private static final File saveFile = new File(saveFolder, "chat-client-info.txt");

    static ArrayList<String> nameList = new ArrayList<>();

    static int userID;
    static volatile String userName = "";
    static volatile int lastSpeakerID = -1;
    static volatile boolean added = false;

    static final int[] capitalWidth = new int[] {
            5, 4, 5, 5, 4, 4, 5, 5, 2, 3, 4, 3, 6, 5, 5, 4, 5, 4, 4, 3, 5, 4, 7, 4, 4, 4
    };
    static final int[] smallWidth = new int[] {
            4, 5, 4, 5, 4, 2, 4, 4, 2, 2, 4, 2, 6, 4, 4, 4, 4, 3, 3, 3, 4, 4, 5, 4, 4, 4
    };

    private Main() {
        if (!saveFolder.exists()) {
            try {
                saveFolder.mkdir();
            } catch (Exception e) {
                System.exit(1);
            }
        }

        if (saveFile.exists()) {
            try {
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

    void setNickname() {
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

        String nickNameMsg = "채팅방에서 사용할 닉네임을 입력해 주세요.\n ";
        int messageType = 3;

        while (true) {
            try {
                userName = JOptionPane.showInputDialog(ui, nickNameMsg, "닉네임 설정", messageType).strip();
            } catch (Exception e) {
                System.exit(1);
            }

            if (userName.length() == 0 || userName.length() > 15) {
                nickNameMsg = "닉네임은 1~15자이어야 합니다.\n ";
                messageType = 2;
                continue;
            }

            if (userName.indexOf('@') >= 0) {
                nickNameMsg = "닉네임에는 '@' 기호를 사용할 수 없습니다.";
                messageType = 2;
                continue;
            }

            try {
                out.println("NEW@" + userName);
                String confirmMsg = in.readLine();

                if (confirmMsg.indexOf("CONFIRM@") == 0) {
                    userID = Integer.parseInt(confirmMsg.substring(confirmMsg.indexOf("@") + 1));

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

            if (newName.equals(userName)) {
                return;
            }

            if (newName.length() == 0 || newName.length() > 15) {
                nickNameMsg = "닉네임은 1~15자이어야 합니다.\n ";
                messageType = 2;
                continue;
            }

            if (newName.indexOf('@') >= 0) {
                nickNameMsg = "닉네임에는 '@' 기호를 사용할 수 없습니다.";
                messageType = 2;
                continue;
            }

            try {
                out.println("CHANGE@" + newName);
                userName = newName;

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

    static void viewRequest() {
        nameList.clear();
        out.println("VIEWNICKNAME@");

        while (true) {
            if (added == true) {
                nameList.sort(null);
                added = false;
                break;
            }
        }
    }

    static void sendMessage(String msg) {
        if (msg.length() == 0) {
            return;
        }

        StringBuffer htmlText = new StringBuffer("");
        int wordCount = 0;

        PutInHTML: for (int i = 0; i < msg.length(); i++) {

            if (msg.charAt(i) >= 'A' && msg.charAt(i) <= 'Z') {
                wordCount += capitalWidth[msg.charAt(i) - 'A'];
            } else if (msg.charAt(i) >= 'a' && msg.charAt(i) <= 'z') {
                wordCount += smallWidth[msg.charAt(i) - 'a'];
            } else if (msg.charAt(i) >= '0' && msg.charAt(i) <= '9') {
                wordCount += 4;
            } else if (msg.charAt(i) == '.' || msg.charAt(i) == ',') {
                wordCount++;
            } else if (msg.charAt(i) == ' ' || msg.charAt(i) == '!') {
                wordCount += 2;
            } else if (msg.charAt(i) == '?' || msg.charAt(i) == '/') {
                wordCount += 3;
            } else {
                if ((int) msg.charAt(i) < 128) {
                    wordCount += 4;
                } else {
                    wordCount += 7;
                }
            }

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

                case '&':
                    htmlText.append("&amp;");
                    break;

                case '@':
                    htmlText.append("&#64;");
                    break;

                case '\n':
                    htmlText.append("<br>");
                    wordCount = 0;
                    continue PutInHTML;

                case ' ':
                    htmlText.append("&nbsp;");
                    break;

                default:
                    htmlText.append(msg.charAt(i));
                    break;
            }

            if (wordCount >= 120) {
                if (i < msg.length() - 1 && msg.charAt(i + 1) != '\n') {
                    htmlText.append("<br>");
                }
                wordCount = 0;
            }
        }

        out.println("MSG@" + userID + "@" + userName + "@" + htmlText);
    }

    synchronized void readMessage(String input) {
        String[] tokens = input.split("@");

        switch (tokens[0]) {
            case "VIEW":
                nameList.add(tokens[1]);
                break;

            case "VIEWEND":
                added = true;
                break;

            case "HASCHANGED":
                if (Integer.parseInt(tokens[1]) == lastSpeakerID) {
                    lastSpeakerID = -1;
                }

                ui.refresh();
                break;

            case "NOTICE":
                ui.showNotices(tokens[1]);
                break;

            case "MSG": {

                int height = 21;

                for (int i = 0; i < tokens[3].length(); i++) {
                    if (i == tokens[3].indexOf("<br>", i)) {
                        height += 19;
                        i += 3;
                    }
                }

                ui.showMessage(Integer.parseInt(tokens[1]), tokens[2], tokens[3], height);
                break;
            }

            default:
                break;
        }
    }

    public static void main(String[] args) {
        try {
            socket = new Socket("192.168.195.136", 12345);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            Main main = new Main();

            while (in != null) {
                main.readMessage(in.readLine());
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "서버 연결에 실패했습니다 ㅠㅠ\n황지인에게 서버를 열어달라고 요청해보세요!");
            System.exit(1);
        }
    }
}