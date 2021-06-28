package licenta.voice;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class Main {
    private static ServerSocket serverSocket = null;
    private static Socket clientSocket = null;
    private static boolean initialized = false;
    private static Map<String, String> commands = new HashMap<>();
    private static float waitTime = 1;

    public static void main(String[] args) {
        try{
            System.out.println("Waiting for connections...");
            serverSocket = new ServerSocket(4447); // 4447 is port number
            clientSocket = serverSocket.accept(); // blocks and listen until a connection is made.
            System.out.println("Connected on port:" + clientSocket.getPort());
            while(!initialized){
                readConfig();
            }
            Start(commands);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        try {
            serverSocket.close();
            clientSocket.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void write(String message) throws IOException {
        try {
            BufferedWriter wr = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
            wr.write(message);
            wr.flush(); // flushes the stream
        }
        catch (Exception e) {
            e.printStackTrace();
            serverSocket.close();
            clientSocket.close();
        }
    }

    public static void readConfig() throws IOException {
        String inputLine = "";
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            if((inputLine = in.readLine()).contains("start")) {
                if((inputLine = in.readLine()) != null) {
                    waitTime = Float.parseFloat(inputLine.replace("listenTime=", ""));
                }
                while((inputLine = in.readLine()) != null){
                    String[] parts = inputLine.split("=");
                    commands.put(parts[0].toLowerCase(), parts[1]);
                }
                initialized = true;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            serverSocket.close();
            clientSocket.close();
        }
    }

    public static void Start(Map<String, String> commands) throws IOException {
        System.out.println("Recognition Started");
        BufferedWriter writer = new BufferedWriter(new FileWriter("src/licenta/voice/grammar/digits.gram"));
        writer.write("#JSGF V1.0;\n");
        writer.write("grammar voice;\n");
        String rule = "exit |";
        for (Map.Entry<String,String> entry : commands.entrySet()){
            rule += entry.getKey() + " | ";
        }
        if (rule != "") {
            rule = rule.substring(0, rule.length() - 3);
            writer.write("public <command> = (computer)(" + rule + ");\n");
        } else {
            writer.write("public <command> = (computer);\n");
        }

        writer.close();
        try {
            voce.SpeechInterface.init("src/licenta/voice/lib", false, true,
            "src/licenta/voice/grammar", "digits");
        }
        catch (Exception e) {
            e.printStackTrace();
            serverSocket.close();
            clientSocket.close();
        }

        while (true)
        {
            try
            {
                Thread.sleep((long)waitTime * 1000);
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
                serverSocket.close();
                clientSocket.close();
                break;
            }
            while (voce.SpeechInterface.getRecognizerQueueSize() > 0)
            {
                String s = voce.SpeechInterface.popRecognizedString();
                if(-1 != s.indexOf("computer")) {
                    String command = s.replace("computer ", "");
                    if(command.equals("exit"))
                    {
                        serverSocket.close();
                        clientSocket.close();
                        voce.SpeechInterface.destroy();
                        System.exit(0);
                    }
                    if(commands.containsKey(command)) {
                        Main.write(commands.get(command));
                    }
                }

                System.out.println("You said: " + s);
            }
        }
        voce.SpeechInterface.destroy();
        System.exit(0);
    }
}
