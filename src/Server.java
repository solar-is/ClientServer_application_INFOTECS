import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;

public class Server extends Thread {
    private static final int messagePort = 45777;
    private static final int filePort = 45778;
    private Socket socket;
    private ArrayList<String> files;

    private void setSocket(Socket socket) {
        this.socket = socket;
        files = new ArrayList<>();
        File file = new File("files.txt");
        try (BufferedReader in = new BufferedReader(new FileReader(file.getAbsoluteFile()))) {
            String s;
            while ((s = in.readLine()) != null) {
                files.add(s);
            }
        } catch (IOException e) {
            System.out.println("Exception: " + e);
        }
        setDaemon(true);
        setPriority(NORM_PRIORITY);
        start();
    }

    private boolean isLoginValid(String login) {
        if (login.isEmpty() || login.equals("\n")) {
            return false;
        }
        if (login.equalsIgnoreCase("quit")) {
            return true;
        }
        boolean val = login.length() <= 4;
        for (char c : login.toCharArray()) {
            val = val && (c >= 'a' && c <= 'z');
        }
        return val;
    }

    private ArrayList<JSON> getAllMessages() {
        ArrayList<JSON> res = new ArrayList<>();
        File file = new File("messages.txt");
        if (!file.exists()) {
            return null;
        }
        try (BufferedReader in = new BufferedReader(new FileReader(file.getAbsoluteFile()))) {
            String s;
            while ((s = in.readLine()) != null) {
                JSON cur = new JSON();
                String[] arr = s.split(" ");
                cur.add("date", arr[0] + ' ' + arr[1]);
                cur.add("id", arr[2]);
                cur.add("login", arr[3]);
                StringBuilder sb = new StringBuilder();
                for (int i = 4; i < arr.length; ++i) {
                    sb.append(arr[i]).append(' ');
                }
                cur.add("message", sb.toString());
                res.add(cur);
            }
        } catch (IOException e) {
            System.out.println("Exception: " + e);
        }
        return res;
    }


    private ArrayList<JSON> getAllMessagesByLogin(String login) {
        ArrayList<JSON> log = getAllMessages();
        ArrayList<JSON> res = new ArrayList<>();
        for (JSON j : log) {
            if (j.get("login").equals(login)) {
                res.add(j);
            }
        }
        return res;
    }

    private boolean deleteMessageById(String login, String id) {
        boolean isDeleted = false;
        ArrayList<JSON> res = new ArrayList<>();
        File file = new File("messages.txt");
        if (!file.exists()) {
            return false;
        }
        StringBuilder sb = new StringBuilder();
        try (BufferedReader in = new BufferedReader(new FileReader(file.getAbsoluteFile()))) {
            String s;
            while ((s = in.readLine()) != null) {
                String[] arr = s.split(" ");
                JSON cur = new JSON();
                cur.add("date", arr[0] + ' ' + arr[1]);
                cur.add("id", arr[2]);
                cur.add("login", arr[3]);
                for (int i = 4; i < arr.length; ++i) {
                    sb.append(arr[i]).append(' ');
                }
                cur.add("message", sb.toString());
                sb.setLength(0);
                if (cur.get("id").equals(id) && cur.get("login").equals(login)) {
                    isDeleted = true;
                    continue;
                }
                res.add(cur);
            }
        } catch (IOException e) {
            System.out.println("Exception: " + e);
        }
        sb.setLength(0);
        for (JSON j : res) {
            sb.append(j.toString()).append('\n');
        }
        new FileManager().write("messages.txt", sb.toString());
        return isDeleted;
    }


    public void run() {
        try {
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            BufferedInputStream biss = new BufferedInputStream(socket.getInputStream());
            BufferedOutputStream boss = new BufferedOutputStream(socket.getOutputStream());

            String line = "";
            String login = "";
            final int port = socket.getLocalPort();
            while (true) {
                if (login.isEmpty()) {
                    out.write("Enter your login or \"quit\". Login should be at lowercase, only 'a'...'z', max length = 4 END\n");
                    out.flush();
                    line = in.readLine();
                    if (line.equalsIgnoreCase("quit")) {
                        System.out.println("Incognito close connection");
                        socket.close();
                        break;
                    }
                    while (!isLoginValid(line)) {
                        out.write("Your login is not valid, please enter another one END\n");
                        out.flush();
                        line = in.readLine();
                    }
                    if (line.equalsIgnoreCase("quit")) {
                        System.out.println("Incognito close connection");
                        socket.close();
                        break;
                    }
                    login = line;
                }
                if (socket.isClosed()) {
                    break;
                }
                String str = login + ", hi! Choose one of this\n" +
                        "1. Enter new message\n" +
                        "2. Show list of my messages\n" +
                        "3. Delete my message by id\n" +
                        "4. Quit from session";
                if (port == 45778) {
                    str += "\n5. Load file on server\n" +
                            "6. Save file from server\n" +
                            "7. Show list of all messages with features";
                }
                out.write(str + "END\n");
                out.flush();
                line = in.readLine();
                boolean isOk = false;
                while (!isOk) {
                    switch (line) {
                        case "1":
                            isOk = true;
                            out.write("Enter your message END\n");
                            out.flush();
                            line = in.readLine();

                            Date date = new Date();
                            int id = date.hashCode() + line.hashCode() + login.hashCode();
                            if (id < 0) id = -id;
                            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");

                            JSON json = new JSON();
                            json.add("login", login);
                            json.add("date", dateFormat.format(date));
                            json.add("id", Integer.toString(id));
                            json.add("message", line);
                            new FileManager().update("messages.txt", json.toString());
                            break;
                        case "2":
                            isOk = true;
                            ArrayList<JSON> messages = getAllMessagesByLogin(login);
                            StringBuilder sb = new StringBuilder();
                            for (JSON j : messages) {
                                sb.append("DATE:").append(j.get("date")).append(" ID:").append(j.get("id"))
                                        .append(" MESSAGE:").append(j.get("message")).append("\n");
                            }
                            out.write(sb.toString() + "\n");
                            out.flush();
                            if (messages.size() == 0) {
                                out.write("You have not any messages\n");
                                out.flush();
                            }
                            break;
                        case "3":
                            isOk = true;
                            out.write("Enter id of the message END\n");
                            out.flush();
                            line = in.readLine();
                            if (deleteMessageById(login, line))
                                out.write("Message was successfully deleted \n");
                            else
                                out.write("There is no message with such id \n");
                            out.flush();
                            break;
                        case "4":
                            isOk = true;
                            run();
                            break;
                        case "5":
                            if (port != 45778) {
                                out.write("Invalid command, please repeat END\n");
                                out.flush();
                                line = in.readLine();
                            } else {
                                isOk = true;
                                out.write("Enter a path to file you want to load END\n");
                                out.flush();
                                line = in.readLine();
                                File file = new File(line);
                                long fileSize = Long.parseLong(in.readLine());
                                try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file))) {
                                    byte[] byteArray = new byte[8192];
                                    int cnt;
                                    while (fileSize > 0 && (cnt = biss.read(byteArray)) != -1) {
                                        bos.write(byteArray, 0, cnt);
                                        bos.flush();
                                        fileSize -= cnt;
                                    }
                                    System.out.println("File " + file.getName() + " loaded on server");
                                    new FileManager().update("files.txt", file.getName());
                                    files.add(file.getName());
                                }
                            }
                            break;
                        case "6":
                            if (port != 45778) {
                                out.write("Invalid command, please repeat END\n");
                                out.flush();
                                line = in.readLine();
                            } else {
                                isOk = true;
                                out.write("Enter a name of file you want to save END\n");
                                out.flush();
                                line = in.readLine();
                                while (!files.contains(line)) {
                                    out.write("There is no such file on server, repeat please END\n");
                                    out.flush();
                                    line = in.readLine();
                                }
                                File file = new File(line);
                                long fileSize = file.length();
                                out.write(Long.toString(file.length()) + '\n');
                                out.flush();
                                try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(line))) {
                                    byte[] byteArray = new byte[8192];
                                    int cnt;
                                    while (fileSize > 0 && (cnt = bis.read(byteArray)) != -1) {
                                        boss.write(byteArray, 0, cnt);
                                        boss.flush();
                                        fileSize -= cnt;
                                    }
                                    System.out.println("Server sent file " + file.getName() + " to client");
                                }
                            }
                            break;
                        case "7":
                            if (port != 45778) {
                                out.write("Invalid command, please repeat END\n");
                                out.flush();
                                line = in.readLine();
                            } else {
                                isOk = true;
                                ArrayList<JSON> msg = getAllMessages();
                                out.write("Would you like to sort the list of messages?\n" +
                                        "1. Yes, by login\n" +
                                        "2. Yes, by date\n" +
                                        "3. No END\n");
                                out.flush();
                                line = in.readLine();
                                if (line.equals("1")) {
                                    if (msg != null)
                                        msg.sort(new Comparator<JSON>() {
                                            @Override
                                            public int compare(JSON o1, JSON o2) {
                                                return o1.get("login").compareTo(o2.get("login"));
                                            }
                                        });
                                } else if (line.equals("2")) {
                                    if (msg != null)
                                        msg.sort(new Comparator<JSON>() {
                                            @Override
                                            public int compare(JSON o1, JSON o2) {
                                                String s1 = o1.get("date");
                                                String s2 = o2.get("date");
                                                Date date1 = new Date(
                                                        Integer.parseInt(s1.substring(0, 4)),
                                                        Integer.parseInt(s1.substring(5, 7)),
                                                        Integer.parseInt(s1.substring(8, 10)),
                                                        Integer.parseInt(s1.substring(11, 13)),
                                                        Integer.parseInt(s1.substring(14, 16)),
                                                        Integer.parseInt(s1.substring(17, 19))
                                                );
                                                Date date2 = new Date(
                                                        Integer.parseInt(s2.substring(0, 4)),
                                                        Integer.parseInt(s2.substring(5, 7)),
                                                        Integer.parseInt(s2.substring(8, 10)),
                                                        Integer.parseInt(s2.substring(11, 13)),
                                                        Integer.parseInt(s2.substring(14, 16)),
                                                        Integer.parseInt(s2.substring(17, 19))
                                                );
                                                return date1.compareTo(date2);
                                            }
                                        });
                                } else if (!line.equals("3")) {
                                    out.write("Invalid command, please repeat END\n");
                                    out.flush();
                                    line = in.readLine();
                                }
                                sb = new StringBuilder();
                                if (msg != null)
                                    for (JSON j : msg) {
                                        sb.append("DATE:").append(j.get("date")).append("  ID:").append(j.get("id"))
                                                .append("  LOGIN:").append(j.get("login")).append("  MESSAGE:")
                                                .append(j.get("message")).append("\n");
                                    }
                                sb.append("Files:").append('\n');
                                for (String s : files) {
                                    sb.append(s).append('\n');
                                }
                                out.write(sb.toString() + "\n");
                                out.flush();
                            }
                            break;
                        default:
                            out.write("Invalid command, please repeat END\n");
                            out.flush();
                            line = in.readLine();
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Exception : " + e);
            e.printStackTrace();
        }
    }

    private static void makeServerSocketForPort(int Port) {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(Port)) {
                while (true) {
                    Socket socket = serverSocket.accept();
                    System.out.println("Client accepted on port " + socket.getLocalPort());
                    new Server().setSocket(socket);
                }
            } catch (IOException e) {
                System.out.println("Exception : " + e);
            }
        }).start();
    }

    public static void main(String[] args) throws IOException {
        File file = new File("messages.txt");
        if (!file.exists()) {
            file.createNewFile();
        }
        file = new File("files.txt");
        if (!file.exists()) {
            file.createNewFile();
        }
        makeServerSocketForPort(messagePort);
        makeServerSocketForPort(filePort);
        System.out.println("Server on ports 45777 and 45778 started");
    }
}