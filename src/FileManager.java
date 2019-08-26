import java.io.*;

class FileManager {
    void write(String filename, String text) {
        File file = new File(filename);
        try (PrintWriter out = new PrintWriter(file.getAbsoluteFile())) {
            out.print(text);
        }
        catch (IOException e) {
            System.out.println("Exception: " + e);
        }
    }

    void update(String filename, String newText) {
        StringBuilder sb = new StringBuilder();
        File file = new File(filename);
        try (BufferedReader in = new BufferedReader(new FileReader(file.getAbsoluteFile()))) {
            String s;
            while ((s = in.readLine()) != null) {
                sb.append(s).append('\n');
            }
        } catch (IOException e) {
            System.out.println("Exception: " + e);
        }
        String oldFile = sb.toString();
        sb.setLength(0);
        sb.append(oldFile);
        sb.append(newText);
        write(filename, sb.toString());
    }
}
