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
    private static final File saveFile = new File(saveFolder, "chat-client2-info.txt");

    static ArrayList<String> nameList = new ArrayList<>();

    static int userID;
    static volatile String userName = "";
    static volatile int lastSpeakerID = -1;
    static volatile boolean added = false;

    private Main() {
        setNickname();
    }

    void setNickname() {
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

        if (userName.strip().length() > 0) {
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

        String nickNameMsg = "채팅방에서 사용할 닉네임을 입력해 주세요.\n ";
        int messageType = 3;

        SetNickname: while (true) {
            try {
                userName = JOptionPane.showInputDialog(ui, nickNameMsg, "닉네임 설정", messageType).strip();
            } catch (Exception e) {
                System.exit(1);
            }

            if (userName.length() == 0 || userName.length() > 15) {
                nickNameMsg = "닉네임은 1~15자이어야 합니다.\n ";
                messageType = 2;
                continue SetNickname;
            }

            if (userName.indexOf(';') >= 0 || userName.indexOf('@') >= 0) {
                nickNameMsg = "닉네임에는 ';'나 '@' 기호를\n사용할 수 없습니다.";
                messageType = 2;
                continue SetNickname;
            }

            try {
                out.println(userName);
                String confirmMsg = in.readLine();

                if (confirmMsg.indexOf("OK@") == 0) {
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

            if (newName.equals(userName)) {
                return;
            }

            if (newName.length() == 0 || newName.length() > 15) {
                nickNameMsg = "닉네임은 1~15자이어야 합니다.\n ";
                messageType = 2;
                continue ChangeNickname;
            }

            if (newName.indexOf(';') >= 0 || newName.indexOf('@') >= 0) {
                nickNameMsg = "닉네임에는 ';'나 '@' 기호를\n사용할 수 없습니다.";
                messageType = 2;
                continue ChangeNickname;
            }

            try {
                out.println(newName + "@change");
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

        out.println("@viewNickname@");
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

        StringBuffer htmlText = new StringBuffer("<html><body>");
        int wordCount = 0;

        for (int i = 0; i < msg.length(); i++) {
            if ((int) msg.charAt(i) < 128) {
                wordCount++;
            } else {
                wordCount += 2;
            }

            switch (msg.charAt(i)) {
                case ' ':
                    htmlText.append("&nbsp;");
                    break;

                case '\n':
                    if (i < msg.length() - 1) {
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
                if (i < msg.length() - 1 && msg.charAt(i + 1) != '\n') {
                    htmlText.append("<br>");
                }
                wordCount = 0;
            }
        }
        htmlText.append("</body></html>");

        out.println(userName + ";" + htmlText + ';' + userID);
    }

    synchronized void readMessage(String input) {
        if (input.indexOf(';') == -1) {
            if (input.equals("@viewend")) {
                added = true;
                return;

            } else if (input.indexOf("@view@") >= 0) {
                String[] info = input.split("@");

                if (Integer.parseInt(info[2]) != userID) {
                    nameList.add(info[0]);
                }
                return;

            } else if (input.indexOf("@changed@") >= 0) {
                int changedID = Integer.parseInt(input.substring(0, input.indexOf("@")));

                if (changedID == lastSpeakerID) {
                    lastSpeakerID = -1;
                }

                ui.refresh();
                return;

            } else if (input.indexOf('@') == 0) {
                ui.showNotices(input.substring(1));
            }
        } else {
            String sendName = input.substring(0, input.indexOf(';'));
            String sendMsg = input.substring(input.indexOf(';') + 1, input.lastIndexOf(';'));
            int sendID = Integer.parseInt(input.substring(input.lastIndexOf(';') + 1));

            int height = 20;
            int lines = 1;

            for (int i = 12; i < sendMsg.length(); i++) {
                if (sendMsg.indexOf("<br>", i) >= 0) {
                    height += (21 + lines / 4);
                    lines++;
                    i = sendMsg.indexOf("<br>", i) + 4;
                }
            }

            ui.showMessage(sendName, sendMsg, sendID, height);
        }
    }

    public static void main(String[] args) {
        try {
            socket = new Socket("localhost", 12345);
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