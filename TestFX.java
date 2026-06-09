public class TestFX {
    public static void main(String[] args) {
        try {
            Class.forName("javafx.application.Application");
            System.out.println("JavaFX is available!");
        } catch (ClassNotFoundException e) {
            System.out.println("JavaFX is NOT available!");
        }
    }
}
