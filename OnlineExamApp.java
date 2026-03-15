import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Online Examination System
 *
 * Features:
 *  - Admin login
 *  - Add new student screen (Admin)
 *  - Student registration page
 *  - Student login
 *  - Online exam dashboard
 *  - Different exams for different users
 *  - Timer + auto-submit
 *  - Results stored in MySQL (XAMPP)
 *  - Admin can view all students' results
 */
public class OnlineExamApp extends JFrame {

    // ====== Simple Student Model (Encapsulation) ======
    static class Student {
        private String studentId;
        private String name;
        private String password;
        private int marks;

        public Student(String studentId, String name, String password) {
            this.studentId = studentId;
            this.name = name;
            this.password = password;
            this.marks = 0;
        }

        public String getStudentId() {
            return studentId;
        }

        public String getName() {
            return name;
        }

        public String getPassword() {
            return password;
        }

        public int getMarks() {
            return marks;
        }

        public void resetMarks() {
            this.marks = 0;
        }

        public void addMarks(int delta) {
            this.marks += delta;
        }
    }

    // ====== Abstract Question (Polymorphism base) ======
    static abstract class Question {
        private int questionId;
        private String questionText;

        public Question(int questionId, String questionText) {
            this.questionId = questionId;
            this.questionText = questionText;
        }

        public int getQuestionId() {
            return questionId;
        }

        public String getQuestionText() {
            return questionText;
        }

        public abstract int grade(String givenAnswer);
    }

    // ====== Multiple-Choice Question (Inheritance) ======
    static class MultipleChoiceQuestion extends Question {
        private String[] options; // A,B,C,D
        private int correctIndex; // 0..3

        public MultipleChoiceQuestion(int id, String text, String[] options, int correctIndex) {
            super(id, text);
            this.options = options;
            this.correctIndex = correctIndex;
        }

        public String[] getOptions() {
            return options;
        }

        @Override
        public int grade(String givenAnswer) {
            if (givenAnswer == null) return 0;
            int idx;
            switch (givenAnswer) {
                case "A": idx = 0; break;
                case "B": idx = 1; break;
                case "C": idx = 2; break;
                case "D": idx = 3; break;
                default: return 0;
            }
            return (idx == correctIndex) ? 1 : 0;
        }
    }

    // ====== Descriptive Question (Inheritance) ======
    static class DescriptiveQuestion extends Question {
        private String keyword;

        public DescriptiveQuestion(int id, String text, String keyword) {
            super(id, text);
            this.keyword = keyword;
        }

        @Override
        public int grade(String givenAnswer) {
            if (givenAnswer == null) return 0;
            if (givenAnswer.toLowerCase().contains(keyword.toLowerCase())) {
                return 1; // 1 mark
            }
            return 0;
        }
    }

    // ====== Exam Class ======
    static class Exam {
        private List<Question> questions = new ArrayList<>();

        public void addQuestion(Question q) {
            questions.add(q);
        }

        public List<Question> getQuestions() {
            return questions;
        }

        public int getTotalMarks() {
            return questions.size();
        }
    }

    // ====== JDBC Utility ======
    static class DBUtil {
        // Change DB details here if needed
        private static final String URL = "jdbc:mysql://localhost:3306/online_exam?useSSL=false&serverTimezone=UTC";
        private static final String USER = "root"; // XAMPP default
        private static final String PASSWORD = ""; // XAMPP default empty

        public static Connection getConnection() throws SQLException {
            return DriverManager.getConnection(URL, USER, PASSWORD);
        }

        // Insert new student (registration or admin add)
        public static void insertStudent(String id, String name, String password) throws SQLException {
            String sql = "INSERT INTO students (student_id, name, password) VALUES (?, ?, ?)";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, id);
                ps.setString(2, name);
                ps.setString(3, password);
                ps.executeUpdate();
            }
        }

        // Validate student login
        public static Student findStudent(String id, String password) throws SQLException {
            String sql = "SELECT student_id, name, password FROM students WHERE student_id = ? AND password = ?";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, id);
                ps.setString(2, password);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return new Student(
                                rs.getString("student_id"),
                                rs.getString("name"),
                                rs.getString("password")
                        );
                    } else {
                        return null;
                    }
                }
            }
        }

        // Save exam result
        public static void saveResult(Student s, String examName, int totalMarks) throws SQLException {
            String sql = "INSERT INTO results (student_id, student_name, exam_name, score, total) VALUES (?, ?, ?, ?, ?)";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, s.getStudentId());
                ps.setString(2, s.getName());
                ps.setString(3, examName);
                ps.setInt(4, s.getMarks());
                ps.setInt(5, totalMarks);
                ps.executeUpdate();
            }
        }

        // NEW: load all results for admin (as a table model)
        public static DefaultTableModel getAllResultsTableModel() {
            String[] cols = {"Student ID", "Name", "Exam", "Score", "Total", "Taken At"};
            DefaultTableModel model = new DefaultTableModel(cols, 0);

            String sql = "SELECT student_id, student_name, exam_name, score, total, taken_at " +
                    "FROM results ORDER BY taken_at DESC";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    Object[] row = new Object[]{
                            rs.getString("student_id"),
                            rs.getString("student_name"),
                            rs.getString("exam_name"),
                            rs.getInt("score"),
                            rs.getInt("total"),
                            rs.getTimestamp("taken_at")
                    };
                    model.addRow(row);
                }
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(null,
                        "Error loading results: " + e.getMessage(),
                        "DB Error",
                        JOptionPane.ERROR_MESSAGE);
            }
            return model;
        }
    }

    // ====== Main App Fields (Screens, Cards, State) ======
    private CardLayout cardLayout;
    private JPanel mainPanel;

    // Home
    private JPanel homePanel;

    // Admin login
    private JTextField txtAdminUser;
    private JPasswordField txtAdminPass;

    // Add student (Admin)
    private JTextField txtAddStudId;
    private JTextField txtAddStudName;
    private JPasswordField txtAddStudPass;

    // Student registration
    private JTextField txtRegStudId;
    private JTextField txtRegStudName;
    private JPasswordField txtRegStudPass;

    // Student login
    private JTextField txtLoginStudId;
    private JPasswordField txtLoginStudPass;

    // Student dashboard
    private JLabel lblDashWelcome;
    private JComboBox<String> cmbExamList;

    // Exam screen (question UI + timer)
    private JLabel lblExamTitle;
    private JLabel lblTimer;
    private JLabel lblQuestion;
    private JRadioButton[] optionButtons;
    private ButtonGroup optionGroup;
    private JTextArea txtDescriptive;
    private JButton btnNextQuestion;

    // Status
    private JLabel lblStatus;

    // State for current exam session
    private Student currentStudent;
    private Exam currentExam;
    private String currentExamName;
    private int currentQuestionIndex;
    private Timer examTimer;
    private int remainingSeconds;
    private boolean examInProgress = false;

    // ====== Constructor ======
    public OnlineExamApp() {
        setTitle("Online Examination System");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        createHomePanel();
        createAdminLoginPanel();
        createAdminMenuPanel();
        createAddStudentPanel();
        createStudentRegisterPanel();
        createStudentLoginPanel();
        createStudentDashboardPanel();
        createExamPanel();

        setContentPane(mainPanel);
        cardLayout.show(mainPanel, "HOME");
    }

    // ====== HOME PANEL (Entry) ======
    private void createHomePanel() {
        homePanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10,10,10,10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel lblTitle = new JLabel("Online Examination System", SwingConstants.CENTER);
        lblTitle.setFont(new Font("Arial", Font.BOLD, 24));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 1;
        homePanel.add(lblTitle, gbc);

        JButton btnAdminLogin = new JButton("Admin Login");
        gbc.gridy = 1;
        homePanel.add(btnAdminLogin, gbc);

        JButton btnStudentLogin = new JButton("Student Login");
        gbc.gridy = 2;
        homePanel.add(btnStudentLogin, gbc);

        JButton btnStudentRegister = new JButton("Student Registration");
        gbc.gridy = 3;
        homePanel.add(btnStudentRegister, gbc);

        JButton btnExit = new JButton("Exit");
        gbc.gridy = 4;
        homePanel.add(btnExit, gbc);

        // Events
        btnAdminLogin.addActionListener(e -> cardLayout.show(mainPanel, "ADMIN_LOGIN"));
        btnStudentLogin.addActionListener(e -> cardLayout.show(mainPanel, "STUDENT_LOGIN"));
        btnStudentRegister.addActionListener(e -> cardLayout.show(mainPanel, "STUDENT_REGISTER"));
        btnExit.addActionListener(e -> System.exit(0));

        mainPanel.add(homePanel, "HOME");
    }

    // ====== ADMIN LOGIN PANEL ======
    private void createAdminLoginPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5,5,5,5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel lblTitle = new JLabel("Admin Login", SwingConstants.CENTER);
        lblTitle.setFont(new Font("Arial", Font.BOLD, 20));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        panel.add(lblTitle, gbc);

        gbc.gridwidth = 1;

        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Username:"), gbc);
        txtAdminUser = new JTextField(15);
        gbc.gridx = 1;
        panel.add(txtAdminUser, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Password:"), gbc);
        txtAdminPass = new JPasswordField(15);
        gbc.gridx = 1;
        panel.add(txtAdminPass, gbc);

        JButton btnLogin = new JButton("Login");
        JButton btnBack = new JButton("Back");

        gbc.gridx = 0; gbc.gridy = 3;
        panel.add(btnLogin, gbc);
        gbc.gridx = 1;
        panel.add(btnBack, gbc);

        // Hardcoded admin credentials
        final String ADMIN_USER = "admin";
        final String ADMIN_PASS = "admin123";

        btnLogin.addActionListener(e -> {
            String u = txtAdminUser.getText().trim();
            String p = new String(txtAdminPass.getPassword());
            if (u.equals(ADMIN_USER) && p.equals(ADMIN_PASS)) {
                JOptionPane.showMessageDialog(this, "Admin login successful!");
                cardLayout.show(mainPanel, "ADMIN_MENU");
            } else {
                JOptionPane.showMessageDialog(this, "Invalid admin credentials.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnBack.addActionListener(e -> cardLayout.show(mainPanel, "HOME"));

        mainPanel.add(panel, "ADMIN_LOGIN");
    }

    // ====== ADMIN MENU (Dashboard) ======
    private void createAdminMenuPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10,10,10,10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel lblTitle = new JLabel("Admin Dashboard", SwingConstants.CENTER);
        lblTitle.setFont(new Font("Arial", Font.BOLD, 22));
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(lblTitle, gbc);

        JButton btnAddStudent = new JButton("Add New Student");
        gbc.gridy = 1;
        panel.add(btnAddStudent, gbc);

        // NEW: View all results button
        JButton btnViewResults = new JButton("View All Results");
        gbc.gridy = 2;
        panel.add(btnViewResults, gbc);

        JButton btnBackHome = new JButton("Back to Home");
        gbc.gridy = 3;
        panel.add(btnBackHome, gbc);

        btnAddStudent.addActionListener(e -> cardLayout.show(mainPanel, "ADD_STUDENT"));
        btnBackHome.addActionListener(e -> cardLayout.show(mainPanel, "HOME"));

        // Open AdminResultsFrame
        btnViewResults.addActionListener(e -> {
            AdminResultsFrame resultsFrame = new AdminResultsFrame();
            resultsFrame.setVisible(true);
        });

        mainPanel.add(panel, "ADMIN_MENU");
    }

    // ====== ADD STUDENT PANEL (Admin) ======
    private void createAddStudentPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5,5,5,5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel lblTitle = new JLabel("Add New Student (Admin)", SwingConstants.CENTER);
        lblTitle.setFont(new Font("Arial", Font.BOLD, 18));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        panel.add(lblTitle, gbc);

        gbc.gridwidth = 1;

        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Student ID:"), gbc);
        txtAddStudId = new JTextField(15);
        gbc.gridx = 1;
        panel.add(txtAddStudId, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Name:"), gbc);
        txtAddStudName = new JTextField(15);
        gbc.gridx = 1;
        panel.add(txtAddStudName, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        panel.add(new JLabel("Password:"), gbc);
        txtAddStudPass = new JPasswordField(15);
        gbc.gridx = 1;
        panel.add(txtAddStudPass, gbc);

        JButton btnSave = new JButton("Save");
        JButton btnBack = new JButton("Back");

        gbc.gridx = 0; gbc.gridy = 4;
        panel.add(btnSave, gbc);
        gbc.gridx = 1;
        panel.add(btnBack, gbc);

        btnSave.addActionListener(e -> {
            String id = txtAddStudId.getText().trim();
            String name = txtAddStudName.getText().trim();
            String pass = new String(txtAddStudPass.getPassword());

            if (id.isEmpty() || name.isEmpty() || pass.isEmpty()) {
                JOptionPane.showMessageDialog(this, "All fields are required.",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                DBUtil.insertStudent(id, name, pass);
                JOptionPane.showMessageDialog(this, "Student added successfully.");
                txtAddStudId.setText("");
                txtAddStudName.setText("");
                txtAddStudPass.setText("");
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error adding student: " + ex.getMessage(),
                        "DB Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnBack.addActionListener(e -> cardLayout.show(mainPanel, "ADMIN_MENU"));

        mainPanel.add(panel, "ADD_STUDENT");
    }

    // ====== STUDENT REGISTRATION PANEL ======
    private void createStudentRegisterPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5,5,5,5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel lblTitle = new JLabel("Student Registration", SwingConstants.CENTER);
        lblTitle.setFont(new Font("Arial", Font.BOLD, 18));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        panel.add(lblTitle, gbc);

        gbc.gridwidth = 1;

        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Student ID:"), gbc);
        txtRegStudId = new JTextField(15);
        gbc.gridx = 1;
        panel.add(txtRegStudId, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Name:"), gbc);
        txtRegStudName = new JTextField(15);
        gbc.gridx = 1;
        panel.add(txtRegStudName, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        panel.add(new JLabel("Password:"), gbc);
        txtRegStudPass = new JPasswordField(15);
        gbc.gridx = 1;
        panel.add(txtRegStudPass, gbc);

        JButton btnRegister = new JButton("Register");
        JButton btnBack = new JButton("Back");

        gbc.gridx = 0; gbc.gridy = 4;
        panel.add(btnRegister, gbc);
        gbc.gridx = 1;
        panel.add(btnBack, gbc);

        btnRegister.addActionListener(e -> {
            String id = txtRegStudId.getText().trim();
            String name = txtRegStudName.getText().trim();
            String pass = new String(txtRegStudPass.getPassword());

            if (id.isEmpty() || name.isEmpty() || pass.isEmpty()) {
                JOptionPane.showMessageDialog(this, "All fields are required.",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                DBUtil.insertStudent(id, name, pass);
                JOptionPane.showMessageDialog(this, "Registration successful. You can now login.");
                txtRegStudId.setText("");
                txtRegStudName.setText("");
                txtRegStudPass.setText("");
                cardLayout.show(mainPanel, "STUDENT_LOGIN");
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error registering student: " + ex.getMessage(),
                        "DB Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnBack.addActionListener(e -> cardLayout.show(mainPanel, "HOME"));

        mainPanel.add(panel, "STUDENT_REGISTER");
    }

    // ====== STUDENT LOGIN PANEL ======
    private void createStudentLoginPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5,5,5,5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel lblTitle = new JLabel("Student Login", SwingConstants.CENTER);
        lblTitle.setFont(new Font("Arial", Font.BOLD, 18));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        panel.add(lblTitle, gbc);

        gbc.gridwidth = 1;

        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Student ID:"), gbc);
        txtLoginStudId = new JTextField(15);
        gbc.gridx = 1;
        panel.add(txtLoginStudId, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Password:"), gbc);
        txtLoginStudPass = new JPasswordField(15);
        gbc.gridx = 1;
        panel.add(txtLoginStudPass, gbc);

        JButton btnLogin = new JButton("Login");
        JButton btnBack = new JButton("Back");

        gbc.gridx = 0; gbc.gridy = 3;
        panel.add(btnLogin, gbc);
        gbc.gridx = 1;
        panel.add(btnBack, gbc);

        btnLogin.addActionListener(e -> {
            String id = txtLoginStudId.getText().trim();
            String pass = new String(txtLoginStudPass.getPassword());

            if (id.isEmpty() || pass.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter ID and password.",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                Student s = DBUtil.findStudent(id, pass);
                if (s == null) {
                    JOptionPane.showMessageDialog(this, "Invalid student credentials.",
                            "Login Failed", JOptionPane.ERROR_MESSAGE);
                } else {
                    currentStudent = s;
                    currentStudent.resetMarks();
                    lblDashWelcome.setText("Welcome, " + currentStudent.getName() +
                            " (" + currentStudent.getStudentId() + ")");
                    cardLayout.show(mainPanel, "STUDENT_DASH");
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error during login: " + ex.getMessage(),
                        "DB Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnBack.addActionListener(e -> cardLayout.show(mainPanel, "HOME"));

        mainPanel.add(panel, "STUDENT_LOGIN");
    }

    // ====== STUDENT DASHBOARD (Exam selection) ======
    private void createStudentDashboardPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10,10,10,10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        lblDashWelcome = new JLabel("Welcome, Student", SwingConstants.CENTER);
        lblDashWelcome.setFont(new Font("Arial", Font.BOLD, 18));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        panel.add(lblDashWelcome, gbc);

        gbc.gridwidth = 1;

        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Choose Exam:"), gbc);

        // Different exams for different users
        cmbExamList = new JComboBox<>(new String[]{
                "Java Basics",
                "OS Basics"
        });
        gbc.gridx = 1;
        panel.add(cmbExamList, gbc);

        JButton btnStartExam = new JButton("Start Exam");
        JButton btnLogout = new JButton("Logout");

        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(btnStartExam, gbc);
        gbc.gridx = 1;
        panel.add(btnLogout, gbc);

        btnStartExam.addActionListener(e -> {
            if (currentStudent == null) {
                JOptionPane.showMessageDialog(this, "Please login first.",
                        "Error", JOptionPane.ERROR_MESSAGE);
                cardLayout.show(mainPanel, "STUDENT_LOGIN");
                return;
            }
            String examName = (String) cmbExamList.getSelectedItem();
            startExamForStudent(examName);
        });

        btnLogout.addActionListener(e -> {
            currentStudent = null;
            cardLayout.show(mainPanel, "HOME");
        });

        mainPanel.add(panel, "STUDENT_DASH");
    }

    // ====== EXAM PANEL (with timer + auto-submit) ======
    private void createExamPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // Top: Exam title + Timer
        JPanel topPanel = new JPanel(new BorderLayout());
        lblExamTitle = new JLabel("Exam", SwingConstants.CENTER);
        lblExamTitle.setFont(new Font("Arial", Font.BOLD, 18));
        topPanel.add(lblExamTitle, BorderLayout.CENTER);

        lblTimer = new JLabel("Time: 00:00", SwingConstants.RIGHT);
        lblTimer.setFont(new Font("Monospaced", Font.BOLD, 16));
        lblTimer.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 10));
        topPanel.add(lblTimer, BorderLayout.EAST);

        panel.add(topPanel, BorderLayout.NORTH);

        // Center: Question + Options / Descriptive
        JPanel centerPanel = new JPanel(new BorderLayout());
        lblQuestion = new JLabel("Question will appear here");
        lblQuestion.setFont(new Font("Arial", Font.PLAIN, 16));
        lblQuestion.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        centerPanel.add(lblQuestion, BorderLayout.NORTH);

        JPanel optionsPanel = new JPanel(new GridLayout(4, 1));
        optionButtons = new JRadioButton[4];
        optionGroup = new ButtonGroup();
        for (int i = 0; i < 4; i++) {
            optionButtons[i] = new JRadioButton("Option " + (i + 1));
            optionGroup.add(optionButtons[i]);
            optionsPanel.add(optionButtons[i]);
        }
        centerPanel.add(optionsPanel, BorderLayout.CENTER);

        txtDescriptive = new JTextArea(5, 40);
        txtDescriptive.setLineWrap(true);
        txtDescriptive.setWrapStyleWord(true);
        JScrollPane scroll = new JScrollPane(txtDescriptive);
        centerPanel.add(scroll, BorderLayout.SOUTH);

        panel.add(centerPanel, BorderLayout.CENTER);

        // Bottom: Next/Submit + status
        JPanel bottomPanel = new JPanel(new BorderLayout());
        btnNextQuestion = new JButton("Next");
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnCancelExam = new JButton("Cancel Exam");
        btnPanel.add(btnCancelExam);
        btnPanel.add(btnNextQuestion);
        bottomPanel.add(btnPanel, BorderLayout.EAST);

        lblStatus = new JLabel("Status: Waiting to start.");
        bottomPanel.add(lblStatus, BorderLayout.WEST);

        panel.add(bottomPanel, BorderLayout.SOUTH);

        // Events
        btnNextQuestion.addActionListener(e -> handleNextQuestion(true));

        btnCancelExam.addActionListener(e -> {
            if (examInProgress) {
                int choice = JOptionPane.showConfirmDialog(
                        this,
                        "Are you sure you want to cancel the exam? Your answers will not be saved.",
                        "Cancel Exam",
                        JOptionPane.YES_NO_OPTION
                );
                if (choice == JOptionPane.YES_OPTION) {
                    stopExamTimer();
                    examInProgress = false;
                    currentStudent.resetMarks();
                    JOptionPane.showMessageDialog(this, "Exam cancelled.");
                    cardLayout.show(mainPanel, "STUDENT_DASH");
                }
            } else {
                cardLayout.show(mainPanel, "STUDENT_DASH");
            }
        });

        mainPanel.add(panel, "EXAM");
    }

    // ====== Start Exam for the Logged-in Student ======
    private void startExamForStudent(String examName) {
        currentExamName = examName;
        currentExam = new Exam();

        // Different question sets for each exam
        if ("Java Basics".equals(examName)) {
            lblExamTitle.setText("Java Basics Exam");

            currentExam.addQuestion(new MultipleChoiceQuestion(
                    1,
                    "Which keyword is used to inherit a class in Java?",
                    new String[]{"implement", "extends", "inherits", "super"},
                    1
            ));

            currentExam.addQuestion(new MultipleChoiceQuestion(
                    2,
                    "Which of these is NOT an OOP concept?",
                    new String[]{"Encapsulation", "Polymorphism", "Recursion", "Inheritance"},
                    2
            ));

            currentExam.addQuestion(new MultipleChoiceQuestion(
                    3,
                    "Which JDBC class is used to execute SQL queries?",
                    new String[]{"Statement", "Scanner", "Executor", "Runner"},
                    0
            ));

            currentExam.addQuestion(new DescriptiveQuestion(
                    4,
                    "Explain briefly what is encapsulation in Java.",
                    "data"
            ));

        } else if ("OS Basics".equals(examName)) {
            lblExamTitle.setText("Operating Systems Basics Exam");

            currentExam.addQuestion(new MultipleChoiceQuestion(
                    1,
                    "What is the main function of an operating system?",
                    new String[]{"To manage hardware resources", "To compile code", "To browse the internet", "To run antivirus"},
                    0
            ));

            currentExam.addQuestion(new MultipleChoiceQuestion(
                    2,
                    "Which of the following is a type of OS scheduling algorithm?",
                    new String[]{"Round Robin", "Bubble Sort", "Binary Search", "Quick Sort"},
                    0
            ));

            currentExam.addQuestion(new MultipleChoiceQuestion(
                   3,
                    "What is virtual memory?",
                    new String[]{"Physical memory", "Secondary storage", "Emulation of RAM using disk space", "Cache memory"},
                    2
            ));

            currentExam.addQuestion(new DescriptiveQuestion(
                    4,
                    "Explain briefly what a deadlock is in operating systems.",
                    "deadlock"
            ));
        }

        currentQuestionIndex = 0;
        currentStudent.resetMarks();
        examInProgress = true;

        // Timer: e.g., 5 minutes = 300 seconds (change if you want)
        remainingSeconds = 300;
        startExamTimer();

        showQuestionOnUI();

        cardLayout.show(mainPanel, "EXAM");
    }

    // ====== Show Question on UI ======
    private void showQuestionOnUI() {
        Question q = currentExam.getQuestions().get(currentQuestionIndex);
        lblQuestion.setText("Q" + (currentQuestionIndex + 1) + ": " + q.getQuestionText());

        optionGroup.clearSelection();
        txtDescriptive.setText("");

        if (q instanceof MultipleChoiceQuestion) {
            MultipleChoiceQuestion mcq = (MultipleChoiceQuestion) q;
            String[] opts = mcq.getOptions();

            for (int i = 0; i < 4; i++) {
                optionButtons[i].setVisible(true);
                optionButtons[i].setText((char) ('A' + i) + ". " + opts[i]);
            }
            txtDescriptive.setVisible(false);

        } else if (q instanceof DescriptiveQuestion) {
            for (int i = 0; i < 4; i++) {
                optionButtons[i].setVisible(false);
            }
            txtDescriptive.setVisible(true);
        }

        if (currentQuestionIndex == currentExam.getQuestions().size() - 1) {
            btnNextQuestion.setText("Submit");
        } else {
            btnNextQuestion.setText("Next");
        }

        lblStatus.setText("Status: Question " + (currentQuestionIndex + 1) +
                " of " + currentExam.getQuestions().size());
    }

    // ====== Timer Handling (auto-submit when time over) ======
    private void startExamTimer() {
        stopExamTimer(); // stop any previous timer

        examTimer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                remainingSeconds--;
                updateTimerLabel();
                if (remainingSeconds <= 0) {
                    // Time over -> auto-submit
                    stopExamTimer();
                    JOptionPane.showMessageDialog(
                            OnlineExamApp.this,
                            "Time is over! Your exam will be submitted automatically.",
                            "Time Up",
                            JOptionPane.INFORMATION_MESSAGE
                    );
                    handleNextQuestion(false, true); // auto submit
                }
            }
        });
        examTimer.start();
        updateTimerLabel();
    }

    private void stopExamTimer() {
        if (examTimer != null) {
            examTimer.stop();
            examTimer = null;
        }
    }

    private void updateTimerLabel() {
        int minutes = remainingSeconds / 60;
        int seconds = remainingSeconds % 60;
        lblTimer.setText(String.format("Time: %02d:%02d", minutes, seconds));
    }

    // Overload for normal click
    private void handleNextQuestion(boolean requireAnswer) {
        handleNextQuestion(requireAnswer, false);
    }

    // ====== Next / Submit Logic ======
    private void handleNextQuestion(boolean requireAnswer, boolean autoSubmit) {
        if (!examInProgress) {
            return;
        }

        Question q = currentExam.getQuestions().get(currentQuestionIndex);
        String givenAnswer = null;

        if (q instanceof MultipleChoiceQuestion) {
            int selectedIndex = -1;
            for (int i = 0; i < 4; i++) {
                if (optionButtons[i].isVisible() && optionButtons[i].isSelected()) {
                    selectedIndex = i;
                    break;
                }
            }
            if (selectedIndex == -1) {
                if (requireAnswer) {
                    JOptionPane.showMessageDialog(this,
                            "Please select an option.",
                            "Answer Required",
                            JOptionPane.WARNING_MESSAGE);
                    return;
                } else {
                    givenAnswer = null; // no answer
                }
            } else {
                givenAnswer = String.valueOf((char) ('A' + selectedIndex));
            }

        } else {
            givenAnswer = txtDescriptive.getText().trim();
            if (givenAnswer.isEmpty() && requireAnswer) {
                JOptionPane.showMessageDialog(this,
                        "Please enter your answer.",
                        "Answer Required",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        int marks = q.grade(givenAnswer);
        currentStudent.addMarks(marks);

        if (currentQuestionIndex < currentExam.getQuestions().size() - 1 && !autoSubmit) {
            currentQuestionIndex++;
            showQuestionOnUI();
        } else {
            finishExam();
        }
    }

    // ====== Finish Exam: Save Result + Allow next users ======
    private void finishExam() {
        stopExamTimer();
        examInProgress = false;

        int totalMarks = currentExam.getTotalMarks();
        int score = currentStudent.getMarks();

        try {
            DBUtil.saveResult(currentStudent, currentExamName, totalMarks);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                    "Error saving result: " + ex.getMessage(),
                    "DB Error",
                    JOptionPane.ERROR_MESSAGE);
        }

        JOptionPane.showMessageDialog(this,
                "Exam Finished!\nStudent: " + currentStudent.getName() +
                        "\nExam: " + currentExamName +
                        "\nScore: " + score + " / " + totalMarks,
                "Result",
                JOptionPane.INFORMATION_MESSAGE);

        // Back to dashboard so *multiple users* can continue
        cardLayout.show(mainPanel, "STUDENT_DASH");
    }

    // ====== ADMIN RESULTS FRAME (NEW) ======
    private static class AdminResultsFrame extends JFrame {
        private JTable table;

        public AdminResultsFrame() {
            setTitle("Admin - All Students Results");
            setSize(900, 450);
            setLocationRelativeTo(null);
            setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            setLayout(new BorderLayout());

            JLabel lbl = new JLabel("All Students Results", SwingConstants.CENTER);
            lbl.setFont(new Font("Arial", Font.BOLD, 20));
            lbl.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            add(lbl, BorderLayout.NORTH);

            table = new JTable(DBUtil.getAllResultsTableModel());
            add(new JScrollPane(table), BorderLayout.CENTER);

            JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton btnRefresh = new JButton("Refresh");
            JButton btnClose = new JButton("Close");
            bottom.add(btnClose);
            bottom.add(btnRefresh);
            add(bottom, BorderLayout.SOUTH);

            btnRefresh.addActionListener(e ->
                    table.setModel(DBUtil.getAllResultsTableModel()));
            btnClose.addActionListener(e -> dispose());
        }
    }

    // ====== main() ======
    public static void main(String[] args) {
        // Load JDBC driver (Connector/J)
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.out.println("MySQL JDBC Driver not found. Make sure the JAR is in the classpath.");
        }

        SwingUtilities.invokeLater(() -> {
            OnlineExamApp app = new OnlineExamApp();
            app.setVisible(true);
        });
    }
}
