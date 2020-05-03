import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import org.json.simple.*;
import org.json.simple.parser.*;


/**
 * @author Dalton Kruppenbacher - Server Setup/Initial Commit
 * @author Connor Stevens - GUI
 * @author Brian Zhu
 * @author Everett Simone
 * @version 0.1
 * Revision Notes: Initial Commit (Server Setup)
 *
 *
 * ISTE 121.01 CPS:ID2
 * Final Project
 *
 * Class description: The CoronosServer is the Server of the Coronos Timeclock Solution. The server will host all of
 *                    the Employee objects and allow an Administrator to adjust settings of both the Server and the
 *                    Employees.
 *
 *                    The duplication of this code without written consent of the authors is strictly prohibited.
 *
 */
public class CoronosServer implements ActionListener {

    private JPanel serverInfo, encompassPanel, chatPanel, opsPanel, settingsPanel, chatBar;
    private JLabel serverAddress, hostLabel, portLabel, connectedUsers;
    private Vector<ObjectOutputStream> messageStreams = new Vector<>();
    private JButton b1, b2, b3, b4, b5;
    private Vector<String> users = new Vector<>();
    private final int PORT_NUMBER = 16789;
    private javax.swing.Timer shutdownTimer;
    private JMenu jmFile, jmHelp, jmServer;
    private JMenuBar jmMenuBar;
    private JMenuItem jmExit, jmAbout, jmShutdown;
    private ObjectOutputStream oos;
    private ObjectInputStream ois;
    private InetAddress localhost;
    private JTextField sendField;
    private JTextArea chatArea;
    private JFrame serverFrame;
    public int connected = 0;
    private ServerSocket ss;
    public Socket s;

    /**
     * Default Constructor of the CoronosServer class
     *      Creates the GUI
     *      Creates and starts a Thread
     * @since 0.1
     */
    public CoronosServer() {

        try{
            localhost = InetAddress.getLocalHost();
        }
        catch(UnknownHostException uhe){}

        uiBuilder();
        usersBuilder();
        
        try {
            //create a serversocket
            ss = new ServerSocket(PORT_NUMBER);
            while(true){
                s = ss.accept();
                InnerThread it = new InnerThread(s);
                //start the threaded object
                it.start();
                //create a InnerThread Object
            }
        } catch(BindException be) {
            System.err.println("EXCEPTION: CoronosServer BindException, " +
                    "something is running on port " + PORT_NUMBER);
        } catch(SocketException se) {
            se.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }//end try catch block
    }//end constructor

    public void actionPerformed(ActionEvent ae){
        String actionString = ae.getActionCommand();
        System.out.println(String.format("[ACTION] - %s", actionString));

        if(actionString.equals("Add Employee")){
            ArrayList<String> usernames = new ArrayList<>();
            for(String user:users){
                usernames.add(user.split(":")[0]);
            }

            JFrame addUser = new JFrame("Coronos - Add User");
            addUser.setLayout(new GridLayout(3, 1));
            JPanel topPanel = new JPanel(new FlowLayout());
            JPanel centerPanel = new JPanel(new FlowLayout());
            JPanel bottomPanel = new JPanel(new FlowLayout());

            JLabel usernameLabel = new JLabel("Username:");
            JTextField usernameField = new JTextField(10);
            JLabel passwordLabel = new JLabel("Password:");
            JTextField passwordField = new JTextField(10);
            JLabel wageLabel = new JLabel("Hourly Pay:");
            JTextField wageField = new JTextField(10);
            JLabel roleLabel = new JLabel("Role:");
            JTextField roleField = new JTextField(10);

            JButton saveButton = new JButton("Save Employee");

            saveButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    String actionString = ae.getActionCommand();
                    if(actionString.equals("Save Employee")){
                        String newUsername = usernameField.getText().toLowerCase();
                        String newPassword = passwordField.getText();
                        String role = roleField.getText();
                        double wage = Double.parseDouble(wageField.getText());
                        int newID = users.size();

                        if(usernames.contains(newUsername)){
                            JOptionPane.showMessageDialog(serverFrame, "User Already Exists", "User Registration Error", JOptionPane.ERROR_MESSAGE);
                        }
                        else if(newUsername.equals("")){
                            JOptionPane.showMessageDialog(serverFrame, "Username Cannot be null", "User Registration Error", JOptionPane.ERROR_MESSAGE);
                        }
                        else if(newPassword.length() < 8){
                            JOptionPane.showMessageDialog(serverFrame, "Password must be longer than 7 characters", "User Registration Error", JOptionPane.ERROR_MESSAGE);
                        }
                        else if(role.equals("")){
                            JOptionPane.showMessageDialog(serverFrame, "Role cannot be null.", "User Registration Error", JOptionPane.ERROR_MESSAGE);
                        }
                        else{
                            addUser(newUsername, newPassword, newID);
                            createRecord(newID, wage, role);
                            users.clear();
                            usersBuilder();
                            addUser.setVisible(false);
                        }
                    }
                }
            });

            topPanel.add(usernameLabel);
            topPanel.add(usernameField);
            topPanel.add(passwordLabel);
            topPanel.add(passwordField);

            centerPanel.add(wageLabel);
            centerPanel.add(wageField);
            centerPanel.add(roleLabel);
            centerPanel.add(roleField);

            bottomPanel.add(saveButton);

            addUser.add(topPanel);
            addUser.add(centerPanel);
            addUser.add(bottomPanel);
            addUser.setLocationRelativeTo(null);
            addUser.setVisible(true);
            addUser.pack();

        }

        if(actionString.equals("Disable Employee")){
            JFrame disableFrame = new JFrame("Disable Employee");
            disableFrame.setLayout(new GridLayout(2, 1));
            JPanel topPanel = new JPanel(new FlowLayout());
            JPanel bottomPanel = new JPanel(new FlowLayout());
            JLabel disableLabel = new JLabel("Disable User:");
            JTextField disableField = new JTextField(10);
            JButton disableButton = new JButton("Disable");

            disableButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    String actionString = ae.getActionCommand();
                    System.out.println(actionString);
                    String requested = disableField.getText().toLowerCase();
                    String name = null;
                    String id = null;
                    boolean success = false;
                    if(actionString.equals("Disable")) {
                        for (String user : users) {
                            name = user.split(":")[0];
                            id = user.split(":")[2];
                            //open file, delete them
                            success = requested.equals(name);
                            System.out.println(Boolean.toString(success));
                        }


                        if (success) {
                            try {
                                //TODO: custom dialog with icon
                                Reader reader = new FileReader(String.format(".\\data\\records\\%s.json", id));
                                JSONParser parser = new JSONParser();
                                JSONObject jsonObject = (JSONObject) parser.parse(reader);
                                reader.close();
                                System.out.println(jsonObject.get("active"));
                                jsonObject.put("active", false);
                                System.out.println(jsonObject.get("active"));
                                Writer writer = new FileWriter(String.format(".\\data\\records\\%s.json", id));
                                writer.write(jsonObject.toJSONString());
                                writer.close();
                                JOptionPane.showMessageDialog(null, String.format("%s has been disabled.", name));
                                disableFrame.setVisible(false);
                            } catch (FileNotFoundException fnfe) {
                                //TODO ERROR DIALOG
                                fnfe.printStackTrace();
                            } catch (IOException ioe) {
                                //TODO ERROR DIALOG
                                ioe.printStackTrace();
                            } catch (ParseException pe) {
                                //TODO ERROR DIALOG
                                pe.printStackTrace();
                            }

                        }
                        else {
                            //TODO: custom dialog with ICON
                            JOptionPane.showMessageDialog(serverFrame, "Could not find user record.", "Error Disabling Employee", JOptionPane.WARNING_MESSAGE);
                        }
                    }
                }
            });

            topPanel.add(disableLabel);
            topPanel.add(disableField);
            bottomPanel.add(disableButton);
            disableFrame.add(topPanel);
            disableFrame.add(bottomPanel);
            disableFrame.setVisible(true);
            disableFrame.pack();
            disableFrame.setLocationRelativeTo(null);
        }

        if(actionString.equals("Enable Employee")){
            JFrame enableFrame = new JFrame("Enable Employee");
            enableFrame.setLayout(new GridLayout(2, 1));
            JPanel topPanel = new JPanel(new FlowLayout());
            JPanel bottomPanel = new JPanel(new FlowLayout());
            JLabel enableLabel = new JLabel("Enable User:");
            JTextField enableField = new JTextField(10);
            JButton enableButton = new JButton("Enable");

            enableButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    String actionString = ae.getActionCommand();
                    System.out.println(actionString);
                    String requested = enableField.getText().toLowerCase();
                    String name = null;
                    String id = null;
                    boolean success = false;
                    if(actionString.equals("Enable")) {
                        for (String user : users) {
                            name = user.split(":")[0];
                            id = user.split(":")[2];
                            //open file, delete them
                            success = requested.equals(name);
                            System.out.println(Boolean.toString(success));
                        }


                        if (success) {
                            try {
                                //TODO: custom dialog with icon
                                Reader reader = new FileReader(String.format(".\\data\\records\\%s.json", id));
                                JSONParser parser = new JSONParser();
                                JSONObject jsonObject = (JSONObject) parser.parse(reader);
                                reader.close();
                                System.out.println(jsonObject.get("active"));
                                jsonObject.put("active", true);
                                System.out.println(jsonObject.get("active"));
                                Writer writer = new FileWriter(String.format(".\\data\\records\\%s.json", id));
                                writer.write(jsonObject.toJSONString());
                                writer.close();
                                JOptionPane.showMessageDialog(null, String.format("%s has been enabled.", name));
                                enableFrame.setVisible(false);
                            } catch (FileNotFoundException fnfe) {
                                //TODO ERROR DIALOG
                                fnfe.printStackTrace();
                            } catch (IOException ioe) {
                                //TODO ERROR DIALOG
                                ioe.printStackTrace();
                            } catch (ParseException pe) {
                                //TODO ERROR DIALOG
                                pe.printStackTrace();
                            }

                        }
                        else {
                            //TODO: custom dialog with ICON
                            JOptionPane.showMessageDialog(serverFrame, "Could not find user record.", "Error Enabling Employee", JOptionPane.WARNING_MESSAGE);
                        }
                    }
                }
            });

            topPanel.add(enableLabel);
            topPanel.add(enableField);
            bottomPanel.add(enableButton);
            enableFrame.add(topPanel);
            enableFrame.add(bottomPanel);
            enableFrame.setVisible(true);
            enableFrame.pack();
            enableFrame.setLocationRelativeTo(null);
        }

        if(actionString.equals("Generate Report")){
            System.out.println(String.format("[%s]", actionString));
        }

        if(actionString.equals("Reset Password")){
            System.out.println(String.format("[%s]", actionString));
        }

        if(actionString.equals("Edit Credentials")){
            System.out.println(String.format("[%s]", actionString));
        }

        if(actionString.equals("Wtf does this do")){
            System.out.println(String.format("[%s]", actionString));
        }

        if(actionString.equals("Shut Down")){

            System.out.println(String.format("[%s]", actionString));
            if(connected > 0){
                System.out.println(String.format("[SHUTDOWN] - [%d Users Connected] - Aborting Shutdown...", connected));
                JOptionPane.showMessageDialog(serverFrame, String.format("Cannot Shut Down, %d clients are connected. Please notify them of downtime before closing the server.", connected), "Shutdown Error", JOptionPane.WARNING_MESSAGE);
            }
            else{
                try{
                    System.err.printf("[SHUTDOWN] - [Timeout] - Waiting to Shut Down...\n", connected);
                    Thread.sleep(2000);
                    System.err.printf("[SHUTDOWN] - [Timer Complete] - Shutting Down...\n", connected);
                }
                catch(InterruptedException ie){}
                System.exit(0);
            }
        }

        if(actionString.equals("About")){
            System.out.println(String.format("[%s]", actionString));
        }

        if(actionString.equals("Save")){
            System.out.println(String.format("[%s]", actionString));
        }
        
        if(actionString.equals("Send")){
            System.out.println(String.format("[%s] - [ADMIN] - %s", actionString, sendField.getText()));
            Message msg = new Message(String.format("ADMIN: %s\n", sendField.getText()));
            sendField.setText("");
            chatArea.append(msg.toString());
            if(messageStreams.size() == 0){
                chatArea.append("***Server Message: No clients are connected. Are you lonely?\n");
            }
            for(ObjectOutputStream client : messageStreams){
                try{
                    client.writeObject(msg);
                    client.flush();
                }
                catch(IOException ioe) {
                    System.err.println("[CHAT] - [SEND] - Error sending chat.");
                }
            }
        }
    }

    public void uiBuilder(){
        jmMenuBar = new JMenuBar();
        //Instantiate JMenuBar

        jmFile = new JMenu("File");
        jmHelp = new JMenu("Help");
        jmServer = new JMenu("Server");
        //Adding Menus to JMenuBar

        jmExit = new JMenuItem("Exit");
        jmAbout = new JMenuItem("About");
        jmShutdown = new JMenuItem("Shut Down");
        jmShutdown.addActionListener(this);
        //Instantiating Options for JMenus

        jmFile.add(jmExit);
        jmHelp.add(jmAbout);
        jmServer.add(jmShutdown);
        //adding Options to JMenus

        jmMenuBar.add(jmFile);
        jmMenuBar.add(jmHelp);
        jmMenuBar.add(jmServer);
        //adding JMenus to JMenuBar

        b1 = new JButton("Add Employee");
        b1.addActionListener(this);
        b2 = new JButton("Enable Employee");
        b2.addActionListener(this);
        b3 = new JButton("Disable Employee");
        b3.addActionListener(this);
        b4 = new JButton("Generate Report");
        b4.addActionListener(this);
        b5 = new JButton("Reset Password");
        b5.addActionListener(this);

        serverFrame = new JFrame("Coronos Server");
        serverFrame.setJMenuBar(jmMenuBar);
        serverFrame.setLayout(new BorderLayout());

        serverInfo = new JPanel();
        serverInfo.setLayout(new FlowLayout());
        serverInfo.setBorder(BorderFactory.createTitledBorder("Information"));
        serverFrame.add(serverInfo, BorderLayout.NORTH);

        encompassPanel = new JPanel(new FlowLayout());

        opsPanel = new JPanel();
        opsPanel.setLayout(new GridLayout(5, 1, 15, 5));
        opsPanel.setBorder(BorderFactory.createTitledBorder("Operations"));

        opsPanel.add(b1);
        opsPanel.add(b2);
        opsPanel.add(b3);
        opsPanel.add(b4);
        opsPanel.add(b5);

        encompassPanel.add(opsPanel);

        chatPanel = new JPanel();
        chatPanel.setLayout(new FlowLayout());
        chatPanel.setBorder(BorderFactory.createTitledBorder("Chat"));
        chatArea = new JTextArea(9, 25);
        chatArea.setEditable(false);
        chatPanel.add(chatArea);
        encompassPanel.add(chatPanel);

        serverFrame.add(encompassPanel);

        hostLabel = new JLabel("IP Address:");
        serverInfo.add(hostLabel);

        serverAddress = new JLabel();
        serverAddress.setText(localhost.toString().split("/")[1]);
        serverInfo.add(serverAddress);

        portLabel = new JLabel((Integer.toString(PORT_NUMBER)));
        JLabel port = new JLabel("Port: ");

        connectedUsers = new JLabel(Integer.toString(connected));
        JLabel connectedLabel = new JLabel("Connected Users: ");
        serverInfo.add(port);
        serverInfo.add(portLabel);
        serverInfo.add(connectedLabel);
        serverInfo.add(connectedUsers);


        chatBar = new JPanel(new FlowLayout());
        JButton send = new JButton("Send");
        send.addActionListener(this);
        sendField = new JTextField(40);
        chatBar.add(send);
        chatBar.add(sendField);
        serverFrame.add(chatBar, BorderLayout.SOUTH);


        serverFrame.setSize(400, 100);
        serverFrame.setVisible(true);
        serverFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        //serverFrame.setResizable(false);
        serverFrame.pack();
        serverFrame.setLocationRelativeTo(null);
    }

    public void usersBuilder(){
        JSONParser parser = new JSONParser();
        
        try {
            Reader reader = new FileReader(".\\data\\users.json");
            JSONObject jsonObject = (JSONObject) parser.parse(reader);
            System.out.printf("[USERS] - [userBuilder] - %s\n", jsonObject);
            JSONArray storedUsers = (JSONArray) jsonObject.get("users");
            System.out.printf("[USERS] - [userBuilder] - Loaded %d users.\n", storedUsers.size());
            users.addAll(storedUsers);
            reader.close();
        }
        
        catch(FileNotFoundException fnfe){
            System.err.println("[ERROR] - [usersBuilder] - File Not Found");
        }

        catch(ParseException pe) {
            System.err.println("[ERROR] - [usersBuilder] - Exception occurred while parsing.");
        }

        catch(IOException ioe){
            System.err.println("[ERROR] - [usersBuilder] - IO Exception.");
        }
    }

    public void addUser(String username, String password, int id){
        JSONParser parser = new JSONParser();

        try {
            Reader reader = new FileReader(".\\data\\users.json");
            JSONObject jsonObject = (JSONObject) parser.parse(reader);
            JSONArray storedUsers = (JSONArray) jsonObject.get("users");
            System.out.printf("Loaded %d users.\n", storedUsers.size());
            String newUser = String.format("%s:%s:%d", username, password, id);
            storedUsers.add(newUser);
            JSONObject newJsonObject = new JSONObject();
            newJsonObject.put("users", storedUsers);
            Writer writer = new FileWriter(".\\data\\users.json");
            writer.write(newJsonObject.toJSONString());
            System.out.printf("Saved %d users.\n", storedUsers.size());
            reader.close();
            writer.close();
        }

        catch(FileNotFoundException fnfe){
            System.err.println("[ERROR] - [usersBuilder] - File Not Found.");
        }

        catch(ParseException pe) {
            System.err.println("[ERROR] - [usersBuilder] - Exception occurred while parsing.");
        }

        catch(IOException ioe){
            System.err.println("[ERROR] - [usersBuilder] - IO Exception.");
        }
    }

    public void createRecord(int id, double pay, String role){
        try{
            String filename = String.format(".\\data\\records\\%d.json", id);
            File file = new File(filename);
            Writer writer = new FileWriter(filename);
            JSONObject obj = new JSONObject();
            obj.put("active", true);
            obj.put("clockedIn", false);
            obj.put("wage", pay);
            obj.put("role", role);
            writer.write(obj.toJSONString());
            writer.close();
        }
        catch(IOException ioe) {
            System.err.println("[ERROR] - [createRecord] - IO Error");
        }
    }

    public static void main(String [] args){
        CoronosServer serv = new CoronosServer();
    }

    class InnerThread extends Thread {

        private Socket threadSocket;

        public InnerThread(Socket s) {
            threadSocket = s;
        }

        public void run() {
            connected++;
            connectedUsers.setText(Integer.toString(connected));
            try{
                oos = new ObjectOutputStream(threadSocket.getOutputStream());
                messageStreams.add(oos);
                ois = new ObjectInputStream(threadSocket.getInputStream());
            }
            catch(IOException ioe){
                System.err.println("[ERROR]: " + ioe);
            }
            while(true) {

                    try {

                            Object ob = (Object) ois.readObject();

                            if (ob instanceof CoronosAuth) {
                                CoronosAuth temp = (CoronosAuth) ob;
                                int userID = 0;
                                String username = temp.getUsername().toLowerCase();
                                String password = temp.getPassword();
                                String ipTotal = ss.getLocalSocketAddress().toString();
                                String ipS = ipTotal.split("/")[0];
                                Boolean enabled = false;
                                JSONObject userJson = null;
                                System.out.printf("[AUTH] - Attempted Login - %s\n[AUTH] - Username: %s\n[AUTH] - Password: %s\n", ipS, username, password);

                                for (String user : users) {
                                    String userName = user.split(":")[0];
                                    String passWord = user.split(":")[1];
                                    if (username.equalsIgnoreCase(userName)) {
                                        if (password.equals(passWord)) {
                                            String id = user.split(":")[2];
                                            userID = Integer.parseInt(id);
                                            JSONParser parser = new JSONParser();
                                            try{
                                                Reader reader = new FileReader(String.format(".\\data\\records\\%s.json", id));
                                                userJson = (JSONObject) parser.parse(reader);
                                                enabled = (Boolean) userJson.get("active");
                                            }
                                            catch(ParseException pe){
                                                System.err.println("[ERROR] - [AUTH] - Login Parse Error");
                                            }
                                            catch(IOException ioe){
                                                System.err.println("[ERROR] - [AUTH] - Login IO Error");
                                            }
                                            if(enabled){
                                                System.out.println("[AUTH] - Successful Login - Valid user");
                                                temp.setAllow(true);
                                            }
                                            else{
                                                System.out.println("[AUTH] - Failed Login - Deactivated user");
                                                temp.setAllow(false, "Account Deactivated");
                                            }
                                        }
                                        else {
                                            System.out.println("[AUTH] - Failed Login - Invalid Password");
                                            temp.setAllow(false, "Invalid Password");
                                        }
                                    }
                                    else {
                                        System.out.println("[AUTH] - Failed Login - User Not Found");
                                        temp.setAllow(false, "Invalid Username");
                                    }
                                }

                                oos.writeObject(temp);
                                oos.flush();
                                if(enabled){
                                    Employee out = new Employee(userJson);
                                    out.setId(userID);
                                    oos.writeObject((Object) out);
                                    oos.flush();
                                }
                            } else if (ob instanceof Message) {
                                Message temp = (Message) ob;
                                System.out.printf("[MESSAGE] - [Received] - %s\n", temp.toString());
                                chatArea.append(temp.toString());
                                for (ObjectOutputStream oos : messageStreams) {
                                    System.out.print("[MESSAGE] - Sent\n");
                                    oos.writeObject(temp);
                                    oos.flush();
                                }
                            } else if (ob instanceof Employee){
                                System.out.println("Got employee...");
                                System.out.println("Saving Employee...");
                                Employee temp = (Employee) ob;
                                int id = temp.getId();
                                Writer writer = new FileWriter(String.format(".\\data\\records\\%s.json", id));
                                String data = temp.getEmployeeData();
                                writer.write(data);
                                writer.flush();
                                writer.close();
                            }

                    }

                    catch(ClassNotFoundException cnfe) {
                        cnfe.printStackTrace();
                    }

                    catch(EOFException eofe){

                    }

                    catch(SocketException se){
                        break;
                    }

                    catch(IOException ioe) {
                    }
            }
            connected--;
            messageStreams.remove(oos);
            connectedUsers.setText(String.format("%d", connected));
            try{
                s.close();
            }
            catch(IOException ioe){
                ioe.printStackTrace();
            }

        }//end run()
    }//end InnerThread
}//end CoronosServer
