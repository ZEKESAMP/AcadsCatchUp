import java.sql.*;
public class Verify {
    public static void main(String[] a) throws Exception {
        Class.forName("org.sqlite.JDBC");
        try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + a[0]);
             Statement s = c.createStatement();
             ResultSet r = s.executeQuery("SELECT id,username,role FROM users ORDER BY id")) {
            System.out.println("ID | Username       | Role");
            System.out.println("---|----------------|----------");
            while (r.next()) System.out.printf("%-3d| %-15s| %s%n",
                r.getInt(1), r.getString(2), r.getString(3));
        }
    }
}
