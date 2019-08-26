import java.io.*;
import java.net.InetAddress;
import java.net.Socket;

public class Client {

    public static void main(String[] args) {
        Socket socket = null;
        try {
            BufferedReader keyboard = new BufferedReader(new InputStreamReader(System.in));
            System.out.println("Enter IP address");
            String host = keyboard.readLine();
            int serverPort = 0;
            while (serverPort != 45777 && serverPort != 45778) {
                System.out.println("Enter port(only 45777 and 45778 is available)");
                String s = keyboard.readLine();
                boolean isOk = true;
                for (int i = 0; i < s.length(); ++i) {
                    if (s.charAt(i) < '4' || s.charAt(i) > '8') {
                        isOk = false;
                        break;
                    }
                }
                if (!isOk || s.isEmpty())
                    continue;

                serverPort = Integer.parseInt(s);
            }
            System.out.println(
                    "Connecting to the server\n\t" +
                            "(IP address " + host +
                            ", port " + serverPort + ")");

            socket = new Socket(InetAddress.getByName(host), serverPort);
            if (!socket.isConnected()) {
                System.out.println("Invalid IP address or port, try again");
                System.exit(0);
            }
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            BufferedInputStream biss = new BufferedInputStream(socket.getInputStream());
            BufferedOutputStream boss = new BufferedOutputStream(socket.getOutputStream());

            String line;
            while (true) {
                line = in.readLine();
                while (!line.endsWith("END")) {
                    System.out.println(line);
                    line = in.readLine();
                }
                line = line.substring(0, line.length() - 3);
                System.out.println(line);

                if (line.startsWith("Enter a path to file you want to load")) {
                    line = keyboard.readLine();
                    File file = new File(line);
                    while (!file.exists()) {
                        System.out.println("File is not existed, repeat please");
                        line = keyboard.readLine();
                        file = new File(line);
                    }
                    out.write(file.getName() + '\n');
                    out.flush();
                    out.write(Long.toString(file.length()) + '\n');
                    out.flush();
                    long fileSize = file.length();
                    try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(line))) {
                        byte[] byteArray = new byte[8192];
                        int cnt;
                        while (fileSize > 0 && (cnt = bis.read(byteArray)) != -1) {
                            boss.write(byteArray, 0, cnt);
                            boss.flush();
                            fileSize -= cnt;
                        }
                        System.out.println("Loading complete");
                    }
                } else if (line.startsWith("Enter a name of file you want to save")) {
                    String fileName = keyboard.readLine();
                    out.write(fileName + "\n");
                    out.flush();
                    line = in.readLine();
                    while (line.startsWith("There is no such file on server")) {
                        line = keyboard.readLine();
                        out.write(line + "\n");
                        out.flush();
                        line = in.readLine();
                    }
                    long fileSize = Long.parseLong(line);
                    try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream("C:\\Users\\Admin\\Desktop\\" + fileName))) {
                        byte[] byteArray = new byte[8192];
                        int cnt;
                        while (fileSize > 0 && (cnt = biss.read(byteArray)) != -1) {
                            bos.write(byteArray, 0, cnt);
                            bos.flush();
                            fileSize -= cnt;
                        }
                        bos.close();
                        System.out.println("File " + fileName + " was saved");
                    }
                } else {
                    line = keyboard.readLine();
                    while (line.equals("\n") || line.isEmpty())
                        line = keyboard.readLine();
                    out.write(line + "\n");
                    out.flush();
                    if (line.equalsIgnoreCase("quit")) {
                        System.out.println("Closing the connection...");
                        break;
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Exception : " + e);
            e.printStackTrace();
        } finally {
            try {
                if (socket != null)
                    socket.close();
            } catch (IOException e) {
                System.out.println("Exception : " + e);
            }
        }
    }
}
